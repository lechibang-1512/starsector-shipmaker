package shipeditor.components.instrument.ship.variant;

import shipeditor.utility.text.StringManager;

import shipeditor.communication.EventBus;
import shipeditor.components.datafiles.entities.CSVEntry;
import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.components.viewer.painters.points.ship.features.FittedWeaponGroup;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import shipeditor.components.viewer.ViewerEnums.FireMode;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.components.rendering.CustomTreeNode;
import shipeditor.communication.events.components.ComponentEvents.SelectWeaponDataEntry;

import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.utility.components.dialog.DialogUtilities;
import shipeditor.utility.overseers.StaticController;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.tree.TreePath;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class WeaponTreeContextMenuController extends MouseAdapter {
    private final VariantWeaponsTree tree;

    public WeaponTreeContextMenuController(VariantWeaponsTree tree) {
        this.tree = tree;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON3){
            Point point = e.getPoint();
            TreePath pathForLocation = tree.getPathForLocation(point.x, point.y);
            if(pathForLocation != null){
                tree.setCachedSelectedNode((CustomTreeNode) pathForLocation.getLastPathComponent());
                showMenu(pathForLocation, e);
            } else{
                tree.setCachedSelectedNode(null);
            }
        }
        super.mousePressed(e);
    }

    private void showMenu(TreePath pathForLocation, MouseEvent e) {
        CustomTreeNode selectedNode = tree.getCachedSelectedNode();
        Object nodeUserObject = selectedNode.getUserObject();
        JPopupMenu contextMenu = createContextMenu(nodeUserObject);
        if (contextMenu != null) {
            tree.setSelectionPath(pathForLocation);
            contextMenu.show(tree, e.getPoint().x, e.getPoint().y);
        }
    }

    private JPopupMenu createContextMenu(Object nodeUserObject) {
        JPopupMenu contextMenu = null;

        if (nodeUserObject instanceof FittedWeaponGroup) {
            contextMenu = getWeaponGroupContextPopupMenu((FittedWeaponGroup) nodeUserObject);
        } else if (nodeUserObject instanceof InstalledFeature) {
            contextMenu = getInstalledFeatureContextMenu((InstalledFeature) nodeUserObject);
        } else if (nodeUserObject instanceof WeaponSlotPoint slot) {
            contextMenu = getEmptySlotContextMenu(slot);
        }

        return contextMenu;
    }

    private JPopupMenu getInstalledFeatureContextMenu(InstalledFeature feature) {
        JPopupMenu contextMenu = new JPopupMenu();

        WeaponSlotPoint slot = tree.getSlotPainter() != null ? tree.getSlotPainter().getSlotByID(feature.getSlotID()) : null;

        if (slot != null && !feature.isContainedInBuiltIns()) {
            JMenuItem changeFeature = new JMenuItem(StringManager.getString("CHANGE_WEAPON"));
            changeFeature.addActionListener(e -> {
                var layer = StaticController.getActiveLayer();
                if (layer instanceof ShipLayer shipLayer) {
                    WeaponCSVEntry picked = DialogUtilities.showWeaponPickerDialog(slot);
                    if (picked != null) {
                        shipLayer.getFeaturesOverseer().installWeapon(slot, picked);
                    }
                }
            });
            contextMenu.add(changeFeature);
        }

        if (!feature.isContainedInBuiltIns()) {
            JMenuItem uninstallFeature = new JMenuItem(StringManager.getString("UNINSTALL_FEATURE"));
            uninstallFeature.addActionListener(e -> {
                var group = feature.getParentGroup();
                EditDispatch.postFeatureUninstalled(group.getWeapons(), feature.getSlotID(),
                        feature, null);
            });
            contextMenu.add(uninstallFeature);
        }

        JMenuItem selectEntry = new JMenuItem(StringManager.getString("SELECT_WEAPON_ENTRY"));
        selectEntry.addActionListener(event ->  {
            CSVEntry dataEntry = feature.getDataEntry();
            if (dataEntry instanceof WeaponCSVEntry weaponEntry) {
                EventBus.publish(new SelectWeaponDataEntry(weaponEntry));
            }
        });
        contextMenu.add(selectEntry);
        return contextMenu;
    }

    private JPopupMenu getEmptySlotContextMenu(WeaponSlotPoint slot) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem installFeature = new JMenuItem(StringManager.getString("INSTALL_WEAPON"));
        installFeature.addActionListener(e -> {
            var layer = StaticController.getActiveLayer();
            if (layer instanceof ShipLayer shipLayer) {
                WeaponCSVEntry picked = DialogUtilities.showWeaponPickerDialog(slot);
                if (picked != null) {
                    shipLayer.getFeaturesOverseer().installWeapon(slot, picked);
                }
            }
        });
        contextMenu.add(installFeature);

        return contextMenu;
    }

    private JPopupMenu getWeaponGroupContextPopupMenu(FittedWeaponGroup weaponGroup) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenu modeSubmenu = getModeSubmenu(weaponGroup);
        contextMenu.add(modeSubmenu);

        JCheckBoxMenuItem autofire = new JCheckBoxMenuItem("Toggle autofire");
        autofire.setSelected(weaponGroup.isAutofire());
        autofire.addActionListener(e -> {
            weaponGroup.setAutofire(autofire.isSelected());
            tree.repaint();
        });
        contextMenu.add(autofire);

        contextMenu.addSeparator();

        JMenuItem removeGroup = new JMenuItem(StringManager.getString("REMOVE_WEAPON_GROUP"));
        removeGroup.addActionListener(e -> tree.removeWeaponGroup(weaponGroup));
        contextMenu.add(removeGroup);
        return contextMenu;
    }

    private JMenu getModeSubmenu(FittedWeaponGroup weaponGroup) {
        JMenu modeSubmenu = new JMenu(StringManager.getString("FIRING_MODE"));

        JMenuItem linkedMode = new JRadioButtonMenuItem("Mode: Linked");
        linkedMode.setSelected(weaponGroup.getMode() == FireMode.LINKED);
        linkedMode.addActionListener(e -> {
            weaponGroup.setMode(FireMode.LINKED);
            tree.repaint();
        });
        modeSubmenu.add(linkedMode);

        JMenuItem alternatingMode = new JRadioButtonMenuItem("Mode: Alternating");
        alternatingMode.setSelected(weaponGroup.getMode() == FireMode.ALTERNATING);
        alternatingMode.addActionListener(e -> {
            weaponGroup.setMode(FireMode.ALTERNATING);
            tree.repaint();
        });
        modeSubmenu.add(alternatingMode);
        return modeSubmenu;
    }
}
