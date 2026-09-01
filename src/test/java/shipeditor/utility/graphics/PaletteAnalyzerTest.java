package shipeditor.utility.graphics;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class PaletteAnalyzerTest {

    @Test
    public void testPaletteAnalysis() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Neutral gray metal (achromatic)
        g.setColor(new Color(128, 128, 128, 255));
        g.fillRect(10, 10, 44, 44);

        // Orange/Gold stripe (chromatic)
        g.setColor(new Color(220, 140, 30, 255));
        g.fillRect(20, 20, 24, 24);

        g.dispose();

        PaletteAnalyzer.PaletteReport report = PaletteAnalyzer.analyzeSprite(img);

        assertNotNull(report);
        assertTrue(report.getTotalOpaquePixels() > 0);
        assertTrue(report.getAchromaticPixels() > 0);
        assertTrue(report.getChromaticPixels() > 0);
        assertFalse(report.getHueSectors().isEmpty());
        assertFalse(report.getLuminanceBands().isEmpty());
    }
}
