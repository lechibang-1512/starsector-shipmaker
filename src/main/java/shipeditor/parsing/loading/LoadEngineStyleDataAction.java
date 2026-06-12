package shipeditor.parsing.loading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.EngineStylesLoaded;
import shipeditor.parsing.FileUtilities;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.representation.ship.EngineStyle;
import shipeditor.representation.GameDataRepository;
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
public class LoadEngineStyleDataAction extends DataLoadingAction {

    @Override
    public Runnable perform() {
        List<IndexedFile> dbFiles = DatabaseQueryService.getFilesByType(StringConstants.ENGINE_STYLE_JSON_TYPE);

        Map<String, EngineStyle> collectedEngineStyles = new LinkedHashMap<>();
        for (IndexedFile dbFile : dbFiles) {
            Path folderPath = SettingsManager.getFolderForModId(dbFile.getModId());
            if (folderPath == null) {
                log.warn(StringValues.NO_FOLDER_FOR_MOD_ENGINE_STYLES, dbFile.getModId());
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
                log.trace(StringValues.ENGINE_STYLE_FILE_FOUND, folderPath);
            }
            Map<String, EngineStyle> stylesFromFile = loadEngineStyleFile(dbFile);
            for (EngineStyle style : stylesFromFile.values()) {
                style.setContainingPackage(folderPath);
            }
            collectedEngineStyles.putAll(stylesFromFile);
        }

        return () -> {
            GameDataRepository gameData = SettingsManager.getGameData();
            gameData.setAllEngineStyles(collectedEngineStyles);
            EventBus.publish(new EngineStylesLoaded(collectedEngineStyles));
        };
    }

    @Override
    public String getTaskName() {
        return StringValues.TASK_ENGINE_STYLES;
    }

    private static Map<String, EngineStyle> loadEngineStyleFile(IndexedFile dbFile) {
        File styleFile = dbFile.getFilePath().toFile();
        ObjectMapper mapper = FileUtilities.getConfigured();
        Map<String, EngineStyle> engineStyles = null;
        if (SettingsManager.isDeveloperModeEnabled()) {
            log.trace(StringValues.FETCHING_ENGINE_STYLE_DATA, styleFile.toPath());
        }
        MapType mapType = null;
        try {
            TypeFactory typeFactory = mapper.getTypeFactory();
            mapType = typeFactory.constructMapType(HashMap.class, String.class, EngineStyle.class);
            if (dbFile.getParsedData() != null) {
                try {
                    engineStyles = mapper.readValue(dbFile.getParsedData(), mapType);
                    if (SettingsManager.isDeveloperModeEnabled()) {
                        log.trace(StringValues.LOADED_ENGINE_STYLES_DB);
                    }
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    if (SettingsManager.isDeveloperModeEnabled()) {
                        log.error(StringValues.FAILED_DESERIALIZE_ENGINE_STYLES_DB, e);
                    } else {
                        log.error(StringValues.FAILED_DESERIALIZE_ENGINE_STYLES_DB);
                    }
                }
            }
            if (engineStyles == null) {
                engineStyles = mapper.readValue(styleFile, mapType);
            }
        } catch (IOException e) {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.trace(StringValues.ENGINE_STYLES_LOAD_FAILED_RETRY, styleFile.getName());
            }
            engineStyles = FileLoading.parseCorrectableJSON(styleFile, mapType);
        }

        if (engineStyles == null) {
            log.error(StringValues.ENGINE_STYLES_LOAD_FAILED_CONCLUSIVE, styleFile.getName());
            return new HashMap<>();
        }

        for (Map.Entry<String, EngineStyle> entry : engineStyles.entrySet()) {
            String engineStyleID = entry.getKey();
            EngineStyle engineStyle = entry.getValue();
            if (engineStyle != null) {
                engineStyle.setEngineStyleID(engineStyleID);
                engineStyle.setFilePath(styleFile.toPath());
            }
        }

        return engineStyles;
    }
}
