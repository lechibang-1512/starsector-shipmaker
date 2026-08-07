package shipeditor.parsing.serialize;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.awt.geom.Point2D;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class CustomSerializersPropertiesTest {

    @Property
    void testWritePoint2DForSingle(@ForAll @DoubleRange(min = -1000.0, max = 1000.0) double x,
                                   @ForAll @DoubleRange(min = -1000.0, max = 1000.0) double y) throws Exception {
        Point2D point = new Point2D.Double(x, y);

        StringWriter writer = new StringWriter();
        JsonGenerator gen = new JsonFactory().createGenerator(writer);

        CustomSerializers.SerializationUtilities.writePoint2DForSingle(point, gen);
        gen.flush();

        String result = writer.toString();

        String expectedX = (x % 1 == 0) ? String.valueOf((int) x) : String.valueOf(x);
        String expectedY = (y % 1 == 0) ? String.valueOf((int) y) : String.valueOf(y);

        assertEquals(expectedX + ", " + expectedY, result);
    }
}
