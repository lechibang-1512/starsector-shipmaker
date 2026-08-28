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
    public static float deltaECie94(float[] lab1, float[] lab2, float kL, float kC, float kH, float K1, float K2) {
        float dL = lab1[0] - lab2[0];
        float a1 = lab1[1], b1 = lab1[2];
        float a2 = lab2[1], b2 = lab2[2];

        double C1 = Math.sqrt(a1 * a1 + b1 * b1);
        double C2 = Math.sqrt(a2 * a2 + b2 * b2);
        double dC = C1 - C2;

        float da = a1 - a2;
        float db = b1 - b2;
        double dH2 = Math.max(0.0, da * da + db * db - dC * dC);
        double dH = Math.sqrt(dH2);

        double sL = 1.0;
        double sC = 1.0 + K1 * C1;
        double sH = 1.0 + K2 * C1;

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
        double L1 = lab1[0], a1 = lab1[1], b1 = lab1[2];
        double L2 = lab2[0], a2 = lab2[1], b2 = lab2[2];

        double C1 = Math.sqrt(a1 * a1 + b1 * b1);
        double C2 = Math.sqrt(a2 * a2 + b2 * b2);
        double C_bar = (C1 + C2) / 2.0;

        double C_bar7 = Math.pow(C_bar, 7.0);
        double G = 0.5 * (1.0 - Math.sqrt(C_bar7 / (C_bar7 + TWENTY_FIVE_POW_7 + 1e-12)));

        double a1_prime = (1.0 + G) * a1;
        double a2_prime = (1.0 + G) * a2;

        double C1_prime = Math.sqrt(a1_prime * a1_prime + b1 * b1);
        double C2_prime = Math.sqrt(a2_prime * a2_prime + b2 * b2);

        double h1_prime = (Math.toDegrees(Math.atan2(b1, a1_prime)) + 360.0) % 360.0;
        double h2_prime = (Math.toDegrees(Math.atan2(b2, a2_prime)) + 360.0) % 360.0;

        double dL_prime = L2 - L1;
        double dC_prime = C2_prime - C1_prime;

        double h_diff = h2_prime - h1_prime;
        double dh_prime = 0.0;

        if (C1_prime * C2_prime != 0.0) {
            if (Math.abs(h_diff) <= 180.0) {
                dh_prime = h_diff;
            } else if (h_diff > 180.0) {
                dh_prime = h_diff - 360.0;
            } else {
                dh_prime = h_diff + 360.0;
            }
        }

        double dH_prime = 2.0 * Math.sqrt(C1_prime * C2_prime) * Math.sin(Math.toRadians(dh_prime / 2.0));

        double L_bar_prime = (L1 + L2) / 2.0;
        double C_bar_prime = (C1_prime + C2_prime) / 2.0;

        double h_bar_prime;
        if (C1_prime * C2_prime == 0.0) {
            h_bar_prime = h1_prime + h2_prime;
        } else if (Math.abs(h1_prime - h2_prime) <= 180.0) {
            h_bar_prime = (h1_prime + h2_prime) / 2.0;
        } else if ((h1_prime + h2_prime) < 360.0) {
            h_bar_prime = (h1_prime + h2_prime + 360.0) / 2.0;
        } else {
            h_bar_prime = (h1_prime + h2_prime - 360.0) / 2.0;
        }

        double T = 1.0 - 0.17 * Math.cos(Math.toRadians(h_bar_prime - 30.0))
                + 0.24 * Math.cos(Math.toRadians(2.0 * h_bar_prime))
                + 0.32 * Math.cos(Math.toRadians(3.0 * h_bar_prime + 6.0))
                - 0.20 * Math.cos(Math.toRadians(4.0 * h_bar_prime - 63.0));

        double d_theta = 30.0 * Math.exp(-Math.pow((h_bar_prime - 275.0) / 25.0, 2.0));

        double C_bar_prime7 = Math.pow(C_bar_prime, 7.0);
        double R_C = 2.0 * Math.sqrt(C_bar_prime7 / (C_bar_prime7 + TWENTY_FIVE_POW_7 + 1e-12));

        double L_bar_50_sq = (L_bar_prime - 50.0) * (L_bar_prime - 50.0);
        double S_L = 1.0 + (0.015 * L_bar_50_sq) / Math.sqrt(20.0 + L_bar_50_sq);
        double S_C = 1.0 + 0.045 * C_bar_prime;
        double S_H = 1.0 + 0.015 * C_bar_prime * T;

        double R_T = -Math.sin(Math.toRadians(2.0 * d_theta)) * R_C;

        double vL = dL_prime / (kL * S_L);
        double vC = dC_prime / (kC * S_C);
        double vH = dH_prime / (kH * S_H);

        double dE_sq = vL * vL + vC * vC + vH * vH + R_T * vC * vH;
        return (float) Math.sqrt(Math.max(0.0, dE_sq));
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

        double C1 = Math.sqrt(a1 * a1 + b1 * b1);
        double C2 = Math.sqrt(a2 * a2 + b2 * b2);
        double dC = C1 - C2;

        double h1 = (Math.toDegrees(Math.atan2(b1, a1)) + 360.0) % 360.0;
        double h2 = (Math.toDegrees(Math.atan2(b2, a2)) + 360.0) % 360.0;
        double dh = ((h2 - h1 + 180.0) % 360.0) - 180.0;
        double dH = 2.0 * Math.sqrt(C1 * C2) * Math.sin(Math.toRadians(dh / 2.0));

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

        double C1 = Math.sqrt(a1 * a1 + b1 * b1);
        double C2 = Math.sqrt(a2 * a2 + b2 * b2);
        double dC = C1 - C2;

        double h1 = (Math.toDegrees(Math.atan2(b1, a1)) + 360.0) % 360.0;
        double h2 = (Math.toDegrees(Math.atan2(b2, a2)) + 360.0) % 360.0;
        double dh = ((h2 - h1 + 180.0) % 360.0) - 180.0;
        double dH = 2.0 * Math.sqrt(C1 * C2) * Math.sin(Math.toRadians(dh / 2.0));

        double meanChroma = (C1 + C2) / 2.0;
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
