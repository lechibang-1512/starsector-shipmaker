package oth.shipeditor.parsing.loading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.files.HullStylesLoaded;
import oth.shipeditor.parsing.FileUtilities;
import oth.shipeditor.persistence.GameDataPackage;
import oth.shipeditor.persistence.Settings;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.persistence.database.DatabaseQueryService;
import oth.shipeditor.persistence.database.IndexedFile;
import oth.shipeditor.representation.GameDataRepository;
import oth.shipeditor.representation.ship.HullStyle;

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
        List<IndexedFile> dbFiles = DatabaseQueryService.getFilesByType("HULL_STYLE_JSON");

        Map<String, HullStyle> collectedHullStyles = new LinkedHashMap<>();
        for (IndexedFile dbFile : dbFiles) {
            Path folderPath = SettingsManager.getFolderForModId(dbFile.getModId());
            if (folderPath == null) {
                log.warn("No folder found for mod_id '{}', skipping hull styles", dbFile.getModId());
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
            log.trace("Hullstyle data file found in mod directory: {}", folderPath);
            Map<String, HullStyle> stylesFromFile = loadHullStyleFile(styleFile);
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
        return "Hull Styles";
    }

    private static Map<String, HullStyle> loadHullStyleFile(File styleFile) {
        ObjectMapper mapper = FileUtilities.getConfigured();
        Map<String, HullStyle> hullStyles = null;
        log.trace("Fetching hullstyle data at: {}..", styleFile.toPath());
        com.fasterxml.jackson.databind.type.MapType mapType = null;
        try {
            TypeFactory typeFactory = mapper.getTypeFactory();
            mapType = typeFactory.constructMapType(HashMap.class, String.class, HullStyle.class);
            hullStyles = mapper.readValue(styleFile, mapType);
        } catch (IOException e) {
            log.trace("Hull styles file loading failed, retrying with correction: {}", styleFile.getName());
            hullStyles = FileLoading.parseCorrectableJSON(styleFile, mapType);
        }

        if (hullStyles == null) {
            log.error("Hull styles file loading failed conclusively: {}", styleFile.getName());
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
