package shipeditor.components.instrument.weapon;

import shipeditor.components.instrument.AbstractLayerInfoPanel;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.weapon.WeaponPainter;

public class WeaponLayerInfoPanel extends AbstractLayerInfoPanel {

    @Override
    protected boolean isValidLayer(LayerPainter layerPainter) {
        return layerPainter instanceof WeaponPainter;
    }

    @Override
    protected void clearData() {

    }

    @Override
    protected void refreshData(ViewerLayer selected) {

    }

}
