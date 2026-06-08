package oth.shipeditor.parsing.loading;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.components.datafiles.entities.CSVEntry;
import oth.shipeditor.persistence.GameDataPackage;
import oth.shipeditor.persistence.Settings;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.persistence.database.DatabaseQueryService;
import oth.shipeditor.persistence.database.IndexedFile;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
abstract class LoadCSVDataAction<T extends CSVEntry> extends DataLoadingAction {

    private final String csvDbType;

    LoadCSVDataAction(String csvDbType) {
        this.csvDbType = csvDbType;
    }

    @Override
    public Runnable perform() {
        log.trace("Commencing CSV data fetching from database index for type: {}", csvDbType);

        Map<String, List<IndexedFile>> filesByMod = DatabaseQueryService.getFilesByTypeGroupedByMod(csvDbType);

        Map<Path, List<T>> entriesByPackage = new HashMap<>();
        for (Map.Entry<String, List<IndexedFile>> modEntry : filesByMod.entrySet()) {
            for (IndexedFile dbFile : modEntry.getValue()) {
                Path folderPath = SettingsManager.getFolderForModId(modEntry.getKey());
                if (folderPath == null) {
                    log.warn("No folder found for mod_id '{}', skipping CSV: {}", modEntry.getKey(), dbFile.getFilePath());
                    continue;
                }

                Settings settings = SettingsManager.getSettings();
                GameDataPackage dataPackage = settings.getPackage(folderPath);
                if (dataPackage != null && dataPackage.isDisabled()) {
                    continue;
                }
                if (LibModFilter.isLibMod(folderPath)) {
                    continue;
                }

                log.trace("Loading CSV table from package: {}", folderPath);
                File table = dbFile.getFilePath().toFile();
                List<T> entriesList = loadPackage(folderPath, table);
                if (entriesList != null) {
                    entriesByPackage.putIfAbsent(folderPath, entriesList);
                }
            }
        }
        return () -> publishResult(entriesByPackage);
    }

    protected abstract void publishResult(Map<Path, List<T>> entriesByPackage);

    protected abstract T instantiateEntry(Map<String, String> row, Path folderPath, Path dataFilePath);

    List<Map<String, String>> parseTable(Path dataFilePath) {
        return FileLoading.parseCSVTable(dataFilePath);
    }

    private List<T> loadPackage(Path folderPath, File table) {
        Path dataFilePath = table.toPath();

        List<Map<String, String>> csvData = parseTable(dataFilePath);

        if (csvData == null) {
            log.info("Datafiles folder without CSV table at: {}", folderPath.toString());
            return null;
        }

        List<T> entryList = new ArrayList<>(csvData.size());
        for (Map<String, String> row : csvData) {
            String rowId = row.get("id");
            if (rowId != null && !rowId.isEmpty()) {
                T newEntry = instantiateEntry(row, folderPath, dataFilePath);
                if (newEntry != null) {
                    entryList.add(newEntry);
                } else {
                    log.error("Failure to load data entry from table, omitting from result data: {}", table);
                }
            }
        }
        return entryList;
    }

}
