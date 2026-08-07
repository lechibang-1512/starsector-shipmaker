package shipeditor.representation;

import shipeditor.components.datafiles.entities.CSVEntry;
import java.nio.file.Path;
import java.util.Map;

@FunctionalInterface
public interface CsvEntryFactory<T extends CSVEntry> {
    T create(Map<String, String> row, Path folderPath, Path dataFilePath);
}
