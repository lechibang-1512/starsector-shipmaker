package shipeditor.utility.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class providing Moore-Neighbor contour tracing with Jacob's stopping criterion.
 */
public final class SpriteContourTracer {

    private SpriteContourTracer() {
    }

    /**
     * Moore-Neighbor boundary tracing using direct grid lookups.
     * Returns contour as list of [x, y] int pairs — no Point object allocation.
     *
     * @param grid   2D boolean grid representing solid pixels
     * @param width  grid width
     * @param height grid height
     * @return list of [x, y] coordinates forming the contour
     */
    public static List<int[]> traceBoundary(boolean[][] grid, int width, int height) {
        // Find starting pixel (top-leftmost) — first true cell in row-major order
        int startX = -1;
        int startY = -1;
        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grid[y][x]) {
                    startX = x;
                    startY = y;
                    break outer;
                }
            }
        }

        if (startX < 0) {
            return Collections.emptyList();
        }

        List<int[]> contour = new ArrayList<>();

        // Directions: Clockwise from N
        int[] dxDir = { 0, 1, 1, 1, 0, -1, -1, -1 };
        int[] dyDir = { -1, -1, 0, 1, 1, 1, 0, -1 };

        int curX = startX;
        int curY = startY;
        int enterDir = 6; // West, since start is top-leftmost

        int secondX = -1;
        int secondY = -1;

        int safetyLimit = width * height * 2;

        while (true) {
            contour.add(new int[]{curX, curY});
            boolean found = false;

            int checkDir = enterDir;

            for (int i = 0; i < 8; i++) {
                int nx = curX + dxDir[checkDir];
                int ny = curY + dyDir[checkDir];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height && grid[ny][nx]) {
                    curX = nx;
                    curY = ny;
                    enterDir = (checkDir + 5) % 8; // Start searching next from relative "behind-left"
                    found = true;
                    break;
                }
                checkDir = (checkDir + 1) % 8;
            }

            if (!found) {
                break;
            }

            if (secondX < 0) {
                secondX = curX;
                secondY = curY;
            } else if (contour.size() > 1) {
                int[] last = contour.get(contour.size() - 1);
                if (last[0] == startX && last[1] == startY && curX == secondX && curY == secondY) {
                    contour.remove(contour.size() - 1);
                    break; // Jacob's stopping criterion met
                }
            }

            if (contour.size() > safetyLimit) {
                break; // Infinite loop safety
            }
        }

        return contour;
    }
}
