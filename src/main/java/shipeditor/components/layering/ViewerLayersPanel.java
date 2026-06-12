package shipeditor.components.layering;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.extern.log4j.Log4j2;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.HullFileOpened;
import shipeditor.communication.events.files.FileEvents.HullSaveQueued;
import shipeditor.communication.events.files.FileEvents.VariantSaveQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerUpdated;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerRemovalQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.communication.events.viewer.layers.LayerEvents.ViewerLayerRemovalConfirmed;
import shipeditor.communication.events.viewer.layers.LayerEvents.ShipLayerCreated;
import shipeditor.communication.events.viewer.layers.LayerEvents.WeaponLayerCreated;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.components.viewer.PrimaryViewer;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipHull;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.components.viewer.layers.weapon.ProjectileLayer;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.components.viewer.layers.weapon.WeaponPainter;
import shipeditor.components.viewer.layers.weapon.WeaponSprites;
import shipeditor.parsing.FileUtilities;
import shipeditor.parsing.loading.OpenSpriteAction;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.representation.weapon.WeaponEnums.WeaponMount;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.components.containers.SortableTabbedPane;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.graphics.opengl.FramebufferUtilities;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.StringValues;
import shipeditor.utility.themes.Themes;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.TabbedPaneUI;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.function.ToIntFunction;
import shipeditor.communication.events.components.ComponentEvents.SelectShipDataEntry;
import shipeditor.communication.events.components.ComponentEvents.LayerTabUpdated;
import shipeditor.communication.events.components.ComponentEvents.WindowRepaintQueued;
import shipeditor.communication.events.files.FileEvents.WeaponSaveQueued;
import shipeditor.communication.events.files.FileEvents.ProjectileSaveQueued;

