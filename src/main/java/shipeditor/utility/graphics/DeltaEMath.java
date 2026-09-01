package shipeditor.utility.graphics;

/**
 * Standard perceptual color distance metrics (CIE76, CIE94, CIEDE2000, LCh, and Lightness-Normalized).
 */
public final class DeltaEMath {

    private static final double TWENTY_FIVE_POW_7 = Math.pow(25.0, 7.0);

    public enum DeltaEMode {
        CIE76,
        CIE94,
        CIEDE2000,
        LCH,
        LIGHTNESS_NORMALIZED
    }

    private DeltaEMath() {
    }

    /**
     * CIE76 Euclidean distance in CIELAB space.
     */
    public static float deltaECie76(float[] lab1, float[] lab2) {
        float dL = lab1[0] - lab2[0];
        float da = lab1[1] - lab2[1];
        float db = lab1[2] - lab2[2];
        return (float) Math.sqrt(dL * dL + da * da + db * db);
    }

    /**
     * CIE94 perceptual color difference (Graphic Arts standard).
     */
    public static float deltaECie94(float[] lab1, float[] lab2, float kL, float kC, float kH, float k1, float k2) {
        float dL = lab1[0] - lab2[0];
        float a1 = lab1[1], b1 = lab1[2];
        float a2 = lab2[1], b2 = lab2[2];

        double c1 = Math.sqrt(a1 * a1 + b1 * b1);
        double c2 = Math.sqrt(a2 * a2 + b2 * b2);
        double dC = c1 - c2;

        float da = a1 - a2;
        float db = b1 - b2;
        double dH2 = Math.max(0.0, da * da + db * db - dC * dC);
        double dH = Math.sqrt(dH2);

        double sL = 1.0;
        double sC = 1.0 + k1 * c1;
        double sH = 1.0 + k2 * c1;

        double vL = dL / (kL * sL);
        double vC = dC / (kC * sC);
        double vH = dH / (kH * sH);

        return (float) Math.sqrt(vL * vL + vC * vC + vH * vH);
    }

    public static float deltaECie94(float[] lab1, float[] lab2) {
        return deltaECie94(lab1, lab2, 1.0f, 1.0f, 1.0f, 0.045f, 0.015f);
    }

    /**
     * CIEDE2000 perceptual color difference formula (Sharma, Wu, Dalal 2005 standard).
     */
    public static float deltaECiede2000(float[] lab1, float[] lab2, float kL, float kC, float kH) {
        double l1 = lab1[0], a1 = lab1[1], b1 = lab1[2];
        double l2 = lab2[0], a2 = lab2[1], b2 = lab2[2];

        double c1 = Math.sqrt(a1 * a1 + b1 * b1);
        double c2 = Math.sqrt(a2 * a2 + b2 * b2);
        double cBar = (c1 + c2) / 2.0;

        double cBar7 = Math.pow(cBar, 7.0);
        double g = 0.5 * (1.0 - Math.sqrt(cBar7 / (cBar7 + TWENTY_FIVE_POW_7 + 1e-12)));

        double a1Prime = (1.0 + g) * a1;
        double a2Prime = (1.0 + g) * a2;

        double c1Prime = Math.sqrt(a1Prime * a1Prime + b1 * b1);
        double c2Prime = Math.sqrt(a2Prime * a2Prime + b2 * b2);

        double h1Prime = (Math.toDegrees(Math.atan2(b1, a1Prime)) + 360.0) % 360.0;
        double h2Prime = (Math.toDegrees(Math.atan2(b2, a2Prime)) + 360.0) % 360.0;

        double dlPrime = l2 - l1;
        double dcPrime = c2Prime - c1Prime;

        double hDiff = h2Prime - h1Prime;
        double dhPrime = 0.0;

        if (c1Prime * c2Prime != 0.0) {
            if (Math.abs(hDiff) <= 180.0) {
                dhPrime = hDiff;
            } else if (hDiff > 180.0) {
                dhPrime = hDiff - 360.0;
            } else {
                dhPrime = hDiff + 360.0;
            }
        }

        double dHPrime = 2.0 * Math.sqrt(c1Prime * c2Prime) * Math.sin(Math.toRadians(dhPrime / 2.0));

        double lBarPrime = (l1 + l2) / 2.0;
        double cBarPrime = (c1Prime + c2Prime) / 2.0;

        double hBarPrime;
        if (c1Prime * c2Prime == 0.0) {
            hBarPrime = h1Prime + h2Prime;
        } else if (Math.abs(h1Prime - h2Prime) <= 180.0) {
            hBarPrime = (h1Prime + h2Prime) / 2.0;
        } else if ((h1Prime + h2Prime) < 360.0) {
            hBarPrime = (h1Prime + h2Prime + 360.0) / 2.0;
        } else {
            hBarPrime = (h1Prime + h2Prime - 360.0) / 2.0;
        }

        double t = 1.0 - 0.17 * Math.cos(Math.toRadians(hBarPrime - 30.0))
                + 0.24 * Math.cos(Math.toRadians(2.0 * hBarPrime))
                + 0.32 * Math.cos(Math.toRadians(3.0 * hBarPrime + 6.0))
                - 0.20 * Math.cos(Math.toRadians(4.0 * hBarPrime - 63.0));

        double dTheta = 30.0 * Math.exp(-Math.pow((hBarPrime - 275.0) / 25.0, 2.0));

        double cBarPrime7 = Math.pow(cBarPrime, 7.0);
        double rC = 2.0 * Math.sqrt(cBarPrime7 / (cBarPrime7 + TWENTY_FIVE_POW_7 + 1e-12));

        double lBar50Sq = (lBarPrime - 50.0) * (lBarPrime - 50.0);
        double sL = 1.0 + (0.015 * lBar50Sq) / Math.sqrt(20.0 + lBar50Sq);
        double sC = 1.0 + 0.045 * cBarPrime;
        double sH = 1.0 + 0.015 * cBarPrime * t;

        double rT = -Math.sin(Math.toRadians(2.0 * dTheta)) * rC;

        double vL = dlPrime / (kL * sL);
        double vC = dcPrime / (kC * sC);
        double vH = dHPrime / (kH * sH);

        double deSq = vL * vL + vC * vC + vH * vH + rT * vC * vH;
        return (float) Math.sqrt(Math.max(0.0, deSq));
    }

