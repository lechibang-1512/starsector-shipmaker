package shipeditor.components.datafiles.trees;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import shipeditor.components.ComponentEnums.OpenDataTarget;
import shipeditor.components.datafiles.entities.CSVEntry;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.StringValues;
import shipeditor.utility.themes.Themes;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("ClassWithTooManyMethods")
@Log4j2
@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP" })
public abstract class DataTreePanel extends JPanel {

    @Getter
    private DefaultMutableTreeNode rootNode;

    @lombok.Setter
    @Getter
    private DefaultMutableTreeNode cachedSelectForMenu;

    @Getter
    private JTree tree;

    @Getter
    private JPanel rightPanel;

    private boolean reloadQueued;

    protected DataTreePanel(String rootName) {
        this.setLayout(new BorderLayout());
        JPanel topContainer = createTopPanel();
        if (topContainer != null) {
            topContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Themes.getBorderColor()));
            this.add(topContainer, BorderLayout.PAGE_START);
        }
        JPanel treePanel = createTreePanel(rootName);
        JSplitPane splitPane = createContentSplitter(treePanel);
        this.add(splitPane, BorderLayout.CENTER);

        this.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (this.isShowing() && this.reloadQueued) {
                    this.reload();
                    this.reloadQueued = false;
                }
            }
        });
    }

    public void queueReload() {
        if (this.isShowing()) {
            this.reload();
            this.reloadQueued = false;
        } else {
            this.reloadQueued = true;
        }
    }

    static boolean isCurrentSkinNotEligible() {
        var activeLayer = StaticController.getActiveLayer();
        var isShipLayer = activeLayer instanceof ShipLayer;
        ShipLayer shipLayer;
        if (isShipLayer) {
            shipLayer = (ShipLayer) activeLayer;
        } else
            return true;

        ShipPainter shipPainter = shipLayer.getPainter();
        if (shipPainter == null || shipPainter.isUninitialized())
            return true;
        var skin = shipPainter.getActiveSkin();
        return skin == null || skin.isBase();
    }

    protected abstract JPanel createTopPanel();

    void expandAllNodes() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    void resetInfoPanel() {
        rightPanel.removeAll();
        rightPanel.add(new JLabel(StringValues.NO_ENTRY_SELECTED));
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    private JSplitPane createContentSplitter(JPanel treeContainer) {
        rightPanel = new JPanel(new GridBagLayout());
        rightPanel.add(new JLabel(StringValues.NO_ENTRY_SELECTED));

        treeContainer.setMinimumSize(new Dimension(120, 100));
        rightPanel.setMinimumSize(new Dimension(120, 100));

        JSplitPane treeSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        treeSplitter.setOneTouchExpandable(true);
        float resizeWeight = getSplitterResizeWeight();
        treeSplitter.setResizeWeight(resizeWeight);
        treeSplitter.setLeftComponent(treeContainer);
        treeSplitter.setRightComponent(rightPanel);
        return treeSplitter;
    }

    protected float getSplitterResizeWeight() {
        return 0.4f;
    }

    private JPanel createTreePanel(String rootName) {
        JPanel createdTreePanel = new JPanel();
        rootNode = new DefaultMutableTreeNode(rootName);
        tree = createCustomTree();
        ToolTipManager.sharedInstance().registerComponent(tree);
        JScrollPane scrollContainer = new JScrollPane(tree);
        createdTreePanel.setLayout(new BorderLayout());
        JPanel searchContainer = createSearchContainer();
        createdTreePanel.add(searchContainer, BorderLayout.PAGE_START);
        createdTreePanel.add(scrollContainer, BorderLayout.CENTER);
        this.initTreePanelListeners(createdTreePanel);
        return createdTreePanel;
    }

    @SuppressWarnings("WeakerAccess")
    protected JPanel createSearchContainer() {
        return new DataTreeSearchController(this, tree).createSearchContainer();
    }

    static GridBagConstraints getDefaultConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.PAGE_START;
        constraints.weighty = 0.0;
        constraints.insets = new Insets(0, 0, 0, 0);
        return constraints;
    }

    protected abstract void initTreePanelListeners(JPanel passedTreePanel);

    DefaultMutableTreeNode getNodeOfEntry(CSVEntry entry) {
        DefaultMutableTreeNode root = this.getRootNode();
        Enumeration<TreeNode> allNodes = root.depthFirstEnumeration();
        Spliterator<TreeNode> spliterator = Spliterators.spliteratorUnknownSize(
                allNodes.asIterator(), Spliterator.ORDERED);
        Stream<TreeNode> stream = StreamSupport.stream(spliterator, false);
        Optional<DefaultMutableTreeNode> treeNode = stream
                .filter(node -> node instanceof DefaultMutableTreeNode)
                .map(node -> (DefaultMutableTreeNode) node)
                .filter(node -> {
                    Object userObject = node.getUserObject();
                    if (userObject instanceof CSVEntry csvEntry) {
                        String entryID = csvEntry.getID();
                        return entryID.equals(entry.getID());
                    }
                    return false;
                }).findFirst();
        return treeNode.orElse(null);
    }

    JTree createCustomTree() {
        JTree customTree = new JTree(getRootNode()) {
            @Override
            public String getToolTipText(MouseEvent event) {
                if (getRowForLocation(event.getX(), event.getY()) == -1)
                    return null;
                TreePath currPath = getPathForLocation(event.getX(), event.getY());
                if (currPath == null)
                    return null;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) currPath.getLastPathComponent();
                Object entry = node.getUserObject();
                return getTooltipForEntry(entry);
            }
        };
        DragSource dragSource = DragSource.getDefaultDragSource();
        DragGestureListener gestureListener = new TreeDataGestureListener(customTree);
        dragSource.createDefaultDragGestureRecognizer(customTree, DnDConstants.ACTION_COPY,
                gestureListener);
        return customTree;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")

    public abstract void reload();

    void sortAndExpandTree() {
        Enumeration<TreeNode> children = rootNode.children();
        List<DefaultMutableTreeNode> nodeList = new ArrayList<>();

        while (children.hasMoreElements()) {
            TreeNode folder = children.nextElement();
            if (folder instanceof DefaultMutableTreeNode packageNode) {
                nodeList.add(packageNode);
            }
        }

        nodeList.sort((firstNode, secondNode) -> {
            Object firstNodeUserObject = firstNode.getUserObject();
            GameDataPackage firstDataPackage = (GameDataPackage) firstNodeUserObject;
            Object secondNodeUserObject = secondNode.getUserObject();
            GameDataPackage secondDataPackage = (GameDataPackage) secondNodeUserObject;

            if (SettingsManager.isCoreFolder(firstDataPackage)) {
                return -1;
            } else if (SettingsManager.isCoreFolder(secondDataPackage)) {
                return 1;
            }

            if (firstDataPackage.isPinned() && !secondDataPackage.isPinned()) {
                return -1;
            } else if (!firstDataPackage.isPinned() && secondDataPackage.isPinned()) {
                return 1;
            }

            String firstFolderName = firstDataPackage.getFolderName();
            String secondFolderName = secondDataPackage.getFolderName();
            return firstFolderName.compareToIgnoreCase(secondFolderName);
        });

        rootNode.removeAllChildren();
        for (DefaultMutableTreeNode packageNode : nodeList) {
            rootNode.add(packageNode);
        }

        Enumeration<TreeNode> updatedPackages = rootNode.children();
        while (updatedPackages.hasMoreElements()) {
            TreeNode folder = updatedPackages.nextElement();
            DataTreePanel.sortFolderNode(folder, (node1, node2) -> {
                String name1 = node1.toString();
                String name2 = node2.toString();
                return name1.compareToIgnoreCase(name2);
            });
        }

        if (tree.getModel() instanceof DefaultTreeModel checked) {
            checked.nodeStructureChanged(rootNode);
        }

        tree.expandPath(new TreePath(rootNode));
        tree.repaint();
    }

    @SuppressWarnings("ConstantConditions")
    private static void sortFolderNode(TreeNode folder, Comparator<DefaultMutableTreeNode> comparator) {
        Enumeration<? extends TreeNode> children = folder.children();
        List<DefaultMutableTreeNode> nodeList = new ArrayList<>();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof DefaultMutableTreeNode checked) {
                nodeList.add(checked);
            }
        }
        nodeList.sort(comparator);
        DefaultMutableTreeNode casted = (DefaultMutableTreeNode) folder;
        casted.removeAllChildren();
        for (MutableTreeNode node : nodeList) {
            casted.add(node);
        }
    }

    static String getTooltipForPackage(GameDataPackage dataPackage) {
        String corePackageLine = "";
        boolean isCoreFolder = SettingsManager.isCoreFolder(dataPackage);
        if (isCoreFolder) {
            corePackageLine = "<p>" + "Is a core package" + "</p>";
        }
        String pinnedPackageLine = "";
        boolean isPinned = dataPackage.isPinned();
        if (isPinned) {
            pinnedPackageLine = "<p>" + "Is pinned" + "</p>";
        }
        if (isCoreFolder || isPinned) {
            return "<html>" + corePackageLine + pinnedPackageLine + "</html>";
        }
        return null;
    }

    void createRightPanelDataTable(CSVEntry entry) {
        Map<String, String> data = entry.getRowData();
        JScrollPane tableContainer = DataTreeTableBuilder.createTableFromMap(data, entry);
        this.addContentToRightPanel(tableContainer, entry);
    }

    private void addContentToRightPanel(JComponent component, CSVEntry entry) {
        GridBagConstraints otherConstraints = new GridBagConstraints();
        otherConstraints.gridx = 0;
        otherConstraints.gridy = 2;
        otherConstraints.fill = GridBagConstraints.BOTH;
        otherConstraints.weightx = 1.0;
        otherConstraints.weighty = 1.0;
        otherConstraints.insets = new Insets(0, 0, 0, 0);

        JPanel tableContainer = new JPanel();
        tableContainer.setLayout(new BorderLayout());

        JPanel buttonsContainer = DataTreeTableBuilder.createTableButtons(entry);

        ComponentUtilities.outfitPanelWithTitle(buttonsContainer,
                new Insets(1, 0, 0, 0), "CSV Data");

        tableContainer.add(component, BorderLayout.CENTER);
        tableContainer.add(buttonsContainer, BorderLayout.PAGE_START);

        rightPanel.add(tableContainer, otherConstraints);
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    static void configureCellRendererColors(Object userObject, JLabel stamp) {
        Color textColor = Themes.getTextColor();
        stamp.setForeground(textColor);
        if (userObject instanceof GameDataPackage dataPackage) {
            stamp.setText(dataPackage.getFolderName());
            if (SettingsManager.isCoreFolder(dataPackage)) {
                stamp.setForeground(Themes.getCorePackageTextColor());
            } else if (dataPackage.isPinned()) {
                stamp.setForeground(Themes.getPinnedPackageTextColor());
            }
        }
    }

    protected abstract String getTooltipForEntry(Object entry);

    protected abstract Class<?> getEntryClass();

    public java.awt.event.MouseAdapter createContextMenuListener() {
        return new DataTreeContextMenuController(this, tree, getEntryClass());
    }

    JPopupMenu getContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem collapsePackage = new JMenuItem("Collapse package");
        collapsePackage.addActionListener(getCollapseAction());
        menu.add(collapsePackage);
        menu.addSeparator();
        JMenuItem openSourceFile = new JMenuItem(StringValues.OPEN_SOURCE_FILE);
        openSourceFile.addActionListener(e -> openEntryPath(OpenDataTarget.FILE));
        menu.add(openSourceFile);
        JMenuItem openInExplorer = new JMenuItem(StringValues.OPEN_CONTAINING_FOLDER);
        openInExplorer.addActionListener(e -> openEntryPath(OpenDataTarget.CONTAINER));
        menu.add(openInExplorer);
        JMenuItem openPackage = new JMenuItem(StringValues.OPEN_DATA_PACKAGE);
        openPackage.addActionListener(e -> openEntryPath(OpenDataTarget.PACKAGE));
        menu.add(openPackage);
        return menu;
    }

    protected abstract void openEntryPath(OpenDataTarget target);

    private ActionListener getCollapseAction() {
        return e -> {
            Class<?> entryClass = getEntryClass();
            if (entryClass.isInstance(this.cachedSelectForMenu.getUserObject())) {
                TreePath selected = this.tree.getSelectionPath();
                if (selected != null) {
                    this.tree.collapsePath(selected.getParentPath());
                }
            }
        };
    }

}
