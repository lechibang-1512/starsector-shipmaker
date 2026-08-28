package shipeditor.utility.graphics;

import lombok.Builder;
import lombok.Data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Fast perceptually-accurate color distribution and palette analyzer for Starsector sprites.
 */
public final class PaletteAnalyzer {

    @Data
    @Builder
    public static class HueSector {
        private String name;
        private int loDeg;
        private int hiDeg;
        private int pixelCount;
        private float percentage;
        private Color avgColor;
        private float avgH;
        private float avgS;
        private float avgV;
    }

    @Data
    @Builder
    public static class LuminanceBand {
        private String name;
        private float loVal;
        private float hiVal;
        private int pixelCount;
        private float percentage;
        private float avgV;
    }

    @Data
    @Builder
    public static class PaletteReport {
        private int totalOpaquePixels;
        private int achromaticPixels;
        private float achromaticPercentage;
        private int chromaticPixels;
        private float chromaticPercentage;
        private List<HueSector> hueSectors;
        private List<LuminanceBand> luminanceBands;
    }

    private static final String[][] HUE_SECTOR_SPECS = {
            {"Red / Engine Glow", "0", "15"},
            {"Terracotta / Brown-Red", "15", "30"},
            {"Ochre / Bronze Gold", "30", "50"},
            {"Yellow / Hazard", "50", "70"},
            {"Yellow-Green", "70", "90"},
            {"Olive / Green Tint", "90", "150"},
            {"Teal / Cyan", "150", "190"},
            {"Cockpit / Blue Sensor", "190", "250"},
            {"Purple / Exotic", "250", "300"},
            {"Magenta / Hot", "300", "345"},
            {"Deep Red Wrap", "345", "360"}
    };

    private static final Object[][] LUMINANCE_SPECS = {
            {"Shadow / Void", 0.00f, 0.15f},
            {"Dark Machinery / Chassis", 0.15f, 0.30f},
            {"Mid-tone Armor Plate", 0.30f, 0.55f},
            {"Light Armor / Paint", 0.55f, 0.75f},
            {"Bright Metal / Highlight", 0.75f, 1.00f}
    };

    private PaletteAnalyzer() {
    }

    public static PaletteReport analyzeSprite(BufferedImage image) {
        if (image == null) return null;
        int w = image.getWidth();
        int h = image.getHeight();

        int[] raw = new int[w * h];
        image.getRGB(0, 0, w, h, raw, 0, w);

        boolean[][] vis = ImageProcessing.extractAlphaMask(image, 15);
        int totalOpaque = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (vis[y][x]) totalOpaque++;
            }
        }

        if (totalOpaque == 0) {
            return PaletteReport.builder()
                    .totalOpaquePixels(0)
                    .hueSectors(new ArrayList<>())
                    .luminanceBands(new ArrayList<>())
                    .build();
        }

        float[] hsv = new float[3];
        int achromaticCount = 0;
        int chromaticCount = 0;

        List<float[]> chromaticPixels = new ArrayList<>(totalOpaque);
        List<float[]> allOpaquePixels = new ArrayList<>(totalOpaque);

        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                if (!vis[y][x]) continue;

                int argb = raw[rowOffset + x];
                float r = ((argb >> 16) & 0xff) / 255.0f;
                float g = ((argb >> 8) & 0xff) / 255.0f;
                float b = (argb & 0xff) / 255.0f;

                ColorSpaceTransform.rgbToHsv(r, g, b, hsv);
                float hDeg = hsv[0] * 360.0f;
                float sat = hsv[1];
                float val = hsv[2];

                float[] pData = new float[]{r, g, b, hDeg, sat, val};
                allOpaquePixels.add(pData);

                if (sat < 0.06f) {
                    achromaticCount++;
                } else {
                    chromaticCount++;
                    chromaticPixels.add(pData);
                }
            }
        }

        // Hue sectors
        List<HueSector> sectors = new ArrayList<>();
        for (String[] spec : HUE_SECTOR_SPECS) {
            String name = spec[0];
            int lo = Integer.parseInt(spec[1]);
            int hi = Integer.parseInt(spec[2]);

            float sumR = 0, sumG = 0, sumB = 0, sumH = 0, sumS = 0, sumV = 0;
            int count = 0;

            for (float[] p : chromaticPixels) {
                float hDeg = p[3];
                boolean inSector = (lo < hi) ? (hDeg >= lo && hDeg < hi) : (hDeg >= lo || hDeg < hi);
                if (inSector) {
                    count++;
                    sumR += p[0]; sumG += p[1]; sumB += p[2];
                    sumH += p[3]; sumS += p[4]; sumV += p[5];
                }
            }

            if (count > 0) {
                float avgR = sumR / count;
                float avgG = sumG / count;
                float avgB = sumB / count;
                sectors.add(HueSector.builder()
                        .name(name)
                        .loDeg(lo)
                        .hiDeg(hi)
                        .pixelCount(count)
                        .percentage(count * 100.0f / totalOpaque)
                        .avgColor(new Color(Math.min(1.0f, avgR), Math.min(1.0f, avgG), Math.min(1.0f, avgB)))
                        .avgH(sumH / count)
                        .avgS(sumS / count)
                        .avgV(sumV / count)
                        .build());
            }
        }

        // Luminance bands
        List<LuminanceBand> bands = new ArrayList<>();
        for (Object[] spec : LUMINANCE_SPECS) {
            String name = (String) spec[0];
            float lo = (float) spec[1];
            float hi = (float) spec[2];

            float sumV = 0;
            int count = 0;

            for (float[] p : allOpaquePixels) {
                float val = p[5];
                if (val >= lo && val < hi) {
                    count++;
                    sumV += val;
                }
            }

            if (count > 0) {
                bands.add(LuminanceBand.builder()
                        .name(name)
                        .loVal(lo)
                        .hiVal(hi)
                        .pixelCount(count)
                        .percentage(count * 100.0f / totalOpaque)
                        .avgV(sumV / count)
                        .build());
            }
        }

        return PaletteReport.builder()
                .totalOpaquePixels(totalOpaque)
                .achromaticPixels(achromaticCount)
                .achromaticPercentage(achromaticCount * 100.0f / totalOpaque)
                .chromaticPixels(chromaticCount)
                .chromaticPercentage(chromaticCount * 100.0f / totalOpaque)
                .hueSectors(sectors)
                .luminanceBands(bands)
                .build();
    }
}
