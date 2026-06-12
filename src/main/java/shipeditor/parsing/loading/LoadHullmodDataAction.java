package shipeditor.parsing.loading;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.HullmodFoldersWalked;
import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Log4j2
public class LoadHullmodDataAction extends LoadCSVDataAction<HullmodCSVEntry> {

    LoadHullmodDataAction() {
        super(StringConstants.HULLMOD_CSV_TYPE);
    }

    @Override
    protected void publishResult(Map<Path, List<HullmodCSVEntry>> entriesByPackage) {
        GameDataRepository gameData = SettingsManager.getGameData();
        gameData.setHullmodEntriesByPackage(entriesByPackage);
        gameData.setHullmodDataLoaded(true);
        EventBus.publish(new HullmodFoldersWalked(entriesByPackage));
    }

    @Override
    protected HullmodCSVEntry instantiateEntry(Map<String, String> row, Path folderPath, Path dataFilePath) {
        return new HullmodCSVEntry(row, folderPath, dataFilePath);
    }

    @Override
    public String getTaskName() {
        return StringValues.TASK_HULLMODS;
    }

}
