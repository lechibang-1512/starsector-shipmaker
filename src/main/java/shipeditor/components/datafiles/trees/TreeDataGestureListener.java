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
                if ("SHIP".equals(type)) {
                    var map = gameData.getAllShipEntries();
                    if (map != null) userObject = map.get(file.getEntityId());
                } else if ("WEAPON".equals(type)) {
                    var map = gameData.getAllWeaponEntries();
                    if (map != null) userObject = map.get(file.getEntityId());
                } else if ("HULLMOD".equals(type)) {
                    var map = gameData.getAllHullmodEntries();
                    if (map != null) userObject = map.get(file.getEntityId());
                } else if ("WING".equals(type)) {
                    var map = gameData.getAllWingEntries();
                    if (map != null) userObject = map.get(file.getEntityId());
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
