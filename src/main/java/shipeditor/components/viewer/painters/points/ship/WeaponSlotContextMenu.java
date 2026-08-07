package shipeditor.components.viewer.painters.points.ship;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.points.PointEvents.PointSelectQueued;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import shipeditor.undo.EditDispatch;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.Map;

public final class WeaponSlotContextMenu {

    private WeaponSlotContextMenu() {
    }

    public static JPopupMenu createModuleContextMenu(WeaponSlotPoint slotPoint, ShipVariant activeVariant) {
        JPopupMenu menu = new JPopupMenu();
        
        Map<String, InstalledFeature> fittedModules = activeVariant != null ? activeVariant.getFittedModules() : null;
        
        InstalledFeature installed = null;
        if (fittedModules != null) {
            installed = fittedModules.get(slotPoint.getId());
        }
        
        if (installed != null) {
            JMenuItem infoItem = new JMenuItem("Installed: " + installed.getName());
            infoItem.setEnabled(false);
            menu.add(infoItem);
            
            JMenuItem clearItem = new JMenuItem("Clear module");
            InstalledFeature toRemove = installed;
            clearItem.addActionListener(e -> EditDispatch.postFeatureUninstalled(fittedModules, slotPoint.getId(), toRemove, null));
            menu.add(clearItem);
        } else {
            JMenuItem emptyItem = new JMenuItem("Slot: " + slotPoint.getId() + " (Empty)");
            emptyItem.setEnabled(false);
            menu.add(emptyItem);
        }
        
        menu.addSeparator();
        
        JMenuItem selectItem = new JMenuItem("Select slot for install");
        selectItem.addActionListener(e -> EventBus.publish(new PointSelectQueued(slotPoint)));
        menu.add(selectItem);
        
        return menu;
    }
}
