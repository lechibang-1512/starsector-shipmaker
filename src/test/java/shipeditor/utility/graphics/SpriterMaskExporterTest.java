package shipeditor.utility.graphics;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class SpriterMaskExporterTest {

    private BufferedImage createSyntheticFrigate() {
        BufferedImage img = new BufferedImage(64, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Hull base
        g.setColor(new Color(130, 130, 130, 255));
        g.fillRoundRect(10, 15, 44, 98, 12, 12);

        // Panel seams
        g.setColor(new Color(30, 30, 30, 255));
        g.drawLine(15, 40, 49, 40);
        g.drawLine(15, 80, 49, 80);

        g.dispose();
        return img;
    }

    @Test
    public void testSpriterMaskExport() {
        BufferedImage ship = createSyntheticFrigate();
        SpriterMaskExporter.MaskExportConfig config = SpriterMaskExporter.MaskExportConfig.builder()
                .fuzziness(68.0f)
                .exportUnpainted(true)
                .build();

        SpriterMaskExporter.MaskExportResult result = SpriterMaskExporter.exportSpriterMasks(ship, config);

        assertNotNull(result);
        assertNotNull(result.getShipMask());
        assertNotNull(result.getPaintjobMask());
        assertNotNull(result.getExposedMask());
        assertNotNull(result.getCavityMask());
        assertNotNull(result.getExposedClippedMask());
        assertNotNull(result.getUnpaintedMask());

        assertTrue(result.getShipPx() > 1000, "Ship mask should capture hull pixels");
        assertTrue(result.getPaintablePx() > 0, "Should detect paintable armor plates");
        assertTrue(result.getExposedPx() > 0, "Exposed mask should cover flat plates");
        assertTrue(result.getCavityPx() > 0, "Cavity mask should detect seams and plate gaps");
    }
}
