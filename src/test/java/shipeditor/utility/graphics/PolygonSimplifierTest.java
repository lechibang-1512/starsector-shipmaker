package shipeditor.utility.graphics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolygonSimplifierTest {

    @Test
    @DisplayName("Collinear points are simplified to start and end")
    void testCollinearSimplification() {
        List<int[]> points = new ArrayList<>();
        points.add(new int[]{0, 0});
        points.add(new int[]{5, 0});
        points.add(new int[]{10, 0});

        List<Point2D> simplified = PolygonSimplifier.simplifyPolygon(points, 1.0);
        assertEquals(2, simplified.size());
        assertEquals(0.0, simplified.get(0).getX());
        assertEquals(0.0, simplified.get(0).getY());
        assertEquals(10.0, simplified.get(1).getX());
        assertEquals(0.0, simplified.get(1).getY());
    }

    @Test
    @DisplayName("Perpendicular distance calculation")
    void testPerpendicularDistance() {
        int[] pt = {5, 5};
        int[] start = {0, 0};
        int[] end = {10, 0};

        double dist = PolygonSimplifier.perpendicularDistance(pt, start, end);
        assertEquals(5.0, dist, 0.0001);
    }
}
