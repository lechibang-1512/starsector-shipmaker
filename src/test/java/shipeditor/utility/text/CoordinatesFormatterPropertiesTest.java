package shipeditor.utility.text;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import java.awt.geom.Point2D;
import static org.junit.jupiter.api.Assertions.*;

class CoordinatesFormatterPropertiesTest {

    @Property
    void testFormatDisplay(@ForAll @DoubleRange(min = -1e6, max = 1e6) double x, 
                           @ForAll @DoubleRange(min = -1e6, max = 1e6) double y) {
        String display = CoordinatesFormatter.formatDisplay(x, y);
        
        assertNotNull(display);
        assertTrue(display.contains(", "));
        
        // It should have at least 3 decimal places (e.g. 0.000)
        String[] parts = display.split(", ");
        assertEquals(2, parts.length);
    }
    
    @Property
    void testRoundPoint(@ForAll @DoubleRange(min = -1e6, max = 1e6) double x, 
                        @ForAll @DoubleRange(min = -1e6, max = 1e6) double y) {
        Point2D point = new Point2D.Double(x, y);
        Point2D rounded = CoordinatesFormatter.roundPoint(point);
        
        assertNotNull(rounded);
        // It shouldn't drift too much
        assertTrue(Math.abs(rounded.getX() - x) <= 0.001);
        assertTrue(Math.abs(rounded.getY() - y) <= 0.001);
    }
}
