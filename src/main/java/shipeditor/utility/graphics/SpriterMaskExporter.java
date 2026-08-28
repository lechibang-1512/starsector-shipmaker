package shipeditor.utility.graphics;

import lombok.Builder;
import lombok.Data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tartiflette spriter layer mask exporter for Photoshop, GIMP, Krita, and Aseprite.
 * <p>
 * Generates non-destructive working masks from a base unpainted hull sprite:
 * <ul>
 *   <li><b>00_ship_mask.png:</b> Cleaned base hull alpha (holes filled, stray pixels removed).</li>
 *   <li><b>01_paintjob_mask.png:</b> Paintable armor plates vs protected machinery/sockets.</li>
 *   <li><b>02_exposed_mask.png:</b> Photoshop Color Range simulation (Fuzziness: 68).</li>
 *   <li><b>03_cavity_mask.png:</b> Spaces between plates, panel line seams & crevices.</li>
 *   <li><b>04_exposed_paintable_clipped.png:</b> Paintjob mask multiplied by exposed armor.</li>
 *   <li><b>05_unpainted_mask.png:</b> Unpainted structural material (inverse of paintjob).</li>
 * </ul>
 */
public final class SpriterMaskExporter {

    @Data
    @Builder
    public static class MaskExportConfig {
        @Builder.Default private float fuzziness = 68.0f;
        private Color sampleColor;
        @Builder.Default private float paintThreshold = 0.40f;
        @Builder.Default private boolean exportUnpainted = true;
        @Builder.Default private int alphaThreshold = 12;
    }

    @Data
    @Builder
    public static class MaskExportResult {
        private int shipPx;
        private int paintablePx;
        private int exposedPx;
        private int cavityPx;
        private int unpaintedPx;
        private float[] sampledRgb;

        private BufferedImage shipMask;
        private BufferedImage paintjobMask;
        private BufferedImage exposedMask;
        private BufferedImage cavityMask;
        private BufferedImage exposedClippedMask;
        private BufferedImage unpaintedMask;
        private BufferedImage baseSprite;
    }

    private SpriterMaskExporter() {
    }

