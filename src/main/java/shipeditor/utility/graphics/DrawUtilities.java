package shipeditor.utility.graphics;

import java.awt.Paint;

import lombok.extern.log4j.Log4j2;
import shipeditor.utility.Utility;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ClassWithTooManyMethods")
@Log4j2
public final class DrawUtilities {

    @SuppressWarnings("StaticCollection")
    private static final Map<Float, Stroke> CACHED_STROKES = new HashMap<>();

    private static final DrawMode DRAW_MODE = DrawMode.FAST;

    private static final AffineTransform CACHED_TRANSFORM = new AffineTransform();

    private DrawUtilities() {
    }



    private static Stroke getStroke(float strokeWidth) {
        Stroke cached = CACHED_STROKES.get(strokeWidth);
        if (cached == null) {
            cached = new BasicStroke(strokeWidth);
            CACHED_STROKES.put(strokeWidth, cached);
        }
        return cached;
    }

    public static void outlineShape(Graphics2D g, Shape shape, Paint color, float strokeWidth) {
        Stroke cached = DrawUtilities.getStroke(strokeWidth);
        DrawUtilities.outlineShape(g, shape, color, cached);
    }

    private static void outlineShape(Graphics2D g, Shape shape, Paint color, Stroke stroke) {
        DrawUtilities.outlineShape(g, shape, color, stroke, DRAW_MODE);
    }

    @SuppressWarnings("WeakerAccess")
    public static void outlineShape(Graphics2D g, Shape shape, Paint color,
                                    Stroke stroke, DrawMode mode) {
        Object oldStrokeControl = g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        Object oldRender = g.getRenderingHint(RenderingHints.KEY_RENDERING);
        Object oldAlpha = g.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION);
        Object oldColorRender = g.getRenderingHint(RenderingHints.KEY_COLOR_RENDERING);

