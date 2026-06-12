package shipeditor.parsing.loading;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import shipeditor.parsing.FileUtilities;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.persistence.database.DatabaseManager;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;
import shipeditor.PrimaryWindow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Handles the background scanning, indexing, and synchronization of Starsector
 * core and mod files.
 * Performs a deep scan on the first run, and a fast differential timestamp
 * check on subsequent runs.
 *
 * @author Shadow
 */
@Log4j2
public final class IndexScannerTask {

    private IndexScannerTask() {
    }

    /**
     * Checks if the database is missing or if any active mod folder has changed
     * since the last scan.
     */
    private static boolean isUpdateNeeded(boolean firstRun, Settings settings) {
        if (firstRun) {
            return true;
        }

        List<Path> targetsToScan = new ArrayList<>();

        // Add Core game folder
        Path corePath = SettingsManager.getCoreFolderPath();
        if (Files.exists(corePath)) {
            targetsToScan.add(corePath);
        }

        // Add active Mod folders
        List<Path> modFolders = SettingsManager.getAllModFolders();
        for (Path modFolder : modFolders) {
            GameDataPackage pkg = settings.getPackage(modFolder);
            if (pkg != null && pkg.isDisabled()) {
                continue;
            }
            if (LibModFilter.isLibMod(modFolder)) {
                continue;
            }
            targetsToScan.add(modFolder);
        }

        Map<String, Long> existingModsMap = DatabaseQueryService.getScannedModsMap();
        java.util.Set<String> activeModIds = new java.util.HashSet<>();

        for (Path folder : targetsToScan) {
            Path fileNamePath = folder.getFileName();
            if (fileNamePath == null) continue;
            String modId = fileNamePath.toString();
            if (SettingsManager.isCoreFolder(folder)) {
                modId = "starsector-core";
            }
            activeModIds.add(modId);

            Long dbLastScanned = existingModsMap.get(modId);

            if (dbLastScanned == null) {
                return true;
            }
        }

        // If the database contains extra mods that are no longer part of active targets, an update is needed to purge them
        for (String dbModId : existingModsMap.keySet()) {
            if (!activeModIds.contains(dbModId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Executes the scanning process. To be run asynchronously during startup.
     */
    public static void scanAndIndexAll() {
        scanAndIndexAll(true);
    }

    /**
     * Executes the scanning process. To be run asynchronously during startup.
     */
    public static void scanAndIndexAll(boolean promptUser) {
        boolean firstRun = !DatabaseManager.isDatabaseValid() || DatabaseManager.isDatabaseEmpty();
        log.info("Checking if background mod indexing is needed. First run: {}", firstRun);

        // Initialize the schema and tables if they don't exist
        DatabaseManager.initializeDatabase();

        Settings settings = SettingsManager.getSettings();
        if (settings == null) {
            log.error(StringValues.SETTINGS_NOT_INITIALIZED_INDEX_CANCEL);
            return;
        }

        if (!isUpdateNeeded(firstRun, settings)) {
            log.info(StringValues.DB_INDEX_UP_TO_DATE_SKIP);
            return;
        }

        // Prompt the user if changes are detected
        final boolean[] shouldUpdate = new boolean[1];
        if (java.awt.GraphicsEnvironment.isHeadless() || !promptUser) {
            shouldUpdate[0] = true;
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    String title = firstRun ? StringValues.INITIAL_DB_INDEX_TITLE : StringValues.MOD_DIR_CHANGES_TITLE;
                    String message = firstRun
                            ? StringValues.INITIAL_DB_INDEX_MSG
                            : StringValues.MOD_DIR_CHANGES_MSG;

                    JOptionPane pane;
                    if (firstRun) {
                        pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION);
                    } else {
                        pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
                    }

                    JDialog dialog = pane.createDialog(PrimaryWindow.getInstance(), title);
                    dialog.setAlwaysOnTop(true);
                    dialog.setVisible(true);
                    dialog.dispose();

                    Object selectedValue = pane.getValue();
                    if (firstRun) {
                        shouldUpdate[0] = true;
                    } else {
                        if (selectedValue instanceof Integer) {
                            int option = (Integer) selectedValue;
                            shouldUpdate[0] = (option == JOptionPane.YES_OPTION);
                        } else {
                            shouldUpdate[0] = false;
                        }
                    }
                });
            } catch (java.lang.reflect.InvocationTargetException | InterruptedException e) {
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.error(StringValues.FAILED_SHOW_UPDATE_DIALOG, e);
                } else {
                    log.error(StringValues.FAILED_SHOW_UPDATE_DIALOG);
                }
                shouldUpdate[0] = true; // Default to update on error
            }
        }

