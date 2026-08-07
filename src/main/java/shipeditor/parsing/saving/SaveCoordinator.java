package shipeditor.parsing.saving;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.HullSaveQueued;
import shipeditor.communication.events.files.FileEvents.VariantSaveQueued;
import shipeditor.communication.events.files.FileEvents.WeaponSaveQueued;
import shipeditor.communication.events.files.FileEvents.ProjectileSaveQueued;

import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.components.viewer.layers.weapon.ProjectileLayer;
import shipeditor.utility.overseers.StaticController;

import javax.swing.JOptionPane;

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
            } else if (event instanceof shipeditor.communication.events.files.FileEvents.CSVSaveQueued checked) {
                SaveCSVAction.saveCSVEntry(checked.entry());
            }
        });
    }

    public static void saveLayer(ViewerLayer layer) {
        if (layer instanceof ShipLayer shipLayer) {
            EventBus.publish(new HullSaveQueued(shipLayer));
            if (shipLayer.getPainter() != null && shipLayer.getPainter().getActiveVariant() != null && !shipLayer.getPainter().getActiveVariant().isEmpty()) {
                EventBus.publish(new VariantSaveQueued(shipLayer.getPainter().getActiveVariant()));
            }
        } else if (layer instanceof WeaponLayer weaponLayer) {
            EventBus.publish(new WeaponSaveQueued(weaponLayer));
        } else if (layer instanceof ProjectileLayer projLayer) {
            EventBus.publish(new ProjectileSaveQueued(projLayer));
        }
    }

    public static void saveActiveLayer() {
        LayerManager layerManager = StaticController.getLayerManager();
        if (layerManager != null && layerManager.getActiveLayer() != null) {
            saveLayer(layerManager.getActiveLayer());
        } else {
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(), "No active layer to save.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    public static void saveAllLayers() {
        LayerManager layerManager = StaticController.getLayerManager();
        if (layerManager != null) {
            for (ViewerLayer layer : layerManager.getLayers()) {
                saveLayer(layer);
            }
        }
    }

}
