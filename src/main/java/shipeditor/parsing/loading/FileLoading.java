package shipeditor.parsing.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.LoadingActionFired;
import shipeditor.communication.events.components.LoadingTaskCompleted;
import shipeditor.communication.events.components.LoadingTaskStarted;

import shipeditor.parsing.FileUtilities;
import shipeditor.parsing.JsonProcessor;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.representation.ship.SkinSpecFile;
import shipeditor.representation.ship.VariantFile;
import shipeditor.representation.weapon.ProjectileSpecFile;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.Errors;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.overseers.ImageCache;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings({ "ClassWithTooManyFields", "OverlyCoupledClass", "ClassWithTooManyMethods" })
@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public final class FileLoading {

    @Getter
    private static final DataLoadingAction loadShips = new LoadShipDataAction();
    @Getter
    private static final DataLoadingAction loadHullmods = new LoadHullmodDataAction();
    @Getter
    private static final DataLoadingAction loadHullStyles = new LoadHullStyleDataAction();
    @Getter
    private static final DataLoadingAction loadEngineStyles = new LoadEngineStyleDataAction();
    @Getter
    private static final DataLoadingAction loadShipSystems = new LoadShipSystemDataAction();
    @Getter
    private static final DataLoadingAction loadWings = new LoadWingDataAction();
    @Getter
    private static final DataLoadingAction loadWeapons = new LoadWeaponsDataAction();
    @Getter
    public static final Action openSprite = new OpenSpriteAction();
    @Getter
    public static final Action openShip = new OpenHullAction();
    @Getter
    private static final Action loadHullAsLayer = new LoadHullAsLayer();
    @Getter
    private static final Action loadSpriteAsHull = new LoadSpriteAsNewHull();

    @Getter
    private static boolean loadingInProgress;

    private static final Map<Path, SoftReference<Map<String, List<Path>>>> directoryIndices = new ConcurrentHashMap<>();

    public static void clearDirectoryCache() {
        directoryIndices.clear();
    }

    private static Map<String, List<Path>> getOrCreateIndex(Path folderPath) {
        if (folderPath == null)
            return java.util.Collections.emptyMap();
        SoftReference<Map<String, List<Path>>> ref = directoryIndices.get(folderPath);
        if (ref != null) {
            Map<String, List<Path>> existing = ref.get();
            if (existing != null) {
                return existing;
            }
        }
        // Build a new index (SoftReference was cleared or never existed).
        Map<String, List<Path>> index = new ConcurrentHashMap<>();
        try (Stream<Path> stream = FileLoading.walk(folderPath)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                Path fileNamePath = file.getFileName();
                if (fileNamePath == null) return;
                String fileName = fileNamePath.toString();
                index.computeIfAbsent(fileName, k -> new ArrayList<>()).add(file);
            });
        } catch (IOException e) {
            log.error("Failed to index folder: " + folderPath, e);
        }
        directoryIndices.put(folderPath, new SoftReference<>(index));
        return index;
    }

    private FileLoading() {
    }

    private static void initializeDatabaseInProcess() {
        try {
            log.info("Starting in-process database indexing scan...");
            IndexScannerTask.scanAndIndexAll(false);
            log.info("In-process database indexing scan completed.");
        } catch (RuntimeException e) {
            log.error("Failed to execute in-process database indexing scan", e);
        }
    }

    public static CompletableFuture<List<Runnable>> loadGameData() {
        clearDirectoryCache();
        EventBus.publish(new LoadingActionFired(true));
        loadingInProgress = true;

        return CompletableFuture.runAsync(FileLoading::initializeDatabaseInProcess)
                .thenCompose(v -> {
                    List<DataLoadingAction> loadActions = List.of(loadShips, loadHullmods, loadHullStyles,
                            loadEngineStyles, loadShipSystems, loadWings, loadWeapons);

                    List<CompletableFuture<Runnable>> futures = new ArrayList<>();
                    for (DataLoadingAction action : loadActions) {
                        CompletableFuture<Runnable> future = CompletableFuture.supplyAsync(() -> {
                            String taskName = action.getTaskName();
                            SwingUtilities.invokeLater(() -> EventBus.publish(new LoadingTaskStarted(taskName)));
                            Runnable result = action.perform();
                            SwingUtilities.invokeLater(() -> EventBus.publish(new LoadingTaskCompleted(taskName)));
                            return result;
                        });
                        futures.add(future);
                    }

                    CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                    return allOf.thenApply(val -> futures.stream().map(CompletableFuture::join).toList());
                }).whenComplete((runnables, ex) -> {
                    if (ex != null) {
                        log.error("Error during loading game data", ex);
                    } else if (runnables != null) {
                        Runnable completionTasks = () -> {
                            clearDirectoryCache();
                            EventBus.publish(new LoadingActionFired(false));
                            loadingInProgress = false;
                            StaticController.reselectCurrentLayer();
                            SettingsManager.updateFileFromRuntime();
                        };
                        if (java.awt.GraphicsEnvironment.isHeadless()) {
                            runnables.forEach(Runnable::run);
                            completionTasks.run();
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                EventBus.publish(new LoadingTaskStarted("Updating UI"));
                                // Delay the heavy UI rebuilds by one event cycle so the label can repaint first.
                                SwingUtilities.invokeLater(() -> executeStaggered(new ArrayList<>(runnables), () -> {
                                    completionTasks.run();
                                    EventBus.publish(new LoadingTaskCompleted("Updating UI"));
                                }));
                            });
                        }
                    }
                });
    }

    private static void executeStaggered(List<Runnable> tasks, Runnable onComplete) {
        if (tasks.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        Runnable task = tasks.remove(0);
        try {
            task.run();
        } catch (Throwable t) {
            log.error("Error during staggered UI execution", t);
        }
        SwingUtilities.invokeLater(() -> executeStaggered(tasks, onComplete));
    }


    /**
     * @param loadAction executed with a separate GUI callback.
     */
    public static Action loadDataAsync(DataLoadingAction loadAction) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EventBus.publish(new LoadingActionFired(true));
                loadingInProgress = true;
                CompletableFuture<Runnable> loadResult = CompletableFuture.supplyAsync(() -> {
                    String taskName = loadAction.getTaskName();
                    SwingUtilities.invokeLater(() -> EventBus.publish(new LoadingTaskStarted(taskName)));
                    Runnable result = loadAction.perform();
                    SwingUtilities.invokeLater(() -> EventBus.publish(new LoadingTaskCompleted(taskName)));
                    return result;
                });
                loadResult.whenComplete((runnable, ex) -> {
                    if (ex != null) {
                        log.error("Error during async loading", ex);
                        SwingUtilities.invokeLater(() -> {
                            EventBus.publish(new LoadingActionFired(false));
                            loadingInProgress = false;
                        });
                    } else if (runnable != null) {
                        SwingUtilities.invokeLater(() -> {
                            runnable.run();
                            EventBus.publish(new LoadingActionFired(false));
                            loadingInProgress = false;
                            SettingsManager.updateFileFromRuntime();
                        });
                    }
                });
            }
        };
    }

    private static Stream<Path> walk(Path start) throws IOException {
        return Files.walk(start, FileVisitOption.FOLLOW_LINKS);
    }

    private static Path searchFileInFolder(Path filePath, Path folderPath) {
        if (folderPath == null)
            return null;

        // Fast path: try direct resolve first to avoid building a full directory index.
        Path directPath = folderPath.resolve(filePath);
        if (Files.exists(directPath)) {
            return directPath;
        }

        // Fallback: use indexed lookup for files in non-standard locations.
        Path fileNamePath = filePath.getFileName();
        if (fileNamePath == null) return null;
        String fileName = fileNamePath.toString();
        Map<String, List<Path>> index = getOrCreateIndex(folderPath);
        List<Path> foundFiles = index.get(fileName);
        if (foundFiles != null) {
            for (Path foundFile : foundFiles) {
                String toString = foundFile.toString();
                if (toString.endsWith(filePath.toString())) {
                    return foundFile;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("NestedTryStatement")
    public static BufferedImage loadImageResource(String imageFilename) {
        Class<FileLoading> loadingClass = FileLoading.class;
        ClassLoader classLoader = loadingClass.getClassLoader();

        URL spritePath = Objects.requireNonNull(classLoader.getResource(imageFilename));
        File spriteFile;
        try {
            URI pathURI = spritePath.toURI();
            if (pathURI.isOpaque()) {
                try (InputStream inputStream = loadingClass.getResourceAsStream("/" + imageFilename)) {
                    if (inputStream != null) {
                        return ImageIO.read(inputStream);
                    } else {
                        throw new RuntimeException("Resource not found!");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            spriteFile = new File(pathURI);
        } catch (URISyntaxException e) {
            String errorMsg = "Image resource loading failed, exception thrown at: " + spritePath;
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        errorMsg,
                        StringValues.FILE_LOADING_ERROR,
                        JOptionPane.ERROR_MESSAGE);
            } else {
                log.error(errorMsg);
            }
            return null;
        }
        return FileLoading.loadSpriteAsImage(spriteFile);
    }

    public static BufferedImage loadSpriteAsImage(File file) {
        return ImageCache.loadImage(file);
    }

    public static Sprite loadSprite(File file) {
        BufferedImage spriteImage = FileLoading.loadSpriteAsImage(file);
        String name = file.getName();
        Path path = file.toPath();
        return new Sprite(spriteImage, path, name);
    }

    /**
     * Searches for the input file, first in passed package folder, then in core
     * data folder, then in mod folders.
     * 
     * @param filePath          should be, for example,
     *                          Path.of("graphics/icons/intel/investigation.png").
     * @param packageFolderPath supposed parent package, where search will start.
     *                          Can be null.
     * @return fetched file if it exists, else NULL.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static File fetchDataFile(Path filePath, Path packageFolderPath) {
        if (filePath == null) {
            log.error("Failed to fetch data file, input path is null.");
            return null;
        }
        Path coreDataFolder = SettingsManager.getCoreFolderPath();
        List<Path> otherModFolders = SettingsManager.getAllModFolders();
        Path result = null;

        if (packageFolderPath != null) {
            // Search in parent mod package.
            result = FileLoading.searchFileInFolder(filePath, packageFolderPath);
        }

        // If not found, search in core folder.
        if (result == null) {
            result = FileLoading.searchFileInFolder(filePath, coreDataFolder);
        }
        if (result != null)
            return result.toFile();

        // If not found, search in other mods.
        for (Path modFolder : otherModFolders) {
            result = FileLoading.searchFileInFolder(filePath, modFolder);
            if (result != null) {
                break;
            }
        }

        if (result != null) {
            return result.toFile();
        } else {
            log.error("Failed to fetch data file for {}!", filePath.getFileName());
        }
        return null;
    }

    public static HullSpecFile loadHullFile(File file) {
        HullSpecFile hullSpecFile = FileLoading.loadDataFile(file, ".ship", HullSpecFile.class);
        if (hullSpecFile != null) {
            hullSpecFile.setFilePath(file.toPath());
            GameDataRepository.putSpec(hullSpecFile);
        }
        return hullSpecFile;
    }

    static WeaponSpecFile loadWeaponFile(File file) {
        WeaponSpecFile weaponSpecFile = FileLoading.loadDataFile(file, ".wpn", WeaponSpecFile.class);
        if (weaponSpecFile != null) {
            weaponSpecFile.setWeaponSpecFilePath(file.toPath());

            if (weaponSpecFile.getType() == null) {
                log.error("Weapon type is NULL in: {}", file.getName());
            }

        }
        return weaponSpecFile;
    }

    public static SkinSpecFile loadSkinFile(File file) {
        SkinSpecFile skinSpecFile = FileLoading.loadDataFile(file, ".skin", SkinSpecFile.class);
        if (skinSpecFile != null) {
            skinSpecFile.setFilePath(file.toPath());
            GameDataRepository.putSpec(skinSpecFile);
        }
        return skinSpecFile;
    }

    static VariantFile loadVariantFile(File file) {
        VariantFile variantFile = FileLoading.loadDataFile(file, StringConstants.VARIANT_EXTENSION, VariantFile.class);
        if (variantFile != null) {
            variantFile.setVariantFilePath(file.toPath());
        }
        return variantFile;
    }

    static ProjectileSpecFile loadProjectileFile(File file) {
        ProjectileSpecFile projectileFile = FileLoading.loadDataFile(file, ".proj", ProjectileSpecFile.class);
        if (projectileFile != null) {
            projectileFile.setProjectileSpecFilePath(file.toPath());
        }
        return projectileFile;
    }

    private static <T> T loadDataFile(File file, String extension, Class<T> dataClass) {
        if (file == null || !file.exists()) {
            log.error("Data file does not exist: {}", file != null ? file.getPath() : "null");
            return null;
        }
        String toString = file.getPath();
        if (!toString.endsWith(extension)) {
            throw new IllegalArgumentException("Tried to resolve data file with invalid extension!");
        }

        if (file.length() == 0) {
            log.warn("Data file is completely empty, skipping: {}", file.getName());
            return null;
        }

        T dataFile;
        try {
            ObjectMapper objectMapper = FileUtilities.getConfigured();
            log.trace("Opening data file: {}", file.getName());
            dataFile = objectMapper.readValue(file, dataClass);
        } catch (IOException e) {
            log.trace("Data file parsing failed, retrying with correction: {}", file.getName());

            dataFile = FileLoading.parseCorrectableJSON(file, dataClass);
            if (dataFile == null) {
                if (e.getClass().getSimpleName().equals("MismatchedInputException") &&
                    e.getMessage() != null && e.getMessage().contains("No content to map due to end-of-input")) {
                    log.warn("Data file has no parsable content, skipping: {}", file.getName());
                } else {
                    log.error("Data file parsing failed conclusively: {}", file.getName());
                    Errors.printToStream(e);
                    if (SettingsManager.areFileErrorPopupsEnabled()) {
                        Errors.showFileError("Data file parsing failed, exception thrown at: " + file);
                    }
                }
            }
        }
        return dataFile;
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private static <T> T parseCorrectableJSON(File file, Class<T> target) {
        ObjectMapper objectMapper = FileUtilities.getConfigured();

        TypeFactory typeFactory = objectMapper.getTypeFactory();
        JavaType javaType = typeFactory.constructType(target);

        return FileLoading.parseCorrectableJSON(file, javaType);
    }

    @SuppressWarnings("AssignmentToNull")
    static <T> T parseCorrectableJSON(File file, JavaType targetType) {
        T result;
        ObjectMapper objectMapper = FileUtilities.getConfigured();

        String content = JsonProcessor.straightenMalformed(file);
        if (content.trim().isEmpty()) {
            return null;
        }
        try (JsonParser parser = objectMapper.createParser(content)) {
            result = objectMapper.readValue(parser, targetType);
        } catch (IOException e) {
            log.error("Corrected JSON parsing failed: {}", file.getName());
            result = null;
            Errors.printToStream(e);
        }
        return result;
    }

    @SuppressWarnings("WeakerAccess")
    public static List<File> fetchFilesWithExtension(Path target, String dotlessExtension) {
        List<File> files = new ArrayList<>();
        try (Stream<Path> pathStream = FileLoading.walk(target)) {
            pathStream.filter(path -> {
                Path fileNamePath = path.getFileName();
                if (fileNamePath == null) return false;
                String toString = fileNamePath.toString();
                return toString.endsWith("." + dotlessExtension);
            })
                    .map(Path::toFile)
                    .forEach(files::add);
        } catch (IOException exception) {
            Errors.printToStream(exception);
        }
        return files;
    }

    static List<Map<String, String>> parseCSVTable(Path path) {
        return FileLoading.parseCSVTable(path, FileLoading.getNormalValidationPredicate());
    }

    /**
     * Target CSV file is expected to have a header row and an ID column designated
     * in said header.
     * 
     * @param path address of the target file.
     * @return List of rows where each row is a Map of string keys and string
     *         values.
     */
    static List<Map<String, String>> parseCSVTable(Path path, Predicate<Map<String, String>> validationPredicate) {
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true);

        CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();
        File csvFile = path.toFile();

        if (!csvFile.isFile()) {
            return null;
        }

        if (csvFile.length() == 0) {
            log.warn("CSV file is completely empty, skipping: {}", csvFile.getName());
            return null;
        }

        List<Map<String, String>> csvData = new ArrayList<>();
        try {
            csvData = FileLoading.readCSVWithCharset(csvFile, csvMapper, csvSchema, validationPredicate,
                    StandardCharsets.UTF_8);
        } catch (Throwable exception) {
            log.warn("UTF-8 CSV loading failed: {}, retrying with ISO_8859_1", csvFile.getAbsolutePath());
            try {
                csvData = FileLoading.readCSVWithCharset(csvFile, csvMapper, csvSchema, validationPredicate,
                        StandardCharsets.ISO_8859_1);
            } catch (Throwable fallbackException) {
                log.error("Data CSV loading failed on fallback: {}", csvFile.getAbsolutePath());
                Errors.printToStream(fallbackException);
                if (SettingsManager.areFileErrorPopupsEnabled()) {
                    Errors.showFileError("Failed to parse CSV table (likely semantic errors), " +
                            "loading incomplete: " + csvFile);
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
            SettingsManager.getGameData().putCsvSchemaForPath(csvFile.toPath(), parsedSchema);

            while (iterator.hasNext()) {
                Map<String, String> row = iterator.next();
                rawData.add(row);
                if (validationPredicate.test(row)) {
                    csvData.add(row);
                }
            }
            SettingsManager.getGameData().putRawCSVDataForPath(csvFile.toPath(), rawData);
        }
        return csvData;
    }

    private static Predicate<Map<String, String>> getNormalValidationPredicate() {
        return row -> {
            String id = row.get(StringConstants.ID);
            String name = row.get("name");
            boolean validID = id != null && !id.isEmpty();
            return validID && !name.startsWith("#");
        };
    }

    static Predicate<Map<String, String>> getWingValidationPredicate() {
        return row -> {
            String id = row.get(StringConstants.ID);
            boolean validID = id != null && !id.isEmpty();
            return validID && !id.startsWith("#");
        };
    }

    /**
     * Re-parses a CSV file from disk, caching both the raw data and schema in
     * GameDataRepository.
     * Used as a fallback when SoftReferences to cached CSV data have been cleared
     * by GC.
     * 
     * @param path the path to the CSV file on disk.
     * @return the list of raw row maps, or null if re-parsing fails.
     */
    public static List<Map<String, String>> reparseCSVForPath(Path path) {
        log.trace("Re-parsing CSV from disk (SoftReference was cleared): {}", path);
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true);
        CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();
        File csvFile = path.toFile();
        if (!csvFile.isFile()) {
            return null;
        }
        // Accept all rows — we need the complete raw data for saving.
        Predicate<Map<String, String>> acceptAll = row -> true;
        try {
            return readCSVWithCharset(csvFile, csvMapper, csvSchema, acceptAll, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            try {
                return readCSVWithCharset(csvFile, csvMapper, csvSchema, acceptAll, StandardCharsets.ISO_8859_1);
            } catch (Throwable fallback) {
                log.error("Failed to re-parse CSV on fallback: {}", path, fallback);
                return null;
            }
        }
    }

}
