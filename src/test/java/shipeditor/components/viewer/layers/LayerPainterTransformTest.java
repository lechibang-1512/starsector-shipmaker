package shipeditor.components.viewer.layers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shipeditor.communication.EventBus;
import shipeditor.utility.graphics.Sprite;

import javax.swing.SwingUtilities;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LayerPainterTransformTest {

    private final List<DummyLayerPainter> createdPainters = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (DummyLayerPainter p : createdPainters) {
            EventBus.unsubscribeByParent(p);
        }
        createdPainters.clear();
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                // Already on EDT
            } else {
                SwingUtilities.invokeAndWait(() -> {});
            }
        } catch (InterruptedException | InvocationTargetException e) {
            // Ignore during cleanup
        }
    }

    private static class TestViewerLayer extends ViewerLayer {}

    private static class DummyLayerPainter extends LayerPainter {
        private Point2D customRotationAnchor;

        DummyLayerPainter() {
            super(new TestViewerLayer());
            this.setUninitialized(false);
        }

        void setCustomRotationAnchor(Point2D anchor) {
            this.customRotationAnchor = anchor;
        }

        @Override
        public Point2D getEntityCenter() {
            return getSpriteCenter();
        }

        @Override
        public Point2D getRotationAnchor() {
            if (customRotationAnchor != null) {
                return customRotationAnchor;
            }
            return super.getRotationAnchor();
        }
    }

    private DummyLayerPainter createPainterWithSprite(int width, int height, Point2D anchor) {
        DummyLayerPainter painter = new DummyLayerPainter();
        createdPainters.add(painter);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Sprite sprite = new Sprite(img, null, "test.png");
        painter.setSprite(sprite);
        painter.setAnchor(anchor);
        return painter;
    }

    @Test
    @DisplayName("Anchor, Sprite Center, and Default Rotation Anchor calculations")
    void testAnchorAndCenterCalculations() {
        DummyLayerPainter painter = createPainterWithSprite(100, 60, new Point2D.Double(10, 20));

        assertEquals(10.0, painter.getAnchor().getX(), 0.001, "Anchor X should match set value");
        assertEquals(20.0, painter.getAnchor().getY(), 0.001, "Anchor Y should match set value");

        Point2D diff = painter.getSpriteCenterDifferenceToAnchor();
        assertEquals(50.0, diff.getX(), 0.001, "Diff X should be width/2");
        assertEquals(30.0, diff.getY(), 0.001, "Diff Y should be height/2");

        Point2D spriteCenter = painter.getSpriteCenter();
        assertEquals(60.0, spriteCenter.getX(), 0.001, "Sprite center X should be anchor.x + width/2");
        assertEquals(50.0, spriteCenter.getY(), 0.001, "Sprite center Y should be anchor.y + height/2");

        // By default, rotation anchor delegates to sprite center
        assertEquals(spriteCenter.getX(), painter.getRotationAnchor().getX(), 0.001, "Default rotation anchor X should be sprite center X");
        assertEquals(spriteCenter.getY(), painter.getRotationAnchor().getY(), 0.001, "Default rotation anchor Y should be sprite center Y");
    }

    @Test
    @DisplayName("Rotation transform rotates points around getRotationAnchor()")
    void testRotationTransformUsesRotationAnchor() {
        DummyLayerPainter painter = createPainterWithSprite(100, 100, new Point2D.Double(0, 0));
        // Center is at (50, 50)
        painter.setRotationRadians(Math.PI / 2); // 90 degrees clockwise

        AffineTransform rotTransform = painter.getRotationTransform();
        assertNotNull(rotTransform);

        // Rotation anchor (50, 50) must remain completely invariant under rotation
        Point2D center = painter.getRotationAnchor();
        Point2D transformedCenter = rotTransform.transform(center, null);
        assertEquals(50.0, transformedCenter.getX(), 0.001);
        assertEquals(50.0, transformedCenter.getY(), 0.001);

        // Top-left (0, 0) rotated 90 deg clockwise around (50, 50) becomes (100, 0)
        Point2D topLeft = new Point2D.Double(0, 0);
        Point2D transformedTopLeft = rotTransform.transform(topLeft, null);
        assertEquals(100.0, transformedTopLeft.getX(), 0.001);
        assertEquals(0.0, transformedTopLeft.getY(), 0.001);
    }

    @Test
    @DisplayName("Custom rotation anchor override is respected by rotation transform")
    void testCustomRotationAnchorRespected() {
        DummyLayerPainter painter = createPainterWithSprite(100, 100, new Point2D.Double(0, 0));
        // Override rotation anchor to an offset pivot (20, 30)
        Point2D customPivot = new Point2D.Double(20, 30);
        painter.setCustomRotationAnchor(customPivot);
        painter.setRotationRadians(Math.PI / 2);

        AffineTransform rotTransform = painter.getRotationTransform();

        // Custom pivot must remain invariant
        Point2D transformedPivot = rotTransform.transform(customPivot, null);
        assertEquals(20.0, transformedPivot.getX(), 0.001);
        assertEquals(30.0, transformedPivot.getY(), 0.001);
    }

    @Test
    @DisplayName("isWorldCursorInsideSprite accurately checks unrotated and rotated bounds")
    void testIsWorldCursorInsideSprite() {
        DummyLayerPainter painter = createPainterWithSprite(100, 100, new Point2D.Double(0, 0));

        // Unrotated checks
        assertTrue(painter.isWorldCursorInsideSprite(new Point2D.Double(50, 50)));
        assertTrue(painter.isWorldCursorInsideSprite(new Point2D.Double(1, 1)));
        assertTrue(painter.isWorldCursorInsideSprite(new Point2D.Double(99, 99)));
        assertFalse(painter.isWorldCursorInsideSprite(new Point2D.Double(-5, 50)));
        assertFalse(painter.isWorldCursorInsideSprite(new Point2D.Double(105, 50)));
        assertFalse(painter.isWorldCursorInsideSprite(new Point2D.Double(50, 105)));

        // Rotate 45 degrees around center (50, 50)
        painter.setRotationRadians(Math.PI / 4);

        // Center is still inside
        assertTrue(painter.isWorldCursorInsideSprite(new Point2D.Double(50, 50)));

        // Top diamond peak extends to y ≈ 50 - 50*sqrt(2) ≈ -20.7
        // (50, -10) is inside rotated diamond
        assertTrue(painter.isWorldCursorInsideSprite(new Point2D.Double(50, -10)));

        // Unrotated corner (0, 0) is now outside the 45-degree diamond
        assertFalse(painter.isWorldCursorInsideSprite(new Point2D.Double(0, 0)));
    }

    @Test
    @DisplayName("getVisualBounds calculates correct AABB for unrotated and rotated sprites")
    void testGetVisualBounds() {
        DummyLayerPainter painter = createPainterWithSprite(100, 50, new Point2D.Double(0, 0));

        // Unrotated bounds
        Rectangle2D unrotated = painter.getVisualBounds();
        assertEquals(0.0, unrotated.getX(), 0.001);
        assertEquals(0.0, unrotated.getY(), 0.001);
        assertEquals(100.0, unrotated.getWidth(), 0.001);
        assertEquals(50.0, unrotated.getHeight(), 0.001);

        // Rotate 90 degrees around center (50, 25)
        painter.setRotationRadians(Math.PI / 2);
        Rectangle2D rotated = painter.getVisualBounds();

        // 100x50 rotated 90 deg has width 50, height 100, centered at (50, 25) -> x in [25, 75], y in [-25, 75]
        assertEquals(25.0, rotated.getX(), 0.001);
        assertEquals(-25.0, rotated.getY(), 0.001);
        assertEquals(50.0, rotated.getWidth(), 0.001);
        assertEquals(100.0, rotated.getHeight(), 0.001);
    }

    @Test
    @DisplayName("Slot positioning math aligns rotation pivot precisely on slot point")
    void testSlotPositioningMath() {
        DummyLayerPainter painter = createPainterWithSprite(120, 80, new Point2D.Double(0, 0));
        // Suppose custom module rotation anchor is at offset (40, 50) from anchor
        Point2D customAnchor = new Point2D.Double(40, 50);
        painter.setCustomRotationAnchor(customAnchor);

        Point2D slotPoint = new Point2D.Double(350.0, 450.0);

        // InstalledFeature alignment formula:
        Point2D rotAnchor = painter.getRotationAnchor();
        Point2D currentAnchor = painter.getAnchor();
        double offsetX = rotAnchor.getX() - currentAnchor.getX();
        double offsetY = rotAnchor.getY() - currentAnchor.getY();
        double targetAnchorX = slotPoint.getX() - offsetX;
        double targetAnchorY = slotPoint.getY() - offsetY;

        painter.setAnchor(new Point2D.Double(targetAnchorX, targetAnchorY));
        // Update the custom rotation anchor position to match new anchor
        painter.setCustomRotationAnchor(new Point2D.Double(targetAnchorX + offsetX, targetAnchorY + offsetY));

        assertEquals(slotPoint.getX(), painter.getRotationAnchor().getX(), 0.001);
        assertEquals(slotPoint.getY(), painter.getRotationAnchor().getY(), 0.001);
    }
}