    public static MaskExportResult exportSpriterMasks(BufferedImage baseSprite, MaskExportConfig config) {
        if (baseSprite == null) return null;
        if (config == null) config = MaskExportConfig.builder().build();

        int w = baseSprite.getWidth();
        int h = baseSprite.getHeight();

        boolean[][] vis = ImageProcessing.extractAlphaMask(baseSprite, config.getAlphaThreshold());
        boolean[][] shipMaskGrid = ImageProcessing.binaryFillHoles(vis);
        float[][] lum = ImageProcessing.extractLuminanceGrid(baseSprite);

        // 1. Paintability Mask
        float[][] paintableGrid = heuristicPaintabilityMask(baseSprite, lum, vis, config.getPaintThreshold());

        // 2. Photoshop Color Range Exposed Mask
        float[] sampleRgb = resolveSampleColor(baseSprite, lum, vis, config);
        float[][] exposedGrid = simulatePhotoshopColorRange(baseSprite, sampleRgb, config.getFuzziness(), shipMaskGrid);

        // 3. Cavity & Recesses Mask
        float[][] cavityGrid = generateCavityRecessesMask(lum, shipMaskGrid, w, h);

        // 4. Clipped Exposed Armor
        float[][] exposedClippedGrid = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                exposedClippedGrid[y][x] = exposedGrid[y][x] * paintableGrid[y][x];
            }
        }

        // 5. Unpainted Mask
        float[][] unpaintedGrid = new float[h][w];
        if (config.isExportUnpainted()) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    unpaintedGrid[y][x] = Math.max(0.0f, Math.min(1.0f, 1.0f - paintableGrid[y][x])) * (shipMaskGrid[y][x] ? 1.0f : 0.0f);
                }
            }
        }

        // Convert grids to BufferedImage masks
        BufferedImage shipMaskImg = createGrayscaleMask(shipMaskGrid, w, h);
        BufferedImage paintjobMaskImg = createGrayscaleMask(paintableGrid, shipMaskGrid, w, h);
        BufferedImage exposedMaskImg = createGrayscaleMask(exposedGrid, shipMaskGrid, w, h);
        BufferedImage cavityMaskImg = createGrayscaleMask(cavityGrid, shipMaskGrid, w, h);
        BufferedImage exposedClippedImg = createGrayscaleMask(exposedClippedGrid, shipMaskGrid, w, h);
        BufferedImage unpaintedMaskImg = config.isExportUnpainted() ? createGrayscaleMask(unpaintedGrid, shipMaskGrid, w, h) : null;

        // Pixel stats
        int shipPx = 0, paintablePx = 0, exposedPx = 0, cavityPx = 0, unpaintedPx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (shipMaskGrid[y][x]) {
                    shipPx++;
                    if (paintableGrid[y][x] > 0.5f) paintablePx++;
                    if (exposedGrid[y][x] > 0.3f) exposedPx++;
                    if (cavityGrid[y][x] > 0.3f) cavityPx++;
                    if (unpaintedGrid[y][x] > 0.5f) unpaintedPx++;
                }
            }
        }

        return MaskExportResult.builder()
                .shipPx(shipPx)
                .paintablePx(paintablePx)
                .exposedPx(exposedPx)
                .cavityPx(cavityPx)
                .unpaintedPx(unpaintedPx)
                .sampledRgb(sampleRgb)
                .shipMask(shipMaskImg)
                .paintjobMask(paintjobMaskImg)
                .exposedMask(exposedMaskImg)
                .cavityMask(cavityMaskImg)
                .exposedClippedMask(exposedClippedImg)
                .unpaintedMask(unpaintedMaskImg)
                .baseSprite(baseSprite)
                .build();
    }

    public static float[][] heuristicPaintabilityMask(BufferedImage baseSprite, float[][] lum, boolean[][] vis, float threshold) {
        int w = baseSprite.getWidth();
        int h = baseSprite.getHeight();

        float[][] blur4 = ImageProcessing.gaussianBlur(lum, 4.0f);
        float[][] cavity = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                cavity[y][x] = Math.max(0.0f, Math.min(1.0f, blur4[y][x] - lum[y][x]));
            }
        }

        float[][] score = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (vis[y][x]) {
                    float s = Math.max(0.0f, Math.min(1.0f, lum[y][x] - (cavity[y][x] * 1.5f)));
                    float paint = Math.max(0.0f, Math.min(1.0f, (s - (threshold - 0.1f)) / 0.2f));
                    score[y][x] = paint;
                }
            }
        }

        float[][] blurred = ImageProcessing.gaussianBlur(score, 0.8f);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!vis[y][x]) blurred[y][x] = 0.0f;
            }
        }
        return blurred;
    }

    public static float[][] simulatePhotoshopColorRange(BufferedImage baseSprite, float[] sampleRgb, float fuzziness, boolean[][] shipMask) {
        int w = baseSprite.getWidth();
        int h = baseSprite.getHeight();

        int[] raw = new int[w * h];
        baseSprite.getRGB(0, 0, w, h, raw, 0, w);

        float[] sampleLab = ColorSpaceTransform.rgbToLab(sampleRgb[0], sampleRgb[1], sampleRgb[2]);
        float radius = (fuzziness / 200.0f) * 85.0f;
        float radiusInv = 1.0f / Math.max(radius, 1e-4f);

        float[][] selection = new float[h][w];
        float[] pxLab = new float[3];

        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                if (!shipMask[y][x]) continue;

                int argb = raw[rowOffset + x];
                float r = ((argb >> 16) & 0xff) / 255.0f;
                float g = ((argb >> 8) & 0xff) / 255.0f;
                float b = (argb & 0xff) / 255.0f;

                ColorSpaceTransform.rgbToLab(r, g, b, pxLab);
                float dist = DeltaEMath.deltaECie76(pxLab, sampleLab);

                float ratio = Math.max(0.0f, Math.min(1.0f, dist * radiusInv));
                selection[y][x] = (float) Math.pow(1.0f - ratio, 1.8);
            }
        }

        return selection;
    }

    private static float[][] generateCavityRecessesMask(float[][] lum, boolean[][] shipMask, int w, int h) {
        float[][] blur4 = ImageProcessing.gaussianBlur(lum, 4.0f);
        float[][] cavity = new float[h][w];
        List<Float> visLums = new ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (shipMask[y][x]) {
                    float cav = Math.max(0.0f, Math.min(1.0f, (blur4[y][x] - lum[y][x]) * 3.5f));
                    cavity[y][x] = cav;
                    visLums.add(lum[y][x]);
                }
            }
        }

        float[][] cavitySoft = ImageProcessing.gaussianBlur(cavity, 0.6f);
        float recessFloor = 0.45f;
        if (!visLums.isEmpty()) {
            Collections.sort(visLums);
            int idx45 = (int) (visLums.size() * 0.45);
            recessFloor = visLums.get(Math.min(idx45, visLums.size() - 1));
        }

        float[][] out = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (shipMask[y][x]) {
                    float recessShadow = Math.max(0.0f, Math.min(1.0f, (recessFloor - lum[y][x]) / 0.25f));
                    out[y][x] = Math.max(0.0f, Math.min(1.0f, cavitySoft[y][x] + recessShadow * 0.7f));
                }
            }
        }
        return out;
    }

    private static float[] resolveSampleColor(BufferedImage baseSprite, float[][] lum, boolean[][] vis, MaskExportConfig config) {
        if (config.getSampleColor() != null) {
            Color c = config.getSampleColor();
            return new float[]{c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f};
        }

        // Auto-sample dominant flat armor
        int w = baseSprite.getWidth();
        int h = baseSprite.getHeight();
        int[] raw = new int[w * h];
        baseSprite.getRGB(0, 0, w, h, raw, 0, w);

        List<Float> visLums = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (vis[y][x]) visLums.add(lum[y][x]);
            }
        }

        if (visLums.isEmpty()) return new float[]{0.5f, 0.5f, 0.5f};
        Collections.sort(visLums);
        float p30 = visLums.get((int) (visLums.size() * 0.30));
        float p85 = visLums.get((int) (visLums.size() * 0.85));

        List<Float> rs = new ArrayList<>();
        List<Float> gs = new ArrayList<>();
        List<Float> bs = new ArrayList<>();

        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                if (vis[y][x] && lum[y][x] >= p30 && lum[y][x] <= p85) {
                    int argb = raw[rowOffset + x];
                    rs.add(((argb >> 16) & 0xff) / 255.0f);
                    gs.add(((argb >> 8) & 0xff) / 255.0f);
                    bs.add((argb & 0xff) / 255.0f);
                }
            }
        }

        if (rs.isEmpty()) {
            return new float[]{0.5f, 0.5f, 0.5f};
        }

        Collections.sort(rs);
        Collections.sort(gs);
        Collections.sort(bs);
        return new float[]{
                rs.get(rs.size() / 2),
                gs.get(gs.size() / 2),
                bs.get(bs.size() / 2)
        };
    }

    private static BufferedImage createGrayscaleMask(boolean[][] mask, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int val = mask[y][x] ? 255 : 0;
                int rgb = (val << 16) | (val << 8) | val;
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    private static BufferedImage createGrayscaleMask(float[][] grid, boolean[][] mask, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int val = mask[y][x] ? Math.max(0, Math.min(255, Math.round(grid[y][x] * 255.0f))) : 0;
                int rgb = (val << 16) | (val << 8) | val;
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }
}
