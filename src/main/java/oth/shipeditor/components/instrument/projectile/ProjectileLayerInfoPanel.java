package oth.shipeditor.components.instrument.projectile;

import oth.shipeditor.components.instrument.AbstractLayerInfoPanel;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.components.viewer.layers.ViewerLayer;
import oth.shipeditor.components.viewer.layers.weapon.ProjectileLayerPainter;

public class ProjectileLayerInfoPanel extends AbstractLayerInfoPanel {

    @Override
    protected boolean isValidLayer(LayerPainter layerPainter) {
        return layerPainter instanceof ProjectileLayerPainter;
    }

    @Override
    protected void clearData() {
    }

    @Override
    protected void refreshData(ViewerLayer selected) {
    }

}
