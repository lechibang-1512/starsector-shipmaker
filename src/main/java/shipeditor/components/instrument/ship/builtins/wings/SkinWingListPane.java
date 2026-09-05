package shipeditor.components.instrument.ship.builtins.wings;

import shipeditor.components.datafiles.entities.WingCSVEntry;
import shipeditor.components.instrument.ship.shared.AbstractWingListPane;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.utility.overseers.StaticController;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SkinWingListPane extends AbstractWingListPane<ShipSkin> {

    SkinWingListPane(Function<ShipSkin, List<WingCSVEntry>> getter,
                     BiConsumer<ShipSkin, List<WingCSVEntry>> sortSetter) {
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
