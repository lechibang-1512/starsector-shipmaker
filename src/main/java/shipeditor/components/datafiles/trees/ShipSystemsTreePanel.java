package shipeditor.components.datafiles.trees;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.ShipSystemsLoaded;
import shipeditor.components.datafiles.entities.ShipSystemCSVEntry;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Log4j2
public
class ShipSystemsTreePanel extends CSVDataTreePanel<ShipSystemCSVEntry>{

    public ShipSystemsTreePanel() {
        super("Shipsystem file packages");
    }

    @Override
    protected Action getLoadDataAction() {
        return FileLoading.loadDataAsync(FileLoading.getLoadShipSystems());
    }

    @Override
    protected String getEntryTypeName() {
        return "shipsystem";
    }

    @Override
    protected Map<String, ShipSystemCSVEntry> getRepository() {
        GameDataRepository gameData = SettingsManager.getGameData();
        return gameData.getAllShipsystemEntries();
    }

    @Override
    protected Map<Path, List<ShipSystemCSVEntry>> getPackageList() {
        GameDataRepository gameData = SettingsManager.getGameData();
        return gameData.getShipSystemEntriesByPackage();
    }

    @Override
    protected void setLoadedStatus() {
        GameDataRepository gameData = SettingsManager.getGameData();
        gameData.setShipsystemDataLoaded(true);
    }

    @Override
    protected void initWalkerListening() {
        EventBus.subscribe(this, event -> {
            if (event instanceof ShipSystemsLoaded checked) {
                this.queueReload();
            }
        });
    }

    @Override
    protected void updateEntryPanel(ShipSystemCSVEntry selected) {
        JPanel rightPanel = getRightPanel();
        rightPanel.removeAll();

        createRightPanelDataTable(selected);
    }

    @Override
    protected ShipSystemCSVEntry getObjectFromNode(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (!(userObject instanceof ShipSystemCSVEntry checked)) return null;
        return checked;
    }

    @Override
    protected Class<?> getEntryClass() {
        return ShipSystemCSVEntry.class;
    }

}
