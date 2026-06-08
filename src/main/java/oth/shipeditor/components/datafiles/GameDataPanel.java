package oth.shipeditor.components.datafiles;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.components.DataTreesReloadQueued;
import oth.shipeditor.communication.events.components.SelectShipDataEntry;
import oth.shipeditor.communication.events.components.SelectWeaponDataEntry;
import oth.shipeditor.communication.events.components.VariantDataTabSelected;
import oth.shipeditor.communication.events.viewer.points.InstrumentModeChanged;
import oth.shipeditor.components.datafiles.styles.EngineStylesPanel;
import oth.shipeditor.components.datafiles.styles.HullStylesPanel;
import oth.shipeditor.components.datafiles.trees.*;
import oth.shipeditor.components.instrument.EditorInstrument;
import oth.shipeditor.components.instrument.ship.variant.VariantDataTab;
import oth.shipeditor.utility.text.StringValues;

import javax.swing.*;
import java.awt.*;

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
        dataTabsContainer = new JTabbedPane(SwingConstants.LEFT);
        
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
        
        dataTabsContainer.addTab("Hull styles", null, new HullStylesPanel(), "Hull styles");
        
        dataTabsContainer.addTab("Engine styles", null, new EngineStylesPanel(), "Engine styles");
        dataTabsContainer.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
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
                hullsTreePanel.reload();
                weaponsTreePanel.reload();
                hullmodsTreePanel.reload();
                systemsTreePanel.reload();
                wingsTreePanel.reload();
                projectilesTreePanel.reload();
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
