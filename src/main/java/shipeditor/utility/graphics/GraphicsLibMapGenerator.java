package shipeditor.utility.graphics;

import lombok.Builder;
import lombok.Data;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Procedural 2.5D normal, surface, and material map generation engine for Starsector GraphicsLib.
 * <p>
 * Generates:
 * <ul>
 *   <li><b>Normal Map (_normal.png):</b> Tangent-space normals (R=X, G=Y [OpenGL Y+], B=Z) with strict [128, 128, 255, 0] transparent canvas baseline.</li>
 *   <li><b>Surface Map (_surface.png):</b> 8-bit Grayscale or RGBA GLSL shader map (R=Emissive Glow, G=Specular Intensity, B=Specular Hardness, A=Alpha).</li>
 *   <li><b>Material Map (_material.png):</b> PBR material properties (R=Roughness, G=Metallic, B=Ambient Occlusion, A=Alpha).</li>
 * </ul>
 */
public final class GraphicsLibMapGenerator {

    public enum SurfaceMode {
        GRAYSCALE,
        RGBA
    }

    public enum Preset {
        STANDARD,
        HIGH_TECH,
        MIDLINE,
        LOW_TECH,
        HEGEMONY,
        REMNANT
    }

    @Data
    @Builder
    public static class GraphicsLibConfig {
        @Builder.Default private float macroWeight = 0.45f;
        @Builder.Default private float macroSigma = 16.0f;
        @Builder.Default private float hullSlopePower = 0.55f;
        @Builder.Default private float hullSlopeWeight = 0.35f;
        @Builder.Default private float macroLumWeight = 0.45f;
        @Builder.Default private float superstructureWeight = 0.20f;
        @Builder.Default private float superstructureSpineFocus = 0.35f;

        @Builder.Default private float bevelWeight = 0.35f;
        @Builder.Default private float chamferRadius = 2.5f;
        @Builder.Default private float chamferEdgeThresh = 0.32f;
        @Builder.Default private float seamDepth = 0.22f;
        @Builder.Default private float microTextureSigma = 0.8f;

        @Builder.Default private float depthScale = 3.6f;
        @Builder.Default private float normalBlurSigma = 0.5f;

        @Builder.Default private float metallicLuminanceThreshold = 0.28f;
        @Builder.Default private float metallicSaturationPenalty = 2.2f;
        @Builder.Default private float compositeMatteRoughness = 0.85f;
        @Builder.Default private float pureMetalRoughness = 0.22f;
        @Builder.Default private float aoSeamWeight = 0.70f;
        @Builder.Default private float aoVolumetricWeight = 0.40f;

        @Builder.Default private float emissiveSatThreshold = 0.45f;
        @Builder.Default private float emissiveValThreshold = 0.82f;
        @Builder.Default private float emissiveLumThreshold = 0.90f;

        @Builder.Default private SurfaceMode surfaceMode = SurfaceMode.GRAYSCALE;
        @Builder.Default private boolean exportHeight = false;
        @Builder.Default private boolean exportShowcase = false;
        @Builder.Default private int alphaThreshold = 10;
    }

    @Data
    @Builder
    public static class GraphicsLibResult {
        private BufferedImage normalMap;
        private BufferedImage surfaceMap;
        private BufferedImage materialMap;
        private BufferedImage heightMap;
        private BufferedImage showcaseImage;
    }

    private GraphicsLibMapGenerator() {
    }

    public static GraphicsLibConfig createConfigForPreset(Preset preset) {
        GraphicsLibConfig.GraphicsLibConfigBuilder b = GraphicsLibConfig.builder();
        switch (preset) {
            case HIGH_TECH -> b.depthScale(3.2f)
                    .macroWeight(0.50f)
                    .bevelWeight(0.30f)
                    .pureMetalRoughness(0.15f)
                    .compositeMatteRoughness(0.70f)
                    .emissiveSatThreshold(0.35f)
                    .emissiveValThreshold(0.75f);
            case LOW_TECH -> b.depthScale(4.2f)
                    .macroWeight(0.40f)
                    .bevelWeight(0.40f)
                    .seamDepth(0.30f)
                    .pureMetalRoughness(0.35f)
                    .compositeMatteRoughness(0.95f)
                    .aoSeamWeight(0.85f);
            case MIDLINE -> b.depthScale(3.6f)
                    .macroWeight(0.45f)
                    .bevelWeight(0.35f)
                    .pureMetalRoughness(0.22f)
                    .compositeMatteRoughness(0.85f);
            case HEGEMONY -> b.depthScale(3.8f)
                    .macroWeight(0.45f)
                    .bevelWeight(0.35f)
                    .metallicSaturationPenalty(2.8f)
                    .pureMetalRoughness(0.25f);
            case REMNANT -> b.depthScale(3.4f)
                    .macroWeight(0.50f)
                    .bevelWeight(0.35f)
                    .pureMetalRoughness(0.12f)
                    .emissiveSatThreshold(0.30f)
                    .emissiveValThreshold(0.70f)
                    .emissiveLumThreshold(0.80f);
            case STANDARD -> {}
        }
        return b.build();
    }

