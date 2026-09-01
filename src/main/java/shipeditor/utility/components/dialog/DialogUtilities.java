package shipeditor.utility.components.dialog;

import shipeditor.components.viewer.entities.WorldPoint;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.painters.points.ship.features.FittedWeaponGroup;
import shipeditor.undo.EditDispatch;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public final class DialogUtilities {

    private DialogUtilities() {
    }

    public static void showAdjustPointDialog(WorldPoint point) {
        PointChangeDialog dialog = new PointChangeDialog(point.getPosition());
        int option = JOptionPane.showConfirmDialog(null, dialog,
                "Change Point Position", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            Point2D newPosition = dialog.getUpdatedPosition();
            EditDispatch.postPointDragged(point, newPosition);
        }
    }

    public static void showWeaponGroupsDialog(ShipVariant variant) {
        WeaponGroupTableDialog dialog = new WeaponGroupTableDialog(variant);
        int option = JOptionPane.showConfirmDialog(null, dialog,
                "Rearrange Weapon Groups", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            List<FittedWeaponGroup> oldGroups = new ArrayList<>(variant.getWeaponGroups());
            List<FittedWeaponGroup> updatedGroups = dialog.getUpdatedGroups();

            EditDispatch.postWeaponGroupsUpdated(variant, oldGroups, updatedGroups);
        }
    }

    public static shipeditor.components.datafiles.entities.WeaponCSVEntry showWeaponPickerDialog(shipeditor.components.viewer.entities.weapon.WeaponSlotPoint slotPoint) {
        final JDialog[] dialogRef = new JDialog[1];
        
        PickWeaponDialog dialogPanel = new PickWeaponDialog(slotPoint, () -> {
            if (dialogRef[0] != null) {
                dialogRef[0].dispose();
            }
        });
        
        JOptionPane optionPane = new JOptionPane(dialogPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = optionPane.createDialog(null, "Select Weapon");
        dialog.setResizable(true);
        dialogRef[0] = dialog;
        
        dialog.setVisible(true);
        
        Object selectedValue = optionPane.getValue();
        if (selectedValue != null && selectedValue.equals(JOptionPane.OK_OPTION)) {
            return dialogPanel.getSelectedWeapon();
        } else if (selectedValue == null && dialogPanel.getSelectedWeapon() != null) {
             return dialogPanel.getSelectedWeapon();
        }
        return null;
    }

    public static shipeditor.components.datafiles.entities.HullmodCSVEntry showHullmodPickerDialog(shipeditor.representation.RepresentationEnums.HullSize hullSize, shipeditor.components.viewer.layers.ship.ShipLayer shipLayer) {
        java.nio.file.Path shipPackage = resolveShipPackage(shipLayer);
        return showHullmodPickerDialog(hullSize, shipPackage);
    }

    public static java.nio.file.Path resolveShipPackage(shipeditor.components.viewer.layers.ship.ShipLayer shipLayer) {
        if (shipLayer == null) return null;
        var shipPainter = shipLayer.getPainter();
        if (shipPainter != null) {
            var activeSkin = shipPainter.getActiveSkin();
            if (activeSkin != null && !activeSkin.isBase()) {
                return activeSkin.getContainingPackage();
            }
        }
        var shipHull = shipLayer.getHull();
        if (shipHull != null) {
            var shipEntry = shipeditor.representation.GameDataRepository.retrieveShipCSVEntryByID(shipHull.getHullID());
            if (shipEntry != null) {
                return shipEntry.getPackageFolderPath();
            }
        }
        return null;
    }

    public static shipeditor.components.datafiles.entities.HullmodCSVEntry showHullmodPickerDialog(shipeditor.representation.RepresentationEnums.HullSize hullSize, java.nio.file.Path shipPackage) {
        final JDialog[] dialogRef = new JDialog[1];

        PickHullmodDialog dialogPanel = new PickHullmodDialog(hullSize, shipPackage, () -> {
            if (dialogRef[0] != null) {
                dialogRef[0].dispose();
            }
        });

        JOptionPane optionPane = new JOptionPane(dialogPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = optionPane.createDialog(null, "Select Hullmod");
        dialog.setResizable(true);
        dialogRef[0] = dialog;

        dialog.setVisible(true);

        Object selectedValue = optionPane.getValue();
        if (selectedValue != null && selectedValue.equals(JOptionPane.OK_OPTION)) {
            return dialogPanel.getSelectedHullmod();
        } else if (selectedValue == null && dialogPanel.getSelectedHullmod() != null) {
            return dialogPanel.getSelectedHullmod();
        }
        return null;
    }

    public static void showSlotCreationDialog() {
        SlotCreationDialog dialog = new SlotCreationDialog(shipeditor.PrimaryWindow.getInstance());
        dialog.setVisible(true);
    }

}
