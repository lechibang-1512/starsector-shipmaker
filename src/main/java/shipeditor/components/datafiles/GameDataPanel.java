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

@Log4j2
public class GameDataPanel extends JPanel {

    private final JTabbedPane dataTabsContainer;
    private final HullsTreePanel hullsTreePanel;
    private final WeaponsTreePanel weaponsTreePanel;
    private final ProjectilesTreePanel projectilesTreePanel;
    private final WingsTreePanel wingsTreePanel;
    private final JTabbedPane weaponsTabbedPane;

    public GameDataPanel() {
        dataTabsContainer = new JTabbedPane(SwingConstants.TOP);
        dataTabsContainer.putClientProperty("JTabbedPane.tabWidthMode", "compact");

        hullsTreePanel = new HullsTreePanel();
        JTabbedPane hullsTabbedPane = new JTabbedPane(SwingConstants.BOTTOM);
        hullsTabbedPane.addTab("List", hullsTreePanel);
        hullsTabbedPane.addTab("Filters", new ShipFilterPanel());
        dataTabsContainer.addTab("Hulls", null, hullsTabbedPane, "Hulls");

        weaponsTreePanel = new WeaponsTreePanel();
        weaponsTabbedPane = new JTabbedPane(SwingConstants.BOTTOM);
        weaponsTabbedPane.addTab("List", weaponsTreePanel);
        weaponsTabbedPane.addTab("Filters", new WeaponFilterPanel());
        dataTabsContainer.addTab("Weapons", null, weaponsTabbedPane, "Weapons");

        projectilesTreePanel = new ProjectilesTreePanel();
        dataTabsContainer.addTab("Projectiles", null, projectilesTreePanel, "Projectiles");

        wingsTreePanel = new WingsTreePanel();
        dataTabsContainer.addTab("Wings", null, wingsTreePanel, "Fighter wings");

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
            } else if (event instanceof DataTreesReloadQueued) {
                hullsTreePanel.queueReload();
                weaponsTreePanel.queueReload();
                projectilesTreePanel.queueReload();
                wingsTreePanel.queueReload();
            } else if (event instanceof SelectWingsDataTab) {
                selectWingsTab();
            }
        });
    }

    private void handleInstrumentModeChange(EditorInstrument newMode) {
        switch (newMode) {
            case VARIANT_WEAPONS -> selectWeaponTab();
            case VARIANT_MODULES -> selectShipTab();
            case BUILT_IN_WINGS -> selectWingsTab();
            default -> {
            }
        }
    }

    private void selectShipTab() {
        java.awt.Component comp = hullsTreePanel.getParent().getParent();
        if (dataTabsContainer.indexOfComponent(comp) != -1) {
            dataTabsContainer.setSelectedComponent(comp);
        }
    }

    private void selectWeaponTab() {
        if (dataTabsContainer.indexOfComponent(weaponsTabbedPane) != -1) {
            dataTabsContainer.setSelectedComponent(weaponsTabbedPane);
        }
    }

    private void selectWingsTab() {
        int index = dataTabsContainer.indexOfComponent(wingsTreePanel);
        if (index != -1) {
            dataTabsContainer.setSelectedIndex(index);
        }
    }

}
