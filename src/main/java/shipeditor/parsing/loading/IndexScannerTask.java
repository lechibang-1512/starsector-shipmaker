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

            long modFolderLastModified = getLastModifiedForFolder(folder);
            Long dbLastScanned = existingModsMap.get(modId);

            if (dbLastScanned == null || modFolderLastModified > dbLastScanned) {
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
            log.error("Settings not initialized, cancelling index scan.");
            return;
        }

        if (!isUpdateNeeded(firstRun, settings)) {
            log.info("Database index is up to date. Skipping index scan.");
            return;
        }

        // Prompt the user if changes are detected
        final boolean[] shouldUpdate = new boolean[1];
        if (java.awt.GraphicsEnvironment.isHeadless() || !promptUser) {
            shouldUpdate[0] = true;
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    String title = firstRun ? "Initial Database Index Setup" : "Mod Directory Changes Detected";
                    String message = firstRun
                            ? "No database index found. A complete initial scan is required to load game data. Click OK to scan and initialize."
                            : "Changes detected in mod folders. Would you like to update the database index now?\n" +
                                    "Updating ensures the data in the editor matches your files, but may take a few seconds.";

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
                log.error("Failed to show update confirmation dialog", e);
                shouldUpdate[0] = true; // Default to update on error
            }
        }

        if (!shouldUpdate[0]) {
            log.info("User declined database index update. Skipping scan.");
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
                log.info("Purging obsolete/deleted mod from database index: {}", dbModId);
                DatabaseQueryService.deleteMod(dbModId);
            }
        }

        long currentScanTime = System.currentTimeMillis();

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Enable Batch Mode
            log.info("Opened SQLite transaction for indexing...");

            try {
                for (Path folder : targetsToScan) {
                    Path fileNamePath = folder.getFileName();
                    if (fileNamePath == null) continue;
                    String modId = fileNamePath.toString();
                    if (SettingsManager.isCoreFolder(folder)) {
                        modId = "starsector-core";
                    }

                    // Check if mod was updated (lastModified checking)
                    long modFolderLastModified = getLastModifiedForFolder(folder);
                    Long dbLastScanned = existingModsMap.get(modId);

                    if (firstRun || dbLastScanned == null || modFolderLastModified > dbLastScanned) {
                        log.info("Scanning mod package: {}", modId);

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
                log.info("SQL transaction committed successfully. Mod index up to date.");
            } catch (SQLException e) {
                conn.rollback();
                log.error("Failed to commit indexing transaction, rolled back changes.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Failed to connect or maintain transaction for SQLite DB", e);
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
                "ship", "SHIP",
                "skin", "SKIN",
                "wpn", "WEAPON",
                "variant", "VARIANT",
                "proj", "PROJECTILE");

        Map<String, Long> dbFilesMap = DatabaseQueryService.getFilesLastModifiedMap(conn, modId);
        Map<String, UUID> dbUuidMap = DatabaseQueryService.getFilesUuidMap(conn, modId);

        String upsertFileSql = """
                INSERT INTO indexed_files (uuid, mod_id, entity_id, entity_name, entity_type, file_name, file_path, last_modified)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    mod_id = excluded.mod_id,
                    entity_id = excluded.entity_id,
                    entity_name = excluded.entity_name,
                    entity_type = excluded.entity_type,
                    file_name = excluded.file_name,
                    file_path = excluded.file_path,
                    last_modified = excluded.last_modified;
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

                        pstmt.setString(1, uuid.toString());
                        pstmt.setString(2, modId);
                        pstmt.setString(3, entityId != null ? entityId : entityName);
                        pstmt.setString(4, entityName);
                        pstmt.setString(5, type);
                        pstmt.setString(6, file.getName());
                        pstmt.setString(7, absPath);
                        pstmt.setLong(8, diskLastModified);
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
        fileTargets.put(Paths.get("data", StringConstants.HULLS, "ship_data.csv"), "SHIP_CSV");
        fileTargets.put(Paths.get("data", "weapons", "weapon_data.csv"), "WEAPON_CSV");
        fileTargets.put(Paths.get("data", "hullmods", "hull_mods.csv"), "HULLMOD_CSV");
        fileTargets.put(Paths.get("data", "shipsystems", "ship_systems.csv"), "SHIPSYSTEM_CSV");
        fileTargets.put(Paths.get("data", StringConstants.HULLS, "wing_data.csv"), "WING_CSV");
        // Style JSON config files
        fileTargets.put(Paths.get("data", StringConstants.CONFIG, "engine_styles.json"), "ENGINE_STYLE_JSON");
        fileTargets.put(Paths.get("data", StringConstants.CONFIG, "hull_styles.json"), "HULL_STYLE_JSON");

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

                    pstmt.setString(1, uuid.toString());
                    pstmt.setString(2, modId);
                    pstmt.setString(3, type.toLowerCase());
                    pstmt.setString(4, file.getName().replaceAll("\\.(csv|json)$", ""));
                    pstmt.setString(5, type);
                    pstmt.setString(6, file.getName());
                    pstmt.setString(7, absPath);
                    pstmt.setLong(8, diskLastModified);
                    pstmt.addBatch();
                }
            }
        }
    }

    private static String extractEntityId(File file, String type, ObjectMapper mapper) {
        String keyToFind;
        switch (type) {
            case "SHIP":
                keyToFind = "hullId";
                break;
            case "SKIN":
                keyToFind = "skinHullId";
                break;
            case "VARIANT":
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
            log.warn("Streaming JSON parse failed for id extraction: {}", file.getName(), e);
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
            log.error("Failed to walk files for indexing: {}", target, exception);
        }
        return filesMap;
    }

    private static long getLastModifiedForFolder(Path folder) {
        Path dataPath = folder.resolve("data");
        Path walkTarget = Files.exists(dataPath) && Files.isDirectory(dataPath) ? dataPath : folder;
        try (Stream<Path> stream = Files.walk(walkTarget)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(path -> path.toFile().lastModified())
                    .max()
                    .orElse(0L);
        } catch (IOException e) {
            return folder.toFile().lastModified();
        }
    }

}
