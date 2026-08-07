package shipeditor.utility.graphics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CollisionHullGeneratorTest {

    @Test
    @DisplayName("Null image returns empty list")
    void testNullImageReturnsEmpty() {
        List<Point2D> bounds = CollisionHullGenerator.generateBounds(null, new Point2D.Double(0, 0));
        assertNotNull(bounds);
        assertTrue(bounds.isEmpty());
    }

    @Test
    @DisplayName("Fully transparent image returns empty list")
    void testTransparentImageReturnsEmpty() {
        BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        List<Point2D> bounds = CollisionHullGenerator.generateBounds(img, new Point2D.Double(50, 50));
        assertNotNull(bounds);
        assertTrue(bounds.isEmpty());
    }

    @Test
    @DisplayName("Generated bounds include anchor offset and apply polygon insetting")
    void testGenerateBoundsAppliesAnchorOffsetAndInsetting() {
        int width = 80;
        int height = 80;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 255, 255, 255));
        // Centered solid square of 40x40 from (20, 20) to (59, 59)
        g.fillRect(20, 20, 40, 40);
        g.dispose();

        Point2D anchor = new Point2D.Double(100.0, 200.0);
        List<Point2D> bounds = CollisionHullGenerator.generateBounds(img, anchor);

        assertNotNull(bounds);
        assertFalse(bounds.isEmpty(), "Bounds should not be empty for opaque region");

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Point2D p : bounds) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());

            // All points should be in world canvas coordinates with anchor offset applied
            assertTrue(p.getX() >= 100.0, "Point X should be offset by anchor X (>= 100): " + p.getX());
            assertTrue(p.getY() >= 200.0, "Point Y should be offset by anchor Y (>= 200): " + p.getY());
        }

        // The raw box in world coordinates would be [120..159, 220..259].
        // Because of the insetPolygon (5px) and dilation, verify it is bounded in that neighborhood
        assertTrue(minX >= 115.0 && minX <= 126.0, "MinX with anchor should be in expected range: " + minX);
        assertTrue(minY >= 215.0 && minY <= 226.0, "MinY with anchor should be in expected range: " + minY);
        assertTrue(maxX >= 153.0 && maxX <= 165.0, "MaxX with anchor should be in expected range: " + maxX);
        assertTrue(maxY >= 253.0 && maxY <= 265.0, "MaxY with anchor should be in expected range: " + maxY);
    }
}
