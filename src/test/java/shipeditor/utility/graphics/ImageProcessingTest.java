package shipeditor.utility.graphics;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class ImageProcessingTest {

    @Test
    public void testGaussianBlur() {
        float[][] grid = new float[32][32];
        grid[16][16] = 1.0f;

        float[][] blurred = ImageProcessing.gaussianBlur(grid, 2.0f);
        assertNotNull(blurred);
        assertTrue(blurred[16][16] > 0.0f);
        assertTrue(blurred[16][17] > 0.0f);
        assertTrue(blurred[16][16] > blurred[16][18]);
    }

    @Test
    public void testSobelGradients() {
        float[][] step = new float[16][16];
        for (int y = 0; y < 16; y++) {
            for (int x = 8; x < 16; x++) {
                step[y][x] = 1.0f;
            }
        }

        float[][] gx = ImageProcessing.sobelX(step);
        float[][] gy = ImageProcessing.sobelY(step);

        assertTrue(gx[8][8] > 0.0f, "Sobel X should detect vertical edge step");
        assertEquals(0.0f, gy[8][8], 0.001f, "Sobel Y should be zero on vertical step");
    }

    @Test
    public void testExactEdt() {
        boolean[][] mask = new boolean[16][16];
        mask[8][8] = true; // single true pixel at (8,8)

        float[][] edt = ImageProcessing.distanceTransformEdt(mask);
        assertEquals(0.0f, edt[8][8], 0.0001f, "Origin pixel should have 0 distance");
        assertEquals(1.0f, edt[8][9], 0.0001f, "Adjacent pixel should have 1.0 distance");
        assertEquals(1.0f, edt[9][8], 0.0001f, "Adjacent pixel should have 1.0 distance");
        assertEquals((float) Math.sqrt(2.0), edt[9][9], 0.0001f, "Diagonal pixel should have sqrt(2) distance");
    }

    @Test
    public void testBinaryFillHoles() {
        boolean[][] ring = new boolean[10][10];
        for (int i = 2; i <= 7; i++) {
            ring[2][i] = true;
            ring[7][i] = true;
            ring[i][2] = true;
            ring[i][7] = true;
        }

        assertFalse(ring[4][4], "Center should originally be hole (false)");
        boolean[][] filled = ImageProcessing.binaryFillHoles(ring);
        assertTrue(filled[4][4], "Center should be filled (true)");
        assertFalse(filled[0][0], "Outside background should remain false");
    }

    @Test
    public void testMorphology() {
        boolean[][] single = new boolean[10][10];
        single[5][5] = true;

        boolean[][] dilated = ImageProcessing.binaryDilation(single, 1);
        assertTrue(dilated[5][5]);
        assertTrue(dilated[4][5]);
        assertTrue(dilated[6][5]);
        assertTrue(dilated[5][4]);
        assertTrue(dilated[5][6]);

        boolean[][] eroded = ImageProcessing.binaryErosion(dilated, 1);
        assertTrue(eroded[5][5]);
        assertFalse(eroded[4][5]);
    }
}