        if (!shouldUpdate[0]) {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.info(StringValues.USER_DECLINED_INDEX_UPDATE);
            }
            return;
        }

        List<Path> targetsToScan = new ArrayList<>();

        // Add Core game folder
        Path corePath = SettingsManager.getCoreFolderPath();
        if (Files.exists(corePath)) {
            targetsToScan.add(corePath);
        }

        // Add active Mod folders
        List<Path> modFolders = SettingsManager.getAllModFolders();
        for (Path modFolder : modFolders) {
            GameDataPackage pkg = settings.getPackage(modFolder);
            if (pkg != null && pkg.isDisabled()) {
                continue;
            }
            if (LibModFilter.isLibMod(modFolder)) {
                continue;
            }
            targetsToScan.add(modFolder);
        }

        Map<String, Long> existingModsMap = DatabaseQueryService.getScannedModsMap();

        // Purge obsolete/deleted mod packages from the database index
        java.util.Set<String> activeModIds = new java.util.HashSet<>();
        for (Path folder : targetsToScan) {
            Path fileNamePath = folder.getFileName();
            if (fileNamePath == null) continue;
            String modId = fileNamePath.toString();
            if (SettingsManager.isCoreFolder(folder)) {
                modId = "starsector-core";
            }
            activeModIds.add(modId);
        }
        for (String dbModId : existingModsMap.keySet()) {
            if (!activeModIds.contains(dbModId)) {
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.info("Purging obsolete/deleted mod from database index: {}", dbModId);
                }
                DatabaseQueryService.deleteMod(dbModId);
            }
        }

        long currentScanTime = System.currentTimeMillis();

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Enable Batch Mode
            log.info(StringValues.SQLITE_TRANSACTION_OPEN);

            try {
                for (Path folder : targetsToScan) {
                    Path fileNamePath = folder.getFileName();
                    if (fileNamePath == null) continue;
                    String modId = fileNamePath.toString();
                    if (SettingsManager.isCoreFolder(folder)) {
                        modId = "starsector-core";
                    }

                    Long dbLastScanned = existingModsMap.get(modId);

                    if (firstRun || dbLastScanned == null) {
                        if (SettingsManager.isDeveloperModeEnabled()) {
                            log.info("Scanning mod package: {}", modId);
                        }

                        // Upsert mod metadata row
                        String upsertModSql = """
                                INSERT INTO mods (id, name, folder_path, last_scanned)
                                VALUES (?, ?, ?, ?)
                                ON CONFLICT(id) DO UPDATE SET
                                    name = excluded.name,
                                    folder_path = excluded.folder_path,
                                    last_scanned = excluded.last_scanned;
                                """;
                        try (PreparedStatement pstmt = conn.prepareStatement(upsertModSql)) {
                            pstmt.setString(1, modId);
                            pstmt.setString(2, modId.equals("starsector-core") ? "Starsector Core" : modId);
                            pstmt.setString(3, folder.toAbsolutePath().toString());
                            pstmt.setLong(4, currentScanTime);
                            pstmt.executeUpdate();
                        }

                        scanModFolder(conn, folder, modId);
                    } else {
                        log.debug("Mod package is up to date, skipping scan: {}", modId);
                    }
                }

                conn.commit();
                log.info(StringValues.SQLITE_TRANSACTION_COMMITTED);
            } catch (SQLException e) {
                conn.rollback();
                log.error(StringValues.SQLITE_TRANSACTION_ROLLBACK_FAILED, e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error(StringValues.SQLITE_CONNECTION_ERROR, e);
        }
    }

    /**
     * Scans a single mod directory recursively for ship, weapon, variant, skin, and
     * csv files.
     */
    private static void scanModFolder(Connection conn, Path modFolder, String modId) throws SQLException {
        List<String> activePaths = new ArrayList<>();
        ObjectMapper mapper = FileUtilities.getConfigured();

        // 1. Scan and index files with specific extensions (.ship, .skin, .wpn,
        // .variant, .proj)
        Map<String, String> extensions = Map.of(
                "ship", StringConstants.SHIP_TYPE,
                "skin", StringConstants.SKIN_TYPE,
                "wpn", StringConstants.WEAPON_TYPE,
                "variant", StringConstants.VARIANT_TYPE,
                "proj", StringConstants.PROJECTILE_TYPE);

        Map<String, Long> dbFilesMap = DatabaseQueryService.getFilesLastModifiedMap(conn, modId);
        Map<String, UUID> dbUuidMap = DatabaseQueryService.getFilesUuidMap(conn, modId);

        String upsertFileSql = """
                INSERT INTO indexed_files (uuid, mod_id, entity_id, entity_name, entity_type, file_name, file_path, last_modified, parsed_data)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    mod_id = excluded.mod_id,
                    entity_id = excluded.entity_id,
                    entity_name = excluded.entity_name,
                    entity_type = excluded.entity_type,
                    file_name = excluded.file_name,
                    file_path = excluded.file_path,
                    last_modified = excluded.last_modified,
                    parsed_data = excluded.parsed_data;
                """;

        Map<String, List<File>> allFiles = fetchFilesWithExtensions(modFolder, extensions.keySet());

        try (PreparedStatement pstmt = conn.prepareStatement(upsertFileSql)) {
            int batchCount = 0;
            for (Map.Entry<String, String> entry : extensions.entrySet()) {
                String ext = entry.getKey();
                String type = entry.getValue();

                List<File> files = allFiles.getOrDefault(ext, Collections.emptyList());
                for (File file : files) {
                    String absPath = file.getAbsolutePath();
                    activePaths.add(absPath);

                    long diskLastModified = file.lastModified();
                    Long dbLastModified = dbFilesMap.get(absPath);

                    // Check if file is new or modified
                    if (dbLastModified == null || diskLastModified > dbLastModified) {
                        String entityId = extractEntityId(file, type, mapper);
                        String entityName = file.getName().replace("." + ext, "");

                        // Lookup or generate UUID
                        UUID uuid = dbUuidMap.get(absPath);
                        if (uuid == null) {
                            uuid = UUID.randomUUID();
                        }

                        String parsedDataJson = null;
                        try {
                            Object parsedObj = null;
                            switch (type) {
                                case StringConstants.SHIP_TYPE:
                                    parsedObj = FileLoading.loadHullFile(file);
                                    break;
                                case StringConstants.SKIN_TYPE:
                                    parsedObj = FileLoading.loadSkinFile(file);
                                    break;
                                case StringConstants.WEAPON_TYPE:
                                    parsedObj = FileLoading.loadWeaponFile(file);
                                    break;
                                case StringConstants.VARIANT_TYPE:
                                    parsedObj = FileLoading.loadVariantFile(file);
                                    break;
                                case StringConstants.PROJECTILE_TYPE:
                                    parsedObj = FileLoading.loadProjectileFile(file);
                                    break;
                                default:
                                    log.warn("Unknown entity type: {}", type);
                                    break;
                            }
                            if (parsedObj != null) {
                                parsedDataJson = mapper.writeValueAsString(parsedObj);
                            }
                        } catch (Exception e) {
                            if (SettingsManager.isDeveloperModeEnabled()) {
                                log.error(StringValues.FAILED_PARSE_SERIALIZE_ENTITY, file.getName(), type, e);
                            } else {
                                log.error(StringValues.FAILED_PARSE_SERIALIZE_ENTITY, file.getName(), type);
                            }
                        }

                        pstmt.setString(1, uuid.toString());
                        pstmt.setString(2, modId);
                        pstmt.setString(3, entityId != null ? entityId : entityName);
                        pstmt.setString(4, entityName);
                        pstmt.setString(5, type);
                        pstmt.setString(6, file.getName());
                        pstmt.setString(7, absPath);
                        pstmt.setLong(8, diskLastModified);
                        pstmt.setString(9, parsedDataJson);
                        pstmt.addBatch();
                        batchCount++;
                        if (batchCount % 500 == 0) {
                            pstmt.executeBatch();
                        }
                    }
                }
            }

            // 2. Scan and index relevant data CSVs (hull_mods, ship_systems, wing_data)
            scanCSVs(modFolder, modId, pstmt, activePaths, dbFilesMap, dbUuidMap);

            // Execute batched inserts
            pstmt.executeBatch();
        }

        // Clean up any database references for files that were deleted from disk
        DatabaseQueryService.deleteOrphanedFiles(conn, modId, activePaths);
    }

    private static void scanCSVs(Path modFolder, String modId, PreparedStatement pstmt,
            List<String> activePaths, Map<String, Long> dbFilesMap,
            Map<String, UUID> dbUuidMap) throws SQLException {

        // CSV data files
        Map<Path, String> fileTargets = new LinkedHashMap<>();
        fileTargets.put(Paths.get("data", StringConstants.HULLS, "ship_data.csv"), StringConstants.SHIP_CSV_TYPE);
        fileTargets.put(Paths.get("data", "weapons", "weapon_data.csv"), StringConstants.WEAPON_CSV_TYPE);
        fileTargets.put(Paths.get("data", "hullmods", "hull_mods.csv"), StringConstants.HULLMOD_CSV_TYPE);
        fileTargets.put(Paths.get("data", "shipsystems", "ship_systems.csv"), StringConstants.SHIPSYSTEM_CSV_TYPE);
        fileTargets.put(Paths.get("data", StringConstants.HULLS, "wing_data.csv"), StringConstants.WING_CSV_TYPE);
        // Style JSON config files
        fileTargets.put(Paths.get("data", StringConstants.CONFIG, "engine_styles.json"), StringConstants.ENGINE_STYLE_JSON_TYPE);
        fileTargets.put(Paths.get("data", StringConstants.CONFIG, "hull_styles.json"), StringConstants.HULL_STYLE_JSON_TYPE);

        for (Map.Entry<Path, String> entry : fileTargets.entrySet()) {
            Path relativePath = entry.getKey();
            String type = entry.getValue();

            Path fullPath = modFolder.resolve(relativePath);
            if (Files.exists(fullPath) && Files.isRegularFile(fullPath)) {
                String absPath = fullPath.toAbsolutePath().toString();
                activePaths.add(absPath);

                File file = fullPath.toFile();
                long diskLastModified = file.lastModified();
                Long dbLastModified = dbFilesMap.get(absPath);

                if (dbLastModified == null || diskLastModified > dbLastModified) {
                    UUID uuid = dbUuidMap.get(absPath);
                    if (uuid == null) {
                        uuid = UUID.randomUUID();
                    }

                    String parsedDataJson = null;
                    try {
                        Object parsedObj = null;
                        if (type.endsWith("_CSV")) {
                            if (type.equals("WING_CSV")) {
                                parsedObj = FileLoading.parseCSVTable(fullPath, FileLoading.getWingValidationPredicate());
                            } else {
                                // For CSVs, parseCSVTable is package-private but we are in the same package.
                                parsedObj = FileLoading.parseCSVTable(fullPath);
                            }
                        } else if (type.endsWith("_JSON")) {
                            // Hull styles, Engine styles
                            ObjectMapper mapper = FileUtilities.getConfigured();
                            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(file);
                            parsedObj = node;
                        }
                        if (parsedObj != null) {
                            ObjectMapper mapper = FileUtilities.getConfigured();
                            parsedDataJson = mapper.writeValueAsString(parsedObj);
                        }
                    } catch (IOException e) {
                        if (SettingsManager.isDeveloperModeEnabled()) {
                            log.error(StringValues.FAILED_PARSE_SERIALIZE_CSV_JSON, file.getName(), type, e);
                        } else {
                            log.error(StringValues.FAILED_PARSE_SERIALIZE_CSV_JSON, file.getName(), type);
                        }
                    }

                    pstmt.setString(1, uuid.toString());
                    pstmt.setString(2, modId);
                    pstmt.setString(3, type.toLowerCase(java.util.Locale.ROOT));
                    pstmt.setString(4, file.getName().replaceAll("\\.(csv|json)$", ""));
                    pstmt.setString(5, type);
                    pstmt.setString(6, file.getName());
                    pstmt.setString(7, absPath);
                    pstmt.setLong(8, diskLastModified);
                    pstmt.setString(9, parsedDataJson);
                    pstmt.addBatch();
                }
            }
        }
    }

    private static String extractEntityId(File file, String type, ObjectMapper mapper) {
        String keyToFind;
        switch (type) {
            case StringConstants.SHIP_TYPE:
                keyToFind = "hullId";
                break;
            case StringConstants.SKIN_TYPE:
                keyToFind = "skinHullId";
                break;
            case StringConstants.VARIANT_TYPE:
                keyToFind = "variantId";
                break;
            default:
                keyToFind = "id";
                break;
        }

        try (java.io.InputStream in = java.nio.file.Files.newInputStream(file.toPath());
             com.fasterxml.jackson.core.JsonParser parser = mapper.getFactory().createParser(in)) {
            while (parser.nextToken() != null) {
                if (parser.currentToken() == com.fasterxml.jackson.core.JsonToken.FIELD_NAME) {
                    if (keyToFind.equals(parser.currentName())) {
                        parser.nextToken();
                        return parser.getText();
                    }
                }
            }
        } catch (IOException e) {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.warn("Streaming JSON parse failed for id extraction: {}", file.getName(), e);
            }
        }

        // Fallback: extract ID using filename matching
        return file.getName().replace(".ship", "")
                .replace(".skin", "")
                .replace(".wpn", "")
                .replace(".variant", "")
                .replace(".proj", "");
    }

    private static Map<String, List<File>> fetchFilesWithExtensions(Path target, Set<String> exts) {
        Map<String, List<File>> filesMap = new HashMap<>();
        for (String ext : exts) {
            filesMap.put(ext, new ArrayList<>());
        }

        Path dataPath = target.resolve("data");
        Path walkTarget = Files.exists(dataPath) && Files.isDirectory(dataPath) ? dataPath : target;
        try (Stream<Path> pathStream = Files.walk(walkTarget)) {
            pathStream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        Path fileNamePath = path.getFileName();
                        if (fileNamePath == null) return;
                        String name = fileNamePath.toString();
                        int dotIndex = name.lastIndexOf('.');
                        if (dotIndex > 0 && dotIndex < name.length() - 1) {
                            String fileExt = name.substring(dotIndex + 1);
                            if (exts.contains(fileExt)) {
                                filesMap.get(fileExt).add(path.toFile());
                            }
                        }
                    });
        } catch (IOException exception) {
            log.error(StringValues.FAILED_WALK_FILES_INDEXING, target, exception);
        }
        return filesMap;
    }


}
