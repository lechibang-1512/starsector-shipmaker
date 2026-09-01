package shipeditor.utility.graphics;

/**
 * High-performance vectorized and per-pixel color space transformations.
 * Supports standard sRGB, CIELAB (D65 standard illuminant), CIELCh, and HSV.
 */
public final class ColorSpaceTransform {

    private static final double D65_XN = 0.95047;
    private static final double D65_YN = 1.00000;
    private static final double D65_ZN = 1.08883;

    private static final double DELTA = 6.0 / 29.0;
    private static final double DELTA_CUBE = DELTA * DELTA * DELTA;
    private static final double THREE_DELTA_SQ = 3.0 * DELTA * DELTA;
    private static final double FOUR_OVER_29 = 4.0 / 29.0;

    private ColorSpaceTransform() {
    }

    /**
     * Standard ITU-R Rec. 709 perceived luminance.
     */
    public static float extractLuminance(float r, float g, float b) {
        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
    }

    /**
     * Converts sRGB float [0, 1] to CIELAB under D65 illuminant.
     *
     * @param r Red [0, 1]
     * @param g Green [0, 1]
     * @param b Blue [0, 1]
     * @param out Float array of size >= 3 [l (0..100), a (-128..127), b (-128..127)]
     */
    public static void rgbToLab(float r, float g, float b, float[] out) {
        double rLin = (r <= 0.04045f) ? (r / 12.92) : Math.pow((r + 0.055) / 1.055, 2.4);
        double gLin = (g <= 0.04045f) ? (g / 12.92) : Math.pow((g + 0.055) / 1.055, 2.4);
        double bLin = (b <= 0.04045f) ? (b / 12.92) : Math.pow((b + 0.055) / 1.055, 2.4);

        double x = (rLin * 0.4124564 + gLin * 0.3575761 + bLin * 0.1804375) / D65_XN;
        double y = (rLin * 0.2126729 + gLin * 0.7151522 + bLin * 0.0721750) / D65_YN;
        double z = (rLin * 0.0193339 + gLin * 0.1191920 + bLin * 0.9503041) / D65_ZN;

        double fx = (x > DELTA_CUBE) ? Math.cbrt(x) : (x / THREE_DELTA_SQ + FOUR_OVER_29);
        double fy = (y > DELTA_CUBE) ? Math.cbrt(y) : (y / THREE_DELTA_SQ + FOUR_OVER_29);
        double fz = (z > DELTA_CUBE) ? Math.cbrt(z) : (z / THREE_DELTA_SQ + FOUR_OVER_29);

        out[0] = (float) (116.0 * fy - 16.0);
        out[1] = (float) (500.0 * (fx - fy));
        out[2] = (float) (200.0 * (fy - fz));
    }

    public static float[] rgbToLab(float r, float g, float b) {
        float[] out = new float[3];
        rgbToLab(r, g, b, out);
        return out;
    }

    /**
     * Converts CIELAB under D65 illuminant to sRGB float [0, 1].
     */
    public static void labToRgb(float l, float a, float b, float[] out) {
        double fy = (l + 16.0) / 116.0;
        double fx = a / 500.0 + fy;
        double fz = fy - b / 200.0;

        double x = D65_XN * ((fx > DELTA) ? (fx * fx * fx) : (THREE_DELTA_SQ * (fx - FOUR_OVER_29)));
        double y = D65_YN * ((fy > DELTA) ? (fy * fy * fy) : (THREE_DELTA_SQ * (fy - FOUR_OVER_29)));
        double z = D65_ZN * ((fz > DELTA) ? (fz * fz * fz) : (THREE_DELTA_SQ * (fz - FOUR_OVER_29)));

        double rLin =  3.2404542 * x - 1.5371385 * y - 0.4985314 * z;
        double gLin = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z;
        double bLin =  0.0556434 * x - 0.2040259 * y + 1.0572252 * z;

        out[0] = (float) clamp01((rLin <= 0.0031308) ? (12.92 * rLin) : (1.055 * Math.pow(Math.max(rLin, 0.0), 1.0 / 2.4) - 0.055));
        out[1] = (float) clamp01((gLin <= 0.0031308) ? (12.92 * gLin) : (1.055 * Math.pow(Math.max(gLin, 0.0), 1.0 / 2.4) - 0.055));
        out[2] = (float) clamp01((bLin <= 0.0031308) ? (12.92 * bLin) : (1.055 * Math.pow(Math.max(bLin, 0.0), 1.0 / 2.4) - 0.055));
    }

