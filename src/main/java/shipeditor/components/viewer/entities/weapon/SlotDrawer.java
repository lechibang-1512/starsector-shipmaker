package shipeditor.components.viewer.entities.weapon;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import shipeditor.representation.weapon.WeaponMount;
import shipeditor.representation.weapon.WeaponSize;
import shipeditor.representation.weapon.WeaponType;
import shipeditor.utility.Utility;
import shipeditor.utility.graphics.DrawUtilities;
import shipeditor.utility.graphics.ShapeUtilities;
import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import shipeditor.utility.overseers.StaticController;

import java.awt.Color;
import java.awt.geom.*;
import shipeditor.utility.graphics.GraphicConstants;

@SuppressWarnings("ClassWithTooManyFields")
@Getter
@Setter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class SlotDrawer {

    private SlotPoint parentPoint;

    private Point2D pointPosition;

    private WeaponMount mount;

    private WeaponSize size;

    private WeaponType type;

    private double angle;

    private double arc;

    private double paintSizeMultiplier = 1;

    private boolean drawArc = true;

    private boolean drawAngle = true;

    private static final AffineTransform SCALE_TRANSFORM = new AffineTransform();

    public SlotDrawer(SlotPoint parent) {
        this.parentPoint = parent;
    }

    public void paintSlotVisuals(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        Point2D position = this.pointPosition;
        AffineTransform worldToScreen = StaticController.getViewer().getWorldToScreen();
        Point2D p0Screen = worldToScreen.transform(new Point2D.Double(0, 0), null);
        Point2D p1Screen = worldToScreen.transform(new Point2D.Double(1, 0), null);
        double wtsScale = p0Screen.distance(p1Screen);

        double circleRadius = 0.10f * paintSizeMultiplier;
        double enlargedRadius = circleRadius * 1.65f;

        float alpha = 1.0f;
        if (parentPoint instanceof WeaponSlotPoint weaponSlotPoint) {
            alpha = (float) weaponSlotPoint.getTransparency();
        }

        Color mountColor = this.type.getColor();
        if (parentPoint != null) {
            mountColor = parentPoint.getCurrentColor();
        }

        org.joml.Vector4f colorGl = new org.joml.Vector4f(
            mountColor.getRed() / 255.0f,
            mountColor.getGreen() / 255.0f,
            mountColor.getBlue() / 255.0f,
            mountColor.getAlpha() / 255.0f * alpha
        );
        org.joml.Vector4f blackGl = new org.joml.Vector4f(0.0f, 0.0f, 0.0f, 0.4f * alpha);
        org.joml.Vector4f whiteGl = new org.joml.Vector4f(1.0f, 1.0f, 1.0f, alpha);

        Point2D centerScreen = worldToScreen.transform(position, null);
        org.joml.Vector2f centerGl = new org.joml.Vector2f((float) centerScreen.getX(), (float) centerScreen.getY());

        this.drawMountShapeGL(shapeRenderer, worldToScreen, wtsScale, centerGl, circleRadius, enlargedRadius, colorGl, blackGl);

        if (drawArc) {
            this.drawArcGL(shapeRenderer, worldToScreen, wtsScale, centerGl, circleRadius, colorGl, blackGl, alpha);
        }
        if (drawAngle) {
            this.drawAnglePointerGL(shapeRenderer, worldToScreen, wtsScale, centerGl, circleRadius, colorGl, whiteGl, alpha);
        }
    }

    private void drawMountShapeGL(ShapeRenderer shapeRenderer, AffineTransform worldToScreen, double wtsScale,
                                  org.joml.Vector2f centerGl, double circleRadius, double enlargedRadius,
                                  org.joml.Vector4f colorGl, org.joml.Vector4f blackGl) {
        WeaponMount slotMount = this.mount;
        WeaponSize slotSize = this.size;

        this.paintMountGL(shapeRenderer, worldToScreen, wtsScale, centerGl, circleRadius, enlargedRadius, 1.0d, slotMount, colorGl, blackGl);

        if (slotSize == WeaponSize.MEDIUM || slotSize == WeaponSize.LARGE) {
            double scaleMedium = 1.25d;
            this.paintMountGL(shapeRenderer, worldToScreen, wtsScale, centerGl, circleRadius, enlargedRadius, scaleMedium, slotMount, colorGl, blackGl);
            if (slotSize == WeaponSize.LARGE) {
                double scaleLarge = 1.5d;
                this.paintMountGL(shapeRenderer, worldToScreen, wtsScale, centerGl, circleRadius, enlargedRadius, scaleLarge, slotMount, colorGl, blackGl);
            }
        }
    }

    private void paintMountGL(ShapeRenderer shapeRenderer, AffineTransform worldToScreen, double wtsScale,
                              org.joml.Vector2f centerGl, double circleRadius, double enlargedRadius,
                              double scale, WeaponMount slotMount,
                              org.joml.Vector4f colorGl, org.joml.Vector4f blackGl) {
        
        Point2D centerScreen = new Point2D.Double(centerGl.x, centerGl.y);
        
        double effectiveHalfExtent = enlargedRadius * scale;
        double effectiveHalfExtentPixels = effectiveHalfExtent * wtsScale;
        
        double minRadiusPixels = 12.0 * paintSizeMultiplier;
        if (scale == 1.25d) minRadiusPixels = 14.0 * paintSizeMultiplier;
        if (scale == 1.5d) minRadiusPixels = 16.0 * paintSizeMultiplier;
        
        double finalHalfExtentPixels = Math.max(effectiveHalfExtentPixels, minRadiusPixels);
        double pixelScale = finalHalfExtentPixels / effectiveHalfExtentPixels;

        switch (slotMount) {
            case TURRET -> {
                org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_THIN);
                shapeRenderer.drawCircle(centerGl, (float) finalHalfExtentPixels, blackGl, false);
                org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_DEFAULT);
                shapeRenderer.drawCircle(centerGl, (float) finalHalfExtentPixels, colorGl, false);
            }
            case HARDPOINT -> {
                Point2D p0 = new Point2D.Double(pointPosition.getX() - effectiveHalfExtent, pointPosition.getY() - effectiveHalfExtent);
                Point2D p1 = new Point2D.Double(pointPosition.getX() + effectiveHalfExtent, pointPosition.getY() - effectiveHalfExtent);
                Point2D p2 = new Point2D.Double(pointPosition.getX() + effectiveHalfExtent, pointPosition.getY() + effectiveHalfExtent);
                Point2D p3 = new Point2D.Double(pointPosition.getX() - effectiveHalfExtent, pointPosition.getY() + effectiveHalfExtent);

                Point2D s0 = worldToScreen.transform(p0, null);
                Point2D s1 = worldToScreen.transform(p1, null);
                Point2D s2 = worldToScreen.transform(p2, null);
                Point2D s3 = worldToScreen.transform(p3, null);

                org.joml.Vector2f v0 = new org.joml.Vector2f(centerGl.x + (float)((s0.getX() - centerScreen.getX()) * pixelScale), centerGl.y + (float)((s0.getY() - centerScreen.getY()) * pixelScale));
                org.joml.Vector2f v1 = new org.joml.Vector2f(centerGl.x + (float)((s1.getX() - centerScreen.getX()) * pixelScale), centerGl.y + (float)((s1.getY() - centerScreen.getY()) * pixelScale));
                org.joml.Vector2f v2 = new org.joml.Vector2f(centerGl.x + (float)((s2.getX() - centerScreen.getX()) * pixelScale), centerGl.y + (float)((s2.getY() - centerScreen.getY()) * pixelScale));
                org.joml.Vector2f v3 = new org.joml.Vector2f(centerGl.x + (float)((s3.getX() - centerScreen.getX()) * pixelScale), centerGl.y + (float)((s3.getY() - centerScreen.getY()) * pixelScale));

                org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_THIN);
                shapeRenderer.drawLine(v0, v1, blackGl);
                shapeRenderer.drawLine(v1, v2, blackGl);
                shapeRenderer.drawLine(v2, v3, blackGl);
                shapeRenderer.drawLine(v3, v0, blackGl);

                org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_DEFAULT);
                shapeRenderer.drawLine(v0, v1, colorGl);
                shapeRenderer.drawLine(v1, v2, colorGl);
                shapeRenderer.drawLine(v2, v3, colorGl);
                shapeRenderer.drawLine(v3, v0, colorGl);
            }
            case HIDDEN -> {
                double d = effectiveHalfExtent * (2.5 / 1.65);

                Point2D p0 = new Point2D.Double(pointPosition.getX() + d * 0.866025, pointPosition.getY() - d * 0.5);
                Point2D p1 = new Point2D.Double(pointPosition.getX(), pointPosition.getY() + d);
                Point2D p2 = new Point2D.Double(pointPosition.getX() - d * 0.866025, pointPosition.getY() - d * 0.5);

                Point2D s0 = worldToScreen.transform(p0, null);
                Point2D s1 = worldToScreen.transform(p1, null);
                Point2D s2 = worldToScreen.transform(p2, null);

                org.joml.Vector2f v0 = new org.joml.Vector2f(centerGl.x + (float)((s0.getX() - centerScreen.getX()) * pixelScale), centerGl.y + (float)((s0.getY() - centerScreen.getY()) * pixelScale));
                org.joml.Vector2f v1 = new org.joml.Vector2f(centerGl.x + (float)((s1.getX() - centerScreen.getX()) * pixelScale), centerGl.y + (float)((s1.getY() - centerScreen.getY()) * pixelScale));
                org.joml.Vector2f v2 = new org.joml.Vector2f(centerGl.x + (float)((s2.getX() - centerScreen.getX()) * pixelScale), centerGl.y + (float)((s2.getY() - centerScreen.getY()) * pixelScale));

                org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_THIN);
                shapeRenderer.drawLine(v0, v1, blackGl);
                shapeRenderer.drawLine(v1, v2, blackGl);
                shapeRenderer.drawLine(v2, v0, blackGl);

                org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_DEFAULT);
                shapeRenderer.drawLine(v0, v1, colorGl);
                shapeRenderer.drawLine(v1, v2, colorGl);
                shapeRenderer.drawLine(v2, v0, colorGl);
            }
        }
    }

    private void drawArcGL(ShapeRenderer shapeRenderer, AffineTransform worldToScreen, double wtsScale,
                           org.joml.Vector2f centerGl, double circleRadius,
                           org.joml.Vector4f colorGl, org.joml.Vector4f blackGl, float alpha) {
        Point2D position = this.pointPosition;
        double slotArc = this.arc;
        double halfArc = slotArc * 0.5d;
        double transformedAngle = Utility.transformAngle(this.angle);

        double arcStartAngle = transformedAngle - halfArc;
        double arcEndAngle = transformedAngle + halfArc;

        double lineLength = 0.55f * paintSizeMultiplier;

        double effectiveCircleRadius = circleRadius;
        double effectiveLineLength = lineLength;
        double effectiveArcRadius = 0.40f * paintSizeMultiplier;

        Point2D arcStartEndpoint = ShapeUtilities.getPointInDirection(position, arcStartAngle, effectiveLineLength);
        Point2D arcStartCirclePoint = ShapeUtilities.getPointInDirection(position, arcStartAngle, effectiveCircleRadius);

        Point2D arcEndEndpoint = ShapeUtilities.getPointInDirection(position, arcEndAngle, effectiveLineLength);
        Point2D arcEndCirclePoint = ShapeUtilities.getPointInDirection(position, arcEndAngle, effectiveCircleRadius);

        Point2D sStartEndpoint = worldToScreen.transform(arcStartEndpoint, null);
        Point2D sStartCirclePoint = worldToScreen.transform(arcStartCirclePoint, null);
        Point2D sEndEndpoint = worldToScreen.transform(arcEndEndpoint, null);
        Point2D sEndCirclePoint = worldToScreen.transform(arcEndCirclePoint, null);

        org.joml.Vector2f vStartEndpoint = new org.joml.Vector2f((float) sStartEndpoint.getX(), (float) sStartEndpoint.getY());
        org.joml.Vector2f vStartCirclePoint = new org.joml.Vector2f((float) sStartCirclePoint.getX(), (float) sStartCirclePoint.getY());
        org.joml.Vector2f vEndEndpoint = new org.joml.Vector2f((float) sEndEndpoint.getX(), (float) sEndEndpoint.getY());
        org.joml.Vector2f vEndCirclePoint = new org.joml.Vector2f((float) sEndCirclePoint.getX(), (float) sEndCirclePoint.getY());

        org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_THIN);
        shapeRenderer.drawLine(vStartEndpoint, vStartCirclePoint, blackGl);
        shapeRenderer.drawLine(vEndEndpoint, vEndCirclePoint, blackGl);

        org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_DEFAULT);
        shapeRenderer.drawLine(vStartEndpoint, vStartCirclePoint, colorGl);
        shapeRenderer.drawLine(vEndEndpoint, vEndCirclePoint, colorGl);

        int segments = 24;
        org.joml.Vector2f prevPtScreen = null;
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double theta = arcStartAngle + t * slotArc;
            Point2D arcPt = ShapeUtilities.getPointInDirection(position, theta, effectiveArcRadius);
            Point2D sArcPt = worldToScreen.transform(arcPt, null);
            org.joml.Vector2f currPtScreen = new org.joml.Vector2f((float) sArcPt.getX(), (float) sArcPt.getY());
            
            if (prevPtScreen != null) {
                org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_THIN);
                shapeRenderer.drawLine(prevPtScreen, currPtScreen, blackGl);
            }
            prevPtScreen = currPtScreen;
        }

        prevPtScreen = null;
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double theta = arcStartAngle + t * slotArc;
            Point2D arcPt = ShapeUtilities.getPointInDirection(position, theta, effectiveArcRadius);
            Point2D sArcPt = worldToScreen.transform(arcPt, null);
            org.joml.Vector2f currPtScreen = new org.joml.Vector2f((float) sArcPt.getX(), (float) sArcPt.getY());
            
            if (prevPtScreen != null) {
                org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_DEFAULT);
                shapeRenderer.drawLine(prevPtScreen, currPtScreen, colorGl);
            }
            prevPtScreen = currPtScreen;
        }
    }

    private void drawAnglePointerGL(ShapeRenderer shapeRenderer, AffineTransform worldToScreen, double wtsScale,
                                     org.joml.Vector2f centerGl, double circleRadius,
                                     org.joml.Vector4f colorGl, org.joml.Vector4f whiteGl, float alpha) {
        Point2D position = this.pointPosition;
        double transformedAngle = Utility.transformAngle(this.angle);

        double effectiveRadius = circleRadius;
        double effectivePointerStart = effectiveRadius * 5.0;

        Point2D lineEndpoint = ShapeUtilities.getPointInDirection(position, transformedAngle, effectivePointerStart);
        Point2D closestIntersection = ShapeUtilities.getPointInDirection(position, transformedAngle, effectiveRadius);

        Point2D sLineEndpoint = worldToScreen.transform(lineEndpoint, null);
        Point2D sClosestIntersection = worldToScreen.transform(closestIntersection, null);

        org.joml.Vector2f vLineEndpoint = new org.joml.Vector2f((float) sLineEndpoint.getX(), (float) sLineEndpoint.getY());
        org.joml.Vector2f vClosestIntersection = new org.joml.Vector2f((float) sClosestIntersection.getX(), (float) sClosestIntersection.getY());

        org.joml.Vector4f blackGl = new org.joml.Vector4f(0.0f, 0.0f, 0.0f, 0.4f * alpha);

        org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_THIN);
        shapeRenderer.drawLine(vLineEndpoint, vClosestIntersection, blackGl);

        org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_DEFAULT);
        shapeRenderer.drawLine(vLineEndpoint, vClosestIntersection, whiteGl);

        org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_THIN);
        shapeRenderer.drawCircle(centerGl, (float) (effectiveRadius * wtsScale), blackGl, false);
        org.lwjgl.opengl.GL11.glLineWidth(GraphicConstants.LINE_WIDTH_DEFAULT);
        shapeRenderer.drawCircle(centerGl, (float) (effectiveRadius * wtsScale), colorGl, false);
    }

}
