package shipeditor.components.instrument.ship.builtins.hullmods;

import shipeditor.utility.text.StringManager;

import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.instrument.ship.shared.HullmodsList;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.data.ShipHull;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.overseers.StaticController;

import shipeditor.utility.components.dialog.DialogUtilities;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import shipeditor.utility.themes.Themes;

public class BaseHullmodsListPane extends JPanel {

    private final HullmodsList modsList;

    private DefaultListModel<HullmodCSVEntry> modsModel;

    private final Function<ShipHull, List<HullmodCSVEntry>> modsGetter;
    private final JButton addButton;
    private final JButton removeButton;

    BaseHullmodsListPane(Function<ShipHull, List<HullmodCSVEntry>> getter,
                         BiConsumer<ShipHull, List<HullmodCSVEntry>> sortSetter) {
        this.modsModel = new DefaultListModel<>();
        this.modsGetter = getter;

        Consumer<HullmodCSVEntry> removeAction = entry -> StaticController.actOnCurrentShip((shipLayer) -> {
            var entryList = modsGetter.apply(shipLayer.getHull());
            EditDispatch.postHullmodRemoved(entryList, shipLayer, entry);
        });

        Consumer<List<HullmodCSVEntry>> sortAction = updatedList ->
                StaticController.actOnCurrentShip((shipLayer) -> {
                    ShipHull shipHull = shipLayer.getHull();
                    var oldMods = modsGetter.apply(shipHull);
                    EditDispatch.postHullmodsSorted(oldMods, updatedList, shipLayer,
                            list -> sortSetter.accept(shipHull, list));
                });

        this.modsList = new HullmodsList(removeAction, modsModel, sortAction);
        modsList.setBorder(new LineBorder(Themes.getBorderColor()));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 4, 2));
        addButton = new JButton(StringManager.getString("ADD"));
        removeButton = new JButton(StringManager.getString("REMOVE"));
        addButton.setEnabled(false);
        removeButton.setEnabled(false);

        addButton.addActionListener(e -> {
            var activeLayer = StaticController.getActiveLayer();
            if (!(activeLayer instanceof ShipLayer shipLayer)) return;
            var shipPainter = shipLayer.getPainter();
            if (shipPainter == null || shipPainter.isUninitialized()) return;
            var shipHull = shipLayer.getHull();
            var shipSize = shipHull != null ? shipHull.getHullSize() : null;

            HullmodCSVEntry picked = DialogUtilities.showHullmodPickerDialog(shipSize, shipLayer);
            if (picked != null) {
                StaticController.actOnCurrentShip((layer) -> {
                    var entryList = modsGetter.apply(layer.getHull());
                    if (entryList != null && !entryList.contains(picked)) {
                        EditDispatch.postHullmodAdded(entryList, layer, picked);
                        refreshListModel(layer);
                    }
                });
            }
        });

        removeButton.addActionListener(e -> {
            HullmodCSVEntry selected = modsList.getSelectedValue();
            if (selected == null) return;
            StaticController.actOnCurrentShip((layer) -> {
                var entryList = modsGetter.apply(layer.getHull());
                if (entryList != null && entryList.contains(selected)) {
                    EditDispatch.postHullmodRemoved(entryList, layer, selected);
                    refreshListModel(layer);
                }
            });
        });

        modsList.addListSelectionListener(e -> {
            removeButton.setEnabled(modsList.getSelectedValue() != null);
        });

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        this.setLayout(new BorderLayout());
        this.add(modsList, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.PAGE_END);
    }

    void refreshListModel(ViewerLayer selected) {
        DefaultListModel<HullmodCSVEntry> newModel = new DefaultListModel<>();
        if (!(selected instanceof ShipLayer checkedLayer)) {
            this.modsModel = newModel;
            this.modsList.setModel(newModel);
            this.modsList.setEnabled(false);
            this.addButton.setEnabled(false);
            this.removeButton.setEnabled(false);
            return;
        }
        ShipHull hull = checkedLayer.getHull();
        if (hull != null) {
            List<HullmodCSVEntry> entries = modsGetter.apply(hull);
            if (entries != null) {
                newModel.addAll(entries);
                this.modsList.setEnabled(true);
                this.addButton.setEnabled(true);
            } else {
                this.modsList.setEnabled(false);
                this.addButton.setEnabled(false);
            }
        } else {
            this.modsList.setEnabled(false);
            this.addButton.setEnabled(false);
        }
        this.removeButton.setEnabled(false);
        this.modsModel = newModel;
        this.modsList.setModel(newModel);
    }

}
