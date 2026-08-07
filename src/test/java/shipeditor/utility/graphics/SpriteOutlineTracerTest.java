package shipeditor.utility.graphics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpriteOutlineTracerTest {

    @Test
    @DisplayName("Null image returns empty contour list")
    void testNullImageReturnsEmpty() {
        List<Point2D> contour = SpriteOutlineTracer.generateExactContour(null);
        assertNotNull(contour);
        assertTrue(contour.isEmpty());
    }

    @Test
    @DisplayName("Fully transparent image returns empty contour list")
    void testTransparentImageReturnsEmpty() {
        BufferedImage img = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        List<Point2D> contour = SpriteOutlineTracer.generateExactContour(img);
        assertNotNull(contour);
        assertTrue(contour.isEmpty());
    }

    @Test
    @DisplayName("Solid rectangle generates exact non-dilated, non-inset contour")
    void testSolidRectangleContour() {
        int width = 50;
        int height = 50;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 0, 0, 255));
        // Draw a 20x20 solid square from (15, 15) to (34, 34)
        g.fillRect(15, 15, 20, 20);
        g.dispose();

        List<Point2D> contour = SpriteOutlineTracer.generateExactContour(img);
        assertNotNull(contour);
        assertFalse(contour.isEmpty(), "Contour should not be empty for opaque region");
        assertTrue(contour.size() >= 4, "Contour should have at least 4 vertices for a rectangle");

        // Compute bounding box of contour vertices
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Point2D p : contour) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());

            // All points must be within local image boundaries
            assertTrue(p.getX() >= 0 && p.getX() < width, "X vertex out of image bounds: " + p.getX());
            assertTrue(p.getY() >= 0 && p.getY() < height, "Y vertex out of image bounds: " + p.getY());
        }

        // Contour bounding box should tightly match the 15..34 filled region (+- 1px tolerance)
        assertEquals(15.0, minX, 1.5, "Contour minX should tightly match filled box without dilation/insetting");
        assertEquals(15.0, minY, 1.5, "Contour minY should tightly match filled box without dilation/insetting");
        assertEquals(34.0, maxX, 1.5, "Contour maxX should tightly match filled box without dilation/insetting");
        assertEquals(34.0, maxY, 1.5, "Contour maxY should tightly match filled box without dilation/insetting");
    }

    @Test
    @DisplayName("L-shaped sprite generates valid contour encompassing all extremities")
    void testLShapeSpriteContour() {
        BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        // Vertical bar: (10, 10) to (15, 30)
        g.fillRect(10, 10, 6, 21);
        // Horizontal bar: (10, 25) to (30, 30)
        g.fillRect(10, 25, 21, 6);
        g.dispose();

        List<Point2D> contour = SpriteOutlineTracer.generateExactContour(img);
        assertNotNull(contour);
        assertTrue(contour.size() >= 6, "L-shape should have at least 6 simplified vertices");

        double minX = contour.stream().mapToDouble(p -> p.getX()).min().orElse(0);
        double maxX = contour.stream().mapToDouble(p -> p.getX()).max().orElse(0);
        double minY = contour.stream().mapToDouble(p -> p.getY()).min().orElse(0);
        double maxY = contour.stream().mapToDouble(p -> p.getY()).max().orElse(0);

        assertEquals(10.0, minX, 1.5);
        assertEquals(10.0, minY, 1.5);
        assertEquals(30.0, maxX, 1.5);
        assertEquals(30.0, maxY, 1.5);
    }
}
