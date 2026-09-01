package shipeditor.components.instrument.ship.builtins.hullmods;

import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.instrument.ship.shared.AbstractHullmodsListPane;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.data.ShipHull;
import shipeditor.utility.overseers.StaticController;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class BaseHullmodsListPane extends AbstractHullmodsListPane<ShipHull> {

    BaseHullmodsListPane(Function<ShipHull, List<HullmodCSVEntry>> getter,
                         BiConsumer<ShipHull, List<HullmodCSVEntry>> sortSetter) {
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
