package shipeditor.utility.graphics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeltaEMathTest {

    @Test
    public void testDeltaECie76() {
        float[] lab1 = {50.0f, 0.0f, 0.0f};
        float[] lab2 = {53.0f, 4.0f, 0.0f};
        float de = DeltaEMath.deltaECie76(lab1, lab2);
        assertEquals(5.0f, de, 0.001f);
    }

    @Test
    public void testDeltaECiede2000SharmaBenchmarks() {
        // Standard Sharma, Wu, Dalal (2005) verification pairs
        float[] p11 = {50.0f, 2.6772f, -79.7751f};
        float[] p12 = {50.0f, 0.0f, -82.7485f};
        float de1 = DeltaEMath.deltaECiede2000(p11, p12);
        assertEquals(2.0425f, de1, 0.005f);

        float[] p21 = {50.0f, 3.1571f, -77.5521f};
        float[] p22 = {50.0f, 0.0f, -82.7485f};
        float de2 = DeltaEMath.deltaECiede2000(p21, p22);
        assertEquals(2.800f, de2, 0.01f);

        float[] p31 = {50.0f, 2.8361f, -74.02f};
        float[] p32 = {50.0f, 0.0f, -82.7485f};
        float de3 = DeltaEMath.deltaECiede2000(p31, p32);
        assertEquals(3.4412f, de3, 0.01f);

        // Identical pair
        float[] pZero1 = {75.0f, -10.0f, 20.0f};
        float[] pZero2 = {75.0f, -10.0f, 20.0f};
        float deZero = DeltaEMath.deltaECiede2000(pZero1, pZero2);
        assertEquals(0.0f, deZero, 0.0001f);
    }

    @Test
    public void testDeltaECie94() {
        float[] lab1 = {50.0f, 0.0f, 0.0f};
        float[] lab2 = {50.0f, 10.0f, 0.0f};
        float de = DeltaEMath.deltaECie94(lab1, lab2);
        assertEquals(10.0f, de, 0.01f);
    }

    @Test
    public void testDeltaELchWeights() {
        float[] lab1 = {30.0f, 20.0f, 10.0f};
        float[] lab2 = {80.0f, 20.0f, 10.0f};
        // wL = 0 ignores the 50 lightness delta
        float deChromaOnly = DeltaEMath.deltaELch(lab1, lab2, 0.0f, 1.0f, 1.0f);
        assertEquals(0.0f, deChromaOnly, 0.0001f);
    }

    @Test
    public void testLightnessNormalizedDeltaE() {
        float[] metalShade1 = {35.0f, 0.0f, -1.0f};
        float[] metalShade2 = {75.0f, 0.0f, -1.0f};
        float deMetal = DeltaEMath.lightnessNormalizedDeltaE(metalShade1, metalShade2);

        float[] paint = {35.0f, 25.0f, 25.0f};
        float dePaint = DeltaEMath.lightnessNormalizedDeltaE(metalShade1, paint);

        assertTrue(dePaint > deMetal, "Chromatic paint distance should exceed natural metal shading gradient");
    }
}
