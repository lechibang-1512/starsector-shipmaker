package shipeditor.components.instrument.ship.variant.hullmods;

import shipeditor.utility.text.StringManager;

import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.instrument.ship.shared.HullmodsList;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
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

class VariantHullmodsListPane extends JPanel {

    private final HullmodsList modsList;

    private DefaultListModel<HullmodCSVEntry> modsModel;

    private final Function<ShipVariant, List<HullmodCSVEntry>> modsGetter;
    private final JButton addButton;
    private final JButton removeButton;

    VariantHullmodsListPane(Function<ShipVariant, List<HullmodCSVEntry>> getter,
                            BiConsumer<ShipVariant, List<HullmodCSVEntry>> sortSetter) {
        this.modsModel = new DefaultListModel<>();
        this.modsGetter = getter;

        Consumer<HullmodCSVEntry> removeAction = entry ->
                StaticController.actOnCurrentVariant((shipLayer, variant) -> {
                    var entryList = modsGetter.apply(variant);
                    EditDispatch.postHullmodRemoved(entryList, shipLayer, entry);
                });

        Consumer<List<HullmodCSVEntry>> sortAction = updatedList ->
                StaticController.actOnCurrentVariant((shipLayer, variant) -> {
                    var oldMods = modsGetter.apply(variant);
                    EditDispatch.postHullmodsSorted(oldMods, updatedList, shipLayer,
                            list -> sortSetter.accept(variant, list));
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
                StaticController.actOnCurrentVariant((layer, variant) -> {
                    var entryList = modsGetter.apply(variant);
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
            StaticController.actOnCurrentVariant((layer, variant) -> {
                var entryList = modsGetter.apply(variant);
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
        ShipPainter painter = checkedLayer.getPainter();
        if (painter != null && !painter.isUninitialized()) {
            ShipVariant active = painter.getActiveVariant();
            if (active != null && !active.isEmpty()) {
                List<HullmodCSVEntry> entries = modsGetter.apply(active);
                if (entries != null) {
                    newModel.addAll(entries);
                }
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
