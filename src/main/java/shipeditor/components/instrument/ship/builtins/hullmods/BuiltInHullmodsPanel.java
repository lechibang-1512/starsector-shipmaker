package shipeditor.components.instrument.ship.builtins.hullmods;

import shipeditor.utility.text.StringManager;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.utility.components.ComponentUtilities;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class BuiltInHullmodsPanel extends JPanel {

    private final BaseHullmodsListPane baseBuiltInModsList;

    private final SkinHullmodsListPane addedBySkinList;

    private final SkinHullmodsListPane removedBySkinList;

    public BuiltInHullmodsPanel() {
        this.setLayout(new BorderLayout());

        this.baseBuiltInModsList = new BaseHullmodsListPane(a -> a.getBuiltInMods(), (a, b) -> a.setBuiltInMods(b));
        ComponentUtilities.outfitPanelWithTitle(baseBuiltInModsList, StringManager.getString("BASE_BUILT_INS"));
        this.addedBySkinList = new SkinHullmodsListPane(a -> a.getBuiltInMods(), (a, b) -> a.setBuiltInMods(b));
        ComponentUtilities.outfitPanelWithTitle(addedBySkinList, StringManager.getString("ADDED_BY_SKIN"));
        this.removedBySkinList = new SkinHullmodsListPane(a -> a.getRemoveBuiltInMods(), (a, b) -> a.setRemoveBuiltInMods(b));
        ComponentUtilities.outfitPanelWithTitle(removedBySkinList, StringManager.getString("REMOVED_BY_SKIN"));

        JPanel container = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 0.33;
        constraints.weightx = 1;
        constraints.ipady = 40;
        constraints.gridy = 0;

        container.add(baseBuiltInModsList, constraints);
        constraints.gridy = 1;
        container.add(addedBySkinList, constraints);
        constraints.gridy = 2;
        container.add(removedBySkinList, constraints);

        JScrollPane scroller = new JScrollPane(container);
        JScrollBar verticalScrollBar = scroller.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);

        this.add(scroller, BorderLayout.CENTER);

        JPanel infoPanel = ComponentUtilities.createDragInfoPanel();
        this.add(infoPanel, BorderLayout.PAGE_START);

        this.initLayerListeners();
    }

    private void initLayerListeners() {
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerWasSelected checked) {
                ViewerLayer selected = checked.selected();
                baseBuiltInModsList.refreshListModel(selected);
                addedBySkinList.refreshListModel(selected);
                removedBySkinList.refreshListModel(selected);
            }
        });
    }

}
