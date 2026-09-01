package shipeditor.utility.graphics;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SocketContourDetectorTest {

    @Test
    public void testSocketDetection() {
        int w = 128, h = 128;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Hull base
        g.setColor(new Color(100, 100, 100, 255));
        g.fillRect(16, 16, 96, 96);

        // Weapon turret socket: bright outer ring + dark center well
        int cx = 64, cy = 64, r = 16;
        g.setColor(new Color(230, 230, 230, 255));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);

        g.setColor(new Color(20, 20, 20, 255));
        int innerR = r / 2;
        g.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

        g.dispose();

        List<SocketContourDetector.SocketCandidate> sockets =
                SocketContourDetector.detectWeaponSockets(img, 0.15f, true);

        assertNotNull(sockets);
        assertFalse(sockets.isEmpty(), "Should detect the circular weapon mount");

        BufferedImage mask = SocketContourDetector.generateSocketProtectionMask(img, sockets, 2);
        assertNotNull(mask);
    }
}
