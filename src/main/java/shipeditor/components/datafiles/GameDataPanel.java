package shipeditor.components.datafiles;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.components.datafiles.trees.*;
import shipeditor.components.ComponentEnums.EditorInstrument;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import shipeditor.communication.events.components.ComponentEvents.SelectWeaponDataEntry;
import shipeditor.communication.events.components.ComponentEvents.DataTreesReloadQueued;
import shipeditor.communication.events.components.ComponentEvents.SelectWingsDataTab;
import shipeditor.communication.events.components.ComponentEvents.SelectShipDataEntry;
import shipeditor.communication.events.viewer.points.PointEvents.InstrumentModeChanged;
import shipeditor.components.datafiles.styles.EngineStylesPanel;
import shipeditor.components.datafiles.styles.HullStylesPanel;
import shipeditor.utility.text.StringValues;

@Log4j2
public class GameDataPanel extends JPanel {

    private final JTabbedPane dataTabsContainer;
    private final HullsTreePanel hullsTreePanel;
    private final WeaponsTreePanel weaponsTreePanel;
    private final ProjectilesTreePanel projectilesTreePanel;
    private final WingsTreePanel wingsTreePanel;
    private final HullmodsTreePanel hullmodsTreePanel;
    private final ShipSystemsTreePanel systemsTreePanel;

    public GameDataPanel() {
        dataTabsContainer = new JTabbedPane(SwingConstants.TOP);
        dataTabsContainer.putClientProperty("JTabbedPane.tabWidthMode", "compact");

        hullsTreePanel = new HullsTreePanel();
        dataTabsContainer.addTab("Hulls", null, hullsTreePanel, "Hulls");

        weaponsTreePanel = new WeaponsTreePanel();
        dataTabsContainer.addTab("Weapons", null, weaponsTreePanel, "Weapons");

        projectilesTreePanel = new ProjectilesTreePanel();
        dataTabsContainer.addTab("Projectiles", null, projectilesTreePanel, "Projectiles");

        wingsTreePanel = new WingsTreePanel();
        dataTabsContainer.addTab("Wings", null, wingsTreePanel, "Fighter wings");

        hullmodsTreePanel = new HullmodsTreePanel();
        dataTabsContainer.addTab(StringValues.HULLMODS, hullmodsTreePanel);

        systemsTreePanel = new ShipSystemsTreePanel();
        dataTabsContainer.addTab("Shipsystems", systemsTreePanel);

        dataTabsContainer.addTab("Hull styles", new HullStylesPanel());
        dataTabsContainer.addTab("Engine styles", new EngineStylesPanel());

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
            } else if (event instanceof DataTreesReloadQueued) {
                hullsTreePanel.queueReload();
                weaponsTreePanel.queueReload();
                projectilesTreePanel.queueReload();
                wingsTreePanel.queueReload();
                hullmodsTreePanel.queueReload();
                systemsTreePanel.queueReload();
            } else if (event instanceof SelectWingsDataTab) {
                selectWingsTab();
            }
        });
    }

    private void handleInstrumentModeChange(EditorInstrument newMode) {
        if (newMode == null) return;
        switch (newMode) {
            case VARIANT_WEAPONS -> selectWeaponTab();
            case VARIANT_MODULES -> selectShipTab();
            case BUILT_IN_WINGS -> selectWingsTab();
            default -> {
            }
        }
    }

    private void selectShipTab() {
        dataTabsContainer.setSelectedComponent(hullsTreePanel);
    }

    private void selectWeaponTab() {
        dataTabsContainer.setSelectedComponent(weaponsTreePanel);
    }

    private void selectWingsTab() {
        int index = dataTabsContainer.indexOfComponent(wingsTreePanel);
        if (index != -1) {
            dataTabsContainer.setSelectedIndex(index);
        }
    }

}
