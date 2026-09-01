package shipeditor.utility.components.dialog;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.HullSaveQueued;
import shipeditor.communication.events.files.FileEvents.VariantSaveQueued;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;

import javax.swing.JOptionPane;

public final class DialogHelper {

    private DialogHelper() {}

    public static void showDuplicateIDError() {
        JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                "Input ID already assigned to slot.",
                "Duplicate ID",
                JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmLayerRemoval(LayerManager manager, ViewerLayer layer) {
        if (!(layer instanceof ShipLayer shipLayer)) {
            return true;
        }

        boolean hullDirty = manager.isHullDirty(shipLayer);
        boolean variantDirty = manager.isVariantDirty(shipLayer);

        if (!hullDirty && !variantDirty) {
            return true;
        }

        manager.setActiveLayer(shipLayer);

        String title = "Unsaved Changes";
        String message = "Layer has unsaved changes. Do you want to save them before closing?";

        if (hullDirty && variantDirty) {
            Object[] options = {"Save Both", "Save Hull", "Save Variant", "Don't Save", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                    shipeditor.PrimaryWindow.getInstance(),
                    message,
                    title,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            switch (choice) {
                case 0 -> { // Save Both
                    EventBus.publish(new HullSaveQueued(shipLayer));
                    ShipVariant variant = shipLayer.getActiveVariant();
                    if (variant != null && !variant.isEmpty()) {
                        EventBus.publish(new VariantSaveQueued(variant));
                    }
                    return !manager.isHullDirty(shipLayer) && !manager.isVariantDirty(shipLayer);
                }
                case 1 -> { // Save Hull
                    EventBus.publish(new HullSaveQueued(shipLayer));
                    return !manager.isHullDirty(shipLayer);
                }
                case 2 -> { // Save Variant
                    ShipVariant variant = shipLayer.getActiveVariant();
                    if (variant != null && !variant.isEmpty()) {
                        EventBus.publish(new VariantSaveQueued(variant));
                    }
                    return !manager.isVariantDirty(shipLayer);
                }
                case 3 -> { // Don't Save
                    manager.getUnsavedChangesRegistry().remove(shipLayer);
                    return true;
                }
                default -> { // Cancel
                    return false;
                }
            }
        } else if (hullDirty) {
            Object[] options = {"Save Hull", "Don't Save", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                    shipeditor.PrimaryWindow.getInstance(),
                    "Hull has unsaved changes. Do you want to save?",
                    title,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            switch (choice) {
                case 0 -> { // Save Hull
                    EventBus.publish(new HullSaveQueued(shipLayer));
                    return !manager.isHullDirty(shipLayer);
                }
                case 1 -> { // Don't Save
                    manager.getUnsavedChangesRegistry().remove(shipLayer);
                    return true;
                }
                default -> { // Cancel
                    return false;
                }
            }
        } else { // variantDirty
            Object[] options = {"Save Variant", "Don't Save", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                    shipeditor.PrimaryWindow.getInstance(),
                    "Variant has unsaved changes. Do you want to save?",
                    title,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            switch (choice) {
                case 0 -> { // Save Variant
                    ShipVariant variant = shipLayer.getActiveVariant();
                    if (variant != null && !variant.isEmpty()) {
                        EventBus.publish(new VariantSaveQueued(variant));
                    }
                    return !manager.isVariantDirty(shipLayer);
                }
                case 1 -> { // Don't Save
                    manager.getUnsavedChangesRegistry().remove(shipLayer);
                    return true;
                }
                default -> { // Cancel
                    return false;
                }
            }
        }
    }

}