    public static float[] labToRgb(float l, float a, float b) {
        float[] out = new float[3];
        labToRgb(l, a, b, out);
        return out;
    }

    /**
     * Converts sRGB float [0, 1] to HSV [0, 1].
     */
    public static void rgbToHsv(float r, float g, float b, float[] out) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float v = max;
        float s = (max > 0.0f) ? (delta / max) : 0.0f;

        float h = 0.0f;
        if (delta > 0.0f) {
            if (Float.compare(max, r) == 0) {
                h = ((g - b) / delta) % 6.0f;
                if (h < 0.0f) h += 6.0f;
            } else if (Float.compare(max, g) == 0) {
                h = ((b - r) / delta) + 2.0f;
            } else {
                h = ((r - g) / delta) + 4.0f;
            }
            h /= 6.0f;
        }

        out[0] = h;
        out[1] = s;
        out[2] = v;
    }

    public static float[] rgbToHsv(float r, float g, float b) {
        float[] out = new float[3];
        rgbToHsv(r, g, b, out);
        return out;
    }

    /**
     * Converts HSV [0, 1] to sRGB float [0, 1].
     */
    public static void hsvToRgb(float h, float s, float v, float[] out) {
        if (s <= 0.0f) {
            out[0] = v;
            out[1] = v;
            out[2] = v;
            return;
        }

        float hNorm = (h % 1.0f);
        if (hNorm < 0.0f) hNorm += 1.0f;
        float h6 = hNorm * 6.0f;
        int i = (int) Math.floor(h6);
        float f = h6 - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);

        switch (i % 6) {
            case 0 -> { out[0] = v; out[1] = t; out[2] = p; }
            case 1 -> { out[0] = q; out[1] = v; out[2] = p; }
            case 2 -> { out[0] = p; out[1] = v; out[2] = t; }
            case 3 -> { out[0] = p; out[1] = q; out[2] = v; }
            case 4 -> { out[0] = t; out[1] = p; out[2] = v; }
            case 5 -> { out[0] = v; out[1] = p; out[2] = q; }
            default -> { out[0] = v; out[1] = v; out[2] = v; }
        }
    }

    public static float[] hsvToRgb(float h, float s, float v) {
        float[] out = new float[3];
        hsvToRgb(h, s, v, out);
        return out;
    }

    /**
     * Converts CIELAB to CIELCh (Lightness [0..100], Chroma [0..150], Hue [0..360) degrees).
     */
    public static void labToLch(float l, float a, float b, float[] out) {
        float c = (float) Math.sqrt(a * a + b * b);
        double hDeg = Math.toDegrees(Math.atan2(b, a));
        if (hDeg < 0.0) hDeg += 360.0;
        out[0] = l;
        out[1] = c;
        out[2] = (float) hDeg;
    }

    public static float[] labToLch(float l, float a, float b) {
        float[] out = new float[3];
        labToLch(l, a, b, out);
        return out;
    }

    /**
     * Converts CIELCh (Hue in degrees [0..360)) to CIELAB.
     */
    public static void lchToLab(float l, float c, float hDeg, float[] out) {
        double hRad = Math.toRadians(hDeg);
        out[0] = l;
        out[1] = (float) (c * Math.cos(hRad));
        out[2] = (float) (c * Math.sin(hRad));
    }

    public static float[] lchToLab(float l, float c, float hDeg) {
        float[] out = new float[3];
        lchToLab(l, c, hDeg, out);
        return out;
    }

    private static double clamp01(double val) {
        return Math.max(0.0, Math.min(1.0, val));
    }
}
