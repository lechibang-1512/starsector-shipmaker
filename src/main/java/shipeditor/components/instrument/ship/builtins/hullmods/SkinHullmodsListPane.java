package shipeditor.components.instrument.ship.builtins.hullmods;

import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.instrument.ship.shared.AbstractHullmodsListPane;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.utility.overseers.StaticController;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SkinHullmodsListPane extends AbstractHullmodsListPane<ShipSkin> {

    SkinHullmodsListPane(Function<ShipSkin, List<HullmodCSVEntry>> getter,
                         BiConsumer<ShipSkin, List<HullmodCSVEntry>> sortSetter) {
        super(getter, sortSetter);
    }

    @Override
    protected void actOnTarget(BiConsumer<ShipLayer, ShipSkin> action) {
        StaticController.actOnCurrentSkin(action);
    }

    @Override
    protected ShipSkin getTarget(ShipLayer checkedLayer) {
        return checkedLayer.getActiveSkin();
    }

    @Override
    protected boolean isValidTarget(ShipSkin target) {
        return target != null && !target.isBase();
    }
}
