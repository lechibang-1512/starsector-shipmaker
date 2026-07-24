package shipeditor.components.datafiles.trees;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.components.ComponentEnums.OpenDataTarget;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.parsing.FileUtilities;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.RepresentationEnums.HullSize;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.representation.ship.SkinSpecFile;
import shipeditor.utility.overseers.StaticController;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import shipeditor.utility.components.UIConstants;
import shipeditor.communication.events.components.ComponentEvents.SelectShipDataEntry;
import shipeditor.communication.events.components.ComponentEvents.GameDataPanelResized;
import shipeditor.communication.events.files.FileEvents.HullTreeEntryCleared;
import shipeditor.communication.events.files.FileEvents.HullTreeReloadQueued;

@Log4j2
public class HullsTreePanel extends DataTreePanel {

    public HullsTreePanel() {
        super("Hull file packages");
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    @Override
    protected String getTooltipForEntry(Object entry) {
        if (entry instanceof ShipCSVEntry shipEntry) {

            String dragHint = "(Double-click or drag to load as layer)";
            if (StaticController.getEditorMode() == EditorInstrument.VARIANT_MODULES) {
                dragHint = "(Drag to install as module)";
            }
            return shipEntry.getMultilineTooltip(dragHint);
        } else if (entry instanceof GameDataPackage dataPackage) {
            return DataTreePanel.getTooltipForPackage(dataPackage);
        }
        return null;
    }

    @Override
    protected void initTreePanelListeners(JPanel passedTreePanel) {
        this.initBusListening();
        this.initComponentListeners();
    }

    private void initBusListening() {
        JTree tree = getTree();
        EventBus.subscribe(this, event -> {
            if (event instanceof HullTreeEntryCleared) {
                resetInfoPanel();
                repaint();
                tree.repaint();
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof HullTreeReloadQueued) {
                this.queueReload();
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof SelectShipDataEntry checked) {
                ShipCSVEntry entry = checked.entry();
                DefaultMutableTreeNode node = getNodeOfEntry(entry);
                if (node != null) {
                    TreePath path = new TreePath(node.getPath());
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                }
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    @Override
    public void reload() {
        JTree tree = getTree();
        DefaultMutableTreeNode rootNode = getRootNode();
        rootNode.removeAllChildren();
        reloadHullList();
        sortAndExpandTree();
        repaint();
        tree.repaint();
    }

    @Override
    protected JPanel createTopPanel() {
        return new JPanel();
    }

    protected JPanel createSearchContainer() {
        JPanel searchContainer = new JPanel(new GridBagLayout());
        searchContainer.setBorder(UIConstants.EMPTY_BORDER);
        JTextField searchField = this.getSearchField();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        searchContainer.add(searchField, gridBagConstraints);
        return searchContainer;
    }

    private JTextField getSearchField() {
        JTextField searchField = new JTextField();
        searchField.setToolTipText("Input is checked against displayed filename and base hull ID as a substring.");
        javax.swing.Timer timer = new javax.swing.Timer(300, e -> {
            ShipFilterPanel.setCurrentTextFilter(searchField.getText());
            this.reload();
        });
        timer.setRepeats(false);

        Document document = searchField.getDocument();
        document.addDocumentListener(new SearchFieldDocumentListener(timer));
        return searchField;
    }

    private void initComponentListeners() {
        JTree tree = getTree();
        tree.addMouseListener(createContextMenuListener());
        tree.addTreeSelectionListener(e -> {
            TreePath selectedNode = e.getNewLeadSelectionPath();
            if (selectedNode == null)
                return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedNode.getLastPathComponent();
            if (node.getUserObject() instanceof ShipCSVEntry checked) {
                JPanel rightPanel = getRightPanel();
                rightPanel.removeAll();
                rightPanel.add(new javax.swing.JLabel("Loading..."));
                rightPanel.revalidate();
                rightPanel.repaint();

                checked.lazyLoadSpecAndSkins().thenAccept(v -> {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        updateEntryPanel(checked);
                        EventBus.publish(new GameDataPanelResized(this.getMinimumSize()));
                    });
                });
            }
        });
        tree.addMouseListener(new DoubleClickLayerLoader());
    }

    void updateEntryPanel(ShipCSVEntry selected) {
        JPanel rightPanel = getRightPanel();
        rightPanel.removeAll();
        GridBagConstraints constraints = DataTreePanel.getDefaultConstraints();
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 5, 0, 5);
        ShipFilesSubpanel shipFilesSubpanel = new ShipFilesSubpanel(rightPanel);
        JPanel shipFilesPanel = shipFilesSubpanel.createShipFilesPanel(selected, this);

        rightPanel.add(shipFilesPanel, constraints);

        createRightPanelDataTable(selected);

        rightPanel.revalidate();
        rightPanel.repaint();
    }

    private void reloadHullList() {
        Map<Path, List<ShipCSVEntry>> shipEntries = ShipFilterPanel.getFilteredEntries();

        if (shipEntries == null || shipEntries.isEmpty())
            return;

        for (Map.Entry<Path, List<ShipCSVEntry>> hullFolder : shipEntries.entrySet()) {
            Settings settings = SettingsManager.getSettings();
            GameDataPackage dataPackage = settings.getPackage(hullFolder.getKey());
            if (dataPackage == null || dataPackage.isDisabled()) {
                continue;
            }

            DefaultMutableTreeNode packageRoot = HullsTreePanel.createPackageNode(hullFolder);
            DefaultMutableTreeNode rootNode = getRootNode();
            rootNode.add(packageRoot);
        }
    }

    private static DefaultMutableTreeNode createPackageNode(Map.Entry<Path, List<ShipCSVEntry>> hullFolder) {
        Path folderPath = hullFolder.getKey();
        Path fileNamePath = folderPath.getFileName();
        String packageName = fileNamePath != null ? fileNamePath.toString() : "";

        Settings settings = SettingsManager.getSettings();

        DefaultMutableTreeNode result;
        if (SettingsManager.isCoreFolder(folderPath)) {
            GameDataPackage corePackage = SettingsManager.getCorePackage();
            result = new DefaultMutableTreeNode(corePackage);
            for (ShipCSVEntry entry : hullFolder.getValue()) {
                MutableTreeNode shipNode = new DefaultMutableTreeNode(entry);
                result.add(shipNode);
            }
        } else {
            GameDataPackage dataPackage = settings.getPackage(packageName);
            result = new DefaultMutableTreeNode(dataPackage);

            for (ShipCSVEntry entry : hullFolder.getValue()) {
                MutableTreeNode shipNode = new DefaultMutableTreeNode(entry);
                result.add(shipNode);
            }
        }

        return result;
    }

    private class LoadLayerFromTree extends AbstractAction {
        @Override
        public boolean isEnabled() {
            DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
            return super.isEnabled() && cachedSelectForMenu.getUserObject() instanceof ShipCSVEntry;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
            if (cachedSelectForMenu.getUserObject() instanceof ShipCSVEntry checked) {
                checked.loadLayerFromEntry();
            }
        }
    }

    @Override
    protected Class<?> getEntryClass() {
        return ShipCSVEntry.class;
    }

    @Override
    protected JTree createCustomTree() {
        JTree custom = super.createCustomTree();
        custom.setCellRenderer(new HullsTreeCellRenderer());
        return custom;
    }

    @Override
    JPopupMenu getContextMenu() {
        JPopupMenu menu = super.getContextMenu();
        DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
        JMenuItem loadAsLayer = new JMenuItem("Load as ship layer");
        loadAsLayer.addActionListener(new HullsTreePanel.LoadLayerFromTree());
        menu.insert(loadAsLayer, 0);
        menu.insert(new JPopupMenu.Separator(), 1);

        if (cachedSelectForMenu.getUserObject() instanceof ShipCSVEntry checked) {
            JMenuItem openSkin = HullsTreePanel.addOpenSkinOption(checked);
            if (openSkin != null) {
                menu.addSeparator();
                menu.add(openSkin);
            }
        }
        return menu;
    }

    private static JMenuItem addOpenSkinOption(ShipCSVEntry checked) {
        SkinSpecFile activeSkinSpecFile = checked.getActiveSkinSpecFile();
        if (activeSkinSpecFile == null || activeSkinSpecFile.isBase())
            return null;
        JMenuItem openSkin = new JMenuItem("Open skin file");
        openSkin.addActionListener(e -> {
            Path toOpen = activeSkinSpecFile.getFilePath();
            FileUtilities.openPathInDesktop(toOpen);
        });
        return openSkin;
    }

    @Override
    protected void openEntryPath(OpenDataTarget target) {
        DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
        if (!(cachedSelectForMenu.getUserObject() instanceof ShipCSVEntry checked))
            return;
        HullSpecFile hullSpecFileFile = checked.getHullSpecFile();
        if (hullSpecFileFile == null) {
            log.error("Hull spec file not loaded for ID: {}", checked.getHullID());
            return;
        }
        Path toOpen;
        switch (target) {
            case FILE -> toOpen = hullSpecFileFile.getFilePath();
            case CONTAINER -> {
                toOpen = hullSpecFileFile.getFilePath().getParent();
                if (toOpen == null)
                    return;
            }
            default -> toOpen = checked.getPackageFolderPath();
        }
        FileUtilities.openPathInDesktop(toOpen);
    }

    private static class HullsTreeCellRenderer extends DefaultTreeCellRenderer {

        @SuppressWarnings("ParameterHidesMemberVariable")
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object object = ((DefaultMutableTreeNode) value).getUserObject();
            DataTreePanel.configureCellRendererColors(object, this);
            setIcon(null);
            if (object instanceof ShipCSVEntry checked && leaf) {
                HullSize hullSize = checked.getSize();
                if (hullSize != null) {
                    setText("[" + hullSize.getDisplayedName() + "] " + getText());
                }
            }
            return this;
        }

    }

    private class DoubleClickLayerLoader extends MouseAdapter {

        @SuppressWarnings("ChainOfInstanceofChecks")
        @Override
        public void mouseClicked(MouseEvent e) {
            // Check for double-click.
            if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 2)
                return;
            JTree tree = getTree();
            Point eventPoint = e.getPoint();
            TreePath pathForLocation = tree.getPathForLocation(eventPoint.x, eventPoint.y);
            if (pathForLocation == null)
                return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) pathForLocation.getLastPathComponent();
            if (node.getUserObject() instanceof ShipCSVEntry checked) {
                checked.loadLayerFromEntry();
            }
        }
    }

    private static class SearchFieldDocumentListener implements DocumentListener {
        private final javax.swing.Timer timer;

        SearchFieldDocumentListener(javax.swing.Timer timer) {
            this.timer = timer;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            timer.restart();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            timer.restart();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            timer.restart();
        }
    }

}