    public static float deltaECiede2000(float[] lab1, float[] lab2) {
        return deltaECiede2000(lab1, lab2, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Lightness-Chroma-Hue decomposed distance with independent weights.
     */
    public static float deltaELch(float[] lab1, float[] lab2, float wL, float wC, float wH) {
        float dL = lab1[0] - lab2[0];
        float a1 = lab1[1], b1 = lab1[2];
        float a2 = lab2[1], b2 = lab2[2];

        double c1 = Math.sqrt(a1 * a1 + b1 * b1);
        double c2 = Math.sqrt(a2 * a2 + b2 * b2);
        double dC = c1 - c2;

        double h1 = (Math.toDegrees(Math.atan2(b1, a1)) + 360.0) % 360.0;
        double h2 = (Math.toDegrees(Math.atan2(b2, a2)) + 360.0) % 360.0;
        double dh = ((h2 - h1 + 180.0) % 360.0) - 180.0;
        double dH = 2.0 * Math.sqrt(c1 * c2) * Math.sin(Math.toRadians(dh / 2.0));

        double termL = wL * dL;
        double termC = wC * dC;
        double termH = wH * dH;

        return (float) Math.sqrt(termL * termL + termC * termC + termH * termH);
    }

    /**
     * Lightness-Normalized perceptual difference factoring out neutral metallic shading gradients.
     */
    public static float lightnessNormalizedDeltaE(float[] lab1, float[] lab2, float lightnessWeight, float chromaWeight, float hueWeight) {
        float dL = lab1[0] - lab2[0];
        float a1 = lab1[1], b1 = lab1[2];
        float a2 = lab2[1], b2 = lab2[2];

        double c1 = Math.sqrt(a1 * a1 + b1 * b1);
        double c2 = Math.sqrt(a2 * a2 + b2 * b2);
        double dC = c1 - c2;

        double h1 = (Math.toDegrees(Math.atan2(b1, a1)) + 360.0) % 360.0;
        double h2 = (Math.toDegrees(Math.atan2(b2, a2)) + 360.0) % 360.0;
        double dh = ((h2 - h1 + 180.0) % 360.0) - 180.0;
        double dH = 2.0 * Math.sqrt(c1 * c2) * Math.sin(Math.toRadians(dh / 2.0));

        double meanChroma = (c1 + c2) / 2.0;
        double chromaFactor = Math.max(0.20, Math.min(1.0, meanChroma / 12.0));
        double effectiveWl = lightnessWeight * chromaFactor;
        double highDlBoost = (Math.abs(dL) > 18.0) ? 1.0 : effectiveWl;

        double termL = highDlBoost * dL;
        double termC = chromaWeight * dC;
        double termH = hueWeight * dH;

        return (float) Math.sqrt(termL * termL + termC * termC + termH * termH);
    }

    public static float lightnessNormalizedDeltaE(float[] lab1, float[] lab2) {
        return lightnessNormalizedDeltaE(lab1, lab2, 0.35f, 1.25f, 1.10f);
    }

    public static float deltaE(float[] lab1, float[] lab2, DeltaEMode mode) {
        return switch (mode) {
            case CIE76 -> deltaECie76(lab1, lab2);
            case CIE94 -> deltaECie94(lab1, lab2);
            case CIEDE2000 -> deltaECiede2000(lab1, lab2);
            case LCH -> deltaELch(lab1, lab2, 1.0f, 1.0f, 1.0f);
            case LIGHTNESS_NORMALIZED -> lightnessNormalizedDeltaE(lab1, lab2);
        };
    }
}