        switch (mode) {
            case NORMAL -> {}
            case QUALITY -> g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            case FAST -> {
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_SPEED);
                g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                        RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
                g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                        RenderingHints.VALUE_COLOR_RENDER_SPEED);
            }
        }

        Paint old = g.getPaint();
        Stroke oldStroke = g.getStroke();
        g.setStroke(stroke);
        g.setPaint(color);

        g.draw(shape);

        if (oldStrokeControl != null) g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, oldStrokeControl);
        if (oldRender != null) g.setRenderingHint(RenderingHints.KEY_RENDERING, oldRender);
        if (oldAlpha != null) g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, oldAlpha);
        if (oldColorRender != null) g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, oldColorRender);

        g.setStroke(oldStroke);
        g.setPaint(old);
    }

    @SuppressWarnings("WeakerAccess")
    public static void fillShape(Graphics2D g, Shape shape, Paint fill) {
        Paint old = g.getPaint();
        g.setPaint(fill);
        g.fill(shape);
        g.setPaint(old);
    }

    public static void drawOutlined(Graphics2D g, Shape shape, Paint color) {
        DrawUtilities.drawOutlined(g, shape, color, false);
    }

    @SuppressWarnings({"BooleanParameter", "WeakerAccess"})
    public static void drawOutlined(Graphics2D g, Shape shape, Paint color, boolean quality) {
        float widthFive = 5.0f;
        Stroke cachedFive = DrawUtilities.getStroke(widthFive);
        float widthThree = 3.0f;
        Stroke cachedThree = DrawUtilities.getStroke(widthThree);
        DrawUtilities.drawOutlined(g, shape, color, quality, cachedFive, cachedThree);
    }



    @SuppressWarnings({"BooleanParameter", "WeakerAccess"})
    public static void drawOutlined(Graphics2D g, Shape shape, Paint color, boolean quality,
                                    Stroke outlineStroke, Stroke coreStroke) {
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldRender = g.getRenderingHint(RenderingHints.KEY_RENDERING);
        Object oldStrokeControl = g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);

        if (quality) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        g.setStroke(outlineStroke);
        g.setPaint(Color.BLACK);
        g.draw(shape);
        g.setStroke(coreStroke);
        g.setPaint(color);
        g.draw(shape);

        g.setStroke(oldStroke);
        g.setPaint(oldPaint);
        
        if (quality) {
            if (oldAA != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
            if (oldRender != null) g.setRenderingHint(RenderingHints.KEY_RENDERING, oldRender);
            if (oldStrokeControl != null) g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, oldStrokeControl);
        }
    }

    public static Shape paintScreenTextOutlined(Graphics2D g, String text, Point2D screenPoint) {
        return DrawUtilities.paintScreenTextOutlined(g, text, null, screenPoint);
    }

    @SuppressWarnings("WeakerAccess")
    public static Shape paintScreenTextOutlined(Graphics2D g, String text, Font fontInput, Point2D screenPoint) {
        return DrawUtilities.paintScreenTextOutlined(g, text, fontInput,
                null, screenPoint, RectangleCorner.BOTTOM_RIGHT);
    }

    /**
     * Note: this method is for painting in screen coordinates only.
     * @param screenPoint desired position of painted String.
     * @param fontInput if null, default value is Orbitron 14.
     * @param strokeInput if null, default value is 2.5f with rounded caps and joins.
     * @param corner determines what corner of painted text's bounding box will correspond to passed screen position.
     * E.g. if BOTTOM_RIGHT, the label will be painted to the upper left of screen point.
     * @return resulting {@link Shape} instance of bounds of the drawn text, from which bounding box positions can be retrieved.
     */

    public static Shape paintScreenTextOutlined(Graphics2D g, String text, Font fontInput, Stroke strokeInput,
                                                Point2D screenPoint, RectangleCorner corner) {
        Font font = fontInput;
        if (font == null) {
            font = Utility.getOrbitron(14);
        }

        GlyphVector glyphVector = font.createGlyphVector(g.getFontRenderContext(), text);
        Shape textShape = glyphVector.getOutline();

        Rectangle2D bounds = textShape.getBounds2D();
        bounds.setRect(screenPoint.getX(), screenPoint.getY(), bounds.getWidth(), bounds.getHeight());
        Point2D delta = ShapeUtilities.calculateCornerCoordinates(bounds, corner);
        double x = delta.getX();
        double y = delta.getY();

        Shape textShapeTranslated = ShapeUtilities.translateShape(textShape,x, y);
        Shape translatedBounds = ShapeUtilities.translateShape(glyphVector.getLogicalBounds(),x, y);

        DrawUtilities.paintOutlinedText(g, translatedBounds, textShapeTranslated, strokeInput);

        return ShapeUtilities.translateShape(glyphVector.getVisualBounds(),x, y);
    }

    private static void paintOutlinedText(Graphics2D g, Shape bounds, Shape textShapeTransformed,
                                          Stroke strokeInput) {
        Color fillColor = Color.WHITE;
        DrawUtilities.paintOutlinedText(g,  bounds, textShapeTransformed, strokeInput, fillColor);
    }

    /**
     * @param bounds will be used to draw shaded background of text.
     * @param strokeInput if null, default BasicStroke of 2.5 will be used, with round caps and joins.
     */
    public static void paintOutlinedText(Graphics2D g, Shape bounds, Shape textShapeTransformed,
                                         Stroke strokeInput, Paint fillColor) {
        Color outlineColor = Color.BLACK;

        Stroke stroke = strokeInput;
        if (stroke == null) {
            stroke = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }

        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldRender = g.getRenderingHint(RenderingHints.KEY_RENDERING);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        DrawUtilities.fillShape(g, bounds, ColorUtilities.setColorAlpha(outlineColor, 50));
        DrawUtilities.outlineShape(g, textShapeTransformed, outlineColor, stroke, DrawMode.QUALITY);
        DrawUtilities.fillShape(g,textShapeTransformed, fillColor);

        if (oldAA != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        if (oldRender != null) g.setRenderingHint(RenderingHints.KEY_RENDERING, oldRender);
    }



    public static void paintInstallableGhostGL(SpriteRenderer spriteRenderer, Matrix4f projection, Matrix4f view,
                                             double rotation, Point2D targetLocation,
                                             Sprite sprite) {
        if (sprite == null) {
            return;
        }

        int textureId = sprite.getTextureId();
        if (textureId == 0) return;

        var spriteImage = sprite.getImage();
        int width = spriteImage.getWidth();
        int height = spriteImage.getHeight();

        double rotationRadians = Math.toRadians(rotation);

        double targetLocationX = targetLocation.getX();
        double targetLocationY = targetLocation.getY();

        Point2D difference = Utility.getSpriteCenterDifferenceToAnchor(spriteImage);
        Point2D anchorForSpriteCenter = new Point2D.Double(targetLocationX - difference.getX(),
                targetLocationY - difference.getY());

        double centerX = anchorForSpriteCenter.getX() + (double) width / 2;
        double centerY = anchorForSpriteCenter.getY() + (double) height / 2;

        org.joml.Vector2f position = new org.joml.Vector2f((float) anchorForSpriteCenter.getX(), (float) anchorForSpriteCenter.getY());
        org.joml.Vector2f size = new org.joml.Vector2f(width, height);
        org.joml.Vector2f rotationAnchor = new org.joml.Vector2f((float) centerX, (float) centerY);
        float opacity = 0.5f;
        org.joml.Vector4f color = new org.joml.Vector4f(1.0f, 1.0f, 1.0f, opacity);

        spriteRenderer.drawSprite(textureId, position, size, rotationAnchor, (float) rotationRadians, color, projection, view);
    }

}

