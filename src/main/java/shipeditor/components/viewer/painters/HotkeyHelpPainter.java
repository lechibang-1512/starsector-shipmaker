package shipeditor.components.viewer.painters;

import shipeditor.utility.graphics.opengl.OpenGLPainter;
import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import lombok.extern.log4j.Log4j2;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.utility.graphics.DrawUtilities;
import shipeditor.utility.objects.Pair;
import shipeditor.utility.overseers.StaticController;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class HotkeyHelpPainter implements OpenGLPainter {

    private final Map<EditorInstrument, BufferedImage> hintsByMode = new EnumMap<>(EditorInstrument.class);

    private final Map<EditorInstrument, Integer> textureIdsByMode = new EnumMap<>(EditorInstrument.class);

    @Override
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        ViewerLayer activeLayer = StaticController.getActiveLayer();
        if (activeLayer == null) return;
        LayerPainter painter = activeLayer.getPainter();
        if (painter == null || painter.isUninitialized()) return;

        Pair<EditorInstrument, List<String>> hints = HotkeyHelpPainter.getHintsToDisplay();
        EditorInstrument current = hints.getFirst();
        List<String> hintList = hints.getSecond();
        if (hintList.isEmpty()) return;

        BufferedImage cachedImage = hintsByMode.computeIfAbsent(current,
                k -> HotkeyHelpPainter.generateImage(current, hintList));

        int textureId = textureIdsByMode.computeIfAbsent(current, k -> 
            shipeditor.utility.graphics.opengl.TextureLoader.loadTexture(cachedImage)
        );

        if (textureId == 0) return;

        int w = StaticController.getViewer().getWidth();
        int h = StaticController.getViewer().getHeight();

        float imgW = cachedImage.getWidth();
        float imgH = cachedImage.getHeight();
        float imageX = w - imgW;
        float imageY = h - imgH;

        org.joml.Vector2f pos = new org.joml.Vector2f(imageX, imageY);
        org.joml.Vector2f size = new org.joml.Vector2f(imgW, imgH);
        org.joml.Vector2f rotationAnchor = new org.joml.Vector2f(imageX + imgW / 2.0f, imageY + imgH / 2.0f);
        org.joml.Vector4f color = new org.joml.Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

        spriteRenderer.drawSprite(textureId, pos, size, rotationAnchor, 0.0f, color, projection, new Matrix4f());
    }

    private static BufferedImage generateImage(EditorInstrument mode, Iterable<String> hintList) {
        double verticalPadding = 10;
        int imageWidth = 300;
        int imageHeight = 150;

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        int anchorY = imageHeight;

        boolean isWeaponsMode = mode == EditorInstrument.VARIANT_WEAPONS;
        if (isWeaponsMode) {
            anchorY += 3;
        }

        Point2D anchor = new Point2D.Double(imageWidth - verticalPadding, anchorY);

        for (String hint : hintList) {
            Shape drawResult = DrawUtilities.paintScreenTextOutlined(g, hint, anchor);
            Rectangle2D resultBounds = drawResult.getBounds2D();
            double topRightX = resultBounds.getX() + resultBounds.getWidth();
            double topRightY = resultBounds.getY();

            if (isWeaponsMode) {
                topRightY += 3;
            }

            anchor = new Point2D.Double(topRightX, topRightY - verticalPadding);
        }

        g.dispose();
        return image;
    }

    private static Pair<EditorInstrument, List<String>> getHintsToDisplay() {
        EditorInstrument current = StaticController.getEditorMode();
        List<String> hints = new ArrayList<>();
        final String radiusHotkey = "Alter radius: CTRL and move";
        final String pointPositionHotkey = "Move point: LMB and drag";
        final String addPointHotkey = "Add point: SHIFT + LMB";
        final String angleHotkey = "Alter angle: ALT + LMB and drag";
        switch (current) {
            case COLLISION, SHIELD -> {
                hints.add(radiusHotkey);
                hints.add(pointPositionHotkey);
            }
            case BOUNDS -> {
                String insertHint = "Insert point: CTRL + LMB";
                String removeHint = "Remove point: Backspace";
                hints.add(removeHint);
                hints.add(insertHint);
                hints.add(addPointHotkey);
                hints.add(pointPositionHotkey);
            }
            case WEAPON_SLOTS -> {
                String arcHint = "Alter arc: ALT + RMB and drag";
                hints.add(angleHotkey);
                hints.add(arcHint);
                hints.add(addPointHotkey);
                hints.add(pointPositionHotkey);
            }
            case LAUNCH_BAYS -> {
                String addPort = "Add port: SHIFT + LMB";
                String addBay = "Add bay: CTRL + LMB";
                hints.add(addPort);
                hints.add(addBay);
            }
            case ENGINES -> {
                String sizeHint = "Alter size: ALT + RMB and drag";
                hints.add(angleHotkey);
                hints.add(sizeHint);
                hints.add(addPointHotkey);
            }
            case VARIANT_WEAPONS -> {
                String installHint = "Install weapon: CTRL+LMB";
                String removeHint = "Uninstall weapon: Backspace";
                hints.add(installHint);
                hints.add(removeHint);
            }
            case VARIANT_MODULES -> {
                String installHint = "Install module: CTRL+LMB";
                String removeHint = "Uninstall module: Backspace";
                hints.add(installHint);
                hints.add(removeHint);
            }
            default -> {
            }
        }
        return new Pair<>(current, hints);
    }

}
