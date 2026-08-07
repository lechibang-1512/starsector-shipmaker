package shipeditor.components.instrument.ship.builtins.hullmods;

import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.instrument.ship.shared.HullmodsList;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.overseers.StaticController;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import shipeditor.utility.themes.Themes;

public class SkinHullmodsListPane extends JPanel {

    private final HullmodsList modsList;

    private DefaultListModel<HullmodCSVEntry> modsModel;

    private final Function<ShipSkin, List<HullmodCSVEntry>> modsGetter;

    SkinHullmodsListPane(Function<ShipSkin, List<HullmodCSVEntry>> getter,
                         BiConsumer<ShipSkin, List<HullmodCSVEntry>> sortSetter) {
        this.modsModel = new DefaultListModel<>();
        this.modsGetter = getter;

        Consumer<HullmodCSVEntry> removeAction = entry ->
                StaticController.actOnCurrentSkin((shipLayer, shipSkin) -> {
            var entryList = modsGetter.apply(shipSkin);
            EditDispatch.postHullmodRemoved(entryList, shipLayer, entry);
        });

        Consumer<List<HullmodCSVEntry>> sortAction = updatedList ->
                StaticController.actOnCurrentSkin((shipLayer, shipSkin) -> {
            var oldMods = modsGetter.apply(shipSkin);
            EditDispatch.postHullmodsSorted(oldMods, updatedList, shipLayer,
                    list -> sortSetter.accept(shipSkin, list));
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
        ShipSkin activeSkin = checkedLayer.getActiveSkin();
        if (activeSkin != null && !activeSkin.isBase()) {
            List<HullmodCSVEntry> entries = modsGetter.apply(activeSkin);
            if (entries != null) {
                newModel.addAll(entries);
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
