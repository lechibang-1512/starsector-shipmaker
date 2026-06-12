package shipeditor.components.instrument.weapon;

import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.components.ComponentUtilities;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Supplier;

public class WeaponFirePanel extends AbstractWeaponPropertiesPanel {

    private WeaponLayer cachedLayer;
    private boolean readyForInput;

    private WeaponProjectileHandler projectileHandler;
    private WeaponFiringLogicHandler firingLogicHandler;
    private WeaponAudioHandler audioHandler;
    private WeaponMuzzleFlashHandler muzzleFlashHandler;
    private WeaponSmokeHandler smokeHandler;

    public WeaponFirePanel() {
        super();
    }

    @Override
    protected void populateContent() {
        this.setLayout(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(this, new Insets(1, 0, 0, 0), "Firing & Audio");

        Supplier<Boolean> readinessChecker = () -> readyForInput;
        Runnable onChange = this::processChange;
        Supplier<WeaponSpecFile> specSupplier = () -> cachedLayer != null ? cachedLayer.getSpecFile() : null;

        projectileHandler = new WeaponProjectileHandler(readinessChecker, onChange, specSupplier);
        firingLogicHandler = new WeaponFiringLogicHandler(readinessChecker, onChange, specSupplier);
        audioHandler = new WeaponAudioHandler(readinessChecker, onChange, specSupplier);
        muzzleFlashHandler = new WeaponMuzzleFlashHandler(readinessChecker, onChange, specSupplier);
        smokeHandler = new WeaponSmokeHandler(readinessChecker, onChange, specSupplier);

        int row = 0;
        row = projectileHandler.populate(this, row);
        row = firingLogicHandler.populate(this, row);
        row = audioHandler.populate(this, row);
        row = muzzleFlashHandler.populate(this, row);
        row = smokeHandler.populate(this, row);

        clearData();
    }

    @Override
    public void refreshContent(LayerPainter layerPainter) {
        if (layerPainter == null || !(layerPainter.getParentLayer() instanceof WeaponLayer weaponLayer)) {
            clearData();
            return;
        }
        cachedLayer = weaponLayer;
        WeaponSpecFile spec = cachedLayer.getSpecFile();
        if (spec == null) {
            clearData();
            return;
        }

        readyForInput = false;

        projectileHandler.refresh(spec);
        firingLogicHandler.refresh(spec);
        audioHandler.refresh(spec);
        muzzleFlashHandler.refresh(spec);
        smokeHandler.refresh(spec);

        readyForInput = true;
    }

    private void clearData() {
        readyForInput = false;

        if (projectileHandler != null) projectileHandler.clear();
        if (firingLogicHandler != null) firingLogicHandler.clear();
        if (audioHandler != null) audioHandler.clear();
        if (muzzleFlashHandler != null) muzzleFlashHandler.clear();
        if (smokeHandler != null) smokeHandler.clear();

        cachedLayer = null;
    }
}
