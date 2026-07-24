package shipeditor.components.viewer.entities.weapon;

import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.entities.AngledPoint;
import shipeditor.components.viewer.layers.weapon.WeaponPainter;
import shipeditor.utility.Utility;
import shipeditor.utility.graphics.ColorUtilities;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import shipeditor.utility.graphics.opengl.SpriteRenderer;


import org.joml.Matrix4f;

import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * Represents a single weapon firing-position offset point.
 * <p>
 * Each offset defines a barrel location (X, Y) and firing angle.
 * Rendering is delegated to {@link OffsetDrawer}, mirroring how
 * {@link WeaponSlotPoint} delegates to {@link SlotDrawer}.
 */
public class OffsetPoint extends AngledPoint {

    /**
     * Distinctive cyan color for offset points — intentionally different from
     * weapon type colors (which are used by {@link WeaponSlotPoint}).
     */
    private static final Color OFFSET_BASE_COLOR = new Color(0, 210, 255);

    private double angle;

    private final OffsetDrawer offsetDrawer;

    public OffsetPoint(Point2D pointPosition, WeaponPainter layer) {
        super(pointPosition, layer);
        this.offsetDrawer = new OffsetDrawer(this);
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
    public EditorInstrument getAssociatedMode() {
        return EditorInstrument.WEAPON_OFFSETS;
    }

    @Override
    protected Color createBaseColor() {
        return OFFSET_BASE_COLOR;
    }

    @Override
    protected Color createSelectColor() {
        return ColorUtilities.getBlendedColor(OFFSET_BASE_COLOR, Color.WHITE, 0.5);
    }

    @Override
    public String getNameForLabel() {
        return "Offset";
    }

    @Override
    protected String[] getHoverLines() {
        Point2D toDisplay = this.getCoordinatesForDisplay();
        String header = "Offset";
        String angleLine = "Angle: " + Utility.round(angle, 1) + "\u00B0";
        String coords = "(" + toDisplay.getX() + ", " + toDisplay.getY() + ")";
        return new String[]{ header, angleLine, coords };
    }

    @Override
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer,
                      Matrix4f projection, Matrix4f view) {
        offsetDrawer.setPointPosition(this.getPosition());
        offsetDrawer.setAngle(this.angle);
        offsetDrawer.setPaintSizeMultiplier(this.getPaintSizeMultiplier());

        offsetDrawer.paintOffsetVisuals(spriteRenderer, shapeRenderer, projection, view);
    }

}