    public static GraphicsLibResult generateTexturePack(BufferedImage sprite, GraphicsLibConfig config) {
        if (sprite == null) return null;
        if (config == null) config = GraphicsLibConfig.builder().build();

        int w = sprite.getWidth();
        int h = sprite.getHeight();

        boolean[][] vis = ImageProcessing.extractAlphaMask(sprite, config.getAlphaThreshold());
        float[][] lum = ImageProcessing.extractLuminanceGrid(sprite);

        // 1. Method A: Macro 3D Hull Heightfield
        float[][] macroHeight = extractMacroHeightfield(vis, lum, w, h, config);

        // 2. Method B: Bevel, Seams, & Chamfers
        float[][] seamMap = extractSeamMap(lum, vis, w, h);
        float[][] chamferProfile = extractChamferProfile(lum, vis, w, h, config.getChamferRadius(), config.getChamferEdgeThresh());
        float[][] microRelief = ImageProcessing.gaussianBlur(lum, config.getMicroTextureSigma());

        float[][] bevelRelief = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bevelRelief[y][x] = (0.65f * chamferProfile[y][x] + 0.35f * microRelief[y][x]) * (vis[y][x] ? 1.0f : 0.0f);
            }
        }

        // Unified Heightfield
        float[][] height = new float[h][w];
        float hMin = Float.MAX_VALUE, hMax = -Float.MAX_VALUE;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (vis[y][x]) {
                    float val = config.getMacroWeight() * macroHeight[y][x]
                              + config.getBevelWeight() * bevelRelief[y][x]
                              - config.getSeamDepth() * seamMap[y][x];
                    height[y][x] = val;
                    if (val < hMin) hMin = val;
                    if (val > hMax) hMax = val;
                }
            }
        }

        if (hMax > hMin) {
            float range = hMax - hMin;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (vis[y][x]) {
                        height[y][x] = (height[y][x] - hMin) / range;
                    }
                }
            }
        }

        // 3. Normal Map
        BufferedImage normalMap = generateNormalMap(height, sprite, vis, config);

        // 4. Surface Map
        BufferedImage surfaceMap = generateSurfaceMap(sprite, seamMap, chamferProfile, vis, config);

        // 5. Material Map
        BufferedImage materialMap = generateMaterialMap(sprite, height, seamMap, vis, config);

        // Optional Height Map
        BufferedImage heightMap = null;
        if (config.isExportHeight()) {
            heightMap = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            int[] raw = new int[w * h];
            sprite.getRGB(0, 0, w, h, raw, 0, w);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int a = (raw[y * w + x] >> 24) & 0xff;
                    int gray = Math.round(height[y][x] * 255.0f);
                    raw[y * w + x] = (a << 24) | (gray << 16) | (gray << 8) | gray;
                }
            }
            heightMap.setRGB(0, 0, w, h, raw, 0, w);
        }

        // Optional Showcase
        BufferedImage showcase = null;
        if (config.isExportShowcase()) {
            showcase = createShowcaseGrid(sprite, normalMap, surfaceMap, materialMap, heightMap);
        }

        return GraphicsLibResult.builder()
                .normalMap(normalMap)
                .surfaceMap(surfaceMap)
                .materialMap(materialMap)
                .heightMap(heightMap)
                .showcaseImage(showcase)
                .build();
    }

    private static float[][] extractMacroHeightfield(boolean[][] vis, float[][] lum, int w, int h, GraphicsLibConfig config) {
        float[][] edt = ImageProcessing.distanceTransformEdt(vis);
        float maxEdt = 0.0f;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (edt[y][x] > maxEdt) maxEdt = edt[y][x];
            }
        }

        float[][] hullSlope = new float[h][w];
        if (maxEdt > 0.0f) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (vis[y][x]) {
                        hullSlope[y][x] = (float) Math.pow(edt[y][x] / maxEdt, config.getHullSlopePower());
                    }
                }
            }
        }

        float[][] visF = new float[h][w];
        float[][] lumVis = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                visF[y][x] = vis[y][x] ? 1.0f : 0.0f;
                lumVis[y][x] = lum[y][x] * visF[y][x];
            }
        }

        float[][] blurLum = ImageProcessing.gaussianBlur(lumVis, config.getMacroSigma());
        float[][] blurVis = ImageProcessing.gaussianBlur(visF, config.getMacroSigma());
        float[][] macroLum = new float[h][w];
        float minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (vis[y][x]) {
                    float bv = blurVis[y][x];
                    float val = (bv > 1e-4f) ? (blurLum[y][x] / bv) : 0.0f;
                    macroLum[y][x] = val;
                    if (val < minV) minV = val;
                    if (val > maxV) maxV = val;
                }
            }
        }

        if (maxV > minV) {
            float r = maxV - minV;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (vis[y][x]) macroLum[y][x] = (macroLum[y][x] - minV) / r;
                }
            }
        }

        // Spine Proximity
        float centerX = w / 2.0f;
        float[] spineWeights = new float[w];
        for (int x = 0; x < w; x++) {
            float distFromSpine = Math.abs(x - centerX) / Math.max(centerX, 1.0f);
            spineWeights[x] = (float) Math.exp(-0.5 * Math.pow(distFromSpine / config.getSuperstructureSpineFocus(), 2.0));
        }

        float[][] fineLum = ImageProcessing.gaussianBlur(lumVis, 2.5f);
        float[][] coarseLum = ImageProcessing.gaussianBlur(lumVis, 9.0f);
        float[][] towerScore = new float[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (vis[y][x]) {
                    float bandpass = Math.max(0.0f, fineLum[y][x] - coarseLum[y][x]);
                    float lumGate = (lum[y][x] > 0.40f) ? 1.0f : 0.0f;
                    towerScore[y][x] = bandpass * spineWeights[x] * lumGate;
                }
            }
        }

        float[][] superstructurePlateau = ImageProcessing.gaussianBlur(towerScore, 1.5f);
        float pMax = 0.0f;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (vis[y][x] && superstructurePlateau[y][x] > pMax) pMax = superstructurePlateau[y][x];
            }
        }
        if (pMax > 0.0f) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (vis[y][x]) superstructurePlateau[y][x] /= pMax;
                }
            }
        }

        float[][] macroHeight = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (vis[y][x]) {
                    macroHeight[y][x] = config.getHullSlopeWeight() * hullSlope[y][x]
                            + config.getMacroLumWeight() * macroLum[y][x]
                            + config.getSuperstructureWeight() * superstructurePlateau[y][x];
                }
            }
        }

        return macroHeight;
    }

    private static float[][] extractSeamMap(float[][] lum, boolean[][] vis, int w, int h) {
        float[][] blur = ImageProcessing.gaussianBlur(lum, 3.0f);
        float[][] seamMap = new float[h][w];
        boolean[][] seamMask = new boolean[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (vis[y][x]) {
                    float tophat = blur[y][x] - lum[y][x];
                    seamMask[y][x] = tophat > 0.075f;
                }
            }
        }

        float[][] maskFloat = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                maskFloat[y][x] = seamMask[y][x] ? 1.0f : 0.0f;
            }
        }

        return ImageProcessing.gaussianBlur(maskFloat, 0.55f);
    }

    private static float[][] extractChamferProfile(float[][] lum, boolean[][] vis, int w, int h, float radius, float edgeThresh) {
        float[][] mag = ImageProcessing.sobelMagnitude(lum);
        boolean[][] nonEdge = new boolean[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean isEdge = vis[y][x] && (mag[y][x] > edgeThresh);
                nonEdge[y][x] = !isEdge;
            }
        }

        float[][] edt = ImageProcessing.distanceTransformEdt(nonEdge);
        float[][] profile = new float[h][w];
        float r = Math.max(0.1f, radius);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (vis[y][x]) {
                    float norm = Math.min(1.0f, edt[y][x] / r);
                    profile[y][x] = (float) (0.5 * (1.0 - Math.cos(Math.PI * norm)));
                }
            }
        }
        return profile;
    }

    private static BufferedImage generateNormalMap(float[][] height, BufferedImage sprite, boolean[][] vis, GraphicsLibConfig config) {
        int w = sprite.getWidth();
        int h = sprite.getHeight();

        float[][] hEval = (config.getNormalBlurSigma() > 0.0f)
                ? ImageProcessing.gaussianBlur(height, config.getNormalBlurSigma())
                : height;

        float[][] dzdx = ImageProcessing.sobelX(hEval);
        float[][] dzdy = ImageProcessing.sobelY(hEval);
        float depthScale = config.getDepthScale();

        int[] srcPixels = new int[w * h];
        sprite.getRGB(0, 0, w, h, srcPixels, 0, w);
        int[] outPixels = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (!vis[y][x]) {
                    outPixels[idx] = (0 << 24) | (128 << 16) | (128 << 8) | 255;
                    continue;
                }

                float gx = dzdx[y][x] * depthScale;
                float gy = dzdy[y][x] * depthScale;
                float gz = 1.0f;

                float len = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);
                if (len <= 0.0f) len = 1.0f;

                float nx = -gx / len;
                float ny =  gy / len; // OpenGL Y+ convention
                float nz =  gz / len;

                int nr = Math.max(0, Math.min(255, Math.round((nx * 0.5f + 0.5f) * 255.0f)));
                int ng = Math.max(0, Math.min(255, Math.round((ny * 0.5f + 0.5f) * 255.0f)));
                int nb = Math.max(0, Math.min(255, Math.round((nz * 0.5f + 0.5f) * 255.0f)));
                int na = (srcPixels[idx] >> 24) & 0xff;

                outPixels[idx] = (na << 24) | (nr << 16) | (ng << 8) | nb;
            }
        }

        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        res.setRGB(0, 0, w, h, outPixels, 0, w);
        return res;
    }

    private static BufferedImage generateSurfaceMap(BufferedImage sprite, float[][] seamMap, float[][] chamferProfile, boolean[][] vis, GraphicsLibConfig config) {
        int w = sprite.getWidth();
        int h = sprite.getHeight();
        int[] srcPixels = new int[w * h];
        sprite.getRGB(0, 0, w, h, srcPixels, 0, w);
        int[] outPixels = new int[w * h];

        float[] hsv = new float[3];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (!vis[y][x]) {
                    outPixels[idx] = 0;
                    continue;
                }

                int argb = srcPixels[idx];
                int a = (argb >> 24) & 0xff;
                float r = ((argb >> 16) & 0xff) / 255.0f;
                float g = ((argb >> 8) & 0xff) / 255.0f;
                float b = (argb & 0xff) / 255.0f;

                float lum = ColorSpaceTransform.extractLuminance(r, g, b);
                ColorSpaceTransform.rgbToHsv(r, g, b, hsv);
                float sat = hsv[1];
                float val = hsv[2];

                float metalMetric = Math.max(0.0f, Math.min(1.0f, (lum - config.getMetallicLuminanceThreshold()) * 1.6f))
                        * Math.max(0.0f, Math.min(1.0f, 1.0f - sat * config.getMetallicSaturationPenalty()));

                boolean composite = (sat > 0.18f) || (lum < 0.22f);
                float mattePenalty = composite ? 0.35f : 1.0f;

                float specIntensity = Math.max(0.0f, Math.min(1.0f, metalMetric * 0.85f * mattePenalty + 0.18f * chamferProfile[y][x] - 0.60f * seamMap[y][x]));

                boolean emissiveMask = (sat > config.getEmissiveSatThreshold() && val > config.getEmissiveValThreshold())
                        || (lum > config.getEmissiveLumThreshold());
                float emissive = emissiveMask ? Math.max(0.5f, Math.min(1.0f, val)) : 0.0f;

                float specHardness = Math.max(0.10f, Math.min(0.95f, 0.20f + 0.65f * metalMetric + 0.15f * chamferProfile[y][x] - (composite ? 0.20f : 0.0f)));

                if (config.getSurfaceMode() == SurfaceMode.GRAYSCALE) {
                    int gray = Math.round(specIntensity * 255.0f);
                    outPixels[idx] = (a << 24) | (gray << 16) | (gray << 8) | gray;
                } else {
                    int sr = Math.round(emissive * 255.0f);
                    int sg = Math.round(specIntensity * 255.0f);
                    int sb = Math.round(specHardness * 255.0f);
                    outPixels[idx] = (a << 24) | (sr << 16) | (sg << 8) | sb;
                }
            }
        }

        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        res.setRGB(0, 0, w, h, outPixels, 0, w);
        return res;
    }

    private static BufferedImage generateMaterialMap(BufferedImage sprite, float[][] height, float[][] seamMap, boolean[][] vis, GraphicsLibConfig config) {
        int w = sprite.getWidth();
        int h = sprite.getHeight();
        int[] srcPixels = new int[w * h];
        sprite.getRGB(0, 0, w, h, srcPixels, 0, w);
        int[] outPixels = new int[w * h];

        float[][] hSmooth = ImageProcessing.gaussianBlur(height, 5.0f);
        float[] hsv = new float[3];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (!vis[y][x]) {
                    outPixels[idx] = (0 << 24) | (128 << 16) | (0 << 8) | 255;
                    continue;
                }

                int argb = srcPixels[idx];
                int a = (argb >> 24) & 0xff;
                float r = ((argb >> 16) & 0xff) / 255.0f;
                float g = ((argb >> 8) & 0xff) / 255.0f;
                float b = (argb & 0xff) / 255.0f;

                float lum = ColorSpaceTransform.extractLuminance(r, g, b);
                ColorSpaceTransform.rgbToHsv(r, g, b, hsv);
                float sat = hsv[1];

                float metalMetric = Math.max(0.0f, Math.min(1.0f, (lum - config.getMetallicLuminanceThreshold()) * 1.6f))
                        * Math.max(0.0f, Math.min(1.0f, 1.0f - sat * config.getMetallicSaturationPenalty()));
                float metallic = Math.max(0.0f, Math.min(1.0f, metalMetric - 0.45f * seamMap[y][x]));

                float roughness = (1.0f - metallic * (1.0f - config.getPureMetalRoughness())) + 0.25f * seamMap[y][x] + ((sat > 0.20f) ? 0.15f : 0.0f);
                roughness = Math.max(config.getPureMetalRoughness(), Math.min(1.0f, roughness));

                float volumetricCavity = Math.max(0.0f, Math.min(1.0f, hSmooth[y][x] - height[y][x]));
                float ao = 1.0f - (config.getAoVolumetricWeight() * volumetricCavity + config.getAoSeamWeight() * seamMap[y][x]);
                ao = Math.max(0.12f, Math.min(1.0f, ao));

                int mr = Math.round(roughness * 255.0f);
                int mg = Math.round(metallic * 255.0f);
                int mb = Math.round(ao * 255.0f);

                outPixels[idx] = (a << 24) | (mr << 16) | (mg << 8) | mb;
            }
        }

        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        res.setRGB(0, 0, w, h, outPixels, 0, w);
        return res;
    }

    public static BufferedImage createShowcaseGrid(BufferedImage sprite, BufferedImage normal, BufferedImage surface, BufferedImage material, BufferedImage height) {
        int w = sprite.getWidth();
        int h = sprite.getHeight();
        int pad = 12;
        int headerH = 32;

        int count = (height != null) ? 5 : 4;
        int totalW = count * (w + pad) + pad;
        int totalH = h + headerH + pad * 2;

        BufferedImage canvas = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(16, 18, 22, 255));
        g.fillRect(0, 0, totalW, totalH);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));

        BufferedImage[] images;
        String[] labels;
        if (height != null) {
            images = new BufferedImage[]{sprite, height, normal, surface, material};
            labels = new String[]{"Original Sprite", "2.5D Heightfield", "GraphicsLib Normal", "Surface Specular", "Material Map"};
        } else {
            images = new BufferedImage[]{sprite, normal, surface, material};
            labels = new String[]{"Original Sprite", "GraphicsLib Normal", "Surface Specular", "Material Map"};
        }

        for (int i = 0; i < count; i++) {
            int px = pad + i * (w + pad);
            int py = pad + headerH;

            g.setColor(new Color(45, 50, 60, 255));
            g.drawRect(px - 1, py - 1, w + 1, h + 1);

            g.drawImage(images[i], px, py, null);

            g.setColor(new Color(220, 225, 235, 255));
            g.drawString(labels[i], px + 4, pad + 18);
        }

        g.dispose();
        return canvas;
    }
}
