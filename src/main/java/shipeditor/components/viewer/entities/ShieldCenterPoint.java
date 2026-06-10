package shipeditor.components.viewer.entities;

import shipeditor.utility.graphics.opengl.OpenGLPainter;
import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import shipeditor.components.instrument.EditorInstrument;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.painters.points.ship.ShieldPointPainter;
import shipeditor.representation.ship.HullStyle;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.Utility;
import shipeditor.utility.graphics.ColorUtilities;
import shipeditor.utility.graphics.DrawUtilities;

import shipeditor.utility.text.StringValues;

import java.awt.Color;
import java.awt.geom.AffineTransform;

import java.awt.geom.Point2D;
import shipeditor.utility.graphics.GraphicConstants;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class ShieldCenterPoint extends BaseWorldPoint {

    @Getter @Setter
    private float shieldRadius;

    private final ShieldPointPainter parentPainter;

    @Getter @Setter
    private HullStyle associatedStyle;

    public ShieldCenterPoint(Point2D pointPosition, float radius, ShipPainter layer, HullStyle style,
                             ShieldPointPainter parent) {
        super(pointPosition, layer);
        this.shieldRadius = radius;
        this.associatedStyle = style;
        this.parentPainter = parent;
    }

    @Override
    protected boolean isInteractable() {
        return StaticController.getEditorMode() == getAssociatedMode() && parentPainter.isInteractionEnabled();
    }

    @Override
    public EditorInstrument getAssociatedMode() {
        return EditorInstrument.SHIELD;
    }

    @Override
    public String getNameForLabel() {
        return StringValues.SHIELD_CENTER;
    }

    private Color getDisplayedShieldColor(Color base) {
        float painterOpacity = parentPainter.getPaintOpacity();
        int alpha = Math.round(painterOpacity * 255); // Convert opacity [0.0, 1.0] to alpha [0, 255].
        int red = base.getRed();
        int green = base.getGreen();
        int blue = base.getBlue();
        return new Color(red, green, blue, alpha);
    }

    @Override
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        AffineTransform worldToScreen = StaticController.getViewer().getWorldToScreen();
        this.updateCursorHitState(worldToScreen);

        // 1. Draw Shield Circle (World Space)
        shapeRenderer.end();
        shapeRenderer.begin(projection, view);

        Point2D position = getPosition();
        org.joml.Vector2f centerWorld = new org.joml.Vector2f((float) position.getX(), (float) position.getY());

        Color innerColor = getDisplayedShieldColor(associatedStyle.getShieldInnerColor());
        org.joml.Vector4f glInnerColor = new org.joml.Vector4f(
            innerColor.getRed() / 255.0f,
            innerColor.getGreen() / 255.0f,
            innerColor.getBlue() / 255.0f,
            innerColor.getAlpha() / 255.0f
        );
        shapeRenderer.drawCircle(centerWorld, getShieldRadius(), glInnerColor, true);

        Color ringColor = associatedStyle.getShieldRingColor();
        org.joml.Vector4f glRingColor = new org.joml.Vector4f(
            ringColor.getRed() / 255.0f,
            ringColor.getGreen() / 255.0f,
            ringColor.getBlue() / 255.0f,
            ringColor.getAlpha() / 255.0f
        );
        org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_THICK);
        shapeRenderer.drawCircle(centerWorld, getShieldRadius(), glRingColor, false);
        org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_DEFAULT);

        shapeRenderer.end();

        // 2. Draw Shield Center Cross (Screen Space)
        shapeRenderer.begin(projection, new org.joml.Matrix4f());

        Color crossColor = getCurrentColor();
        org.joml.Vector4f glCrossColor = new org.joml.Vector4f(
            crossColor.getRed() / 255.0f,
            crossColor.getGreen() / 255.0f,
            crossColor.getBlue() / 255.0f,
            1.0f
        );
        Point2D screenLoc = worldToScreen.transform(position, null);
        org.joml.Vector2f screenPos = new org.joml.Vector2f((float) screenLoc.getX(), (float) screenLoc.getY());

        float halfLen = 4.24f;
        shapeRenderer.drawLine(new org.joml.Vector2f(screenPos.x - halfLen, screenPos.y - halfLen), new org.joml.Vector2f(screenPos.x + halfLen, screenPos.y + halfLen), glCrossColor);
        shapeRenderer.drawLine(new org.joml.Vector2f(screenPos.x - halfLen, screenPos.y + halfLen), new org.joml.Vector2f(screenPos.x + halfLen, screenPos.y - halfLen), glCrossColor);
    }

    @Override
    protected Color createBaseColor() {
        return new Color(0, 175, 240);
    }

    @Override
    protected Color createHoverColor() {
        return ColorUtilities.getBlendedColor(createBaseColor(),
                createSelectColor(), 0.5f);
    }

    @Override
    @SuppressWarnings("WeakerAccess")
    protected Color createSelectColor() {
        return createBaseColor();
    }



    @Override
    public String toString() {
        return "ShieldCenter";
    }

}