@SuppressWarnings("OverlyCoupledClass")
@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public final class ViewerLayersPanel extends SortableTabbedPane {

    private static final String LAYER = "Layer #";

    /**
     * Expected to be the same instance that is originally created and assigned in viewer;
     * Reference in this class is present for both conceptual and convenience purposes.
     */
    private final LayerManager layerManager;

    private final Map<ViewerLayer, LayerTab> tabIndex;
    private final Map<LayerTab, ViewerLayer> tabToLayer;

    public ViewerLayersPanel(LayerManager manager) {
        this.layerManager = manager;
        this.tabIndex = new HashMap<>();
        this.tabToLayer = new HashMap<>();

        this.putClientProperty("JTabbedPane.tabClosable", true);
        this.putClientProperty("JTabbedPane.tabCloseToolTipText", "Remove this layer");
        this.putClientProperty( "JTabbedPane.tabCloseCallback", (IntConsumer) index -> {
            LayerTab tab = (LayerTab) getComponentAt(index);
            ViewerLayer layer = getLayerByTab(tab);
            EventBus.publish(new LayerRemovalQueued(layer));
        });
        this.putClientProperty("JTabbedPane.showTabSeparators", true);
        this.putClientProperty("JTabbedPane.hasFullBorder", false);
        this.putClientProperty("JTabbedPane.tabHeight", 32);
        this.putClientProperty("JTabbedPane.minimumTabWidth", 100);

        this.initLayerListeners();
        this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.addChangeListener(event -> {
            ViewerLayer newlySelected = getLayerByTab((LayerTab) getSelectedComponent());
            log.trace("Layer panel change!");
            // If the change results from the last layer being removed and the newly selected layer is null,
            // call to set active layer is unnecessary as this case is handled directly by layer manager.
            ViewerLayer activeLayer = layerManager.getActiveLayer();

            if (newlySelected != null && activeLayer != newlySelected) {
                layerManager.setActiveLayer(newlySelected);
            }
        });
        this.addMouseListener(new TabContextListener());
        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,  Themes.getBorderColor()));
    }

    @SuppressWarnings({"OverlyCoupledMethod", "ChainOfInstanceofChecks"})
    private void initLayerListeners() {
        EventBus.subscribe(this, event -> {
            if (event instanceof ShipLayerCreated checked) {
                ShipLayer layer = checked.newLayer();
                ShipLayerTab created = new ShipLayerTab(layer);
                tabIndex.put(layer, created);
                tabToLayer.put(created, layer);
                String tooltip = created.getTabTooltip();
                this.addTab(LAYER + (getTabCount() + 1), null, tabIndex.get(layer), tooltip);
                EventBus.publish(new WindowRepaintQueued());
            }
            else if (event instanceof WeaponLayerCreated checked) {
                WeaponLayer layer = checked.newLayer();
                WeaponLayerTab created = new WeaponLayerTab(layer);
                tabIndex.put(layer, created);
                tabToLayer.put(created, layer);
                String tooltip = created.getTabTooltip();
                this.addTab(LAYER + (getTabCount() + 1), null, tabIndex.get(layer), tooltip);
                EventBus.publish(new WindowRepaintQueued());
            }
            else if (event instanceof shipeditor.communication.events.viewer.layers.LayerEvents.ProjectileLayerCreated checked) {
                shipeditor.components.viewer.layers.weapon.ProjectileLayer layer = checked.newLayer();
                ProjectileLayerTab created = new ProjectileLayerTab(layer);
                tabIndex.put(layer, created);
                tabToLayer.put(created, layer);
                String tooltip = created.getTabTooltip();
                this.addTab(LAYER + (getTabCount() + 1), null, tabIndex.get(layer), tooltip);
                EventBus.publish(new WindowRepaintQueued());
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof ActiveLayerUpdated checked) {
                this.handleTabUpdates(checked.updated());
            } else if (event instanceof LayerTabUpdated checked) {
                this.handleTabUpdates(checked.layer());
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof ViewerLayerRemovalConfirmed checked) {
                ViewerLayer layer = checked.removed();
                closeLayer(layer);
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerWasSelected checked) {
                ViewerLayer newlySelected = checked.selected();
                ViewerLayer selectedTabLayer = getLayerByTab((LayerTab) getSelectedComponent());
                if (newlySelected == selectedTabLayer) return;
                this.setSelectedIndex(indexOfComponent(tabIndex.get(newlySelected)));
            }
        });
    }

    private void handleTabUpdates(ViewerLayer eventLayer) {
        LayerTab updated = tabIndex.get(eventLayer);
        if (updated instanceof ShipLayerTab checkedShipTab && eventLayer instanceof ShipLayer checkedLayer) {
            this.updateShipTab(checkedShipTab, checkedLayer);
        } else if (updated instanceof WeaponLayerTab checkedWeaponTab && eventLayer instanceof WeaponLayer checkedLayer) {
            this.updateWeaponTab(checkedWeaponTab, checkedLayer);
        } else if (updated instanceof ProjectileLayerTab checkedProjectileTab && eventLayer instanceof shipeditor.components.viewer.layers.weapon.ProjectileLayer checkedLayer) {
            this.updateProjectileTab(checkedProjectileTab, checkedLayer);
        }
    }

    private void updateProjectileTab(ProjectileLayerTab tab, shipeditor.components.viewer.layers.weapon.ProjectileLayer layer) {
        String tabTitle = layer.getSpecFileName();
        if (tabTitle == null || tabTitle.isEmpty()) {
            List<ViewerLayer> layers = layerManager.getLayers();
            int index = layers.indexOf(layer) + 1;
            tabTitle = LAYER + index;
        }

        if (layerManager.isLayerDirty(layer)) {
            tabTitle += "*";
        }
        
        this.setTitleAt(indexOfComponent(tab), shipeditor.utility.Utility.wrapTextWithHtml(tabTitle, 7));
        this.setToolTipTextAt(indexOfComponent(tab), tab.getTabTooltip());
    }

    private void updateShipTab(ShipLayerTab tab, ShipLayer layer) {
        LayerPainter painter = layer.getPainter();
        if (painter == null) return;
        Sprite sprite = painter.getSprite();
        if (sprite != null) {
            tab.setSpriteFileName(sprite.getFilename());
            this.setToolTipTextAt(indexOfComponent(tab), tab.getTabTooltip());
        }

        String hullName = "";

        ShipHull shipHull = layer.getHull();

        if (shipHull != null && shipHull.getHullName() != null && !shipHull.getHullName().isEmpty()) {
            hullName = shipHull.getHullName();
        } else if (layer.getHullFileName() != null && !layer.getHullFileName().isEmpty()) {
            hullName = layer.getHullFileName();
        } else if (sprite != null && sprite.getFilename() != null && !sprite.getFilename().isEmpty()) {
            hullName = sprite.getFilename();
        }

        if (hullName.isEmpty()) {
            List<ViewerLayer> layers = layerManager.getLayers();
            int index = layers.indexOf(layer) + 1;
            hullName = LAYER + index;
        }

        tab.setHullFileName(layer.getHullFileName());

        ShipPainter layerPainter = layer.getPainter();
        if (layerPainter == null) return;

        tab.setSkinFileNames(layer.getSkinFileNames());
        
        String tabTitle = hullName;
        ShipSkin activeSkin = layerPainter.getActiveSkin();
        if (activeSkin == null || activeSkin.isBase()) {
            tab.setActiveSkinFileName("");
        } else {
            String skinFileName = activeSkin.getFileName();
            tab.setActiveSkinFileName(skinFileName);
            if (activeSkin.getHullName() != null) {
                tabTitle = activeSkin.getHullName();
            }
        }

        if (layerManager.isLayerDirty(layer)) {
            tabTitle += "*";
        }
        this.setTitleAt(indexOfComponent(tab), shipeditor.utility.Utility.wrapTextWithHtml(tabTitle, 7));
        this.setToolTipTextAt(indexOfComponent(tab), tab.getTabTooltip());
    }

    private void updateWeaponTab(WeaponLayerTab tab, WeaponLayer layer) {
        WeaponPainter painter = layer.getPainter();
        WeaponMount mount = painter.getMount();
        WeaponSprites weaponSprites = painter.getWeaponSprites();

        Sprite mainSprite = weaponSprites.getMainSprite(mount);
        if (mainSprite != null) {
            tab.setSpriteName(mainSprite.getFilename());
        }

        Sprite underSprite = weaponSprites.getUnderSprite(mount);
        if (underSprite != null) {
            tab.setUnderSpriteName(underSprite.getFilename());
        }

        Sprite gunSprite = weaponSprites.getGunSprite(mount);
        if (gunSprite != null) {
            tab.setGunSpriteName(gunSprite.getFilename());
        }

        Sprite glowSprite = weaponSprites.getGlowSprite(mount);
        if (glowSprite != null) {
            tab.setGlowSpriteName(glowSprite.getFilename());
        }

        WeaponSpecFile specFile = layer.getSpecFile();
        if (specFile != null) {
            tab.setSpecFileName(layer.getSpecFileName());
        }

        String weaponName = layer.getWeaponName();
        String tabTitle = "";
        
        if (weaponName != null && !weaponName.isEmpty()) {
            tabTitle = weaponName;
        } else if (layer.getSpecFileName() != null && !layer.getSpecFileName().isEmpty()) {
            tabTitle = layer.getSpecFileName();
        } else if (mainSprite != null && mainSprite.getFilename() != null && !mainSprite.getFilename().isEmpty()) {
            tabTitle = mainSprite.getFilename();
        } else {
            List<ViewerLayer> layers = layerManager.getLayers();
            int index = layers.indexOf(layer) + 1;
            tabTitle = LAYER + index;
        }

        if (layerManager.isLayerDirty(layer)) {
            tabTitle += "*";
        }
        
        this.setTitleAt(indexOfComponent(tab), shipeditor.utility.Utility.wrapTextWithHtml(tabTitle, 7));
        this.setToolTipTextAt(indexOfComponent(tab), tab.getTabTooltip());
    }

    @Override
    protected void sortTabObjects() {
        List<ViewerLayer> layers = new ArrayList<>(tabIndex.keySet());

        ToIntFunction<ViewerLayer> intFunction = layer -> indexOfComponent(tabIndex.get(layer));
        layers.sort(Comparator.comparingInt(intFunction));
        layerManager.setLayers(layers);
    }

    private void closeLayer(ViewerLayer layer) {
        LayerTab tab = tabIndex.get(layer);
        if (tab != null) {
            this.removeTabAt(indexOfComponent(tab));
            tabToLayer.remove(tab);
        }
        tabIndex.remove(layer);
        EventBus.publish(new WindowRepaintQueued());
    }

    private ViewerLayer getLayerByTab(LayerTab value) {
        return tabToLayer.get(value);
    }

    @SuppressWarnings("PackageVisibleInnerClass")
    class TabContextListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if(SwingUtilities.isRightMouseButton(e)){
                TabbedPaneUI paneUI = getUI();
                int targetTab = paneUI.tabForCoordinate(ViewerLayersPanel.this, e.getX(), e.getY());
                if (targetTab < 0) {
                    JPopupMenu menu = new JPopupMenu();
                    menu.add(shipeditor.menubar.WindowMenu.createAddLayerOption());
                    menu.show(ViewerLayersPanel.this, e.getPoint().x, e.getPoint().y);
                    return;
                }
                LayerTab tab = (LayerTab) getComponentAt(targetTab);
                ViewerLayer layer = getLayerByTab(tab);

                showMenuIfMatching(layer, e);
            }
        }

        private void showMenuIfMatching(ViewerLayer layer, MouseEvent e) {
            if (layer instanceof ShipLayer shipLayer) {
                var menu = TabContextListener.createContextMenu(shipLayer);
                menu.show(ViewerLayersPanel.this, e.getPoint().x, e.getPoint().y);
            } else if (layer instanceof WeaponLayer weaponLayer) {
                var menu = TabContextListener.createContextMenu(weaponLayer);
                menu.show(ViewerLayersPanel.this, e.getPoint().x, e.getPoint().y);
            } else if (layer instanceof ProjectileLayer projectileLayer) {
                var menu = TabContextListener.createContextMenu(projectileLayer);
                menu.show(ViewerLayersPanel.this, e.getPoint().x, e.getPoint().y);
            }
        }

        @SuppressWarnings("OverlyCoupledMethod")
        private static JPopupMenu createContextMenu(ShipLayer shipLayer) {
            ShipPainter shipPainter = shipLayer.getPainter();
            JPopupMenu menu = new JPopupMenu();

            JMenuItem openSprite = new JMenuItem("Load new sprite");
            openSprite.addActionListener(e -> OpenSpriteAction.openSpriteAndDo(sprite -> {
                PrimaryViewer viewer = StaticController.getViewer();
                viewer.loadSpriteToLayer(shipLayer, sprite);
            }));
            menu.add(openSprite);

            if (shipPainter == null) {
                return menu;
            }

            JMenuItem createHullData = new JMenuItem("Create new ship data");
            createHullData.addActionListener(event -> {
                HullSpecFile created = new HullSpecFile();
                shipLayer.initializeHullData(created);
                EventBus.publish(new HullFileOpened(created, null));
            });
            menu.add(createHullData);

            if (shipLayer.getHull() == null) {
                return menu;
            }

            menu.addSeparator();

            JMenuItem selectEntry = new JMenuItem(StringValues.SELECT_SHIP_ENTRY);
            String baseHullID = GameDataRepository.getBaseHullID(shipLayer.getShipID());
            if (baseHullID != null && !baseHullID.isEmpty()) {
                ShipCSVEntry entry = GameDataRepository.retrieveShipCSVEntryByID(baseHullID);
                if (entry != null) {
                    selectEntry.addActionListener(event -> EventBus.publish(new SelectShipDataEntry(entry)));
                } else {
                    selectEntry.setEnabled(false);
                }
            } else {
                selectEntry.setEnabled(false);
            }
            menu.add(selectEntry);

            menu.addSeparator();

            JMenuItem saveHullData = new JMenuItem("Save hull data");

            saveHullData.addActionListener(event -> EventBus.publish(new HullSaveQueued(shipLayer)));
            menu.add(saveHullData);

            JMenuItem saveActiveVariant = new JMenuItem("Save active variant");
            var activeVariant = shipPainter.getActiveVariant();
            if (activeVariant != null && !activeVariant.isEmpty()) {
                saveActiveVariant.addActionListener(event -> EventBus.publish(new VariantSaveQueued(activeVariant)));
            } else {
                saveActiveVariant.setEnabled(false);
            }
            menu.add(saveActiveVariant);

            menu.addSeparator();

            JMenuItem flipPoints = new JMenuItem("Flip ship points");
            flipPoints.addActionListener(event -> shipPainter.flipShipPointsHorizontally());
            menu.add(flipPoints);

            menu.addSeparator();

            menu.add(createPrintLayerOption(shipLayer));

            return menu;
        }

        private static JPopupMenu createContextMenu(WeaponLayer weaponLayer) {
            JPopupMenu menu = new JPopupMenu();

            if (weaponLayer.getSpecFile() != null) {
                JMenuItem saveWeaponData = new JMenuItem("Save weapon data");
                saveWeaponData.addActionListener(event -> EventBus.publish(new WeaponSaveQueued(weaponLayer)));
                menu.add(saveWeaponData);
                menu.addSeparator();
            }

            menu.add(createPrintLayerOption(weaponLayer));
            return menu;
        }

        private static JPopupMenu createContextMenu(ProjectileLayer projectileLayer) {
            JPopupMenu menu = new JPopupMenu();

            if (projectileLayer.getSpecFile() != null) {
                JMenuItem saveProjectileData = new JMenuItem("Save projectile data");
                saveProjectileData.addActionListener(event -> EventBus.publish(new ProjectileSaveQueued(projectileLayer)));
                menu.add(saveProjectileData);
                menu.addSeparator();
            }

            menu.add(createPrintLayerOption(projectileLayer));
            return menu;
        }

        private static JMenuItem createPrintLayerOption(ViewerLayer layer) {
            JMenuItem printLayer = new JMenuItem("Print layer to image");
            printLayer.addActionListener(event -> {
                JFileChooser chooser = FileUtilities.getImageChooser();
                int returnVal = chooser.showSaveDialog(shipeditor.PrimaryWindow.getInstance());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = FileUtilities.ensureFileExtension(chooser, "png");
                    
                    int width = 0;
                    int height = 0;
                    LayerPainter layerPainter = layer.getPainter();
                    if (layerPainter != null && !layerPainter.isUninitialized()) {
                        width = layerPainter.getSpriteSize().width;
                        height = layerPainter.getSpriteSize().height;
                    }
                    if (width <= 0 || height <= 0) {
                        JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(), "Layer is empty or invalid size.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int finalWidth = width;
                    int finalHeight = height;
                    StaticController.getViewer().queueGLTask(() -> FramebufferUtilities.printLayerToImage(layer, finalWidth, finalHeight, file));
                }
            });
            return printLayer;
        }

    }

}
