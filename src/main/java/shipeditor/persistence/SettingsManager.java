package shipeditor.persistence;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import shipeditor.parsing.FileUtilities;
import shipeditor.representation.GameDataRepository;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public final class SettingsManager {

    @Getter
    private static Settings settings;

    public static void setSettings(Settings settings) {
        SettingsManager.settings = settings;
        clearModFoldersCache();
    }

    private static List<Path> cachedModFolders;

    public static synchronized void clearModFoldersCache() {
        cachedModFolders = null;
    }

    @Getter
    private static final GameDataRepository gameData = new GameDataRepository();

    @Getter
    private static Path applicationDirectory;

    @Getter
    private static String coreFolderName;

    private static Path settingsFilePath;

    @Getter
    private static final String projectVersion = "0.0.1e-hotfix";

    private static GameDataPackage corePackage;

    private SettingsManager() {
    }

    static Settings createDefault() {
        Settings empty = new Settings();
        empty.setBackgroundColor(null);
        return empty;
    }

    static void setCoreFolderName(String folderName) {
        SettingsManager.coreFolderName = folderName;
        if (corePackage != null) {
            corePackage.setFolderName(folderName);
        }
    }

    static ObjectMapper getMapperForSettingsFile() {
        return FileUtilities.getConfigured();
    }

    /**
     * @return all directories in "mods" folder.
     *         Caller is expected to do the filtering.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static synchronized List<Path> getAllModFolders() {
        if (cachedModFolders != null) {
            return new ArrayList<>(cachedModFolders);
        }
        if (settings == null || settings.getModFolderPath() == null) {
            return new ArrayList<>();
        }
        List<Path> dataFolders = new ArrayList<>();
        Path modFolder = Paths.get(settings.getModFolderPath());
        if (Files.exists(modFolder) && Files.isDirectory(modFolder)) {
            try (Stream<Path> childDirectories = Files.list(modFolder)) {
                childDirectories.filter(Files::isDirectory).forEach(dataFolders::add);
            } catch (IOException exception) {
                log.error("Failed to list mod folders in: {}", modFolder, exception);
            }
        }
        cachedModFolders = dataFolders;
        return new ArrayList<>(dataFolders);
    }

    public static Path getCoreFolderPath() {
        return Path.of(settings.getCoreFolderPath());
    }

    public static boolean areFileErrorPopupsEnabled() {
        return settings != null && settings.showLoadingErrors;
    }

    public static boolean isDeveloperModeEnabled() {
        return settings != null && settings.developerMode;
    }

    public static boolean isNumericSuffixesForSlotsEnabled() {
        return settings.numericSuffixesForSlots;
    }

    public static boolean isDataAutoloadEnabled() {
        return settings.loadDataAtStart;
    }

    public static synchronized File getSettingsPath() {
        if (settingsFilePath != null) {
            return settingsFilePath.toFile();
        } else {
            Path workingDirectory = Paths.get("").toAbsolutePath();
            Path settingsPath = workingDirectory.resolve("ship_editor_settings.json");

            applicationDirectory = settingsPath.getParent();
            settingsFilePath = settingsPath;

            return settingsPath.toFile();
        }
    }

    public static void updateFileFromRuntime() {
        if (SettingsManager.settings == null)
            return;
        log.trace("Updating settings: getting path and mapper...");
        ObjectMapper mapper = SettingsManager.getMapperForSettingsFile();
        File settingsFile = SettingsManager.getSettingsPath();
        log.info("Updating settings: overwriting JSON file...");
        SettingsManager.writeSettingsToFile(mapper, settingsFile, SettingsManager.settings);
    }

    static void writeSettingsToFile(ObjectMapper mapper, File settingsFile, Settings writable) {
        try {
            mapper.writeValue(settingsFile, writable);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write settings file!", e);
        }
    }

    public static boolean isCoreFolder(GameDataPackage dataPackage) {
        String packageFolderName = dataPackage.getFolderName();
        return SettingsManager.isCoreFolder(packageFolderName);
    }

    public static boolean isCoreFolder(Path directory) {
        Path fileNamePath = directory.getFileName();
        if (fileNamePath == null) return false;
        return SettingsManager.isCoreFolder(fileNamePath.toString());
    }

    public static boolean isCoreFolder(String folderName) {
        return folderName.equals(SettingsManager.getCoreFolderName());
    }

    /**
     * Resolves a database mod_id (folder name or "starsector-core") back to the
     * full folder Path.
     * Returns null if no matching folder is found.
     */
    public static Path getFolderForModId(String modId) {
        Path corePath = getCoreFolderPath();
        // "starsector-core" is a fixed alias used during indexing
        Path coreFileName = corePath.getFileName();
        if ("starsector-core".equals(modId) || isCoreFolder(corePath)) {
            if ((coreFileName != null && coreFileName.toString().equals(modId)) || "starsector-core".equals(modId)) {
                return corePath;
            }
        }

        // Search in all mod folders
        for (Path modFolder : getAllModFolders()) {
            Path modFileName = modFolder.getFileName();
            if (modFileName != null && modFileName.toString().equals(modId)) {
                return modFolder;
            }
        }
        return null;
    }

    public static synchronized GameDataPackage getCorePackage() {
        String corePackageName = SettingsManager.getCoreFolderName();
        if (corePackage == null) {
            corePackage = new GameDataPackage(corePackageName, false, false);
        }
        return corePackage;
    }

    public static <T> void announcePackages(Map<Path, List<T>> packages) {
        if (packages == null) {
            return;
        }
        for (Map.Entry<Path, List<T>> entry : packages.entrySet()) {
            Path path = entry.getKey();
            if (!SettingsManager.isCoreFolder(path)) {
                settings.addDataPackage(path);
            }
        }
    }

}
