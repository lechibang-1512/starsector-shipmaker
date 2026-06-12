package shipeditor.components.datafiles.trees;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.ComponentEvents.DataTreesReloadQueued;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.SettingsManager;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DataTreeContextMenuController extends MouseAdapter {

    private final DataTreePanel treePanel;
    private final JTree tree;
    private final Class<?> entryClass;

    public DataTreeContextMenuController(DataTreePanel treePanel, JTree tree,
            Class<?> entryClass) {
        this.treePanel = treePanel;
        this.tree = tree;
        this.entryClass = entryClass;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(javax.swing.SwingUtilities.isRightMouseButton(e)){
            TreePath pathForLocation = tree.getPathForLocation(e.getPoint().x, e.getPoint().y);
            if (pathForLocation != null) {
                DefaultMutableTreeNode cachedSelectForMenu = (DefaultMutableTreeNode) pathForLocation
                        .getLastPathComponent();
                treePanel.setCachedSelectForMenu(cachedSelectForMenu);
                JPopupMenu contextMenu = treePanel.getContextMenu();
                showMenuIfMatching(contextMenu, pathForLocation, e, cachedSelectForMenu);
            } else {
                treePanel.setCachedSelectForMenu(null);
            }
        }
        super.mousePressed(e);
    }

    private void showMenuIfMatching(JPopupMenu contextMenu, TreePath pathForLocation, MouseEvent e,
            DefaultMutableTreeNode cachedSelectForMenu) {
        Object userObject = cachedSelectForMenu.getUserObject();
        if (entryClass.isInstance(userObject)) {
            tree.setSelectionPath(pathForLocation);
            contextMenu.show(tree, e.getPoint().x, e.getPoint().y);
        } else if (userObject instanceof GameDataPackage dataPackage && !SettingsManager.isCoreFolder(dataPackage)) {
            JPopupMenu menu = new JPopupMenu();

            if (dataPackage.isPinned()) {
                JMenuItem unpinPackage = new JMenuItem("Unpin package");
                unpinPackage.addActionListener(event -> {
                    dataPackage.setPinned(false);
                    SettingsManager.updateFileFromRuntime();
                    EventBus.publish(new DataTreesReloadQueued());
                });
                menu.add(unpinPackage);
            } else {
                JMenuItem pinPackage = new JMenuItem("Pin package");
                pinPackage.addActionListener(event -> {
                    dataPackage.setPinned(true);
                    SettingsManager.updateFileFromRuntime();
                    EventBus.publish(new DataTreesReloadQueued());
                });
                menu.add(pinPackage);
            }

            JMenuItem disablePackage = new JMenuItem("Disable package");
            disablePackage.addActionListener(event -> {
                dataPackage.setDisabled(true);
                SettingsManager.updateFileFromRuntime();
                EventBus.publish(new DataTreesReloadQueued());
            });
            menu.add(disablePackage);

            menu.show(tree, e.getPoint().x, e.getPoint().y);
        }
    }

}
