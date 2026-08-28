package shipeditor.utility.graphics;

import lombok.Builder;
import lombok.Data;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extracts and transfers painted liveries, faction decals, and panel seam line-art from sprites.
 */
public final class StencilExtractor {

    public enum BlendMode {
        OVER,
        MULTIPLY,
        SCREEN,
        OVERLAY,
        SOFT_LIGHT
    }

    public enum AutoMode {
        ALL,
        CHROMATIC,
        LIGHT
    }

    @Data
    @Builder
    public static class StencilDiffConfig {
        @Builder.Default private float threshold = 8.0f;
        @Builder.Default private float featherRadius = 1.0f;
        @Builder.Default private DeltaEMath.DeltaEMode metric = DeltaEMath.DeltaEMode.CIEDE2000;
        @Builder.Default private boolean adaptive = true;
        @Builder.Default private int alphaThreshold = 30;
    }

    @Data
    @Builder
    public static class StencilAutoConfig {
        @Builder.Default private float threshold = 12.0f;
        @Builder.Default private float featherRadius = 1.0f;
        @Builder.Default private DeltaEMath.DeltaEMode metric = DeltaEMath.DeltaEMode.CIEDE2000;
        @Builder.Default private AutoMode mode = AutoMode.ALL;
        @Builder.Default private float lightThreshold = 16.0f;
        @Builder.Default private int alphaThreshold = 30;
    }

    private StencilExtractor() {
    }

