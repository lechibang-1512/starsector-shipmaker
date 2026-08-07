package shipeditor.utility.graphics;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Traces sprite alpha boundaries for UI highlighting.
 * <p>
 * This class produces pixel-accurate contour outlines from a sprite's opaque pixels,
 * intended for visual selection indicators (e.g., module selection highlights in the viewer).
 * The output is <b>not</b> suitable for game data export — use {@link CollisionHullGenerator}
 * for Starsector {@code .ship} collision bounds instead.
 * <p>
 * Algorithm:
 * <ol>
 *   <li>Collect all opaque pixels (alpha &gt; threshold).</li>
 *   <li>Moore-Neighbor Tracing to extract the boundary contour.</li>
 *   <li>Ramer-Douglas-Peucker simplification to remove staircase pixel artifacts
 *       while preserving sharp features (epsilon = 1.0px).</li>
 * </ol>
 * The result is in <b>local pixel coordinates</b> {@code [0, width] × [0, height]}.
 * Callers must apply their own anchor offset and rotation transforms.
 *
 * @see CollisionHullGenerator
 */
public final class SpriteOutlineTracer {

    private static final int ALPHA_THRESHOLD = 10;

    private SpriteOutlineTracer() {
    }

    /**
     * Generates an exact contour of the sprite's opaque pixels in local pixel coordinates.
     *
     * @param image The sprite image to trace.
     * @return A list of Point2D vertices forming the contour polygon, or an empty list
     *         if the image is null or fully transparent.
     */
    public static List<Point2D> generateExactContour(BufferedImage image) {
        if (image == null) return Collections.emptyList();

        int width = image.getWidth();
        int height = image.getHeight();

        Set<Point> opaquePixels = new HashSet<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isOpaque(image, x, y)) {
                    opaquePixels.add(new Point(x, y));
                }
            }
        }

        if (opaquePixels.isEmpty()) return Collections.emptyList();

        List<Point> contour = traceBoundary(opaquePixels, width, height);
        if (contour.isEmpty()) return Collections.emptyList();

        // Simplify slightly to eliminate staircase pixel artifacts while preserving exact sharp features
        return simplifyPolygon(contour, 1.0);
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
                    enterDir = (checkDir + 5) % 8;
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
}
