package shipeditor.parsing.loading;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.ShipSystemsLoaded;
import shipeditor.components.datafiles.entities.ShipSystemCSVEntry;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Log4j2
public class LoadShipSystemDataAction extends LoadCSVDataAction<ShipSystemCSVEntry> {

    LoadShipSystemDataAction() {
        super(StringConstants.SHIPSYSTEM_CSV_TYPE);
    }

    @Override
    protected void publishResult(Map<Path, List<ShipSystemCSVEntry>> entriesByPackage) {
        GameDataRepository gameData = SettingsManager.getGameData();
        gameData.setShipSystemEntriesByPackage(entriesByPackage);
        gameData.setShipsystemDataLoaded(true);
        EventBus.publish(new ShipSystemsLoaded(entriesByPackage));
    }

    @Override
    protected ShipSystemCSVEntry instantiateEntry(Map<String, String> row, Path folderPath, Path dataFilePath) {
        return new ShipSystemCSVEntry(row, folderPath, dataFilePath);
    }

    @Override
    public String getTaskName() {
        return StringValues.TASK_SHIP_SYSTEMS;
    }

}
