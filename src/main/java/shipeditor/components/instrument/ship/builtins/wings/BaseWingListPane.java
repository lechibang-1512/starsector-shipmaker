package shipeditor.components.instrument.ship.builtins.wings;

import shipeditor.components.datafiles.entities.WingCSVEntry;
import shipeditor.components.instrument.ship.shared.AbstractWingListPane;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.data.ShipHull;
import shipeditor.utility.overseers.StaticController;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class BaseWingListPane extends AbstractWingListPane<ShipHull> {

    BaseWingListPane(Function<ShipHull, List<WingCSVEntry>> getter,
                     BiConsumer<ShipHull, List<WingCSVEntry>> sortSetter) {
        super(getter, sortSetter);
    }

    @Override
    protected void actOnTarget(BiConsumer<ShipLayer, ShipHull> action) {
        StaticController.actOnCurrentShip(layer -> {
            ShipHull hull = layer.getHull();
            if (hull != null) action.accept(layer, hull);
        });
    }

    @Override
    protected ShipHull getTarget(ShipLayer checkedLayer) {
        return checkedLayer.getHull();
    }
}
