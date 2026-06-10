package shipeditor.components.layering;

import lombok.Getter;
import shipeditor.components.viewer.layers.weapon.ProjectileLayer;
import shipeditor.utility.Utility;

public class ProjectileLayerTab extends LayerTab {

    @Getter
    private final ProjectileLayer projectileLayer;

    public ProjectileLayerTab(ProjectileLayer layer) {
        super(layer);
        this.projectileLayer = layer;
    }

    @Override
    public String getTabTooltip() {
        return Utility.getWithLinebreaks("Projectile spec: " + projectileLayer.getSpecFileName(),
                "Projectile ID: " + projectileLayer.getSpecFile().getId());
    }
}
