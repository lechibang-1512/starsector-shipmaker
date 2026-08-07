package shipeditor.utility.graphics;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import java.awt.Shape;
import java.awt.geom.*;
import static org.junit.jupiter.api.Assertions.*;

class ShapeUtilitiesPropertiesTest {

    @Property
    void testTranslateShape(@ForAll @DoubleRange(min = -1e4, max = 1e4) double x,
                            @ForAll @DoubleRange(min = -1e4, max = 1e4) double y,
                            @ForAll @DoubleRange(min = -1e4, max = 1e4) double dx,
                            @ForAll @DoubleRange(min = -1e4, max = 1e4) double dy,
                            @ForAll @DoubleRange(min = 1, max = 1e4) double width,
                            @ForAll @DoubleRange(min = 1, max = 1e4) double height) {
        
        Rectangle2D.Double rect = new Rectangle2D.Double(x, y, width, height);
        Shape translated = ShapeUtilities.translateShape(rect, dx, dy);
        
        Rectangle2D bounds = translated.getBounds2D();
        
        assertEquals(x + dx, bounds.getX(), 0.001);
        assertEquals(y + dy, bounds.getY(), 0.001);
        assertEquals(width, bounds.getWidth(), 0.001);
        assertEquals(height, bounds.getHeight(), 0.001);
    }
    
    @Property
    void testCreateCircle(@ForAll @DoubleRange(min = -1e4, max = 1e4) double x,
                          @ForAll @DoubleRange(min = -1e4, max = 1e4) double y,
                          @ForAll @FloatRange(min = 1f, max = 1e4f) float radius) {
        Point2D center = new Point2D.Double(x, y);
        Ellipse2D circle = ShapeUtilities.createCircle(center, radius);
        
        assertEquals(x - radius, circle.getX(), 0.001);
        assertEquals(y - radius, circle.getY(), 0.001);
        assertEquals(radius * 2, circle.getWidth(), 0.001);
        assertEquals(radius * 2, circle.getHeight(), 0.001);
    }
    
    @Property
    void testGetPointInDirection(@ForAll @DoubleRange(min = -1e4, max = 1e4) double x,
                                 @ForAll @DoubleRange(min = -1e4, max = 1e4) double y,
                                 @ForAll @DoubleRange(min = -720, max = 720) double angleDegrees,
                                 @ForAll @DoubleRange(min = 0, max = 1e4) double length) {
        Point2D start = new Point2D.Double(x, y);
        Point2D end = ShapeUtilities.getPointInDirection(start, angleDegrees, length);
        
        double actualLength = start.distance(end);
        assertEquals(length, actualLength, 0.001);
        
        if (length > 0.001) {
            double actualAngle = Math.toDegrees(Math.atan2(end.getY() - start.getY(), end.getX() - start.getX()));
            double expectedNormalized = ((angleDegrees % 360) + 360) % 360;
            double actualNormalized = ((actualAngle % 360) + 360) % 360;
            
            // Allow for a very small floating point error for angles
            double diff = Math.abs(expectedNormalized - actualNormalized);
            assertTrue(diff < 0.001 || Math.abs(diff - 360.0) < 0.001);
        }
    }
}
