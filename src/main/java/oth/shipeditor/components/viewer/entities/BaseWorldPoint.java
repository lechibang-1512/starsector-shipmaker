package oth.shipeditor.components.viewer.entities;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import de.javagl.viewer.Painter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.BusEventListener;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.viewer.points.AnchorOffsetQueued;
import oth.shipeditor.components.instrument.EditorInstrument;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.components.viewer.painters.TextPainter;
import oth.shipeditor.undo.UndoOverseer;
import oth.shipeditor.utility.overseers.MiscCaching;
import oth.shipeditor.utility.overseers.StaticController;
import oth.shipeditor.utility.Utility;
import oth.shipeditor.utility.graphics.ColorUtilities;
import oth.shipeditor.utility.graphics.DrawUtilities;
import oth.shipeditor.utility.graphics.ShapeUtilities;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class BaseWorldPoint implements WorldPoint, Painter {

    @Getter @Setter
    private LayerPainter parent;

    @Getter
    private final Point2D position;

    @Getter
    private final TextPainter coordsLabel;

    @Getter @Setter
    private boolean cursorInBounds;

    @Getter @Setter
    private boolean pointSelected;

    @Getter
    private final AffineTransform delegateWorldToScreen;

    private BusEventListener anchorDragListener;

    @Getter @Setter
    private double paintSizeMultiplier = 1;

    public EditorInstrument getAssociatedMode() {
        return EditorInstrument.LAYER;
    }

    public BaseWorldPoint() {
        this(new Point2D.Double());
    }

    public BaseWorldPoint(Point2D pointPosition) {
        this(new Point2D.Double(pointPosition.getX(), pointPosition.getY()), null);

    }

    public BaseWorldPoint(Point2D pointPosition, LayerPainter parentPainter) {
        this.position = new Point2D.Double(pointPosition.getX(), pointPosition.getY());
        this.parent = parentPainter;
        this.delegateWorldToScreen = new AffineTransform();
        this.coordsLabel = new TextPainter();
        if (parentPainter != null) {
            this.initLayerListening();
        }
    }

    protected void paintCoordsLabel(Graphics2D g, AffineTransform worldToScreen) {
        Point2D coordsPoint = getPosition();

        DrawUtilities.drawWithConditionalOpacity(g, graphics2D -> {
            String[] hoverLines = getHoverLines();

            coordsLabel.setWorldPosition(coordsPoint);

            if (hoverLines != null && hoverLines.length > 1) {
                if (isCursorInBounds() || isPointSelected()) {
                    Font headerFont = Utility.getOrbitron(14);
                    Font detailFont = Utility.getOrbitron(12);
                    coordsLabel.paintMultiLineText(graphics2D, worldToScreen,
                            hoverLines, headerFont, detailFont, Color.WHITE);
                } else {
                    String text = hoverLines[0];
                    coordsLabel.setText(text);
                    coordsLabel.paintText(graphics2D, worldToScreen);
                }
            } else {
                String text = (hoverLines != null && hoverLines.length == 1)
                        ? hoverLines[0]
                        : getNameForLabel();
                coordsLabel.setText(text);
                coordsLabel.paintText(graphics2D, worldToScreen);
            }
        });
    }

    /**
     * Returns the lines to display in the on-hover pop-out tooltip.
     * Subclasses override this to provide richer multi-line information.
     * The first line is rendered with a larger header font.
     */
    protected String[] getHoverLines() {
        Point2D toDisplay = this.getCoordinatesForDisplay();
        return new String[] {
                getNameForLabel() + " (" + toDisplay.getX() + ", " + toDisplay.getY() + ")"
        };
    }

    public String getNameForLabel() {
        return "Point";
    }

    public String getPositionText() {
        Point2D location = this.getCoordinatesForDisplay();
        return Utility.getPointPositionText(location);
    }
    
    private void initLayerListening() {
        anchorDragListener = event -> {
            if (event instanceof AnchorOffsetQueued checked && checked.layer() == this.parent) {
                Point2D offset = checked.difference();
                Point2D oldPosition = this.getPosition();
                this.setPosition(oldPosition.getX() - offset.getX(),
                        oldPosition.getY() - offset.getY());
                UndoOverseer.adjustPointEditsOffset(this, offset);
            }
        };
        EventBus.subscribe(anchorDragListener);
    }

    public void cleanupForRemoval() {
        EventBus.unsubscribe(anchorDragListener);
    }

    Color createHoverColor() {
        return ColorUtilities.getBlendedColor(createBaseColor(), createSelectColor(),0.5);
    }

    @SuppressWarnings("WeakerAccess")
    protected Color createSelectColor() {
        return new Color(0xFFFF0000, true);
    }

    protected Color createBaseColor() {
        return new Color(0xFFFFFFFF, true);
    }

    boolean isInteractable() {
        LayerPainter layer = getParent();
        if (layer == null) {
            return true;
        }
        return StaticController.getEditorMode() == getAssociatedMode() && layer.isLayerActive();
    }

    @SuppressWarnings("WeakerAccess")
    public Color getCurrentColor() {
        Color result;
        if (this.pointSelected && isInteractable()) {
            result = createSelectColor();
        } else if (this.cursorInBounds && isInteractable()) {
            result = createHoverColor();
        } else {
            result = createBaseColor();
        }
        return result;
    }

    @SuppressWarnings("SameParameterValue")
    private Shape getShapeForPoint(AffineTransform worldToScreen, float worldSize, float screenSize) {
        Shape circle = ShapeUtilities.createCircle(position, (float) (worldSize * paintSizeMultiplier));

        return ShapeUtilities.ensureDynamicScaleShape(worldToScreen,
                position, circle, screenSize * paintSizeMultiplier);
    }

    public void setPosition(double x, double y) {
        this.position.setLocation(x, y);
    }

    public void setPosition(Point2D input) {
        this.setPosition(input.getX(), input.getY());
    }

    public Point2D getCoordinatesForDisplay() {
        Point2D pointPosition = this.getPosition();
        return Utility.getPointCoordinatesForDisplay(pointPosition);
    }

    protected void updateCursorHitState(AffineTransform worldToScreen) {
        Point2D screenLoc = worldToScreen.transform(position, MiscCaching.getNewPoint());
        double distSq = screenLoc.distanceSq(StaticController.getRawCursor());
        double radius = 8.0 * paintSizeMultiplier;
        boolean withinRadius = distSq <= (radius * radius);
        this.setCursorInBounds(withinRadius);
    }

    @Override
    public void paint(Graphics2D g, AffineTransform worldToScreen, double w, double h) {
        Shape shape = this.getShapeForPoint(worldToScreen, 0.10f, 12);

        this.updateCursorHitState(worldToScreen);

        DrawUtilities.outlineShape(g, shape, Color.BLACK, 1.5f);
        DrawUtilities.fillShape(g, shape, getCurrentColor());
    }

}
