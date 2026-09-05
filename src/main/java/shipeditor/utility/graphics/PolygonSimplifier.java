package shipeditor.utility.graphics;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class providing Douglas-Peucker polygon simplification algorithms.
 */
public final class PolygonSimplifier {

    private PolygonSimplifier() {
    }

    /**
     * Recursively simplifies a polygon path defined by integer coordinates using the Douglas-Peucker algorithm.
     *
     * @param points  the list of [x, y] coordinates
     * @param epsilon the distance tolerance threshold
     * @return the simplified polygon as a list of {@link Point2D}
     */
    public static List<Point2D> simplifyPolygon(List<int[]> points, double epsilon) {
        if (points.size() < 3) {
            List<Point2D> res = new ArrayList<>();
            for (int[] p : points) {
                res.add(new Point2D.Double(p[0], p[1]));
            }
            return res;
        }

        double maxDistance = 0.0;
        int index = 0;
        int end = points.size() - 1;

        int[] first = points.get(0);
        int[] last = points.get(end);

        for (int i = 1; i < end; i++) {
            double distance = perpendicularDistance(points.get(i), first, last);
            if (distance > maxDistance) {
                index = i;
                maxDistance = distance;
            }
        }

        List<Point2D> result = new ArrayList<>();
        if (maxDistance > epsilon) {
            List<int[]> firstLine = points.subList(0, index + 1);
            List<int[]> secondLine = points.subList(index, end + 1);

            List<Point2D> firstResult = simplifyPolygon(firstLine, epsilon);
            List<Point2D> secondResult = simplifyPolygon(secondLine, epsilon);

            firstResult.remove(firstResult.size() - 1);
            result.addAll(firstResult);
            result.addAll(secondResult);
        } else {
            result.add(new Point2D.Double(first[0], first[1]));
            result.add(new Point2D.Double(last[0], last[1]));
        }

        return result;
    }

    /**
     * Calculates the perpendicular distance from a point to a line segment.
     *
     * @param pt        the target point [x, y]
     * @param lineStart line segment start point [x, y]
     * @param lineEnd   line segment end point [x, y]
     * @return perpendicular distance
     */
    public static double perpendicularDistance(int[] pt, int[] lineStart, int[] lineEnd) {
        double dx = lineEnd[0] - lineStart[0];
        double dy = lineEnd[1] - lineStart[1];

        if (dx == 0 && dy == 0) {
            return Math.hypot(pt[0] - lineStart[0], pt[1] - lineStart[1]);
        }

        double t = ((pt[0] - lineStart[0]) * dx + (pt[1] - lineStart[1]) * dy) / (dx * dx + dy * dy);

        if (t < 0) {
            return Math.hypot(pt[0] - lineStart[0], pt[1] - lineStart[1]);
        } else if (t > 1) {
            return Math.hypot(pt[0] - lineEnd[0], pt[1] - lineEnd[1]);
        }

        double closestX = lineStart[0] + t * dx;
        double closestY = lineStart[1] + t * dy;

        return Math.hypot(pt[0] - closestX, pt[1] - closestY);
    }
}
