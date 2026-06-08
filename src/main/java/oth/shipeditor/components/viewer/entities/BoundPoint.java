package oth.shipeditor.components.viewer.entities;

import oth.shipeditor.components.instrument.EditorInstrument;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.components.viewer.layers.ship.ShipPainter;
import oth.shipeditor.components.viewer.painters.points.ship.BoundPointsPainter;
import oth.shipeditor.utility.graphics.DrawUtilities;
import oth.shipeditor.utility.graphics.ShapeUtilities;
import oth.shipeditor.utility.text.StringValues;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;

public class BoundPoint extends BaseWorldPoint {

    public BoundPoint(Point2D pointPosition, ShipPainter layer) {
        super(pointPosition, layer);
    }

    @Override
    public EditorInstrument getAssociatedMode() {
        return EditorInstrument.BOUNDS;
    }

    public static Shape getShapeForPoint(AffineTransform worldToScreen, Point2D position, double sizeMult) {
        Shape hexagon = ShapeUtilities.createHexagon(position, 0.10f * sizeMult);

        return ShapeUtilities.ensureDynamicScaleShape(worldToScreen,
                position, hexagon, 12 * sizeMult);
    }

    @Override
    public void paint(Graphics2D g, AffineTransform worldToScreen, double w, double h) {
        Point2D position = getPosition();

        Shape hexagon = BoundPoint.getShapeForPoint(worldToScreen, position, getPaintSizeMultiplier());

        this.updateCursorHitState(worldToScreen);

        DrawUtilities.outlineShape(g, hexagon, Color.BLACK, 2);
        DrawUtilities.fillShape(g, hexagon, getCurrentColor());

        this.paintCoordsLabel(g, worldToScreen);
    }

    @Override
    public String getNameForLabel() {
        return StringValues.BOUND;
    }

    @Override
    protected String[] getHoverLines() {
        Point2D toDisplay = this.getCoordinatesForDisplay();
        String indexLabel = StringValues.BOUND;
        LayerPainter layerPainter = getParent();
        if (layerPainter instanceof ShipPainter shipPainter) {
            BoundPointsPainter boundsPainter = shipPainter.getBoundsPainter();
            List<BoundPoint> points = boundsPainter.getPointsIndex();
            int index = points.indexOf(this);
            if (index >= 0) {
                indexLabel = StringValues.BOUND + " #" + index;
            }
        }
        String coords = "(" + toDisplay.getX() + ", " + toDisplay.getY() + ")";
        return new String[] { indexLabel, coords };
    }

    @Override
    public String toString() {
        Class<? extends BoundPoint> identity = this.getClass();
        return identity.getSimpleName() + " " + getPositionText();
    }

}
