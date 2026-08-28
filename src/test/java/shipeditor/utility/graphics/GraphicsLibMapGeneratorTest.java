package shipeditor.utility.graphics;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class GraphicsLibMapGeneratorTest {

    private BufferedImage createSyntheticShip(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Hull base
        g.setColor(new Color(120, 120, 130, 255));
        g.fillRoundRect(w / 4, h / 6, w / 2, (h * 2) / 3, 16, 16);

        // Raised spine
        g.setColor(new Color(170, 170, 180, 255));
        g.fillRect(w / 2 - 4, h / 4, 8, h / 2);

        // Plasma conduits / lights
        g.setColor(new Color(60, 180, 255, 255));
        g.fillRect(w / 2 - 2, h / 3, 4, 12);

        g.dispose();
        return img;
    }

    @Test
    public void testTexturePackGeneration() {
        BufferedImage ship = createSyntheticShip(64, 96);
        GraphicsLibMapGenerator.GraphicsLibConfig config = GraphicsLibMapGenerator.GraphicsLibConfig.builder()
                .exportHeight(true)
                .exportShowcase(true)
                .surfaceMode(GraphicsLibMapGenerator.SurfaceMode.RGBA)
                .build();

        GraphicsLibMapGenerator.GraphicsLibResult result = GraphicsLibMapGenerator.generateTexturePack(ship, config);

        assertNotNull(result);
        assertNotNull(result.getNormalMap());
        assertNotNull(result.getSurfaceMap());
        assertNotNull(result.getMaterialMap());
        assertNotNull(result.getHeightMap());
        assertNotNull(result.getShowcaseImage());

        assertEquals(64, result.getNormalMap().getWidth());
        assertEquals(96, result.getNormalMap().getHeight());

        // Check transparent canvas baseline in normal map (must be 128, 128, 255, 0)
        int bgNorm = result.getNormalMap().getRGB(0, 0);
        int a = (bgNorm >> 24) & 0xff;
        int r = (bgNorm >> 16) & 0xff;
        int g = (bgNorm >> 8) & 0xff;
        int b = bgNorm & 0xff;

        assertEquals(0, a, "Normal map transparent canvas alpha must be 0");
        assertEquals(128, r, "Normal map flat baseline Red must be 128");
        assertEquals(128, g, "Normal map flat baseline Green must be 128");
        assertEquals(255, b, "Normal map flat baseline Blue must be 255");
    }

    @Test
    public void testPresets() {
        for (GraphicsLibMapGenerator.Preset preset : GraphicsLibMapGenerator.Preset.values()) {
            GraphicsLibMapGenerator.GraphicsLibConfig cfg = GraphicsLibMapGenerator.createConfigForPreset(preset);
            assertNotNull(cfg);
            assertTrue(cfg.getDepthScale() > 0.0f);
        }
    }
}
