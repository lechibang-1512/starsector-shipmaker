package shipeditor.components.instrument.ship.centers;

import shipeditor.components.viewer.entities.ShipCenterPoint;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.painters.points.ship.CenterPointPainter;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.widgets.PointLocationWidget;
import shipeditor.utility.text.StringValues;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.geom.Point2D;
import java.util.function.Consumer;
import java.util.function.Supplier;

class ShipCenterLocationWidget extends PointLocationWidget {

    private final CollisionPanel parentPanel;

    ShipCenterLocationWidget(CollisionPanel parent) {
        this.parentPanel = parent;
    }

    @Override
    protected void populateContent() {
        super.populateContent();
        String name = "Center position:";
        var dependentCoordsPanel = createDependentCoordinatesLabel(name);
        this.add(dependentCoordsPanel, BorderLayout.PAGE_START);
    }

    @Override
    protected void addWidgetRow(JPanel contentContainer, JLabel label, JComponent component, int ordering) {
        ComponentUtilities.addLabelAndComponent(contentContainer,
                label, component, 3, 5, 0, ordering);
    }

    @Override
    protected boolean isLayerPainterEligible(LayerPainter layerPainter) {
        return layerPainter instanceof ShipPainter shipPainter && !shipPainter.isUninitialized();
    }

    @Override
    protected String getPanelTitleText() {
        return StringValues.SHIP_CENTER;
    }

    @Override
    protected Supplier<Point2D> retrieveGetter() {
        return () -> {
            LayerPainter cachedLayerPainter = parentPanel.getCachedLayerPainter();
            if (isLayerPainterEligible(cachedLayerPainter)) {
                CenterPointPainter centerPointPainter = ((ShipPainter) cachedLayerPainter).getCenterPointPainter();
                ShipCenterPoint shipCenterPoint = centerPointPainter.getCenterPoint();
                return shipCenterPoint.getPosition();
            }
            return null;
        };
    }

    @Override
    protected Consumer<Point2D> retrieveSetter() {
        return point -> {
            LayerPainter cachedLayerPainter = parentPanel.getCachedLayerPainter();
            if (isLayerPainterEligible(cachedLayerPainter)) {
                CenterPointPainter centerPointPainter = ((ShipPainter) cachedLayerPainter).getCenterPointPainter();
                ShipCenterPoint shipCenterPoint = centerPointPainter.getCenterPoint();
                EditDispatch.postPointDragged(shipCenterPoint, point);
            }
        };
    }

}
