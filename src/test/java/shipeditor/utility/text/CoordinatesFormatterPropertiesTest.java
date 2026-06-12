package shipeditor.utility.text;

import net.jqwik.api.*;
import java.awt.geom.Point2D;
import static org.junit.jupiter.api.Assertions.*;

class CoordinatesFormatterPropertiesTest {

    @Property
    void testRoundMaintainsValue(@ForAll double value) {
        double result = CoordinatesFormatter.round(value);
        
        if (Double.isNaN(value)) {
            assertTrue(Double.isNaN(result), "NaN should remain NaN");
        } else if (Double.isInfinite(value)) {
            assertTrue(Double.isInfinite(result), "Infinity should remain Infinity");
            assertEquals(Math.signum(value), Math.signum(result), "Infinity sign should match");
        } else if (Math.abs(value) < (Long.MAX_VALUE / 1000.0)) {
            // For values that don't overflow the Long conversion
            double difference = Math.abs(value - result);
            assertTrue(difference <= 0.005, "Difference should be small, but was " + difference + " for value " + value);
        } else {
            // For very large values, what happens?
            double difference = Math.abs(value - result);
            assertTrue(difference <= Math.abs(value * 0.01), "Difference should be proportional for large numbers");
        }
    }

    @Property
    void testRoundPointMaintainsValue(@ForAll double x, @ForAll double y) {
        Point2D point = new Point2D.Double(x, y);
        Point2D result = CoordinatesFormatter.roundPoint(point);
        
        if (Double.isNaN(x)) {
            assertTrue(Double.isNaN(result.getX()));
        }
        if (Double.isNaN(y)) {
            assertTrue(Double.isNaN(result.getY()));
        }
    }
}
