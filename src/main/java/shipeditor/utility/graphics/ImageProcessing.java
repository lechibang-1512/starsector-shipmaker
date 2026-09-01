package shipeditor.utility.graphics;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/**
 * High-performance 2D image processing utilities:
 * Gaussian blur, Sobel gradients, exact Euclidean Distance Transform (EDT),
 * morphological operators (dilation, erosion, opening, hole filling), and guided filters.
 */
public final class ImageProcessing {

    private static final float INF = 1e20f;

    private ImageProcessing() {
    }

    // =========================================================================
    // 1. Separable Gaussian Blur
    // =========================================================================

    public static float[][] gaussianBlur(float[][] input, float sigma) {
        if (sigma <= 0.01f) {
            return cloneGrid(input);
        }

        int h = input.length;
        int w = input[0].length;
        int radius = Math.max(1, (int) Math.ceil(sigma * 3.0f));
        float[] kernel = createGaussianKernel(radius, sigma);

        float[][] temp = new float[h][w];
        float[][] output = new float[h][w];

        // Horizontal pass
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float sum = 0.0f;
                for (int k = -radius; k <= radius; k++) {
                    int px = Math.min(Math.max(x + k, 0), w - 1);
                    sum += input[y][px] * kernel[k + radius];
                }
                temp[y][x] = sum;
            }
        }

        // Vertical pass
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float sum = 0.0f;
                for (int k = -radius; k <= radius; k++) {
                    int py = Math.min(Math.max(y + k, 0), h - 1);
                    sum += temp[py][x] * kernel[k + radius];
                }
                output[y][x] = sum;
            }
        }

        return output;
    }

    private static float[] createGaussianKernel(int radius, float sigma) {
        int size = 2 * radius + 1;
        float[] kernel = new float[size];
        float sum = 0.0f;
        double twoSigmaSq = 2.0 * sigma * sigma;

        for (int i = -radius; i <= radius; i++) {
            float val = (float) Math.exp(-(i * i) / twoSigmaSq);
            kernel[i + radius] = val;
            sum += val;
        }

        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }

    // =========================================================================
    // 2. Sobel & Spatial Derivatives
    // =========================================================================

    public static float[][] sobelX(float[][] input) {
        int h = input.length;
        int w = input[0].length;
        float[][] out = new float[h][w];

        for (int y = 0; y < h; y++) {
            int ym = Math.max(0, y - 1);
            int yp = Math.min(h - 1, y + 1);
            for (int x = 0; x < w; x++) {
                int xm = Math.max(0, x - 1);
                int xp = Math.min(w - 1, x + 1);

                float gx = (input[ym][xp] + 2.0f * input[y][xp] + input[yp][xp])
                         - (input[ym][xm] + 2.0f * input[y][xm] + input[yp][xm]);
                out[y][x] = gx / 8.0f;
            }
        }
        return out;
    }

    public static float[][] sobelY(float[][] input) {
        int h = input.length;
        int w = input[0].length;
        float[][] out = new float[h][w];

        for (int y = 0; y < h; y++) {
            int ym = Math.max(0, y - 1);
            int yp = Math.min(h - 1, y + 1);
            for (int x = 0; x < w; x++) {
                int xm = Math.max(0, x - 1);
                int xp = Math.min(w - 1, x + 1);

                float gy = (input[yp][xm] + 2.0f * input[yp][x] + input[yp][xp])
                         - (input[ym][xm] + 2.0f * input[ym][x] + input[ym][xp]);
                out[y][x] = gy / 8.0f;
            }
        }
        return out;
    }

    public static float[][] sobelMagnitude(float[][] input) {
        int h = input.length;
        int w = input[0].length;
        float[][] gx = sobelX(input);
        float[][] gy = sobelY(input);
        float[][] out = new float[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out[y][x] = (float) Math.sqrt(gx[y][x] * gx[y][x] + gy[y][x] * gy[y][x]);
            }
        }
        return out;
    }

    // =========================================================================
    // 3. Exact Euclidean Distance Transform (EDT) via Felzenszwalb-Huttenlocher
    // =========================================================================

    public static float[][] distanceTransformEdt(boolean[][] mask) {
        int h = mask.length;
        int w = mask[0].length;
        float[][] d = new float[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                d[y][x] = mask[y][x] ? 0.0f : INF;
            }
        }

        // 1D EDT along columns
        float[] col = new float[h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                col[y] = d[y][x];
            }
            float[] dtCol = edt1D(col, h);
            for (int y = 0; y < h; y++) {
                d[y][x] = dtCol[y];
            }
        }

        // 1D EDT along rows
        float[] row = new float[w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                row[x] = d[y][x];
            }
            float[] dtRow = edt1D(row, w);
            for (int x = 0; x < w; x++) {
                d[y][x] = (float) Math.sqrt(dtRow[x]);
            }
        }

        return d;
    }

    private static float[] edt1D(float[] f, int n) {
        float[] d = new float[n];
        int[] v = new int[n];
        float[] z = new float[n + 1];
        int k = 0;
        v[0] = 0;
        z[0] = -INF;
        z[1] = INF;

        for (int q = 1; q < n; q++) {
            float s = ((f[q] + (float) q * q) - (f[v[k]] + (float) v[k] * v[k])) / (2.0f * q - 2.0f * v[k]);
            while (s <= z[k]) {
                k--;
                s = ((f[q] + (float) q * q) - (f[v[k]] + (float) v[k] * v[k])) / (2.0f * q - 2.0f * v[k]);
            }
            k++;
            v[k] = q;
            z[k] = s;
            z[k + 1] = INF;
        }

        k = 0;
        for (int q = 0; q < n; q++) {
            while (z[k + 1] < q) {
                k++;
            }
            float diff = q - v[k];
            d[q] = diff * diff + f[v[k]];
        }
        return d;
    }

    // =========================================================================
    // 4. Morphological Operators & Hole Filling
    // =========================================================================

    public static boolean[][] binaryFillHoles(boolean[][] mask) {
        int h = mask.length;
        int w = mask[0].length;
        boolean[][] visited = new boolean[h][w];
        Queue<int[]> queue = new ArrayDeque<>();

        // Seed boundary transparent pixels
        for (int x = 0; x < w; x++) {
            if (!mask[0][x]) { visited[0][x] = true; queue.add(new int[]{x, 0}); }
            if (!mask[h - 1][x]) { visited[h - 1][x] = true; queue.add(new int[]{x, h - 1}); }
        }
        for (int y = 0; y < h; y++) {
            if (!mask[y][0] && !visited[y][0]) { visited[y][0] = true; queue.add(new int[]{0, y}); }
            if (!mask[y][w - 1] && !visited[y][w - 1]) { visited[y][w - 1] = true; queue.add(new int[]{w - 1, y}); }
        }

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] pt = queue.poll();
            int px = pt[0], py = pt[1];

            for (int[] dir : dirs) {
                int nx = px + dir[0];
                int ny = py + dir[1];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[ny][nx] && !mask[ny][nx]) {
                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        // Holes are unvisited non-boundary connected pixels
        boolean[][] out = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out[y][x] = !visited[y][x];
            }
        }
        return out;
    }

    public static boolean[][] binaryDilation(boolean[][] mask, int radius) {
        int h = mask.length;
        int w = mask[0].length;
        boolean[][] out = new boolean[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (mask[y][x]) {
                    int yMin = Math.max(0, y - radius);
                    int yMax = Math.min(h - 1, y + radius);
                    int xMin = Math.max(0, x - radius);
                    int xMax = Math.min(w - 1, x + radius);
                    for (int ny = yMin; ny <= yMax; ny++) {
                        for (int nx = xMin; nx <= xMax; nx++) {
                            out[ny][nx] = true;
                        }
                    }
                }
            }
        }
        return out;
    }

    public static boolean[][] binaryErosion(boolean[][] mask, int radius) {
        int h = mask.length;
        int w = mask[0].length;
        boolean[][] out = new boolean[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean allTrue = true;
                int yMin = Math.max(0, y - radius);
                int yMax = Math.min(h - 1, y + radius);
                int xMin = Math.max(0, x - radius);
                int xMax = Math.min(w - 1, x + radius);

                for (int ny = yMin; ny <= yMax && allTrue; ny++) {
                    for (int nx = xMin; nx <= xMax && allTrue; nx++) {
                        if (!mask[ny][nx]) {
                            allTrue = false;
                        }
                    }
                }
                out[y][x] = allTrue;
            }
        }
        return out;
    }

    public static boolean[][] binaryOpening(boolean[][] mask, int radius) {
        return binaryDilation(binaryErosion(mask, radius), radius);
    }

    // =========================================================================
    // 5. Box Filter & Guided Filter (He et al.)
    // =========================================================================

    public static float[][] boxFilter(float[][] img, int r) {
        int h = img.length;
        int w = img[0].length;
        float[][] temp = new float[h][w];
        float[][] out = new float[h][w];
        int size = 2 * r + 1;

        // Horizontal pass
        for (int y = 0; y < h; y++) {
            float sum = 0.0f;
            for (int x = -r; x <= r; x++) {
                int px = Math.min(Math.max(x, 0), w - 1);
                sum += img[y][px];
            }
            temp[y][0] = sum / size;
            for (int x = 1; x < w; x++) {
                int pxOut = Math.min(Math.max(x - r - 1, 0), w - 1);
                int pxIn = Math.min(Math.max(x + r, 0), w - 1);
                sum += img[y][pxIn] - img[y][pxOut];
                temp[y][x] = sum / size;
            }
        }

        // Vertical pass
        for (int x = 0; x < w; x++) {
            float sum = 0.0f;
            for (int y = -r; y <= r; y++) {
                int py = Math.min(Math.max(y, 0), h - 1);
                sum += temp[py][x];
            }
            out[0][x] = sum / size;
            for (int y = 1; y < h; y++) {
                int pyOut = Math.min(Math.max(y - r - 1, 0), h - 1);
                int pyIn = Math.min(Math.max(y + r, 0), h - 1);
                sum += temp[pyIn][x] - temp[pyOut][x];
                out[y][x] = sum / size;
            }
        }

        return out;
    }

    public static float[][] guidedFilter(float[][] p, float[][] I, int r, float eps) {
        int h = p.length;
        int w = p[0].length;

        float[][] meanI = boxFilter(I, r);
        float[][] meanP = boxFilter(p, r);

        float[][] Ip = new float[h][w];
        float[][] II = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Ip[y][x] = I[y][x] * p[y][x];
                II[y][x] = I[y][x] * I[y][x];
            }
        }

        float[][] meanIp = boxFilter(Ip, r);
        float[][] meanII = boxFilter(II, r);

        float[][] a = new float[h][w];
        float[][] b = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float covIp = meanIp[y][x] - meanI[y][x] * meanP[y][x];
                float varI = meanII[y][x] - meanI[y][x] * meanI[y][x];
                a[y][x] = covIp / (varI + eps);
                b[y][x] = meanP[y][x] - a[y][x] * meanI[y][x];
            }
        }

        float[][] meanA = boxFilter(a, r);
        float[][] meanB = boxFilter(b, r);

        float[][] q = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float val = meanA[y][x] * I[y][x] + meanB[y][x];
                q[y][x] = Math.max(0.0f, Math.min(1.0f, val));
            }
        }

        return q;
    }

    public static float[][] edgeAwareFeather(boolean[][] mask, float[][] guideLum, float radius, boolean preserveInterior) {
        int h = mask.length;
        int w = mask[0].length;
        float[][] p = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                p[y][x] = mask[y][x] ? 1.0f : 0.0f;
            }
        }

        int r = Math.max(1, Math.round(radius));
        float[][] filtered = guidedFilter(p, guideLum, r, 1e-3f);

        if (preserveInterior) {
            boolean[][] interior = binaryErosion(mask, 1);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (interior[y][x]) {
                        filtered[y][x] = Math.max(filtered[y][x], 1.0f);
                    }
                }
            }
        }

        return filtered;
    }

    // =========================================================================
    // 6. Helpers: BufferedImage conversions & grid clones
    // =========================================================================

    public static float[][] cloneGrid(float[][] grid) {
        int h = grid.length;
        float[][] copy = new float[h][];
        for (int y = 0; y < h; y++) {
            copy[y] = Arrays.copyOf(grid[y], grid[y].length);
        }
        return copy;
    }

    public static float[][] extractLuminanceGrid(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] rgbArray = new int[w * h];
        image.getRGB(0, 0, w, h, rgbArray, 0, w);

        float[][] lum = new float[h][w];
        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                int argb = rgbArray[rowOffset + x];
                float r = ((argb >> 16) & 0xff) / 255.0f;
                float g = ((argb >> 8) & 0xff) / 255.0f;
                float b = (argb & 0xff) / 255.0f;
                lum[y][x] = ColorSpaceTransform.extractLuminance(r, g, b);
            }
        }
        return lum;
    }

    public static boolean[][] extractAlphaMask(BufferedImage image, int threshold) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] rgbArray = new int[w * h];
        image.getRGB(0, 0, w, h, rgbArray, 0, w);

        boolean[][] mask = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                int a = (rgbArray[rowOffset + x] >> 24) & 0xff;
                mask[y][x] = a > threshold;
            }
        }
        return mask;
    }
}
