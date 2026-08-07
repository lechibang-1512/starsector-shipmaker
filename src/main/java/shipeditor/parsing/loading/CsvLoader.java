package shipeditor.parsing.loading;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.extern.log4j.Log4j2;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.Errors;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Log4j2
public final class CsvLoader {

    private CsvLoader() {
    }

    public static List<Map<String, String>> parseCSVTable(Path path) {
        return parseCSVTable(path, getNormalValidationPredicate());
    }

    public static List<Map<String, String>> parseCSVTable(Path path, Predicate<Map<String, String>> validationPredicate) {
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true);

        CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();
        File csvFile = path.toFile();

        if (!csvFile.isFile()) {
            return null;
        }

        if (csvFile.length() == 0) {
            log.warn(StringValues.CSV_FILE_EMPTY, csvFile.getName());
            return null;
        }

        List<Map<String, String>> csvData = new ArrayList<>();
        try {
            csvData = readCSVWithCharset(csvFile, csvMapper, csvSchema, validationPredicate, StandardCharsets.ISO_8859_1);
        } catch (Throwable exception) {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.warn(StringValues.CSV_ISO_LOAD_FAILED, csvFile.getAbsolutePath(), exception);
            } else {
                log.warn(StringValues.CSV_ISO_LOAD_FAILED, csvFile.getAbsolutePath());
            }
            try {
                csvData = readCSVWithCharset(csvFile, csvMapper, csvSchema, validationPredicate, StandardCharsets.UTF_8);
            } catch (Throwable fallbackException) {
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.error(StringValues.CSV_FALLBACK_LOAD_FAILED, csvFile.getAbsolutePath(), fallbackException);
                    Errors.printToStream(fallbackException);
                } else {
                    log.error(StringValues.CSV_FALLBACK_LOAD_FAILED, csvFile.getAbsolutePath());
                }
                if (SettingsManager.areFileErrorPopupsEnabled()) {
                    Errors.showFileError(StringValues.CSV_PARSE_FAILED + csvFile, fallbackException);
                }
                return csvData;
            }
        }
        return csvData;
    }

    private static List<Map<String, String>> readCSVWithCharset(File csvFile, CsvMapper csvMapper, CsvSchema csvSchema,
            Predicate<Map<String, String>> validationPredicate,
            java.nio.charset.Charset charset) throws IOException {
        List<Map<String, String>> csvData = new ArrayList<>();
        List<Map<String, String>> rawData = new ArrayList<>();
        try (java.io.Reader reader = Files.newBufferedReader(csvFile.toPath(), charset);
                MappingIterator<Map<String, String>> iterator = csvMapper.readerFor(Map.class)
                        .with(csvSchema)
                        .readValues(reader)) {

            CsvSchema parsedSchema = (CsvSchema) iterator.getParser().getSchema();

            while (iterator.hasNext()) {
                Map<String, String> row = iterator.next();
                Map<String, String> optimizedRow = new java.util.LinkedHashMap<>(row.size());
                for (Map.Entry<String, String> entry : row.entrySet()) {
                    String key = entry.getKey().intern();
                    String value = entry.getValue();
                    if (value != null) {
                        value = value.intern();
                    }
                    optimizedRow.put(key, value);
                }
                rawData.add(optimizedRow);
                if (validationPredicate.test(optimizedRow)) {
                    csvData.add(optimizedRow);
                }
            }
            SettingsManager.getGameData().putCachedCSVData(csvFile.toPath(), rawData, parsedSchema);
        }
        return csvData;
    }

    public static Predicate<Map<String, String>> getNormalValidationPredicate() {
        return row -> {
            String id = row.get(StringConstants.ID);
            String name = row.get("name");
            boolean validID = id != null && !id.isEmpty();
            return validID && (name == null || !name.startsWith("#"));
        };
    }

    public static Predicate<Map<String, String>> getWingValidationPredicate() {
        return row -> {
            String id = row.get(StringConstants.ID);
            return id != null && !id.isEmpty() && !id.startsWith("#");
        };
    }

    public static List<Map<String, String>> reparseCSVForPath(Path path) {
        if (SettingsManager.isDeveloperModeEnabled()) {
            log.trace(StringValues.REPARSING_CSV_DISK, path);
        }
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true);
        CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();
        File csvFile = path.toFile();
        if (!csvFile.isFile()) {
            return null;
        }
        Predicate<Map<String, String>> acceptAll = row -> true;
        try {
            return readCSVWithCharset(csvFile, csvMapper, csvSchema, acceptAll, StandardCharsets.ISO_8859_1);
        } catch (Throwable e) {
            try {
                return readCSVWithCharset(csvFile, csvMapper, csvSchema, acceptAll, StandardCharsets.UTF_8);
            } catch (Throwable fallback) {
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.error(StringValues.CSV_REPARSE_FALLBACK_FAILED, path, fallback);
                } else {
                    log.error(StringValues.CSV_REPARSE_FALLBACK_FAILED, path);
                }
                return null;
            }
        }
    }
}
