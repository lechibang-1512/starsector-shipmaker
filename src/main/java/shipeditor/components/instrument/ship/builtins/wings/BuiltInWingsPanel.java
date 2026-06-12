package shipeditor.components.instrument.ship.builtins.wings;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.data.ShipHull;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
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

public class BuiltInWingsPanel extends JPanel {

    private final BaseWingListPane baseBuiltInWingsList;

    private final SkinWingListPane addedBySkinList;

    private final SkinWingListPane removedBySkinList;

    private JLabel totalBayCount;

    private JLabel builtInWingsCount;

    public BuiltInWingsPanel() {
        this.setLayout(new BorderLayout());

        this.baseBuiltInWingsList = new BaseWingListPane(ShipHull::getBuiltInWings, ShipHull::setBuiltInWings);
        ComponentUtilities.outfitPanelWithTitle(baseBuiltInWingsList, StringValues.BASE_BUILT_INS);
        this.addedBySkinList = new SkinWingListPane(ShipSkin::getBuiltInWings, ShipSkin::setBuiltInWings);
        ComponentUtilities.outfitPanelWithTitle(addedBySkinList, StringValues.ADDED_BY_SKIN);
        this.removedBySkinList = new SkinWingListPane(ShipSkin::getRemoveBuiltInWings, ShipSkin::setRemoveBuiltInWings);
        ComponentUtilities.outfitPanelWithTitle(removedBySkinList, StringValues.REMOVED_BY_SKIN);

        JPanel container = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 0.33;
        constraints.weightx = 1;
        constraints.ipady = 40;
        constraints.gridy = 0;

        container.add(baseBuiltInWingsList, constraints);
        constraints.gridy = 1;
        container.add(addedBySkinList, constraints);
        constraints.gridy = 2;
        container.add(removedBySkinList, constraints);

        JScrollPane scroller = new JScrollPane(container);
        JScrollBar verticalScrollBar = scroller.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);

        this.add(scroller, BorderLayout.CENTER);

        JPanel infoPanel = createInfoPanel();
        this.add(infoPanel, BorderLayout.PAGE_START);

        this.initLayerListeners();
    }

    private JPanel createInfoPanel() {
        JPanel container = new JPanel(new BorderLayout());

        JPanel dragHintPanel = ComponentUtilities.createDragInfoPanel();
        container.add(dragHintPanel, BorderLayout.PAGE_START);

        JPanel infoPanel = new JPanel();
        ComponentUtilities.outfitPanelWithTitle(infoPanel, "Built-in wings");
        infoPanel.setLayout(new GridBagLayout());

        JLabel totalBaysLabel = new JLabel(StringValues.TOTAL_SHIP_BAYS);
        totalBayCount = new JLabel();

        ComponentUtilities.addLabelAndComponent(infoPanel, totalBaysLabel, totalBayCount, 0);

        JLabel totalBuiltInsLabel = new JLabel(StringValues.TOTAL_BUILT_IN_WINGS);
        builtInWingsCount = new JLabel();

        ComponentUtilities.addLabelAndComponent(infoPanel, totalBuiltInsLabel, builtInWingsCount, 1);

        container.add(infoPanel, BorderLayout.CENTER);

        return container;
    }

    private void initLayerListeners() {
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerWasSelected checked) {
                ViewerLayer selected = checked.selected();

                refreshLayerInfo(selected);

                baseBuiltInWingsList.refreshListModel(selected);
                addedBySkinList.refreshListModel(selected);
                removedBySkinList.refreshListModel(selected);
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof InstrumentRepaintQueued checked) {
                if (checked.editorMode() == EditorInstrument.BUILT_IN_WINGS) {
                    this.refreshLayerInfo(StaticController.getActiveLayer());
                }
            }
        });
    }

    private void refreshLayerInfo(ViewerLayer selected) {
        String notInitialized = StringValues.NOT_INITIALIZED;

        if (selected instanceof ShipLayer shipLayer) {
            String totalBays = Utility.translateIntegerValue(shipLayer::getBayCount);
            totalBayCount.setText(totalBays);
            String totalBuiltIns =  Utility.translateIntegerValue(shipLayer::getBuiltInWingsCount);
            builtInWingsCount.setText(totalBuiltIns);
        } else {
            totalBayCount.setText(notInitialized);
            builtInWingsCount.setText(notInitialized);
        }
    }

}
