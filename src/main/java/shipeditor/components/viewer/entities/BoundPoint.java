package shipeditor.components.viewer.entities;

import shipeditor.utility.graphics.opengl.OpenGLPainter;
import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import shipeditor.components.instrument.EditorInstrument;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.painters.points.ship.BoundPointsPainter;
import shipeditor.utility.graphics.DrawUtilities;
import shipeditor.utility.graphics.ShapeUtilities;
import shipeditor.utility.text.StringValues;

import java.awt.Shape;
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
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        super.paint(spriteRenderer, shapeRenderer, projection, view);
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
