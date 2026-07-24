package shipeditor.persistence.database;

import lombok.extern.log4j.Log4j2;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles all SQLite queries, indexing operations, and lookups.
 * Provides both synchronous database execution and asynchronous futures.
 *
 * @author Shadow
 */
@Log4j2
public final class DatabaseQueryService {

    public record FileInfo(long lastModified, String fileHash) {}

    private DatabaseQueryService() {}

    // --- Synchronous Modifications (Used by Background Scanner) ---

    public static void upsertMod(String id, String name, String folderPath, long lastScanned) {
        String sql = """
            INSERT INTO mods (id, name, folder_path, last_scanned)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                folder_path = excluded.folder_path,
                last_scanned = excluded.last_scanned;
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, name);
            pstmt.setString(3, folderPath);
            pstmt.setLong(4, lastScanned);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to upsert mod: {}", id, e);
        }
    }

    public static void deleteMod(String id) {
        String sql = "DELETE FROM mods WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            log.info("Deleted mod package from database: {}", id);
        } catch (SQLException e) {
            log.error("Failed to delete mod: {}", id, e);
        }
    }

    public static Map<String, Long> getScannedModsMap() {
        Map<String, Long> mods = new HashMap<>();
        String sql = "SELECT id, last_scanned FROM mods;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                mods.put(rs.getString("id"), rs.getLong("last_scanned"));
            }
        } catch (SQLException e) {
            log.error("Failed to query mods list", e);
        }
        return mods;
    }

    public static void upsertIndexedFile(IndexedFile file) {
        String sql = """
            INSERT INTO indexed_files (uuid, mod_id, entity_id, entity_name, entity_type, file_name, file_path, last_modified, parsed_data, file_hash)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                mod_id = excluded.mod_id,
                entity_id = excluded.entity_id,
                entity_name = excluded.entity_name,
                entity_type = excluded.entity_type,
                file_name = excluded.file_name,
                file_path = excluded.file_path,
                last_modified = excluded.last_modified,
                parsed_data = excluded.parsed_data,
                file_hash = excluded.file_hash;
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, file.getUuid().toString());
            pstmt.setString(2, file.getModId());
            pstmt.setString(3, file.getEntityId());
            pstmt.setString(4, file.getEntityName());
            pstmt.setString(5, file.getEntityType());
            pstmt.setString(6, file.getFileName());
            pstmt.setString(7, file.getFilePath().toAbsolutePath().toString());
            pstmt.setLong(8, file.getLastModified());
            pstmt.setString(9, file.getParsedData());
            pstmt.setString(10, file.getFileHash());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to upsert indexed file: {}", file.getFilePath(), e);
        }
    }

    public static void deleteIndexedFile(UUID uuid) {
        String sql = "DELETE FROM indexed_files WHERE uuid = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete indexed file: {}", uuid, e);
        }
    }

    /**
     * Purges database records for files that were deleted from disk.
     */
    public static void deleteOrphanedFiles(Connection conn, String modId, List<String> activePaths) {
        if (activePaths == null || activePaths.isEmpty()) {
            String sql = "DELETE FROM indexed_files WHERE mod_id = ?;";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, modId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                log.error("Failed to delete all files for mod: {}", modId, e);
            }
            return;
        }

        java.util.Set<String> activeSet = new java.util.HashSet<>(activePaths);
        List<String> toDelete = new ArrayList<>();

        String selectSql = "SELECT file_path FROM indexed_files WHERE mod_id = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, modId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String path = rs.getString("file_path");
                    if (!activeSet.contains(path)) {
                        toDelete.add(path);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query files for orphan cleanup: {}", modId, e);
            return;
        }

        if (!toDelete.isEmpty()) {
            String deleteSql = "DELETE FROM indexed_files WHERE mod_id = ? AND file_path = ?;";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                int count = 0;
                for (String path : toDelete) {
                    pstmt.setString(1, modId);
                    pstmt.setString(2, path);
                    pstmt.addBatch();
                    count++;
                    if (count % 500 == 0) {
                        pstmt.executeBatch();
                    }
                }
                pstmt.executeBatch();
            } catch (SQLException e) {
                log.error("Failed to delete orphaned files for mod: {}", modId, e);
            }
        }
    }

    // --- Synchronous Lookups (Used by GameDataRepository) ---

    public static String getFileNameForEntity(String entityId, String type) {
        String sql = "SELECT file_name FROM indexed_files WHERE entity_id = ? AND entity_type = ? LIMIT 1;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entityId);
            pstmt.setString(2, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("file_name");
                }
            }
        } catch (SQLException e) {
            log.error("Failed to lookup filename for entity: {} ({})", entityId, type, e);
        }
        return "";
    }

    public static Path getFilePathForEntity(String entityId, String type) {
        String sql = "SELECT file_path FROM indexed_files WHERE entity_id = ? AND entity_type = ? LIMIT 1;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, entityId);
            pstmt.setString(2, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Path.of(rs.getString("file_path"));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to lookup path for entity: {} ({})", entityId, type, e);
        }
        return null;
    }

    public static IndexedFile getFileByPath(String path) {
        String sql = "SELECT * FROM indexed_files WHERE file_path = ? LIMIT 1;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, path);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToIndexedFile(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to lookup file by path: {}", path, e);
        }
        return null;
    }

    public static Map<String, FileInfo> getFilesInfoMap(Connection conn, String modId) {
        Map<String, FileInfo> fileMap = new HashMap<>();
        String sql = "SELECT file_path, last_modified, file_hash FROM indexed_files WHERE mod_id = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, modId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    fileMap.put(rs.getString("file_path"), new FileInfo(rs.getLong("last_modified"), rs.getString("file_hash")));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query files map for mod: {}", modId, e);
        }
        return fileMap;
    }

    public static Map<String, UUID> getFilesUuidMap(Connection conn, String modId) {
        Map<String, UUID> uuidMap = new HashMap<>();
        String sql = "SELECT file_path, uuid FROM indexed_files WHERE mod_id = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, modId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    uuidMap.put(rs.getString("file_path"), UUID.fromString(rs.getString("uuid")));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query UUIDs map for mod: {}", modId, e);
        }
        return uuidMap;
    }

    // --- Synchronous Lookups (Used by loading actions) ---

    /**
     * Returns all indexed files of a given type across all mods.
     */
    public static List<IndexedFile> getFilesByType(String type) {
        List<IndexedFile> results = new ArrayList<>();
        String sql = "SELECT * FROM indexed_files WHERE entity_type = ? ORDER BY mod_id ASC, entity_id ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToIndexedFile(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query files by type: {}", type, e);
        }
        return results;
    }

    /**
     * Returns all indexed files of a given type, grouped by mod_id. Useful for per-package loading.
     */
    public static Map<String, List<IndexedFile>> getFilesByTypeGroupedByMod(String type) {
        Map<String, List<IndexedFile>> grouped = new LinkedHashMap<>();
        for (IndexedFile file : getFilesByType(type)) {
            grouped.computeIfAbsent(file.getModId(), k -> new ArrayList<>()).add(file);
        }
        return grouped;
    }

    // --- Asynchronous Lookups (Used by UI for instant rendering) ---

    public static CompletableFuture<List<IndexedFile>> getFilesByTypeAsync(String type) {
        return CompletableFuture.supplyAsync(() -> {
            List<IndexedFile> results = new ArrayList<>();
            String sql = "SELECT * FROM indexed_files WHERE entity_type = ? ORDER BY entity_id ASC;";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, type);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapRowToIndexedFile(rs));
                    }
                }
            } catch (SQLException e) {
                log.error("Failed to query files by type: {}", type, e);
            }
            return results;
        });
    }

    public static CompletableFuture<List<IndexedFile>> getFilesByModAndTypeAsync(String modId, String type) {
        return CompletableFuture.supplyAsync(() -> {
            List<IndexedFile> results = new ArrayList<>();
            String sql = "SELECT * FROM indexed_files WHERE mod_id = ? AND entity_type = ? ORDER BY entity_id ASC;";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, modId);
                pstmt.setString(2, type);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapRowToIndexedFile(rs));
                    }
                }
            } catch (SQLException e) {
                log.error("Failed to query files by mod and type: {} ({})", modId, type, e);
            }
            return results;
        });
    }

    // --- Helper Methods ---

    private static IndexedFile mapRowToIndexedFile(ResultSet rs) throws SQLException {
        return IndexedFile.builder()
                .uuid(UUID.fromString(rs.getString("uuid")))
                .modId(rs.getString("mod_id"))
                .entityId(rs.getString("entity_id"))
                .entityName(rs.getString("entity_name"))
                .entityType(rs.getString("entity_type"))
                .fileName(rs.getString("file_name"))
                .filePath(Path.of(rs.getString("file_path")))
                .lastModified(rs.getLong("last_modified"))
                .parsedData(rs.getString("parsed_data"))
                .fileHash(rs.getString("file_hash"))
                .build();
    }

}
