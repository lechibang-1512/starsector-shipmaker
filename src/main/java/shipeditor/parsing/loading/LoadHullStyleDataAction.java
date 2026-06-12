package shipeditor.parsing.loading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.HullStylesLoaded;
import shipeditor.parsing.FileUtilities;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.ship.HullStyle;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class LoadHullStyleDataAction extends DataLoadingAction {

    @Override
    public Runnable perform() {
        List<IndexedFile> dbFiles = DatabaseQueryService.getFilesByType(StringConstants.HULL_STYLE_JSON_TYPE);

        Map<String, HullStyle> collectedHullStyles = new LinkedHashMap<>();
        for (IndexedFile dbFile : dbFiles) {
            Path folderPath = SettingsManager.getFolderForModId(dbFile.getModId());
            if (folderPath == null) {
                log.warn(StringValues.NO_FOLDER_FOR_MOD_HULL_STYLES, dbFile.getModId());
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
                log.trace(StringValues.HULL_STYLE_FILE_FOUND, folderPath);
            }
            Map<String, HullStyle> stylesFromFile = loadHullStyleFile(dbFile);
            for (HullStyle style : stylesFromFile.values()) {
                style.setContainingPackage(folderPath);
            }
            collectedHullStyles.putAll(stylesFromFile);
        }

        return () -> {
            GameDataRepository gameData = SettingsManager.getGameData();
            gameData.setAllHullStyles(collectedHullStyles);
            EventBus.publish(new HullStylesLoaded(collectedHullStyles));
        };
    }

    @Override
    public String getTaskName() {
        return StringValues.TASK_HULL_STYLES;
    }

    private static Map<String, HullStyle> loadHullStyleFile(IndexedFile dbFile) {
        File styleFile = dbFile.getFilePath().toFile();
        ObjectMapper mapper = FileUtilities.getConfigured();
        Map<String, HullStyle> hullStyles = null;
        if (SettingsManager.isDeveloperModeEnabled()) {
            log.trace(StringValues.FETCHING_HULL_STYLE_DATA, styleFile.toPath());
        }
        com.fasterxml.jackson.databind.type.MapType mapType = null;
        try {
            TypeFactory typeFactory = mapper.getTypeFactory();
            mapType = typeFactory.constructMapType(HashMap.class, String.class, HullStyle.class);
            if (dbFile.getParsedData() != null) {
                try {
                    hullStyles = mapper.readValue(dbFile.getParsedData(), mapType);
                    if (SettingsManager.isDeveloperModeEnabled()) {
                        log.trace(StringValues.LOADED_HULL_STYLES_DB);
                    }
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    if (SettingsManager.isDeveloperModeEnabled()) {
                        log.error(StringValues.FAILED_DESERIALIZE_HULL_STYLES_DB, e);
                    } else {
                        log.error(StringValues.FAILED_DESERIALIZE_HULL_STYLES_DB);
                    }
                }
            }
            if (hullStyles == null) {
                hullStyles = mapper.readValue(styleFile, mapType);
            }
        } catch (IOException e) {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.trace(StringValues.HULL_STYLES_LOAD_FAILED_RETRY, styleFile.getName());
            }
            hullStyles = FileLoading.parseCorrectableJSON(styleFile, mapType);
        }

        if (hullStyles == null) {
            log.error(StringValues.HULL_STYLES_LOAD_FAILED_CONCLUSIVE, styleFile.getName());
            return new HashMap<>();
        }

        for (Map.Entry<String, HullStyle> entry : hullStyles.entrySet()) {
            String hullStyleID = entry.getKey();
            HullStyle hullStyle = entry.getValue();
            if (hullStyle != null) {
                hullStyle.setHullStyleID(hullStyleID);
                hullStyle.setFilePath(styleFile.toPath());
            }
        }
        return hullStyles;
    }

}