    /**
     * Extracts painted decals by computing perceptual Delta-E differences between base hull and faction skin.
     */
    public static BufferedImage extractDiffStencil(BufferedImage base, BufferedImage skin, StencilDiffConfig config) {
        if (base == null || skin == null) return null;
        if (config == null) config = StencilDiffConfig.builder().build();

        int w = base.getWidth();
        int h = base.getHeight();
        if (skin.getWidth() != w || skin.getHeight() != h) {
            throw new IllegalArgumentException("Dimension mismatch between base and skin sprite");
        }

        int[] baseRaw = new int[w * h];
        int[] skinRaw = new int[w * h];
        base.getRGB(0, 0, w, h, baseRaw, 0, w);
        skin.getRGB(0, 0, w, h, skinRaw, 0, w);

        boolean[][] vis = ImageProcessing.extractAlphaMask(skin, config.getAlphaThreshold());
        float[][] lumSkin = ImageProcessing.extractLuminanceGrid(skin);

        boolean[][] stencilMask = new boolean[h][w];
        float[] baseLab = new float[3];
        float[] skinLab = new float[3];

        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                if (!vis[y][x]) continue;

                int bArgb = baseRaw[rowOffset + x];
                int sArgb = skinRaw[rowOffset + x];

                float br = ((bArgb >> 16) & 0xff) / 255.0f;
                float bg = ((bArgb >> 8) & 0xff) / 255.0f;
                float bb = (bArgb & 0xff) / 255.0f;

                float sr = ((sArgb >> 16) & 0xff) / 255.0f;
                float sg = ((sArgb >> 8) & 0xff) / 255.0f;
                float sb = (sArgb & 0xff) / 255.0f;

                ColorSpaceTransform.rgbToLab(br, bg, bb, baseLab);
                ColorSpaceTransform.rgbToLab(sr, sg, sb, skinLab);

                float de = DeltaEMath.deltaE(baseLab, skinLab, config.getMetric());

                if (config.isAdaptive()) {
                    float lBase = baseLab[0];
                    float lSkin = skinLab[0];
                    float dl = lSkin - lBase;

                    float cBase = (float) Math.sqrt(baseLab[1] * baseLab[1] + baseLab[2] * baseLab[2]);
                    float cSkin = (float) Math.sqrt(skinLab[1] * skinLab[1] + skinLab[2] * skinLab[2]);
                    float dc = Math.abs(cSkin - cBase);

                    boolean isMetalHighlight = (lBase > 65.0f) && (cBase < 6.0f) && (cSkin < 6.0f);
                    float highlightScaling = 1.0f + 0.60f * Math.max(0.0f, Math.min(1.0f, (lBase - 60.0f) / 25.0f));
                    float effThreshold = isMetalHighlight ? (config.getThreshold() * highlightScaling) : config.getThreshold();

                    boolean isBleachedPaint = (dl > 14.0f) && (lSkin > 65.0f) && (lBase > 20.0f);
                    boolean isChromaticPaint = (dc > 5.0f) && (de > config.getThreshold() * 0.75f);

                    stencilMask[y][x] = (de > effThreshold) || isBleachedPaint || isChromaticPaint;
                } else {
                    stencilMask[y][x] = de > config.getThreshold();
                }
            }
        }

        float[][] maskFloat;
        if (config.getFeatherRadius() > 0.0f) {
            maskFloat = ImageProcessing.edgeAwareFeather(stencilMask, lumSkin, config.getFeatherRadius(), true);
        } else {
            maskFloat = new float[h][w];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    maskFloat[y][x] = stencilMask[y][x] ? 1.0f : 0.0f;
                }
            }
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] outRaw = new int[w * h];
        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                int sArgb = skinRaw[rowOffset + x];
                int sa = (sArgb >> 24) & 0xff;
                int outAlpha = Math.round(maskFloat[y][x] * sa);
                outRaw[rowOffset + x] = (outAlpha << 24) | (sArgb & 0x00ffffff);
            }
        }
        out.setRGB(0, 0, w, h, outRaw, 0, w);
        return out;
    }

    /**
     * Automatically extracts accent liveries and light-colored paint markings from a single sprite.
     */
    public static BufferedImage extractAutoStencil(BufferedImage skin, StencilAutoConfig config) {
        if (skin == null) return null;
        if (config == null) config = StencilAutoConfig.builder().build();

        int w = skin.getWidth();
        int h = skin.getHeight();
        int[] skinRaw = new int[w * h];
        skin.getRGB(0, 0, w, h, skinRaw, 0, w);

        boolean[][] vis = ImageProcessing.extractAlphaMask(skin, config.getAlphaThreshold());
        float[][] lum = ImageProcessing.extractLuminanceGrid(skin);

        boolean[][] validHull = new boolean[h][w];
        List<Float> validL = new ArrayList<>();
        List<Float> validA = new ArrayList<>();
        List<Float> validB = new ArrayList<>();

        float[] lab = new float[3];
        float[] hsv = new float[3];

        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                if (!vis[y][x]) continue;

                int argb = skinRaw[rowOffset + x];
                float r = ((argb >> 16) & 0xff) / 255.0f;
                float g = ((argb >> 8) & 0xff) / 255.0f;
                float b = (argb & 0xff) / 255.0f;

                ColorSpaceTransform.rgbToHsv(r, g, b, hsv);
                float sat = hsv[1], val = hsv[2];

                boolean isValid = (val > 0.20f) && !(val > 0.85f && sat > 0.65f);
                validHull[y][x] = isValid;

                if (isValid) {
                    ColorSpaceTransform.rgbToLab(r, g, b, lab);
                    validL.add(lab[0]);
                    validA.add(lab[1]);
                    validB.add(lab[2]);
                }
            }
        }

        if (validL.isEmpty()) {
            return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }

        Collections.sort(validL);
        Collections.sort(validA);
        Collections.sort(validB);
        float[] dominantLab = new float[]{
                validL.get(validL.size() / 2),
                validA.get(validA.size() / 2),
                validB.get(validB.size() / 2)
        };

        float[][] localBgLum = ImageProcessing.gaussianBlur(lum, 4.5f);
        float[][] gradMag = ImageProcessing.sobelMagnitude(lum);

        boolean[][] chromaticAccent = new boolean[h][w];
        boolean[][] lightCandidate = new boolean[h][w];

        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                if (!validHull[y][x]) continue;

                int argb = skinRaw[rowOffset + x];
                float r = ((argb >> 16) & 0xff) / 255.0f;
                float g = ((argb >> 8) & 0xff) / 255.0f;
                float b = (argb & 0xff) / 255.0f;

                ColorSpaceTransform.rgbToLab(r, g, b, lab);
                float de = DeltaEMath.deltaE(lab, dominantLab, config.getMetric());
                float chroma = (float) Math.sqrt(lab[1] * lab[1] + lab[2] * lab[2]);

                chromaticAccent[y][x] = (de > config.getThreshold()) && (chroma > 6.0f);

                float lumStep = (lum[y][x] - localBgLum[y][x]) * 100.0f;
                lightCandidate[y][x] = (lumStep > config.getLightThreshold()) && (lum[y][x] > 0.60f) && (gradMag[y][x] < 0.14f);
            }
        }

        boolean[][] lightPanels = ImageProcessing.binaryOpening(lightCandidate, 1);
        boolean[][] stencilMask = new boolean[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                stencilMask[y][x] = switch (config.getMode()) {
                    case CHROMATIC -> chromaticAccent[y][x];
                    case LIGHT -> lightPanels[y][x];
                    case ALL -> chromaticAccent[y][x] || lightPanels[y][x];
                };
            }
        }

        float[][] maskFloat = (config.getFeatherRadius() > 0.0f)
                ? ImageProcessing.edgeAwareFeather(stencilMask, lum, config.getFeatherRadius(), true)
                : createFloatGrid(stencilMask);

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] outRaw = new int[w * h];
        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                int sa = (skinRaw[rowOffset + x] >> 24) & 0xff;
                int outAlpha = Math.round(maskFloat[y][x] * sa);
                outRaw[rowOffset + x] = (outAlpha << 24) | 0x00ffffff; // Normalized white stencil
            }
        }
        out.setRGB(0, 0, w, h, outRaw, 0, w);
        return out;
    }

    /**
     * Extracts panel seams and mechanical line-art using Sobel gradient magnitude.
     */
    public static BufferedImage extractPanelSeams(BufferedImage sprite, float edgeStrength) {
        if (sprite == null) return null;
        int w = sprite.getWidth();
        int h = sprite.getHeight();

        int[] raw = new int[w * h];
        sprite.getRGB(0, 0, w, h, raw, 0, w);

        boolean[][] vis = ImageProcessing.extractAlphaMask(sprite, 15);
        float[][] lum = ImageProcessing.extractLuminanceGrid(sprite);
        float[][] gradMag = ImageProcessing.sobelMagnitude(lum);

        int[] outRaw = new int[w * h];
        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                if (!vis[y][x]) continue;

                boolean darkStructure = lum[y][x] < 0.20f;
                boolean edge = gradMag[y][x] > edgeStrength;

                if (edge || darkStructure) {
                    float opacity = darkStructure ? 1.0f : Math.max(0.0f, Math.min(1.0f, gradMag[y][x] * 5.0f + 0.30f));
                    int alpha = Math.round(opacity * 255.0f);
                    outRaw[rowOffset + x] = (alpha << 24); // Black line art with variable alpha
                }
            }
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, outRaw, 0, w);
        return out;
    }

    /**
     * Composites stencil onto base hull using various blend modes.
     */
    public static BufferedImage applyStencil(BufferedImage base, BufferedImage stencil, BlendMode mode) {
        if (base == null || stencil == null) return base;
        int w = base.getWidth();
        int h = base.getHeight();

        BufferedImage scaledStencil = stencil;
        if (stencil.getWidth() != w || stencil.getHeight() != h) {
            scaledStencil = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaledStencil.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(stencil, 0, 0, w, h, null);
            g.dispose();
        }

        int[] baseRaw = new int[w * h];
        int[] stencilRaw = new int[w * h];
        base.getRGB(0, 0, w, h, baseRaw, 0, w);
        scaledStencil.getRGB(0, 0, w, h, stencilRaw, 0, w);

        int[] outRaw = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            int bArgb = baseRaw[i];
            int sArgb = stencilRaw[i];

            float ba = ((bArgb >> 24) & 0xff) / 255.0f;
            float br = ((bArgb >> 16) & 0xff) / 255.0f;
            float bg = ((bArgb >> 8) & 0xff) / 255.0f;
            float bb = (bArgb & 0xff) / 255.0f;

            float sa = ((sArgb >> 24) & 0xff) / 255.0f;
            float sr = ((sArgb >> 16) & 0xff) / 255.0f;
            float sg = ((sArgb >> 8) & 0xff) / 255.0f;
            float sb = (sArgb & 0xff) / 255.0f;

            float outR, outG, outB, outA;

            switch (mode) {
                case MULTIPLY -> {
                    outR = (br * sr) * sa + br * (1.0f - sa);
                    outG = (bg * sg) * sa + bg * (1.0f - sa);
                    outB = (bb * sb) * sa + bb * (1.0f - sa);
                    outA = ba;
                }
                case SCREEN -> {
                    outR = (1.0f - (1.0f - br) * (1.0f - sr)) * sa + br * (1.0f - sa);
                    outG = (1.0f - (1.0f - bg) * (1.0f - sg)) * sa + bg * (1.0f - sa);
                    outB = (1.0f - (1.0f - bb) * (1.0f - sb)) * sa + bb * (1.0f - sa);
                    outA = ba;
                }
                case OVERLAY -> {
                    float overR = (br < 0.5f) ? (2.0f * br * sr) : (1.0f - 2.0f * (1.0f - br) * (1.0f - sr));
                    float overG = (bg < 0.5f) ? (2.0f * bg * sg) : (1.0f - 2.0f * (1.0f - bg) * (1.0f - sg));
                    float overB = (bb < 0.5f) ? (2.0f * bb * sb) : (1.0f - 2.0f * (1.0f - bb) * (1.0f - sb));
                    outR = overR * sa + br * (1.0f - sa);
                    outG = overG * sa + bg * (1.0f - sa);
                    outB = overB * sa + bb * (1.0f - sa);
                    outA = ba;
                }
                case SOFT_LIGHT -> {
                    float slR = (1.0f - 2.0f * sr) * br * br + 2.0f * sr * br;
                    float slG = (1.0f - 2.0f * sg) * bg * bg + 2.0f * sg * bg;
                    float slB = (1.0f - 2.0f * sb) * bb * bb + 2.0f * sb * bb;
                    outR = slR * sa + br * (1.0f - sa);
                    outG = slG * sa + bg * (1.0f - sa);
                    outB = slB * sa + bb * (1.0f - sa);
                    outA = ba;
                }
                case OVER -> {
                    outR = sr * sa + br * (1.0f - sa);
                    outG = sg * sa + bg * (1.0f - sa);
                    outB = sb * sa + bb * (1.0f - sa);
                    outA = sa + ba * (1.0f - sa);
                }
                default -> {
                    outR = sr; outG = sg; outB = sb; outA = sa;
                }
            }

            int ir = Math.max(0, Math.min(255, Math.round(outR * 255.0f)));
            int ig = Math.max(0, Math.min(255, Math.round(outG * 255.0f)));
            int ib = Math.max(0, Math.min(255, Math.round(outB * 255.0f)));
            int ia = Math.max(0, Math.min(255, Math.round(outA * 255.0f)));

            outRaw[i] = (ia << 24) | (ir << 16) | (ig << 8) | ib;
        }

        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        res.setRGB(0, 0, w, h, outRaw, 0, w);
        return res;
    }

    private static float[][] createFloatGrid(boolean[][] mask) {
        int h = mask.length;
        int w = mask[0].length;
        float[][] out = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out[y][x] = mask[y][x] ? 1.0f : 0.0f;
            }
        }
        return out;
    }
}
