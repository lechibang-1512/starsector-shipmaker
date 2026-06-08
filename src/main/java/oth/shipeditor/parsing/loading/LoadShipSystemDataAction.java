package oth.shipeditor.parsing.loading;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.files.ShipSystemsLoaded;
import oth.shipeditor.components.datafiles.entities.ShipSystemCSVEntry;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.representation.GameDataRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Log4j2
public class LoadShipSystemDataAction extends LoadCSVDataAction<ShipSystemCSVEntry> {

    LoadShipSystemDataAction() {
        super("SHIPSYSTEM_CSV");
    }

    @Override
    protected void publishResult(Map<Path, List<ShipSystemCSVEntry>> entriesByPackage) {
        GameDataRepository gameData = SettingsManager.getGameData();
        gameData.setShipSystemEntriesByPackage(entriesByPackage);
        EventBus.publish(new ShipSystemsLoaded(entriesByPackage));
    }

    @Override
    protected ShipSystemCSVEntry instantiateEntry(Map<String, String> row, Path folderPath, Path dataFilePath) {
        return new ShipSystemCSVEntry(row, folderPath, dataFilePath);
    }

    @Override
    public String getTaskName() {
        return "Ship Systems";
    }

}
