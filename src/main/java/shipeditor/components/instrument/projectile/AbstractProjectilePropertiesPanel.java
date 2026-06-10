package shipeditor.components.instrument.projectile;

import shipeditor.components.instrument.AbstractDataPropertiesPanel;
import shipeditor.components.viewer.layers.weapon.ProjectileLayerPainter;

public abstract class AbstractProjectilePropertiesPanel extends AbstractDataPropertiesPanel<ProjectileLayerPainter> {

    protected AbstractProjectilePropertiesPanel() {
        super(ProjectileLayerPainter.class);
    }

}
