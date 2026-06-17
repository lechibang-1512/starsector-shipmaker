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
import javax.swing.JComboBox;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import shipeditor.communication.events.components.ComponentEvents.DataTreesReloadQueued;
import shipeditor.communication.events.components.ComponentEvents.VariantDataTabSelected;
import shipeditor.communication.events.viewer.points.PointEvents.InstrumentModeChanged;

@Log4j2
public class DataReferencePanel extends JPanel {

    private final HullmodsTreePanel hullmodsTreePanel;
    private final ShipSystemsTreePanel systemsTreePanel;
    private final WingsTreePanel wingsTreePanel;
    private final ProjectilesTreePanel projectilesTreePanel;
    
    private final CardLayout dataCardLayout;
    private final JPanel dataCardsPanel;
    private final JComboBox<String> dataComboBox;

    public DataReferencePanel() {
        hullmodsTreePanel = new HullmodsTreePanel();
        systemsTreePanel = new ShipSystemsTreePanel();
        wingsTreePanel = new WingsTreePanel();
        projectilesTreePanel = new ProjectilesTreePanel();
        
        this.setLayout(new BorderLayout());
        
        dataCardLayout = new CardLayout();
        dataCardsPanel = new JPanel(dataCardLayout);
        
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
        
        this.add(dataComboBox, BorderLayout.NORTH);
        this.add(dataCardsPanel, BorderLayout.CENTER);
        
        this.initEventListening();
    }

    private void initEventListening() {
        EventBus.subscribe(this, event -> {
            if (event instanceof InstrumentModeChanged updated) {
                handleInstrumentModeChange(updated.newMode());
            } else if (event instanceof VariantDataTabSelected tabEvent) {
                VariantDataTab variantDataTab = tabEvent.selected();
                if (variantDataTab == VariantDataTab.HULLMODS) {
                    selectHullmodsTab();
                } else if (variantDataTab == VariantDataTab.WINGS) {
                    selectWingsTab();
                }
            } else if (event instanceof DataTreesReloadQueued) {
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
            default -> {}
        }
    }

    private void selectHullmodsTab() {
        dataComboBox.setSelectedItem(StringValues.HULLMODS);
        GameDataReferenceWindow.showWindow();
    }

    private void selectWingsTab() {
        dataComboBox.setSelectedItem(StringValues.WINGS);
        GameDataReferenceWindow.showWindow();
    }

}
