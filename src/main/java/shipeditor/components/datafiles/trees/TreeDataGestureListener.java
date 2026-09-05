package shipeditor.components.datafiles.trees;

import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.components.datafiles.entities.WingCSVEntry;
import shipeditor.components.datafiles.entities.transferable.Transferables.TransferableHullmod;
import shipeditor.components.datafiles.entities.transferable.Transferables.TransferableWeapon;
import shipeditor.components.datafiles.entities.transferable.Transferables.TransferableEntry;
import shipeditor.components.datafiles.entities.transferable.Transferables.TransferableShip;
import shipeditor.components.datafiles.entities.transferable.Transferables.TransferableWing;
import shipeditor.components.viewer.ViewerDragListener;
import shipeditor.components.viewer.ViewerDropReceiver;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;

public class TreeDataGestureListener implements DragGestureListener {

    private final JTree tree;

    TreeDataGestureListener(JTree inputTree) {
        this.tree = inputTree;
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObject = node.getUserObject();

            if (userObject instanceof shipeditor.persistence.database.IndexedFile file) {
                var gameData = shipeditor.persistence.SettingsManager.getGameData();
                String type = file.getEntityType();
                switch (type) {
                    case "SHIP" -> {
                        if (gameData != null) userObject = gameData.getOrCreateShipEntry(file);
                    }
                    case "WEAPON" -> {
                        if (gameData != null) userObject = gameData.getOrCreateWeaponEntry(file);
                    }
                    case "HULLMOD" -> {
                        var map = gameData != null ? gameData.getAllHullmodEntries() : null;
                        if (map != null) userObject = map.get(file.getEntityId());
                    }
                    case "WING" -> {
                        var map = gameData != null ? gameData.getAllWingEntries() : null;
                        if (map != null) userObject = map.get(file.getEntityId());
                    }
                    default -> {}
                }
            }

            Transferable transferable;
            if (userObject instanceof ShipCSVEntry shipEntry) {
                transferable = new TransferableShip(shipEntry, tree);
                ViewerDropReceiver.commenceDragToViewer(shipEntry, TransferableEntry.TRANSFERABLE_SHIP);
            } else if (userObject instanceof WeaponCSVEntry weaponEntry) {
                transferable = new TransferableWeapon(weaponEntry, tree);
                ViewerDropReceiver.commenceDragToViewer(weaponEntry, TransferableEntry.TRANSFERABLE_WEAPON);
            } else if (userObject instanceof HullmodCSVEntry hullmodEntry) {
                transferable = new TransferableHullmod(hullmodEntry, tree);
            } else if (userObject instanceof WingCSVEntry wingEntry) {
                transferable = new TransferableWing(wingEntry, tree);
            } else {
                ViewerDropReceiver.finishDragToViewer();
                return;
            }

            dge.startDrag(DragSource.DefaultMoveDrop, transferable, new ViewerDragListener());
        }
    }

}
