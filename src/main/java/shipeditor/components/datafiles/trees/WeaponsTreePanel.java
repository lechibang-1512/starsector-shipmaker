package shipeditor.components.datafiles.trees;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.components.viewer.layers.ship.FeaturesOverseer;
import shipeditor.components.viewer.layers.weapon.WeaponSprites;
import shipeditor.parsing.FileUtilities;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.weapon.ProjectileSpecFile;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.utility.Utility;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.text.StringValues;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import shipeditor.utility.components.UIConstants;
import shipeditor.communication.events.components.ComponentEvents.SelectWeaponDataEntry;
import shipeditor.communication.events.files.FileEvents.WeaponTreeReloadQueued;

@Log4j2
public class WeaponsTreePanel extends CSVDataTreePanel<WeaponCSVEntry>{

    private boolean autoExpandNodes;

    public WeaponsTreePanel() {
        super("Weapon file packages");
    }

    @Override
    protected Action getLoadDataAction() {
        return FileLoading.loadDataAsync(FileLoading.getLoadWeapons());
    }

    @Override
    protected String getEntryTypeName() {
        return "weapon";
    }

    @Override
    protected Map<String, WeaponCSVEntry> getRepository() {
        GameDataRepository gameData = SettingsManager.getGameData();
        return gameData.getAllWeaponEntries();
    }

    @Override
    protected Map<Path, List<WeaponCSVEntry>> getPackageList() {
        GameDataRepository gameData = SettingsManager.getGameData();
        return gameData.getWeaponEntriesByPackage();
    }

    @Override
    protected JTree createCustomTree() {
        JTree custom = super.createCustomTree();
        custom.setCellRenderer(new WeaponsTreeCellRenderer());
        return custom;
    }

    /**
     * This could very well be a full-fledged panel stamp, with type icon borders and size icon too.
     * Unfortunately, there's not enough development time for that.
     */
    private static class WeaponsTreeCellRenderer extends DefaultTreeCellRenderer {

