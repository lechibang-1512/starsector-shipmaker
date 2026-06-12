package shipeditor.components.instrument.ship.variant.modules;

import javax.swing.ListModel;

import shipeditor.communication.EventBus;
import shipeditor.components.datafiles.entities.CSVEntry;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.components.instrument.ship.shared.InstalledFeatureList;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import shipeditor.utility.text.StringValues;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Map;
import java.util.function.Consumer;
import shipeditor.communication.events.components.ComponentEvents.SelectShipDataEntry;

public final class ModuleList extends InstalledFeatureList {

    private final Runnable refresher;

    ModuleList(Runnable refreshAction, ListModel<InstalledFeature> dataModel,
               Consumer<InstalledFeature> removeAction,
               Consumer<Map<String, InstalledFeature>> sortAction) {
        super(dataModel, removeAction, sortAction, null);
        this.refresher = refreshAction;
    }

    @Override
    protected void handleEntrySelection(InstalledFeature feature) {
        refresher.run();
    }

    @Override
    protected boolean isSupported(Transferable transferable) {
        DataFlavor[] dataFlavors = transferable.getTransferDataFlavors();
        boolean isFeature = dataFlavors[0].equals(FEATURE_FLAVOR);

        String humanPresentableName = dataFlavors[1].getHumanPresentableName();
        boolean isSameList = humanPresentableName.equals(String.valueOf(this.hashCode()));
        return isFeature && isSameList;
    }

    @Override
    protected JPopupMenu getContextMenu() {
        JPopupMenu menu = super.getContextMenu();
        InstalledFeature selected = getSelectedValue();

        if (menu != null && selected != null) {
            JMenuItem loadAsLayer = new JMenuItem("Load as separate layer");

            loadAsLayer.addActionListener(event -> actOnSelectedEntry(InstalledFeature::loadAsSeparateLayer));
            if (!(selected.getDataEntry() instanceof ShipCSVEntry)) {
                loadAsLayer.setEnabled(false);
            }
            menu.add(loadAsLayer);
        }
        return menu;
    }

    @Override
    protected JMenuItem getSelectEntryOption(InstalledFeature selected) {
        JMenuItem selectEntry = new JMenuItem(StringValues.SELECT_SHIP_ENTRY);
        selectEntry.addActionListener(event -> actOnSelectedEntry(feature -> {
            CSVEntry dataEntry = feature.getDataEntry();
            if (dataEntry instanceof ShipCSVEntry shipEntry) {
                EventBus.publish(new SelectShipDataEntry(shipEntry));
            }
        }));
        if (!(selected.getDataEntry() instanceof ShipCSVEntry)) {
            selectEntry.setEnabled(false);
        }
        return selectEntry;
    }

}
