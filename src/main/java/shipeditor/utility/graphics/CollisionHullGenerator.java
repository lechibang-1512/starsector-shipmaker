package shipeditor.utility.graphics;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility for generating collision bounds from a sprite's alpha channel.
 * Uses the Monotone Chain algorithm to compute the convex hull of all opaque pixels.
 */
public final class CollisionHullGenerator {

    private static final int ALPHA_THRESHOLD = 10;

    private CollisionHullGenerator() {
    }

    /**
     * Generates a convex hull of world coordinates based on the opaque pixels of the given image.
     * 
     * @param image The sprite image.
     * @param centerOffset The center point of the ship in pixel coordinates (top-left origin).
     * @return A list of Point2D representing the vertices of the convex hull in Starsector world space.
     */
    public static List<Point2D> generateBounds(BufferedImage image, Point2D centerOffset) {
        if (image == null) return Collections.emptyList();

        int width = image.getWidth();
        int height = image.getHeight();
        List<Point2D> opaquePixels = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xff;
                if (alpha > ALPHA_THRESHOLD) {
                    opaquePixels.add(new Point2D.Double(x, y));
                }
            }
        }

        if (opaquePixels.isEmpty()) return Collections.emptyList();
        if (opaquePixels.size() < 3) return opaquePixels;

        List<Point2D> hull = computeConvexHull(opaquePixels);

        // Convert pixel coordinates (top-left origin) to world coordinates (center origin, Y goes up)
        List<Point2D> worldBounds = new ArrayList<>();
        double cx = centerOffset.getX();
        double cy = centerOffset.getY();
        for (Point2D p : hull) {
            double worldX = p.getX() - cx;
            double worldY = cy - p.getY();
            worldBounds.add(new Point2D.Double(worldX, worldY));
        }

        return worldBounds;
    }

    /**
     * Computes the convex hull of a set of 2D points using the Monotone Chain algorithm.
     */
    static List<Point2D> computeConvexHull(List<Point2D> points) {
        // Sort lexicographically (x-coordinate, then y-coordinate)
        points.sort(Comparator.comparingDouble(Point2D::getX)
                              .thenComparingDouble(Point2D::getY));

        List<Point2D> hull = new ArrayList<>();

        // Build lower hull
        for (Point2D p : points) {
            while (hull.size() >= 2 && crossProduct(hull.get(hull.size() - 2), hull.get(hull.size() - 1), p) <= 0) {
                hull.remove(hull.size() - 1);
            }
            hull.add(p);
        }

        // Build upper hull
        int t = hull.size() + 1;
        for (int i = points.size() - 2; i >= 0; i--) {
            Point2D p = points.get(i);
            while (hull.size() >= t && crossProduct(hull.get(hull.size() - 2), hull.get(hull.size() - 1), p) <= 0) {
                hull.remove(hull.size() - 1);
            }
            hull.add(p);
        }

        // Remove the last point because it's the same as the first point in the lower hull
        hull.remove(hull.size() - 1);

        return hull;
    }

    /**
     * 2D cross product of OA and OB vectors, i.e. z-component of their 3D cross product.
     * Returns a positive value, if OAB makes a counter-clockwise turn,
     * negative for clockwise turn, and zero if the points are collinear.
     */
    private static double crossProduct(Point2D o, Point2D a, Point2D b) {
        return (a.getX() - o.getX()) * (b.getY() - o.getY()) - (a.getY() - o.getY()) * (b.getX() - o.getX());
    }
}