        @SuppressWarnings("ParameterHidesMemberVariable")
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object object = ((DefaultMutableTreeNode) value).getUserObject();
            DataTreePanel.configureCellRendererColors(object, this);
            if (object instanceof WeaponCSVEntry checked && leaf) {
                WeaponType hullSize = checked.getType();
                setIcon(ComponentUtilities.createIconFromColor(hullSize.getColor(), 10, 10));
            }
            return this;
        }

    }

    @Override
    protected void setLoadedStatus() {
        GameDataRepository gameData = SettingsManager.getGameData();
        gameData.setWeaponsDataLoaded(true);
    }

    @Override
    protected void initWalkerListening() {
        EventBus.subscribe(this, event -> {
            if (event instanceof WeaponTreeReloadQueued) {
                this.queueReload();
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof SelectWeaponDataEntry checked) {
                WeaponCSVEntry entry = checked.entry();
                DefaultMutableTreeNode node = getNodeOfEntry(entry);
                if (node != null) {
                    JTree tree = this.getTree();
                    TreePath path = new TreePath(node.getPath());
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                }
            }
        });
    }

    @Override
    public void reload() {
        Map<Path, List<WeaponCSVEntry>> weaponPackageList = WeaponFilterPanel.getFilteredEntries();
        populateEntries(weaponPackageList);

        JTree tree = getTree();

        if (autoExpandNodes) {
            this.expandAllNodes();
        }

        tree.repaint();
    }

    @Override
    void resetInfoPanel() {
        super.resetInfoPanel();
    }

    protected JPanel createSearchContainer() {
        JPanel searchContainer = new JPanel(new GridBagLayout());
        searchContainer.setBorder(UIConstants.EMPTY_BORDER);

        JTextField searchField = this.getTextField();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        searchContainer.add(searchField, gridBagConstraints);
        JButton searchButton = new JButton(StringValues.SEARCH);
        searchButton.addActionListener(e -> this.reload());
        searchField.addActionListener(e -> searchButton.doClick());
        searchContainer.add(searchButton);
        return searchContainer;
    }

    private JTextField getTextField() {
        JTextField searchField = new JTextField();
        searchField.setToolTipText("Input is checked against displayed filename and weapon ID as a substring.");

        javax.swing.Timer timer = new javax.swing.Timer(300, e -> {
            WeaponFilterPanel.setCurrentTextFilter(searchField.getText());
            this.reload();
        });
        timer.setRepeats(false);

        Document document = searchField.getDocument();
        document.addDocumentListener(new DocumentListener() {
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
        });
        return searchField;
    }

    @Override
    protected JPanel createTopPanel() {
        JPanel topPanel = new JPanel();

        JCheckBox expandNodes = new JCheckBox("Auto-expand nodes");
        expandNodes.addActionListener(e -> this.autoExpandNodes = expandNodes.isSelected());

        topPanel.add(expandNodes);

        return topPanel;
    }

    @Override
    protected void loadAllEntries(Map<Path, List<WeaponCSVEntry>> entries) {
        if (entries == null || entries.isEmpty()) {
            log.info("No entries registered: input empty.");
            return;
        }

        int nodeCount = 0;

        for (Map.Entry<Path, List<WeaponCSVEntry>> entryFolder : entries.entrySet()) {
            Settings settings = SettingsManager.getSettings();
            Path path = entryFolder.getKey();
            String folderName = FileUtilities.extractFolderName(path.toString());
            GameDataPackage dataPackage = settings.getPackage(folderName);
            if (dataPackage == null || dataPackage.isDisabled()) {
                continue;
            }

            DefaultMutableTreeNode packageRoot;
            if (SettingsManager.isCoreFolder(folderName)) {
                GameDataPackage corePackage = SettingsManager.getCorePackage();
                packageRoot = new DefaultMutableTreeNode(corePackage);
                for (WeaponCSVEntry entry : entryFolder.getValue()) {
                    MutableTreeNode entryNode = new DefaultMutableTreeNode(entry);
                    packageRoot.add(entryNode);
                    nodeCount++;
                }
            } else {
                GameDataPackage modPackage = settings.getPackage(folderName);
                packageRoot = new DefaultMutableTreeNode(modPackage);

                for (WeaponCSVEntry entry : entryFolder.getValue()) {
                    MutableTreeNode entryNode = new DefaultMutableTreeNode(entry);
                    packageRoot.add(entryNode);
                    nodeCount++;
                }
            }

            DefaultMutableTreeNode rootNode = getRootNode();
            rootNode.add(packageRoot);
        }
        log.info("Total {} {} entry nodes shown.", nodeCount, getEntryTypeName());
        setLoadedStatus();
    }

    @Override
    protected String getTooltipForEntry(Object entry) {
        if (entry instanceof WeaponCSVEntry weaponEntry) {
            String dragHint = "(Double-click or drag to load as weapon layer)";
            return weaponEntry.getMultilineTooltip(dragHint);
        }
        return super.getTooltipForEntry(entry);
    }

    @Override
    protected void updateEntryPanel(WeaponCSVEntry selected) {
        JPanel rightPanel = getRightPanel();
        rightPanel.removeAll();

        GridBagConstraints constraints = DataTreePanel.getDefaultConstraints();
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 5, 0, 5);
        Sprite sprite = selected.getWeaponImage();
        if (sprite != null) {
            String tooltip = Utility.getTooltipForSprite(sprite);
            JLabel spriteIcon = ComponentUtilities.createIconFromImage(sprite.getImage(), tooltip, 128);
            JPanel iconPanel = new JPanel();
            iconPanel.add(spriteIcon);
            rightPanel.add(iconPanel, constraints);
        }

        JPanel specFilePanel = new JPanel();
        specFilePanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        ComponentUtilities.outfitPanelWithTitle(specFilePanel, new Insets(1, 0, 0, 0),
                "Weapon Info");
        specFilePanel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel labelContainer = new JPanel();
        labelContainer.setAlignmentX(LEFT_ALIGNMENT);
        labelContainer.setBorder(new EmptyBorder(2, 0, 0, 0));
        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.PAGE_AXIS));

        WeaponSpecFile specFile = selected.getSpecFile();
        labelContainer.add(WeaponsTreePanel.createWeaponFileLabel(specFile));

        var projectileSpecFile = GameDataRepository.getProjectileByID(specFile.getProjectileSpecId());
        if (projectileSpecFile != null) {
            labelContainer.add(Box.createVerticalStrut(2));
            labelContainer.add(WeaponsTreePanel.createProjectileFileLabel(projectileSpecFile));
        }

        WeaponSprites sprites = selected.getSprites();
        WeaponsTreePanel.populateSpriteFileLabels(labelContainer, sprites);

        labelContainer.add(Box.createVerticalStrut(4));
        labelContainer.add(WeaponsTreePanel.createInstallableSlotsLabel(selected));

        specFilePanel.add(labelContainer);
        constraints.gridy = 1;
        rightPanel.add(specFilePanel, constraints);

        createRightPanelDataTable(selected);

        FeaturesOverseer.setWeaponForInstall(selected);
    }

    private static void populateSpriteFileLabels(JPanel labelContainer, WeaponSprites sprites) {
        Sprite turretSprite = sprites.getTurretSprite();
        WeaponsTreePanel.addSpriteLabel(labelContainer, turretSprite, "Turret sprite: ");
        Sprite turretGunSprite = sprites.getTurretGunSprite();
        WeaponsTreePanel.addSpriteLabel(labelContainer, turretGunSprite, "Turret gun sprite: ");
        Sprite turretGlowSprite = sprites.getTurretGlowSprite();
        WeaponsTreePanel.addSpriteLabel(labelContainer, turretGlowSprite, "Turret glow sprite: ");
        Sprite turretUnderSprite = sprites.getTurretUnderSprite();
        WeaponsTreePanel.addSpriteLabel(labelContainer, turretUnderSprite, "Turret under sprite: ");

        Sprite hardpointSprite = sprites.getHardpointSprite();
        WeaponsTreePanel.addSpriteLabel(labelContainer, hardpointSprite, "Hardpoint sprite: ");
        Sprite hardpointGunSprite = sprites.getHardpointGunSprite();
        WeaponsTreePanel.addSpriteLabel(labelContainer, hardpointGunSprite, "Hardpoint gun sprite: ");
        Sprite hardpointGlowSprite = sprites.getHardpointGlowSprite();
        WeaponsTreePanel.addSpriteLabel(labelContainer, hardpointGlowSprite, "Hardpoint glow sprite: ");
        Sprite hardpointUnderSprite = sprites.getHardpointUnderSprite();
        WeaponsTreePanel.addSpriteLabel(labelContainer, hardpointUnderSprite, "Hardpoint under sprite: ");
    }

    private static void addSpriteLabel(JPanel labelContainer, Sprite sprite, String description) {
        if (sprite != null) {
            JLabel label = ComponentUtilities.createFileLabel(sprite.getPath(), description);
            labelContainer.add(Box.createVerticalStrut(2));
            labelContainer.add(label);
        }
    }

    private static JLabel createWeaponFileLabel(WeaponSpecFile weaponSpecFile) {
        Path weaponSpecFilePath = weaponSpecFile.getWeaponSpecFilePath();
        return ComponentUtilities.createFileLabel(weaponSpecFilePath, "Weapon file : ");
    }

    private static JLabel createProjectileFileLabel(ProjectileSpecFile projectileSpecFile) {
        Path projectileSpecFilePath = projectileSpecFile.getProjectileSpecFilePath();
        return ComponentUtilities.createFileLabel(projectileSpecFilePath, "Projectile file : ");
    }

    private static JLabel createInstallableSlotsLabel(WeaponCSVEntry weapon) {
        WeaponType type = weapon.getType();
        List<String> installableIn = new java.util.ArrayList<>();
        if (type == WeaponType.BALLISTIC || type == WeaponType.HYBRID || type == WeaponType.COMPOSITE) {
            installableIn.add("Ballistic");
        }
        if (type == WeaponType.ENERGY || type == WeaponType.HYBRID || type == WeaponType.SYNERGY) {
            installableIn.add("Energy");
        }
        if (type == WeaponType.MISSILE || type == WeaponType.COMPOSITE || type == WeaponType.SYNERGY) {
            installableIn.add("Missile");
        }
        if (type == WeaponType.UNIVERSAL || (type != WeaponType.LAUNCH_BAY && type != WeaponType.BUILT_IN && type != WeaponType.DECORATIVE && type != WeaponType.SYSTEM && type != WeaponType.STATION_MODULE)) {
            installableIn.add("Universal");
        }
        if (type == WeaponType.SYNERGY || type == WeaponType.ENERGY || type == WeaponType.MISSILE) {
            installableIn.add("Synergy");
        }
        if (type == WeaponType.HYBRID || type == WeaponType.BALLISTIC || type == WeaponType.ENERGY) {
            installableIn.add("Hybrid");
        }
        if (type == WeaponType.COMPOSITE || type == WeaponType.BALLISTIC || type == WeaponType.MISSILE) {
            installableIn.add("Composite");
        }
        
        String text = installableIn.isEmpty() ? "Installable in slots: None" : "Installable in slots: " + String.join(", ", installableIn);
        return new JLabel(text);
    }

    @Override
    JPopupMenu getContextMenu() {
        JPopupMenu menu = super.getContextMenu();
        DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
        if (cachedSelectForMenu.getUserObject() instanceof WeaponCSVEntry) {
            JMenuItem loadAsLayer = new JMenuItem("Load as weapon layer");
            loadAsLayer.addActionListener(new LoadWeaponLayerFromTree());
            menu.insert(loadAsLayer, 0);
            menu.insert(new JPopupMenu.Separator(), 1);
        }
        return menu;
    }

    private class LoadWeaponLayerFromTree extends AbstractAction {
        @Override
        public boolean isEnabled() {
            DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
            return super.isEnabled() && cachedSelectForMenu.getUserObject() instanceof WeaponCSVEntry;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode cachedSelectForMenu = getCachedSelectForMenu();
            if (cachedSelectForMenu.getUserObject() instanceof WeaponCSVEntry checked) {
                checked.loadLayerFromEntry();
            }
        }
    }

    @Override
    protected WeaponCSVEntry getObjectFromNode(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (!(userObject instanceof WeaponCSVEntry checked)) return null;
        return checked;
    }

    @Override
    protected Class<?> getEntryClass() {
        return WeaponCSVEntry.class;
    }

    @Override
    protected void initTreePanelListeners(JPanel passedTreePanel) {
        super.initTreePanelListeners(passedTreePanel);
        getTree().addMouseListener(new DoubleClickLayerLoader());
    }

    private class DoubleClickLayerLoader extends java.awt.event.MouseAdapter {
        @SuppressWarnings("ChainOfInstanceofChecks")
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
            if (e.getButton() != java.awt.event.MouseEvent.BUTTON1 || e.getClickCount() < 2) return;
            JTree tree = getTree();
            java.awt.Point eventPoint = e.getPoint();
            TreePath pathForLocation = tree.getPathForLocation(eventPoint.x, eventPoint.y);
            if (pathForLocation == null) return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) pathForLocation.getLastPathComponent();
            if (node.getUserObject() instanceof WeaponCSVEntry checked) {
                checked.loadLayerFromEntry();
            }
        }
    }

}
