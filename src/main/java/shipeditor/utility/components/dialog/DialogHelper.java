package shipeditor.utility.components.dialog;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.HullSaveQueued;
import shipeditor.communication.events.files.FileEvents.VariantSaveQueued;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.text.StringManager;

import javax.swing.JOptionPane;
import java.nio.file.Path;

public final class DialogHelper {

    private DialogHelper() {}

    public static void showDuplicateIDError() {
        JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                StringManager.getString("INPUT_ID_ALREADY_ASSIGNED_TO_SLOT_MSG"),
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
                    StringManager.getString("HULL_HAS_UNSAVED_CHANGES_DO_YOU_WANT_TO_MSG"),
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
                    StringManager.getString("VARIANT_HAS_UNSAVED_CHANGES_DO_YOU_WANT_MSG"),
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

    /**
     * Resolves the human-readable display name for a package folder path.
     *
     * @param packagePath the directory path of the package/mod
     * @return "Starsector Core", the folder name, or "Unknown" if null
     */
    public static String resolvePackageName(Path packagePath) {
        if (packagePath == null) {
            return "Unknown";
        }
        if (SettingsManager.isCoreFolder(packagePath)) {
            return "Starsector Core";
        }
        Path fileName = packagePath.getFileName();
        return fileName != null ? fileName.toString() : packagePath.toString();
    }

    /**
     * Resolves the display name for a mod ID string.
     *
     * @param modId the mod ID string
     * @return "Starsector Core" if core/blank, otherwise the mod ID
     */
    public static String resolveModIdName(String modId) {
        if (modId == null || modId.isBlank() || "starsector-core".equals(modId)) {
            return "Starsector Core";
        }
        return modId;
    }

}
