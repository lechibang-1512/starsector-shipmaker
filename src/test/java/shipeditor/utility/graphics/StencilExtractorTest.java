package shipeditor.utility.graphics;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class StencilExtractorTest {

    private BufferedImage createBaseDestroyer() {
        BufferedImage img = new BufferedImage(96, 192, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(new Color(125, 125, 130, 255));
        g.fillRoundRect(16, 20, 64, 152, 16, 16);

        // Seams
        g.setColor(new Color(25, 25, 25, 255));
        g.drawLine(20, 60, 76, 60);
        g.drawLine(20, 120, 76, 120);

        g.dispose();
        return img;
    }

    private BufferedImage createPaintedDestroyer(BufferedImage base) {
        BufferedImage skin = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = skin.createGraphics();
        g.drawImage(base, 0, 0, null);

        // Red decals on wings
        g.setColor(new Color(230, 30, 30, 255));
        g.fillRect(18, 40, 14, 60);
        g.fillRect(64, 40, 14, 60);

        g.dispose();
        return skin;
    }

    @Test
    public void testDiffStencilExtraction() {
        BufferedImage base = createBaseDestroyer();
        BufferedImage skin = createPaintedDestroyer(base);

        StencilExtractor.StencilDiffConfig config = StencilExtractor.StencilDiffConfig.builder()
                .threshold(8.0f)
                .build();

        BufferedImage stencil = StencilExtractor.extractDiffStencil(base, skin, config);
        assertNotNull(stencil);

        int paintedCount = 0;
        for (int y = 0; y < 192; y++) {
            for (int x = 0; x < 96; x++) {
                int a = (stencil.getRGB(x, y) >> 24) & 0xff;
                if (a > 128) paintedCount++;
            }
        }

        assertTrue(paintedCount > 1000, "Diff stencil should isolate painted decal pixels");
    }

    @Test
    public void testPanelSeamsExtraction() {
        BufferedImage base = createBaseDestroyer();
        BufferedImage seams = StencilExtractor.extractPanelSeams(base, 0.08f);

        assertNotNull(seams);
        int seamCount = 0;
        for (int y = 0; y < 192; y++) {
            for (int x = 0; x < 96; x++) {
                int a = (seams.getRGB(x, y) >> 24) & 0xff;
                if (a > 25) seamCount++;
            }
        }
        assertTrue(seamCount > 100, "Panel seam extractor should find structural lines");
    }

    @Test
    public void testBlendModes() {
        BufferedImage base = createBaseDestroyer();
        BufferedImage skin = createPaintedDestroyer(base);
        BufferedImage stencil = StencilExtractor.extractDiffStencil(base, skin, null);

        for (StencilExtractor.BlendMode mode : StencilExtractor.BlendMode.values()) {
            BufferedImage blended = StencilExtractor.applyStencil(base, stencil, mode);
            assertNotNull(blended);
            assertEquals(96, blended.getWidth());
            assertEquals(192, blended.getHeight());
        }
    }
}
