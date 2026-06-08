package oth.shipeditor.components.datafiles.entities;

import java.nio.file.Path;
import java.util.Map;

public interface CSVEntry {

    Map<String, String> getRowData();

    String getID();

    String getMultilineTooltip();

    Path getPackageFolderPath();

    Path getTableFilePath();

}
