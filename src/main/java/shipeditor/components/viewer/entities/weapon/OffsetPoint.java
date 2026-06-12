package shipeditor.components.viewer.entities.weapon;

import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import shipeditor.components.viewer.entities.AngledPoint;
import shipeditor.components.viewer.layers.weapon.WeaponPainter;
import shipeditor.utility.graphics.ShapeUtilities;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.Utility;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class OffsetPoint extends AngledPoint {

    private double angle;

    public OffsetPoint(Point2D pointPosition, WeaponPainter layer) {
        super(pointPosition, layer);
    }

    @Override
    public void setAngle(double degrees) {
        this.angle = degrees;
    }

    @Override
    public double getAngle() {
        return this.angle;
    }

    @Override
    public void changeSlotAngle(double degrees) {
        this.setAngle(degrees);
    }

    @Override
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        AffineTransform worldToScreen = StaticController.getViewer().getWorldToScreen();
        this.updateCursorHitState(worldToScreen);

        super.paint(spriteRenderer, shapeRenderer, projection, view);

        Point2D position = this.getPosition();
        double transformedAngle = Utility.transformAngle(this.angle);
        Point2D lineEndpoint = ShapeUtilities.getPointInDirection(position,
                transformedAngle, 0.5 * getPaintSizeMultiplier());

        Point2D startScreen = worldToScreen.transform(position, null);
        Point2D endScreen = worldToScreen.transform(lineEndpoint, null);

        org.joml.Vector2f startVec = new org.joml.Vector2f((float) startScreen.getX(), (float) startScreen.getY());
        org.joml.Vector2f endVec = new org.joml.Vector2f((float) endScreen.getX(), (float) endScreen.getY());

        Color color = getCurrentColor();
        org.joml.Vector4f colorVec = new org.joml.Vector4f(
            color.getRed() / 255.0f,
            color.getGreen() / 255.0f,
            color.getBlue() / 255.0f,
            color.getAlpha() / 255.0f
        );

        org.lwjgl.opengl.GL11.glLineWidth(4.0f);
        shapeRenderer.drawLine(startVec, endVec, new org.joml.Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
        org.lwjgl.opengl.GL11.glLineWidth(2.0f);
        shapeRenderer.drawLine(startVec, endVec, colorVec);
    }

}
