package shipeditor.components.datafiles;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.DataTreesReloadQueued;
import shipeditor.communication.events.components.SelectShipDataEntry;
import shipeditor.communication.events.components.SelectWeaponDataEntry;
import shipeditor.communication.events.components.VariantDataTabSelected;
import shipeditor.communication.events.viewer.points.InstrumentModeChanged;
import shipeditor.components.datafiles.styles.EngineStylesPanel;
import shipeditor.components.datafiles.styles.HullStylesPanel;
import shipeditor.components.datafiles.trees.*;
import shipeditor.components.instrument.EditorInstrument;
import shipeditor.components.instrument.ship.variant.VariantDataTab;
import shipeditor.utility.text.StringValues;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

@Log4j2
public class GameDataPanel extends JPanel {

    private final JTabbedPane dataTabsContainer;
    private final HullsTreePanel hullsTreePanel;
    private final WeaponsTreePanel weaponsTreePanel;
    private final HullmodsTreePanel hullmodsTreePanel;
    private final ShipSystemsTreePanel systemsTreePanel;
    private final WingsTreePanel wingsTreePanel;
    private final ProjectilesTreePanel projectilesTreePanel;

    public GameDataPanel() {
        dataTabsContainer = new JTabbedPane(SwingConstants.TOP);
        dataTabsContainer.putClientProperty("JTabbedPane.tabWidthMode", "compact");
        
        hullsTreePanel = new HullsTreePanel();
        dataTabsContainer.addTab("Hulls", null, hullsTreePanel, "Hulls");
        
        weaponsTreePanel = new WeaponsTreePanel();
        dataTabsContainer.addTab("Weapons", null, weaponsTreePanel, "Weapons");
        
        hullmodsTreePanel = new HullmodsTreePanel();
        dataTabsContainer.addTab(StringValues.HULLMODS, null, hullmodsTreePanel, StringValues.HULLMODS);
        
        systemsTreePanel = new ShipSystemsTreePanel();
        dataTabsContainer.addTab("Shipsystems", null, systemsTreePanel, "Shipsystems");
        
        wingsTreePanel = new WingsTreePanel();
        dataTabsContainer.addTab(StringValues.WINGS, null, wingsTreePanel, StringValues.WINGS);

        projectilesTreePanel = new ProjectilesTreePanel();
        dataTabsContainer.addTab("Projectiles", null, projectilesTreePanel, "Projectiles");
        
        JPanel otherDataPanel = new JPanel(new BorderLayout());
        JTabbedPane otherDataTabs = new JTabbedPane(SwingConstants.TOP);
        otherDataTabs.putClientProperty("JTabbedPane.showTabSeparators", true);
        otherDataTabs.putClientProperty("JTabbedPane.hasFullBorder", false);
        otherDataTabs.putClientProperty("JTabbedPane.tabWidthMode", "compact");
        otherDataTabs.addTab("Hull styles", null, new HullStylesPanel(), "Hull styles");
        otherDataTabs.addTab("Engine styles", null, new EngineStylesPanel(), "Engine styles");
        otherDataPanel.add(otherDataTabs, BorderLayout.CENTER);
        dataTabsContainer.addTab("Other data", null, otherDataPanel, "Other data");

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
        dataTabsContainer.setSelectedComponent(hullsTreePanel);
    }

    private void selectWeaponTab() {
        dataTabsContainer.setSelectedComponent(weaponsTreePanel);
    }

    private void selectHullmodsTab() {
        dataTabsContainer.setSelectedComponent(hullmodsTreePanel);
    }

    private void selectWingsTab() {
        dataTabsContainer.setSelectedComponent(wingsTreePanel);
    }

}
