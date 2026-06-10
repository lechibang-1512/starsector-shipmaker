package shipeditor.components.instrument.ship;

import shipeditor.components.instrument.AbstractDataPropertiesPanel;
import shipeditor.components.viewer.layers.ship.ShipPainter;

public abstract class AbstractShipPropertiesPanel extends AbstractDataPropertiesPanel<ShipPainter> {

    protected AbstractShipPropertiesPanel() {
        super(ShipPainter.class);
    }

}
