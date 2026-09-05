package shipeditor.utility.graphics;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * Traces sprite alpha boundaries for UI highlighting.
 * <p>
 * This class produces pixel-accurate contour outlines from a sprite's opaque pixels,
 * intended for visual selection indicators (e.g., module selection highlights in the viewer).
 * The output is <b>not</b> suitable for game data export — use {@link CollisionHullGenerator}
 * for Starsector {@code .ship} collision bounds instead.
 * <p>
 * Algorithm:
 * <ol>
 *   <li>Collect all opaque pixels (alpha &gt; threshold).</li>
 *   <li>Moore-Neighbor Tracing to extract the boundary contour.</li>
 *   <li>Ramer-Douglas-Peucker simplification to remove staircase pixel artifacts
 *       while preserving sharp features (epsilon = 1.0px).</li>
 * </ol>
 * The result is in <b>local pixel coordinates</b> {@code [0, width] × [0, height]}.
 * Callers must apply their own anchor offset and rotation transforms.
 *
 * @see CollisionHullGenerator
 */
public final class SpriteOutlineTracer {

    private static final int ALPHA_THRESHOLD = 10;

    private SpriteOutlineTracer() {
    }

    /**
     * Generates an exact contour of the sprite's opaque pixels in local pixel coordinates.
     *
     * @param image The sprite image to trace.
     * @return A list of Point2D vertices forming the contour polygon, or an empty list
     *         if the image is null or fully transparent.
     */
    public static List<Point2D> generateExactContour(BufferedImage image) {
        if (image == null) return Collections.emptyList();

        int width = image.getWidth();
        int height = image.getHeight();

        int[] rgbArray = image.getRGB(0, 0, width, height, null, 0, width);

        // Build opaque pixel grid from bulk array
        boolean[][] opaque = new boolean[height][width];
        boolean hasOpaque = false;
        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int alpha = (rgbArray[rowOffset + x] >> 24) & 0xff;
                if (alpha > ALPHA_THRESHOLD) {
                    opaque[y][x] = true;
                    hasOpaque = true;
                }
            }
        }

        if (!hasOpaque) return Collections.emptyList();

        List<int[]> contour = SpriteContourTracer.traceBoundary(opaque, width, height);
        if (contour.isEmpty()) return Collections.emptyList();

        // Simplify slightly to eliminate staircase pixel artifacts while preserving exact sharp features
        return PolygonSimplifier.simplifyPolygon(contour, 1.0);
    }
}
