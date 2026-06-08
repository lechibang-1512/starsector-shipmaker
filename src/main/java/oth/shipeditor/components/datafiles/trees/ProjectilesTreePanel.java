package oth.shipeditor.components.datafiles.trees;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.files.WeaponTreeReloadQueued;
import oth.shipeditor.components.datafiles.OpenDataTarget;
import oth.shipeditor.components.viewer.layers.weapon.ProjectileLayer;
import oth.shipeditor.components.layering.ProjectileLayerTab;
import oth.shipeditor.components.viewer.layers.weapon.ProjectileLayerPainter;
import oth.shipeditor.parsing.FileUtilities;
import oth.shipeditor.parsing.loading.FileLoading;
import oth.shipeditor.persistence.GameDataPackage;
import oth.shipeditor.persistence.Settings;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.representation.GameDataRepository;
import oth.shipeditor.representation.weapon.ProjectileSpecFile;
import oth.shipeditor.utility.components.ComponentUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Log4j2
public class ProjectilesTreePanel extends DataTreePanel {

    public ProjectilesTreePanel() {
        super("Projectile file packages");
    }

    @Override
    protected void initTreePanelListeners(JPanel passedTreePanel) {
        EventBus.subscribe(this, event -> {
            if (event instanceof WeaponTreeReloadQueued) {
                this.reload();
            }
        });

        JTree tree = getTree();
        tree.addMouseListener(new ContextMenuListener());
        tree.addMouseListener(new DoubleClickLayerLoader());
    }

    @Override
    public void reload() {
        JTree tree = getTree();
        DefaultMutableTreeNode rootNode = getRootNode();
        rootNode.removeAllChildren();
        reloadProjectileList();
        sortAndExpandTree();
        repaint();
        tree.repaint();
    }

    private void reloadProjectileList() {
        GameDataRepository gameData = SettingsManager.getGameData();
        Map<Path, List<ProjectileSpecFile>> projectileEntries = gameData.getProjectileEntriesByPackage();

        if (projectileEntries == null || projectileEntries.isEmpty()) return;

        for (Map.Entry<Path, List<ProjectileSpecFile>> folder : projectileEntries.entrySet()) {
            Settings settings = SettingsManager.getSettings();
            GameDataPackage dataPackage = settings.getPackage(folder.getKey());
            if (dataPackage == null || dataPackage.isDisabled()) {
                continue;
            }

            DefaultMutableTreeNode packageRoot = createPackageNode(folder);
            DefaultMutableTreeNode rootNode = getRootNode();
            rootNode.add(packageRoot);
        }
    }

    private static DefaultMutableTreeNode createPackageNode(Map.Entry<Path, List<ProjectileSpecFile>> folder) {
        Path folderPath = folder.getKey();
        String packageName = folderPath.getFileName().toString();
        Settings settings = SettingsManager.getSettings();

        DefaultMutableTreeNode result;
        if (SettingsManager.isCoreFolder(folderPath)) {
            GameDataPackage corePackage = SettingsManager.getCorePackage();
            result = new DefaultMutableTreeNode(corePackage);
        } else {
            GameDataPackage dataPackage = settings.getPackage(packageName);
            result = new DefaultMutableTreeNode(dataPackage);
        }

        for (ProjectileSpecFile entry : folder.getValue()) {
            MutableTreeNode node = new DefaultMutableTreeNode(entry);
            result.add(node);
        }

        return result;
    }

    @Override
    protected Class<?> getEntryClass() {
        return ProjectileSpecFile.class;
    }

    @Override
    protected JPanel createTopPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        return buttonPanel;
    }

    @Override
    protected String getTooltipForEntry(Object entry) {
        if (entry instanceof ProjectileSpecFile proj) {
            return "<html><b>" + proj.getId() + "</b><br>(Double-click to load as layer)</html>";
        } else if (entry instanceof GameDataPackage dataPackage) {
            return DataTreePanel.getTooltipForPackage(dataPackage);
        }
        return null;
    }

    @Override
    JPopupMenu getContextMenu() {
        JPopupMenu menu = super.getContextMenu();
        DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
        if (cachedSelectForMenu.getUserObject() instanceof ProjectileSpecFile) {
            menu.addSeparator();
            JMenuItem loadAsLayer = new JMenuItem("Load as projectile layer");
            loadAsLayer.addActionListener(new LoadLayerFromTree());
            menu.add(loadAsLayer);
        }
        return menu;
    }

    @Override
    protected void openEntryPath(OpenDataTarget target) {
        DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
        if (!(cachedSelectForMenu.getUserObject() instanceof ProjectileSpecFile checked)) return;
        Path toOpen = checked.getProjectileSpecFilePath();
        if (target == OpenDataTarget.CONTAINER && toOpen != null) {
            toOpen = toOpen.getParent();
        }
        if (toOpen != null) {
            FileUtilities.openPathInDesktop(toOpen);
        }
    }

    private class LoadLayerFromTree extends AbstractAction {
        @Override
        public boolean isEnabled() {
            DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
            return super.isEnabled() && cachedSelectForMenu.getUserObject() instanceof ProjectileSpecFile;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
            if (cachedSelectForMenu.getUserObject() instanceof ProjectileSpecFile checked) {
                loadProjectileAsLayer(checked);
            }
        }
    }

    private class DoubleClickLayerLoader extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 2) return;
            JTree tree = getTree();
            Point eventPoint = e.getPoint();
            TreePath pathForLocation = tree.getPathForLocation(eventPoint.x, eventPoint.y);
            if (pathForLocation == null) return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) pathForLocation.getLastPathComponent();
            if (node != null && node.getUserObject() instanceof ProjectileSpecFile checked) {
                loadProjectileAsLayer(checked);
            }
        }
    }

    private void loadProjectileAsLayer(ProjectileSpecFile specFile) {
        ProjectileLayer layer = new ProjectileLayer();
        layer.setSpecFile(specFile);

        String spritePathStr = specFile.getSprite();
        if (spritePathStr != null) {
            Path spritePath = Path.of(spritePathStr);
            Path containingPackage = specFile.getContainingPackage();
            java.io.File file = FileLoading.fetchDataFile(spritePath, containingPackage);
            oth.shipeditor.utility.graphics.Sprite sprite = FileLoading.loadSprite(file);

            ProjectileLayerPainter painter = new ProjectileLayerPainter(layer, sprite, specFile);
            layer.setPainter(painter);

            oth.shipeditor.communication.events.viewer.layers.weapons.ProjectileLayerCreated event = 
                new oth.shipeditor.communication.events.viewer.layers.weapons.ProjectileLayerCreated(layer);
            oth.shipeditor.utility.overseers.StaticController.getViewer().getLayerManager().getLayers().add(layer);
            oth.shipeditor.communication.EventBus.publish(event);
        }
    }

    @Override
    protected JTree createCustomTree() {
        JTree custom = super.createCustomTree();
        custom.setCellRenderer(new ProjectilesTreeCellRenderer());
        return custom;
    }

    private static class ProjectilesTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object object = ((DefaultMutableTreeNode) value).getUserObject();
            DataTreePanel.configureCellRendererColors(object, this);
            if (object instanceof ProjectileSpecFile checked && leaf) {
                setText(checked.getId());
            }
            return this;
        }
    }

}
