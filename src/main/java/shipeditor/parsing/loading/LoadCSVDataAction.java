package shipeditor.parsing.loading;

import lombok.extern.log4j.Log4j2;
import shipeditor.components.datafiles.entities.CSVEntry;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.utility.text.StringValues;

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
        if (SettingsManager.isDeveloperModeEnabled()) {
            log.trace(StringValues.COMMENCING_CSV_FETCH, csvDbType);
        }

        Map<String, List<IndexedFile>> filesByMod = DatabaseQueryService.getFilesByTypeGroupedByMod(csvDbType);

        Map<Path, List<T>> entriesByPackage = new HashMap<>();
        for (Map.Entry<String, List<IndexedFile>> modEntry : filesByMod.entrySet()) {
            for (IndexedFile dbFile : modEntry.getValue()) {
                Path folderPath = SettingsManager.getFolderForModId(modEntry.getKey());
                if (folderPath == null) {
                    log.warn(StringValues.NO_FOLDER_FOR_MOD_ID, modEntry.getKey(), dbFile.getFilePath());
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

                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.trace(StringValues.LOADING_CSV_TABLE, folderPath);
                }
                List<T> entriesList = loadPackage(folderPath, dbFile);
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

    private List<T> loadPackage(Path folderPath, IndexedFile dbFile) {
        Path dataFilePath = dbFile.getFilePath();

        List<Map<String, String>> csvData = null;
        if (dbFile.getParsedData() != null) {
            try {
                csvData = shipeditor.parsing.FileUtilities.getConfigured().readValue(
                        dbFile.getParsedData(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {});
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.trace(StringValues.CSV_LOADED_DB_CACHE);
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.error(StringValues.CSV_DESERIALIZE_DB_CACHE_FAILED, e);
                } else {
                    log.error(StringValues.CSV_DESERIALIZE_DB_CACHE_FAILED);
                }
            }
        }
        if (csvData == null) {
            csvData = parseTable(dataFilePath);
        }

        if (csvData == null) {
            log.info(StringValues.DATAFILES_FOLDER_NO_CSV, folderPath.toString());
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
                    log.error(StringValues.CSV_ENTRY_LOAD_FAILED, dataFilePath);
                }
            }
        }
        return entryList;
    }

}
