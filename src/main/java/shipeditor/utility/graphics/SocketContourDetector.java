package shipeditor.utility.graphics;

import lombok.Builder;
import lombok.Data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Computer vision socket and turret mount contour detector with bilateral symmetry verification.
 */
public final class SocketContourDetector {

    @Data
    @Builder
    public static class SocketCandidate {
        private int x;
        private int y;
        private int radius;
        private String size;
        private float score;
    }

    private SocketContourDetector() {
    }

    public static List<SocketCandidate> detectWeaponSockets(BufferedImage sprite, float minConfidence, boolean enforceSymmetry) {
        if (sprite == null) return Collections.emptyList();
        int w = sprite.getWidth();
        int h = sprite.getHeight();

        boolean[][] vis = ImageProcessing.extractAlphaMask(sprite, 20);
        float[][] lum = ImageProcessing.extractLuminanceGrid(sprite);
        float centerX = w / 2.0f;

        List<SocketCandidate> candidates = new ArrayList<>();

        Object[][] ranges = {
                {"SMALL", 6, 12},
                {"MEDIUM", 14, 22},
                {"LARGE", 24, 36}
        };

        for (Object[] range : ranges) {
            String sizeName = (String) range[0];
            int rMin = (int) range[1];
            int rMax = (int) range[2];

            for (int r = rMin; r <= rMax; r += 2) {
                float[][] ringResponse = convolveRing(lum, r, w, h);
                float[][] centerResponse = convolveCenter(lum, r, w, h);

                for (int y = r + 2; y < h - r - 2; y++) {
                    for (int x = r + 2; x < w - r - 2; x++) {
                        if (!vis[y][x]) continue;

                        float contrast = ringResponse[y][x] - centerResponse[y][x];
                        if (contrast >= minConfidence) {
                            // Check local peak in (2r+1) neighborhood
                            if (isLocalPeak(ringResponse, centerResponse, x, y, r, contrast, w, h)) {
                                candidates.add(SocketCandidate.builder()
                                        .x(x)
                                        .y(y)
                                        .radius(r)
                                        .size(sizeName)
                                        .score(contrast)
                                        .build());
                            }
                        }
                    }
                }
            }
        }

        // Bilateral symmetry reinforcement
        if (enforceSymmetry) {
            for (SocketCandidate cand : candidates) {
                float mirrorX = 2.0f * centerX - cand.getX();
                float mirrorY = cand.getY();

                if (Math.abs(cand.getX() - centerX) < 4.0f) {
                    cand.setScore(cand.getScore() * 1.15f);
                } else {
                    boolean hasPartner = false;
                    for (SocketCandidate other : candidates) {
                        if (other != cand && other.getSize().equals(cand.getSize())) {
                            double dist = Math.hypot(other.getX() - mirrorX, other.getY() - mirrorY);
                            if (dist < cand.getRadius() * 0.75) {
                                hasPartner = true;
                                break;
                            }
                        }
                    }
                    if (hasPartner) {
                        cand.setScore(cand.getScore() * 1.25f);
                    }
                }
            }
        }

        // Non-maximum suppression
        candidates.sort(Comparator.comparingDouble(SocketCandidate::getScore).reversed());
        List<SocketCandidate> finalMounts = new ArrayList<>();

        for (SocketCandidate cand : candidates) {
            boolean overlap = false;
            for (SocketCandidate chosen : finalMounts) {
                double d = Math.hypot(cand.getX() - chosen.getX(), cand.getY() - chosen.getY());
                if (d < Math.max(cand.getRadius(), chosen.getRadius()) * 0.75) {
                    overlap = true;
                    break;
                }
            }
            if (!overlap) {
                finalMounts.add(cand);
            }
        }

        return finalMounts;
    }

    public static BufferedImage generateSocketProtectionMask(BufferedImage sprite, List<SocketCandidate> sockets, int padding) {
        if (sprite == null) return null;
        int w = sprite.getWidth();
        int h = sprite.getHeight();

        BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = mask.createGraphics();
        g.setColor(Color.WHITE);

        if (sockets != null) {
            for (SocketCandidate s : sockets) {
                int r = s.getRadius() + padding;
                g.fillOval(s.getX() - r, s.getY() - r, r * 2, r * 2);
            }
        }

        g.dispose();
        return mask;
    }

    private static float[][] convolveRing(float[][] lum, int r, int w, int h) {
        float[][] out = new float[h][w];
        int count = 0;
        List<int[]> offsets = new ArrayList<>();

        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                double dist = Math.hypot(dx, dy);
                if (dist >= (r - 1.2) && dist <= (r + 1.2)) {
                    offsets.add(new int[]{dx, dy});
                    count++;
                }
            }
        }

        if (count == 0) return out;
        float inv = 1.0f / count;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float sum = 0.0f;
                for (int[] off : offsets) {
                    int px = Math.min(Math.max(x + off[0], 0), w - 1);
                    int py = Math.min(Math.max(y + off[1], 0), h - 1);
                    sum += lum[py][px];
                }
                out[y][x] = sum * inv;
            }
        }
        return out;
    }

    private static float[][] convolveCenter(float[][] lum, int r, int w, int h) {
        float[][] out = new float[h][w];
        int count = 0;
        List<int[]> offsets = new ArrayList<>();
        double innerR = r * 0.50;

        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                double dist = Math.hypot(dx, dy);
                if (dist <= innerR) {
                    offsets.add(new int[]{dx, dy});
                    count++;
                }
            }
        }

        if (count == 0) return out;
        float inv = 1.0f / count;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float sum = 0.0f;
                for (int[] off : offsets) {
                    int px = Math.min(Math.max(x + off[0], 0), w - 1);
                    int py = Math.min(Math.max(y + off[1], 0), h - 1);
                    sum += lum[py][px];
                }
                out[y][x] = sum * inv;
            }
        }
        return out;
    }

    private static boolean isLocalPeak(float[][] ring, float[][] center, int cx, int cy, int r, float val, int w, int h) {
        int checkRadius = Math.max(1, r / 2);
        for (int dy = -checkRadius; dy <= checkRadius; dy++) {
            for (int dx = -checkRadius; dx <= checkRadius; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = cx + dx;
                int ny = cy + dy;
                if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                    float otherVal = ring[ny][nx] - center[ny][nx];
                    if (otherVal > val) return false;
                }
            }
        }
        return true;
    }
}
