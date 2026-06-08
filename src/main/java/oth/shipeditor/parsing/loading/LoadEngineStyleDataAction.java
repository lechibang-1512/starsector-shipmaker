package oth.shipeditor.parsing.loading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.files.EngineStylesLoaded;
import oth.shipeditor.parsing.FileUtilities;
import oth.shipeditor.persistence.GameDataPackage;
import oth.shipeditor.persistence.Settings;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.persistence.database.DatabaseQueryService;
import oth.shipeditor.persistence.database.IndexedFile;
import oth.shipeditor.representation.ship.EngineStyle;
import oth.shipeditor.representation.GameDataRepository;

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
        List<IndexedFile> dbFiles = DatabaseQueryService.getFilesByType("ENGINE_STYLE_JSON");

        Map<String, EngineStyle> collectedEngineStyles = new LinkedHashMap<>();
        for (IndexedFile dbFile : dbFiles) {
            Path folderPath = SettingsManager.getFolderForModId(dbFile.getModId());
            if (folderPath == null) {
                log.warn("No folder found for mod_id '{}', skipping engine styles", dbFile.getModId());
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

            File styleFile = dbFile.getFilePath().toFile();
            log.trace("Engine style data file found in mod directory: {}", folderPath);
            Map<String, EngineStyle> stylesFromFile = loadEngineStyleFile(styleFile);
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
        return "Engine Styles";
    }

    private static Map<String, EngineStyle> loadEngineStyleFile(File styleFile) {
        ObjectMapper mapper = FileUtilities.getConfigured();
        Map<String, EngineStyle> engineStyles;
        log.trace("Fetching engine style data at: {}..", styleFile.toPath());
        MapType mapType = null;
        try {
            TypeFactory typeFactory = mapper.getTypeFactory();
            mapType = typeFactory.constructMapType(HashMap.class, String.class, EngineStyle.class);
            engineStyles = mapper.readValue(styleFile, mapType);
        } catch (IOException e) {
            log.trace("Engine styles file loading failed, retrying with correction: {}", styleFile.getName());
            engineStyles = FileLoading.parseCorrectableJSON(styleFile, mapType);
        }

        if (engineStyles == null) {
            log.error("Engine styles file loading failed conclusively: {}", styleFile.getName());
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
