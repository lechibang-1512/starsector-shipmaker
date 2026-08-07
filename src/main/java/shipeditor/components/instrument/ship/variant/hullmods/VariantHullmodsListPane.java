package shipeditor.components.instrument.ship.variant.hullmods;

import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.instrument.ship.shared.HullmodsList;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.overseers.StaticController;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import shipeditor.utility.themes.Themes;

class VariantHullmodsListPane extends JPanel {

    private final HullmodsList modsList;

    private DefaultListModel<HullmodCSVEntry> modsModel;

    private final Function<ShipVariant, List<HullmodCSVEntry>> modsGetter;

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
        this.setLayout(new BorderLayout());
        this.add(modsList, BorderLayout.CENTER);
    }

    void refreshListModel(ViewerLayer selected) {
        DefaultListModel<HullmodCSVEntry> newModel = new DefaultListModel<>();
        if (!(selected instanceof ShipLayer checkedLayer)) {
            this.modsModel = newModel;
            this.modsList.setModel(newModel);
            this.modsList.setEnabled(false);
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
            } else {
                this.modsList.setEnabled(false);
            }
        } else {
            this.modsList.setEnabled(false);
        }
        this.modsModel = newModel;
        this.modsList.setModel(newModel);
    }

}
