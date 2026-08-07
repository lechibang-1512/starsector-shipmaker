package shipeditor.components.instrument.ship.variant.hullmods;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.utility.Utility;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.StringValues;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;

public class VariantHullmodsPanel extends JPanel {

    private final VariantHullmodsListPane normalModsPanel;

    private final VariantHullmodsListPane permaModsPanel;

    private final VariantHullmodsListPane sModsPanel;

    private final SuppressedModsPanel suppressedModsPanel;


    public VariantHullmodsPanel() {
        this.setLayout(new BorderLayout());

        this.normalModsPanel = new VariantHullmodsListPane(a -> a.getHullMods(), (a, b) -> a.setHullMods(b));
        ComponentUtilities.outfitPanelWithTitle(normalModsPanel, "Normal");
        this.permaModsPanel = new VariantHullmodsListPane(a -> a.getPermaMods(), (a, b) -> a.setPermaMods(b));
        ComponentUtilities.outfitPanelWithTitle(permaModsPanel, "Permanent");
        this.sModsPanel = new VariantHullmodsListPane(a -> a.getSMods(), (a, b) -> a.setSMods(b));
        ComponentUtilities.outfitPanelWithTitle(sModsPanel, "S-Mods");

        this.suppressedModsPanel = new SuppressedModsPanel();

        JPanel container = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 0.25;
        constraints.weightx = 1;
        constraints.ipady = 40;
        constraints.gridy = 0;

        container.add(normalModsPanel, constraints);
        constraints.gridy = 1;
        container.add(permaModsPanel, constraints);
        constraints.gridy = 2;
        container.add(sModsPanel, constraints);
        constraints.gridy = 3;
        container.add(suppressedModsPanel, constraints);

        JScrollPane scroller = new JScrollPane(container);
        JScrollBar verticalScrollBar = scroller.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);

        this.add(scroller, BorderLayout.CENTER);

        this.initLayerListeners();
    }


    private void initLayerListeners() {
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerWasSelected checked) {
                ViewerLayer selected = checked.selected();
                normalModsPanel.refreshListModel(selected);
                permaModsPanel.refreshListModel(selected);
                sModsPanel.refreshListModel(selected);
                suppressedModsPanel.refreshListModel(selected);

                refreshLayerInfo(selected);
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof InstrumentRepaintQueued checked) {
                if (checked.editorMode() == EditorInstrument.VARIANT_DATA) {
                    this.refreshLayerInfo(StaticController.getActiveLayer());
                }
            }
        });
    }

    private void refreshLayerInfo(ViewerLayer selected) {
        // Now handled globally by OPSummaryBar, but can keep this method in case we add other info later
    }

}
