package oth.shipeditor.parsing.loading;

import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.files.WingDataLoaded;
import oth.shipeditor.components.datafiles.entities.WingCSVEntry;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.representation.GameDataRepository;
import oth.shipeditor.utility.text.StringValues;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LoadWingDataAction extends LoadCSVDataAction<WingCSVEntry> {

    LoadWingDataAction() {
        super("WING_CSV");
    }

    @Override
    protected void publishResult(Map<Path, List<WingCSVEntry>> entriesByPackage) {
        GameDataRepository data = SettingsManager.getGameData();
        if (!data.isShipDataLoaded()) {
            JOptionPane.showMessageDialog(oth.shipeditor.PrimaryWindow.getInstance(),
                    "Ship data is not loaded, wings unavailable for display.",
                    StringValues.FILE_LOADING_ERROR,
                    JOptionPane.ERROR_MESSAGE);
        }
        data.setWingEntriesByPackage(entriesByPackage);
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
        return "Wings";
    }

}
