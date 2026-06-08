package oth.shipeditor.persistence.database;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.persistence.SettingsManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the SQLite database connection, file creation, and schema initialization.
 *
 * @author Shadow
 */
@Log4j2
public final class DatabaseManager {

    private static Path databaseFilePath;

    private DatabaseManager() {}

    /**
     * Gets the path to the SQLite database file, located in the same directory as the settings file.
     */
    public static synchronized Path getDatabaseFilePath() {
        if (databaseFilePath == null) {
            Path settingsPath = SettingsManager.getSettingsPath().toPath();
            Path settingsDir = settingsPath.getParent();
            if (settingsDir == null) {
                settingsDir = Path.of("");
            }
            databaseFilePath = settingsDir.resolve("ship_editor_database.sqlite");
        }
        return databaseFilePath;
    }

    /**
     * Checks if the SQLite database file already exists.
     */
    public static boolean databaseExists() {
        return Files.exists(getDatabaseFilePath());
    }

    /**
     * Opens and returns a new connection to the SQLite database.
     */
    public static Connection getConnection() throws SQLException {
        // Ensure JDBC class is loaded
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            log.error("SQLite JDBC Driver not found in classpath!", e);
            throw new SQLException("SQLite JDBC Driver not found", e);
        }

        String dbUrl = "jdbc:sqlite:" + getDatabaseFilePath().toAbsolutePath().toString().replace("\\", "/") + "?busy_timeout=5000";
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
            stmt.execute("PRAGMA cache_size = -64000;");
            stmt.execute("PRAGMA temp_store = MEMORY;");
        }
        return conn;
    }

    /**
     * Initializes the database tables and indexes if they do not exist.
     */
    public static void initializeDatabase() {
        log.info("Initializing SQLite database at: {}", getDatabaseFilePath().toAbsolutePath());

        try {
            Path parentDir = getDatabaseFilePath().getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
        } catch (IOException e) {
            log.error("Failed to create database directory: {}", getDatabaseFilePath().getParent(), e);
        }

        String createModsTable = """
            CREATE TABLE IF NOT EXISTS mods (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                folder_path TEXT NOT NULL,
                last_scanned INTEGER NOT NULL
            );
            """;

        String createFilesTable = """
            CREATE TABLE IF NOT EXISTS indexed_files (
                uuid TEXT PRIMARY KEY,
                mod_id TEXT,
                entity_id TEXT,
                entity_name TEXT,
                entity_type TEXT NOT NULL,
                file_name TEXT NOT NULL,
                file_path TEXT NOT NULL,
                last_modified INTEGER NOT NULL,
                FOREIGN KEY(mod_id) REFERENCES mods(id) ON DELETE CASCADE
            );
            """;

        String createIndexEntityId = "CREATE INDEX IF NOT EXISTS idx_entity_id ON indexed_files(entity_id);";
        String createIndexEntityType = "CREATE INDEX IF NOT EXISTS idx_entity_type ON indexed_files(entity_type);";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute(createModsTable);
            stmt.execute(createFilesTable);
            stmt.execute(createIndexEntityId);
            stmt.execute(createIndexEntityType);

            log.info("Database tables and indexes verified/created successfully.");
        } catch (SQLException e) {
            log.error("Failed to initialize database tables", e);
        }
    }

}
