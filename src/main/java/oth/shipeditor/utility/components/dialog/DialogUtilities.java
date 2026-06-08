package oth.shipeditor.utility.components.dialog;

import oth.shipeditor.components.viewer.entities.WorldPoint;
import oth.shipeditor.components.viewer.layers.ship.data.ShipVariant;
import oth.shipeditor.components.viewer.painters.points.ship.features.FittedWeaponGroup;
import oth.shipeditor.undo.EditDispatch;
import oth.shipeditor.utility.overseers.StaticController;

import javax.swing.*;
import java.awt.geom.Point2D;
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
            List<FittedWeaponGroup> updatedGroups = dialog.getUpdatedGroups();

            var oldGroups = variant.getWeaponGroups();
            updatedGroups.forEach(weaponGroup -> {
                int index = updatedGroups.indexOf(weaponGroup);
                if (index < oldGroups.size() - 1) {
                    var group = oldGroups.get(index);

                    if (group != null) {
                        weaponGroup.setAutofire(group.isAutofire());
                        weaponGroup.setMode(group.getMode());
                    }
                }
            });

            variant.setWeaponGroups(updatedGroups);

            var repainter = StaticController.getScheduler();
            repainter.queueVariantWeaponsRepaint();
        }
    }

    public static oth.shipeditor.components.datafiles.entities.WeaponCSVEntry showWeaponPickerDialog(oth.shipeditor.components.viewer.entities.weapon.WeaponSlotPoint slotPoint) {
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

}
