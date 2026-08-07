package shipeditor.utility.graphics;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Generates collision bounds for Starsector {@code .ship} file export.
 * <p>
 * Uses a pure Java approach:
 * <ol>
 *   <li>BFS Blob Detection to isolate the main ship body.</li>
 *   <li>Morphological dilation to bridge transparent gaps in split hulls.</li>
 *   <li>Moore-Neighbor Tracing to extract the boundary contour.</li>
 *   <li>Ramer-Douglas-Peucker algorithm to simplify the vertex count.</li>
 *   <li>Polygon insetting to tighten the hull to the visible sprite edge.</li>
 * </ol>
 * <p>
 * <b>Not for UI highlighting.</b> For visual selection outlines, use {@link SpriteOutlineTracer} instead.
 *
 * @see SpriteOutlineTracer
 */
public final class CollisionHullGenerator {

    private static final int ALPHA_THRESHOLD = 10;
    private static final double SIMPLIFICATION_EPSILON = 2.0;

    private CollisionHullGenerator() {
    }

    /**
     * Generates a concave hull of world coordinates based on the opaque pixels of the given image.
     * 
     * @param image The sprite image.
     * @param anchor The top-left anchor point of the sprite in canvas coordinates.
     * @return A list of Point2D representing the vertices of the concave hull in canvas coordinates.
     */
    public static List<Point2D> generateBounds(BufferedImage image, Point2D anchor) {
        if (image == null) return Collections.emptyList();

        int width = image.getWidth();
        int height = image.getHeight();

        // 1. Dilate opaque pixels to bridge any transparent gaps (e.g. split hulls)
        Set<Point> dilatedPixels = getDilatedOpaquePixels(image, width, height, 3);
        if (dilatedPixels.isEmpty()) return Collections.emptyList();

        // 2. Find the blob closest to the center of the sprite
        Set<Point> targetBlob = findCenterBlob(dilatedPixels, width, height);
        if (targetBlob.isEmpty()) return Collections.emptyList();

        // 3. Trace boundary using Moore-Neighbor
        List<Point> contour = traceBoundary(targetBlob, width, height);
        if (contour.isEmpty()) return Collections.emptyList();

        // 4. Simplify with RDP
        List<Point2D> simplified = simplifyPolygon(contour, SIMPLIFICATION_EPSILON);

        // 5. Inset bounds to undo dilation and trim by an extra ~5-10% as requested
        List<Point2D> insetPoints = insetPolygon(simplified, 5.0);

        // Convert pixel coordinates (top-left origin) to canvas coordinates
        List<Point2D> canvasBounds = new ArrayList<>();
        double anchorX = anchor != null ? anchor.getX() : 0.0;
        double anchorY = anchor != null ? anchor.getY() : 0.0;
        for (Point2D p : insetPoints) {
            double canvasX = anchorX + p.getX();
            double canvasY = anchorY + p.getY();
            canvasBounds.add(new Point2D.Double(canvasX, canvasY));
        }
        return canvasBounds;
    }


