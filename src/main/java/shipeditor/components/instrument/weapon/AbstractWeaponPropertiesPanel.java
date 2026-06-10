package shipeditor.components.instrument.weapon;

import shipeditor.components.instrument.AbstractDataPropertiesPanel;
import shipeditor.components.viewer.layers.weapon.WeaponPainter;

public abstract class AbstractWeaponPropertiesPanel extends AbstractDataPropertiesPanel<WeaponPainter> {

    protected AbstractWeaponPropertiesPanel() {
        super(WeaponPainter.class);
    }

}
