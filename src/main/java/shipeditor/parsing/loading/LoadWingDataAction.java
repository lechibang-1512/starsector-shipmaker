package shipeditor.parsing.loading;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.WingDataLoaded;
import shipeditor.components.datafiles.entities.WingCSVEntry;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LoadWingDataAction extends LoadCSVDataAction<WingCSVEntry> {

    LoadWingDataAction() {
        super(StringConstants.WING_CSV_TYPE);
    }

    @Override
    protected void publishResult(Map<Path, List<WingCSVEntry>> entriesByPackage) {
        GameDataRepository data = SettingsManager.getGameData();
        data.setWingEntriesByPackage(entriesByPackage);
        data.setWingDataLoaded(true);
        EventBus.publish(new WingDataLoaded(entriesByPackage));
    }

    @Override
    protected WingCSVEntry instantiateEntry(Map<String, String> row, Path folderPath, Path dataFilePath) {
        return new WingCSVEntry(row, folderPath, dataFilePath);
    }

    @Override
    protected List<Map<String, String>> parseTable(Path dataFilePath) {
        return FileLoading.parseCSVTable(dataFilePath, FileLoading.getWingValidationPredicate());
    }

    @Override
    public String getTaskName() {
        return StringValues.TASK_WINGS;
    }

}
