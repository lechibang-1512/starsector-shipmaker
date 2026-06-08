package oth.shipeditor.persistence.database;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Represents a lightweight record of an indexed file in the SQLite database.
 * Used for listing and lazy-loading in the UI.
 *
 * @author Shadow
 */
@Getter
@Setter
@Builder
@ToString
public class IndexedFile {

    private final UUID uuid;

    private final String modId;

    private final String entityId;

    private final String entityName;

    private final String entityType;

    private final String fileName;

    private final Path filePath;

    private final long lastModified;

}
