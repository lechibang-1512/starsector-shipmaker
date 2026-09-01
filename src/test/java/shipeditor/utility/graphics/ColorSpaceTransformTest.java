package shipeditor.utility.graphics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColorSpaceTransformTest {

    @Test
    public void testRgbLabRoundtrip() {
        float[][] testRgbs = {
                {1.0f, 1.0f, 1.0f},
                {0.0f, 0.0f, 0.0f},
                {0.5f, 0.5f, 0.5f},
                {0.8f, 0.2f, 0.3f},
                {0.1f, 0.7f, 0.9f},
                {0.92f, 0.88f, 0.82f}, // Desert tan
                {0.95f, 0.95f, 0.98f}  // Bleached steel
        };

        float[] lab = new float[3];
        float[] recRgb = new float[3];

        for (float[] rgb : testRgbs) {
            ColorSpaceTransform.rgbToLab(rgb[0], rgb[1], rgb[2], lab);
            ColorSpaceTransform.labToRgb(lab[0], lab[1], lab[2], recRgb);

            assertEquals(rgb[0], recRgb[0], 0.005f, "Red roundtrip mismatch");
            assertEquals(rgb[1], recRgb[1], 0.005f, "Green roundtrip mismatch");
            assertEquals(rgb[2], recRgb[2], 0.005f, "Blue roundtrip mismatch");
        }
    }

    @Test
    public void testRgbHsvRoundtrip() {
        float[][] testRgbs = {
                {1.0f, 0.0f, 0.0f},
                {0.0f, 1.0f, 0.0f},
                {0.0f, 0.0f, 1.0f},
                {0.5f, 0.5f, 0.5f},
                {0.85f, 0.75f, 0.60f}
        };

        float[] hsv = new float[3];
        float[] recRgb = new float[3];

        for (float[] rgb : testRgbs) {
            ColorSpaceTransform.rgbToHsv(rgb[0], rgb[1], rgb[2], hsv);
            ColorSpaceTransform.hsvToRgb(hsv[0], hsv[1], hsv[2], recRgb);

            assertEquals(rgb[0], recRgb[0], 0.001f, "HSV Red roundtrip mismatch");
            assertEquals(rgb[1], recRgb[1], 0.001f, "HSV Green roundtrip mismatch");
            assertEquals(rgb[2], recRgb[2], 0.001f, "HSV Blue roundtrip mismatch");
        }
    }

    @Test
    public void testLabLchRoundtrip() {
        float[][] testLabs = {
                {50.0f, 10.0f, -20.0f},
                {90.0f, -5.0f, 15.0f},
                {25.0f, 0.0f, 0.0f},
                {85.0f, 2.5f, -4.0f}
        };

        float[] lch = new float[3];
        float[] recLab = new float[3];

        for (float[] lab : testLabs) {
            ColorSpaceTransform.labToLch(lab[0], lab[1], lab[2], lch);
            ColorSpaceTransform.lchToLab(lch[0], lch[1], lch[2], recLab);

            assertEquals(lab[0], recLab[0], 0.001f, "L roundtrip mismatch");
            assertEquals(lab[1], recLab[1], 0.001f, "a roundtrip mismatch");
            assertEquals(lab[2], recLab[2], 0.001f, "b roundtrip mismatch");
        }
    }

    @Test
    public void testLuminance() {
        float whiteLum = ColorSpaceTransform.extractLuminance(1.0f, 1.0f, 1.0f);
        float blackLum = ColorSpaceTransform.extractLuminance(0.0f, 0.0f, 0.0f);
        float greenLum = ColorSpaceTransform.extractLuminance(0.0f, 1.0f, 0.0f);

        assertEquals(1.0f, whiteLum, 0.001f);
        assertEquals(0.0f, blackLum, 0.001f);
        assertEquals(0.7152f, greenLum, 0.001f);
    }
}
