package shipeditor.components.datafiles;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.components.datafiles.styles.EngineStylesPanel;
import shipeditor.components.datafiles.styles.HullStylesPanel;
import shipeditor.components.datafiles.trees.*;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.ComponentEnums.VariantDataTab;
import shipeditor.utility.text.StringValues;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.JComboBox;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import shipeditor.communication.events.components.ComponentEvents.SelectWeaponDataEntry;
import shipeditor.communication.events.components.ComponentEvents.DataTreesReloadQueued;
import shipeditor.communication.events.components.ComponentEvents.SelectShipDataEntry;
import shipeditor.communication.events.components.ComponentEvents.VariantDataTabSelected;
import shipeditor.communication.events.viewer.points.PointEvents.InstrumentModeChanged;

@Log4j2
public class GameDataPanel extends JPanel {

    private final JTabbedPane dataTabsContainer;
    private final HullsTreePanel hullsTreePanel;
    private final WeaponsTreePanel weaponsTreePanel;
    private final HullmodsTreePanel hullmodsTreePanel;
    private final ShipSystemsTreePanel systemsTreePanel;
    private final WingsTreePanel wingsTreePanel;
    private final ProjectilesTreePanel projectilesTreePanel;
    
    private final JPanel dataContainerPanel;
    private final JComboBox<String> dataComboBox;

    public GameDataPanel() {
        dataTabsContainer = new JTabbedPane(SwingConstants.TOP);
        dataTabsContainer.putClientProperty("JTabbedPane.tabWidthMode", "compact");
        
        hullsTreePanel = new HullsTreePanel();
        JTabbedPane hullsTabbedPane = new JTabbedPane(SwingConstants.BOTTOM);
        hullsTabbedPane.addTab("List", hullsTreePanel);
        hullsTabbedPane.addTab("Filters", new ShipFilterPanel());
        dataTabsContainer.addTab("Hulls", null, hullsTabbedPane, "Hulls");
        
        weaponsTreePanel = new WeaponsTreePanel();
        JTabbedPane weaponsTabbedPane = new JTabbedPane(SwingConstants.BOTTOM);
        weaponsTabbedPane.addTab("List", weaponsTreePanel);
        weaponsTabbedPane.addTab("Filters", new WeaponFilterPanel());
        dataTabsContainer.addTab("Weapons", null, weaponsTabbedPane, "Weapons");
        
        hullmodsTreePanel = new HullmodsTreePanel();
        systemsTreePanel = new ShipSystemsTreePanel();
        wingsTreePanel = new WingsTreePanel();
        projectilesTreePanel = new ProjectilesTreePanel();
        
        dataContainerPanel = new JPanel(new BorderLayout());
        
        CardLayout dataCardLayout = new CardLayout();
        JPanel dataCardsPanel = new JPanel(dataCardLayout);
        
        dataCardsPanel.add(hullmodsTreePanel, StringValues.HULLMODS);
        dataCardsPanel.add(systemsTreePanel, "Shipsystems");
        dataCardsPanel.add(wingsTreePanel, StringValues.WINGS);
        dataCardsPanel.add(projectilesTreePanel, "Projectiles");
        dataCardsPanel.add(new HullStylesPanel(), "Hull styles");
        dataCardsPanel.add(new EngineStylesPanel(), "Engine styles");
        
        dataComboBox = new JComboBox<>(new String[] {
            StringValues.HULLMODS,
            "Shipsystems",
            StringValues.WINGS,
            "Projectiles",
            "Hull styles",
            "Engine styles"
        });
        
        dataComboBox.addActionListener(e -> {
            String selected = (String) dataComboBox.getSelectedItem();
            if (selected != null) {
                dataCardLayout.show(dataCardsPanel, selected);
            }
        });
        
        dataContainerPanel.add(dataComboBox, BorderLayout.NORTH);
        dataContainerPanel.add(dataCardsPanel, BorderLayout.CENTER);
        
        dataTabsContainer.addTab("Data", null, dataContainerPanel, "Data");

        dataTabsContainer.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        this.setLayout(new BorderLayout());
        this.add(dataTabsContainer, BorderLayout.CENTER);

        this.initEventListening();
    }

    private void initEventListening() {
        EventBus.subscribe(this, event -> {
            if (event instanceof SelectShipDataEntry) {
                selectShipTab();
            } else if (event instanceof SelectWeaponDataEntry) {
                selectWeaponTab();
            } else if (event instanceof InstrumentModeChanged updated) {
                handleInstrumentModeChange(updated.newMode());
            } else if (event instanceof VariantDataTabSelected tabEvent) {
                VariantDataTab variantDataTab = tabEvent.selected();
                if (variantDataTab == VariantDataTab.HULLMODS) {
                    selectHullmodsTab();
                } else if (variantDataTab == VariantDataTab.WINGS) {
                    selectWingsTab();
                }
            } else if (event instanceof DataTreesReloadQueued) {
                hullsTreePanel.queueReload();
                weaponsTreePanel.queueReload();
                hullmodsTreePanel.queueReload();
                systemsTreePanel.queueReload();
                wingsTreePanel.queueReload();
                projectilesTreePanel.queueReload();
            }
        });
    }

    private void handleInstrumentModeChange(EditorInstrument newMode) {
        switch (newMode) {
            case BUILT_IN_MODS -> selectHullmodsTab();
            case BUILT_IN_WINGS -> selectWingsTab();
            case VARIANT_WEAPONS -> selectWeaponTab();
            case VARIANT_MODULES -> selectShipTab();
            default -> {}
        }
    }

    private void selectShipTab() {
        dataTabsContainer.setSelectedComponent(hullsTreePanel.getParent().getParent());
    }

    private void selectWeaponTab() {
        dataTabsContainer.setSelectedComponent(weaponsTreePanel.getParent().getParent());
    }

    private void selectHullmodsTab() {
        dataTabsContainer.setSelectedComponent(dataContainerPanel);
        dataComboBox.setSelectedItem(StringValues.HULLMODS);
    }

    private void selectWingsTab() {
        dataTabsContainer.setSelectedComponent(dataContainerPanel);
        dataComboBox.setSelectedItem(StringValues.WINGS);
    }

}
