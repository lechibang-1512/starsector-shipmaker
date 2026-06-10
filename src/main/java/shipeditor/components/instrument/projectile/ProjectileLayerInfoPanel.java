package shipeditor.components.instrument.projectile;

import shipeditor.components.instrument.AbstractLayerInfoPanel;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.weapon.ProjectileLayerPainter;

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