    private static Set<Point> getDilatedOpaquePixels(BufferedImage image, int width, int height, int radius) {
        Set<Point> opaquePixels = new HashSet<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isOpaque(image, x, y)) {
                    opaquePixels.add(new Point(x, y));
                }
            }
        }
        
        Set<Point> dilated = new HashSet<>(opaquePixels);
        for (Point p : opaquePixels) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (dx*dx + dy*dy <= radius*radius) { // Circular dilation
                        int nx = p.x + dx;
                        int ny = p.y + dy;
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            dilated.add(new Point(nx, ny));
                        }
                    }
                }
            }
        }
        return dilated;
    }

    private static Set<Point> findCenterBlob(Set<Point> pixels, int width, int height) {
        Point center = new Point(width / 2, height / 2);
        Point nearest = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (Point p : pixels) {
            double distSq = Math.pow(p.x - center.x, 2) + Math.pow(p.y - center.y, 2);
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                nearest = p;
            }
        }

        if (nearest == null) return new HashSet<>();

        boolean[][] visited = new boolean[width][height];
        Set<Point> blob = new HashSet<>();
        Queue<Point> queue = new LinkedList<>();
        
        queue.add(nearest);
        blob.add(nearest);
        visited[nearest.x][nearest.y] = true;

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            
            // 8-way connectivity for ships to avoid gaps in thin diagonal structures
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = p.x + dx;
                    int ny = p.y + dy;
                    
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height && !visited[nx][ny]) {
                        Point neighbor = new Point(nx, ny);
                        if (pixels.contains(neighbor)) {
                            visited[nx][ny] = true;
                            queue.add(neighbor);
                            blob.add(neighbor);
                        }
                    }
                }
            }
        }

        return blob;
    }

    private static boolean isOpaque(BufferedImage image, int x, int y) {
        int argb = image.getRGB(x, y);
        int alpha = (argb >> 24) & 0xff;
        return alpha > ALPHA_THRESHOLD;
    }

    private static List<Point> traceBoundary(Set<Point> blob, int width, int height) {
        if (blob.isEmpty()) return Collections.emptyList();

        // Find starting pixel (top-leftmost)
        Point start = null;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point p = new Point(x, y);
                if (blob.contains(p)) {
                    start = p;
                    break;
                }
            }
            if (start != null) break;
        }

        if (start == null) return Collections.emptyList();

        List<Point> contour = new ArrayList<>();
        
        // Directions: Clockwise from N
        int[] dx = { 0, 1, 1, 1, 0, -1, -1, -1 };
        int[] dy = { -1, -1, 0, 1, 1, 1, 0, -1 };
        
        Point current = start;
        int enterDir = 6; // West, since start is top-leftmost

        Point second = null;
        
        while (true) {
            contour.add(current);
            boolean found = false;
            
            int checkDir = enterDir;
            
            for (int i = 0; i < 8; i++) {
                Point neighbor = new Point(current.x + dx[checkDir], current.y + dy[checkDir]);
                
                if (blob.contains(neighbor)) {
                    current = neighbor;
                    enterDir = (checkDir + 5) % 8; // Start searching next from relative "behind-left"
                    found = true;
                    break;
                }
                checkDir = (checkDir + 1) % 8;
            }
            
            if (!found) {
                break;
            }
            
            if (second == null) {
                second = current;
            } else if (contour.size() > 1 && contour.get(contour.size() - 1).equals(start) && current.equals(second)) {
                contour.remove(contour.size() - 1);
                break; // Jacob's stopping criterion met
            }
            
            if (contour.size() > width * height * 2) break; // Infinite loop safety
        }

        return contour;
    }

    private static List<Point2D> simplifyPolygon(List<Point> points, double epsilon) {
        if (points.size() < 3) {
            List<Point2D> res = new ArrayList<>();
            for (Point p : points) res.add(new Point2D.Double(p.x, p.y));
            return res;
        }

        double maxDistance = 0.0;
        int index = 0;
        int end = points.size() - 1;

        for (int i = 1; i < end; i++) {
            double distance = perpendicularDistance(points.get(i), points.get(0), points.get(end));
            if (distance > maxDistance) {
                index = i;
                maxDistance = distance;
            }
        }

        List<Point2D> result = new ArrayList<>();
        if (maxDistance > epsilon) {
            List<Point> firstLine = points.subList(0, index + 1);
            List<Point> secondLine = points.subList(index, end + 1);
            
            List<Point2D> firstResult = simplifyPolygon(firstLine, epsilon);
            List<Point2D> secondResult = simplifyPolygon(secondLine, epsilon);
            
            firstResult.remove(firstResult.size() - 1);
            result.addAll(firstResult);
            result.addAll(secondResult);
        } else {
            result.add(new Point2D.Double(points.get(0).x, points.get(0).y));
            result.add(new Point2D.Double(points.get(end).x, points.get(end).y));
        }

        return result;
    }

    private static double perpendicularDistance(Point pt, Point lineStart, Point lineEnd) {
        double dx = lineEnd.x - lineStart.x;
        double dy = lineEnd.y - lineStart.y;
        
        if (dx == 0 && dy == 0) {
            return Math.hypot(pt.x - lineStart.x, pt.y - lineStart.y);
        }
        
        double t = ((pt.x - lineStart.x) * dx + (pt.y - lineStart.y) * dy) / (dx * dx + dy * dy);
        
        if (t < 0) {
            return Math.hypot(pt.x - lineStart.x, pt.y - lineStart.y);
        } else if (t > 1) {
            return Math.hypot(pt.x - lineEnd.x, pt.y - lineEnd.y);
        }
        
        double closestX = lineStart.x + t * dx;
        double closestY = lineStart.y + t * dy;
        
        return Math.hypot(pt.x - closestX, pt.y - closestY);
    }

    private static List<Point2D> insetPolygon(List<Point2D> poly, double insetAmount) {
        if (poly.size() < 3) return poly;
        List<Point2D> inset = new ArrayList<>();
        int n = poly.size();
        for (int i = 0; i < n; i++) {
            Point2D prev = poly.get((i - 1 + n) % n);
            Point2D curr = poly.get(i);
            Point2D next = poly.get((i + 1) % n);

            double dx1 = curr.getX() - prev.getX();
            double dy1 = curr.getY() - prev.getY();
            double len1 = Math.sqrt(dx1*dx1 + dy1*dy1);
            if (len1 > 0) { dx1 /= len1; dy1 /= len1; }

            double dx2 = next.getX() - curr.getX();
            double dy2 = next.getY() - curr.getY();
            double len2 = Math.sqrt(dx2*dx2 + dy2*dy2);
            if (len2 > 0) { dx2 /= len2; dy2 /= len2; }

            // Inward normals (clockwise polygon -> right side is inside)
            // Normal to (dx, dy) is (-dy, dx)
            double nx1 = -dy1; double ny1 = dx1;
            double nx2 = -dy2; double ny2 = dx2;

            // Average normal
            double nx = nx1 + nx2;
            double ny = ny1 + ny2;
            double len = Math.sqrt(nx*nx + ny*ny);
            
            if (len > 0.0001) {
                nx /= len;
                ny /= len;
                
                // Calculate correct miter length to preserve sharp corners
                double dot = nx * nx1 + ny * ny1;
                double miterLength = insetAmount;
                if (dot > 0.1) { // Avoid massive spikes for very sharp angles
                    miterLength = insetAmount / dot;
                    // Cap the miter length to prevent extreme spikes on zig-zags
                    miterLength = Math.min(miterLength, insetAmount * 3.0);
                }
                
                inset.add(new Point2D.Double(curr.getX() + nx * miterLength, curr.getY() + ny * miterLength));
            } else {
                inset.add(new Point2D.Double(curr.getX() + nx1 * insetAmount, curr.getY() + ny1 * insetAmount));
            }
        }
        return inset;
    }
}
