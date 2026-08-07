package shipeditor.utility.graphics;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import java.awt.Color;
import static org.junit.jupiter.api.Assertions.*;

class ColorUtilitiesPropertiesTest {

    @Provide
    Arbitrary<Color> validColors() {
        return Arbitraries.integers().between(0, 255).flatMap(r ->
               Arbitraries.integers().between(0, 255).flatMap(g ->
               Arbitraries.integers().between(0, 255).map(b -> new Color(r, g, b))
        ));
    }

    @Property
    void darkenReducesRGB(@ForAll("validColors") Color color, @ForAll @DoubleRange(min = 0.0, max = 1.0) double factor) {
        Color darkened = ColorUtilities.darken(color, factor);
        assertTrue(darkened.getRed() <= color.getRed());
        assertTrue(darkened.getGreen() <= color.getGreen());
        assertTrue(darkened.getBlue() <= color.getBlue());
        
        // Edge cases
        if (factor == 0.0) {
            assertEquals(0, darkened.getRed());
            assertEquals(0, darkened.getGreen());
            assertEquals(0, darkened.getBlue());
        }
    }

    @Property
    void lightenIncreasesRGB(@ForAll("validColors") Color color, @ForAll @DoubleRange(min = 0.0, max = 1.0) double factor) {
        Color lightened = ColorUtilities.lighten(color, factor);
        assertTrue(lightened.getRed() >= color.getRed());
        assertTrue(lightened.getGreen() >= color.getGreen());
        assertTrue(lightened.getBlue() >= color.getBlue());
    }
}
