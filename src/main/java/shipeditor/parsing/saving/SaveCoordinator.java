package shipeditor.parsing.saving;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.saving.HullSaveQueued;
import shipeditor.communication.events.files.saving.ProjectileSaveQueued;
import shipeditor.communication.events.files.saving.VariantSaveQueued;
import shipeditor.communication.events.files.saving.WeaponSaveQueued;

public final class SaveCoordinator {

    private SaveCoordinator() {
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    public static void init() {
        EventBus.subscribe(SaveCoordinator.class, event -> {
            if (event instanceof VariantSaveQueued checked) {
                SaveVariantAction.saveVariant(checked.variant());
            } else if (event instanceof HullSaveQueued checked) {
                SaveHullAction.saveHullFromLayer(checked.shipLayer());
            } else if (event instanceof WeaponSaveQueued checked) {
                SaveWeaponAction.saveWeaponFromLayer(checked.weaponLayer());
            } else if (event instanceof ProjectileSaveQueued checked) {
                SaveProjectileAction.saveProjectileFromLayer(checked.projectileLayer());
            } else if (event instanceof shipeditor.communication.events.files.saving.CSVSaveQueued checked) {
                SaveCSVAction.saveCSVEntry(checked.entry());
            }
        });
    }

}
