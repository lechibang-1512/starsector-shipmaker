package shipeditor.components.datafiles.trees;

import lombok.extern.log4j.Log4j2;
import shipeditor.components.ComponentEnums.OpenDataTarget;
import shipeditor.components.datafiles.entities.CSVEntry;
import shipeditor.parsing.FileUtilities;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Log4j2
public abstract class CSVDataTreePanel<T extends CSVEntry> extends DataTreePanel {

    CSVDataTreePanel(String rootName) {
        super(rootName);
    }

    protected abstract Action getLoadDataAction();

    protected abstract String getEntryTypeName();

    @Override
    protected JTree createCustomTree() {
        JTree custom = super.createCustomTree();
        custom.setCellRenderer(new CSVDataCellRenderer());
        return custom;
    }

    private static class CSVDataCellRenderer extends DefaultTreeCellRenderer {

        @SuppressWarnings("ParameterHidesMemberVariable")
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object object = ((DefaultMutableTreeNode) value).getUserObject();
            DataTreePanel.configureCellRendererColors(object, this);
            return this;
        }

    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    @Override
    protected String getTooltipForEntry(Object entry) {
        if (entry instanceof GameDataPackage dataPackage) {
            return DataTreePanel.getTooltipForPackage(dataPackage);
        } else if (entry instanceof CSVEntry checked) {
            return checked.getMultilineTooltip();
        }
        return null;
    }

    @Override
    protected JPanel createTopPanel() {
        return null;
    }

    @Override
    public void reload() {
        Map<Path, List<T>> packages = getPackageList();
        populateEntries(packages);
    }

    void populateEntries(Map<Path, List<T>> entriesByPackage) {
        DefaultMutableTreeNode rootNode = getRootNode();
        rootNode.removeAllChildren();
        loadAllEntries(entriesByPackage);
        sortAndExpandTree();
        repaint();
    }

    protected abstract Map<String, T> getRepository();

    protected abstract Map<Path, List<T>> getPackageList();

    protected abstract void setLoadedStatus();

    protected void loadAllEntries(Map<Path, List<T>> entries) {
        Map<String, T> entriesRepository = getRepository();
        for (Map.Entry<Path, List<T>> entry : entries.entrySet()) {
            Settings settings = SettingsManager.getSettings();
            Path path = entry.getKey();
            String folderName = FileUtilities.extractFolderName(path.toString());
            GameDataPackage dataPackage = settings.getPackage(folderName);
            if (dataPackage == null || dataPackage.isDisabled()) {
                continue;
            }

            DefaultMutableTreeNode packageRoot = createPackageNode(entry, entriesRepository);
            DefaultMutableTreeNode rootNode = getRootNode();
            rootNode.add(packageRoot);
        }
        log.info("Total {} {} entries registered.", entriesRepository.size(), getEntryTypeName());
        setLoadedStatus();
    }

    private DefaultMutableTreeNode createPackageNode(Map.Entry<Path, List<T>> entryFolder,
                                                     Map<String, T> entriesRepository) {
        Path path = entryFolder.getKey();
        String packagePath = path.toString();
        String folderName = FileUtilities.extractFolderName(packagePath);
        Settings settings = SettingsManager.getSettings();

        DefaultMutableTreeNode result;
        if (SettingsManager.isCoreFolder(folderName)) {
            GameDataPackage corePackage = SettingsManager.getCorePackage();
            result = new DefaultMutableTreeNode(corePackage);
            for (T entry : entryFolder.getValue()) {
                MutableTreeNode entryNode = new DefaultMutableTreeNode(entry);
                entriesRepository.putIfAbsent(entry.getID(), entry);
                result.add(entryNode);
            }
        } else {
            GameDataPackage dataPackage = settings.getPackage(folderName);
            result = new DefaultMutableTreeNode(dataPackage);

            for (T entry : entryFolder.getValue()) {
                MutableTreeNode entryNode = new DefaultMutableTreeNode(entry);
                entriesRepository.putIfAbsent(entry.getID(), entry);
                result.add(entryNode);
            }
        }

        return result;
    }

    @Override
    protected void initTreePanelListeners(JPanel passedTreePanel) {
        initComponentListeners();
        initWalkerListening();
    }

    protected abstract void initWalkerListening();

    private void initComponentListeners() {
        JTree tree = getTree();
        tree.addMouseListener(createContextMenuListener());
        tree.addTreeSelectionListener(e -> {
            TreePath selectedNode = e.getNewLeadSelectionPath();
            if (selectedNode == null) return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedNode.getLastPathComponent();
            T entryObject = getObjectFromNode(node);
            if (entryObject != null) {
                updateEntryPanel(entryObject);
            }
        });
    }

    protected abstract void updateEntryPanel(T selected);

    protected abstract T getObjectFromNode(DefaultMutableTreeNode node);

    @Override
    protected void openEntryPath(OpenDataTarget target) {
        DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
        T entryObject = getObjectFromNode(cachedSelectForMenu);
        if (entryObject == null) return;
        Path toOpen;
        switch (target) {
            case FILE -> toOpen = entryObject.getTableFilePath();
            case CONTAINER -> {
                toOpen = entryObject.getTableFilePath().getParent();
                if (toOpen == null) return;
            }
            default -> toOpen = entryObject.getPackageFolderPath();
        }
        FileUtilities.openPathInDesktop(toOpen);
    }

}
