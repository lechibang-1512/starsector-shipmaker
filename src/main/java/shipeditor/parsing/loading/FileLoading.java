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
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;

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
import shipeditor.communication.events.components.ComponentEvents.LoadingTaskCompleted;
import shipeditor.communication.events.components.ComponentEvents.LoadingTaskStarted;
import shipeditor.communication.events.components.ComponentEvents.LoadingActionFired;

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

    @Getter @Setter
    private static volatile boolean loadingInProgress;

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
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.error(StringValues.FAILED_TO_INDEX_FOLDER, folderPath, e);
            } else {
                log.error(StringValues.FAILED_TO_INDEX_FOLDER, folderPath);
            }
        }
        directoryIndices.put(folderPath, new SoftReference<>(index));
        return index;
    }

    private FileLoading() {
    }

    private static void initializeDatabaseInProcess() {
        try {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.info(StringValues.STARTING_DB_INDEX_SCAN);
            }
            IndexScannerTask.scanAndIndexAll(false);
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.info(StringValues.DB_INDEX_SCAN_COMPLETED);
            }
        } catch (RuntimeException e) {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.error(StringValues.DB_INDEX_SCAN_FAILED, e);
            } else {
                log.error(StringValues.DB_INDEX_SCAN_FAILED);
            }
        }
    }

    public static CompletableFuture<List<Runnable>> loadGameData() {
        clearDirectoryCache();
        EventBus.publish(new LoadingActionFired(true));
        FileLoading.setLoadingInProgress(true);

        return CompletableFuture.runAsync(FileLoading::initializeDatabaseInProcess)
                .thenCompose(v -> {
                    // Weapons and hullmods must commit their flags (setWeaponsDataLoaded / setHullmodDataLoaded)
                    // on the EDT before the ship-data runnable fires HullTreeReloadQueued, which can trigger
                    // variant initialization that guards on those two flags.  Background loading is concurrent
                    // regardless of this list order; only the EDT runnable execution is sequential.
                    List<DataLoadingAction> loadActions = List.of(loadWeapons, loadHullmods, loadHullStyles,
                            loadEngineStyles, loadShipSystems, loadWings, loadShips);

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
                        if (SettingsManager.isDeveloperModeEnabled()) {
                            log.error(StringValues.ERROR_LOADING_GAME_DATA, ex);
                        } else {
                            log.error(StringValues.ERROR_LOADING_GAME_DATA);
                        }
                    } else if (runnables != null) {
                        Runnable completionTasks = () -> {
                            clearDirectoryCache();
                            EventBus.publish(new LoadingActionFired(false));
                            FileLoading.setLoadingInProgress(false);
                            StaticController.reselectCurrentLayer();
                            SettingsManager.updateFileFromRuntime();
                        };
                        if (java.awt.GraphicsEnvironment.isHeadless()) {
                            runnables.forEach(Runnable::run);
                            completionTasks.run();
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                EventBus.publish(new LoadingTaskStarted(StringValues.UPDATING_UI));
                                // Delay the heavy UI rebuilds by one event cycle so the label can repaint first.
                                SwingUtilities.invokeLater(() -> executeStaggered(new ArrayList<>(runnables), () -> {
                                    completionTasks.run();
                                    EventBus.publish(new LoadingTaskCompleted(StringValues.UPDATING_UI));
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
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.error(StringValues.STAGGERED_UI_ERROR, t);
            } else {
                log.error(StringValues.STAGGERED_UI_ERROR);
            }
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
                FileLoading.setLoadingInProgress(true);
                CompletableFuture<Runnable> loadResult = CompletableFuture.supplyAsync(() -> {
                    String taskName = loadAction.getTaskName();
                    SwingUtilities.invokeLater(() -> EventBus.publish(new LoadingTaskStarted(taskName)));
                    Runnable result = loadAction.perform();
                    SwingUtilities.invokeLater(() -> EventBus.publish(new LoadingTaskCompleted(taskName)));
                    return result;
                });
                loadResult.whenComplete((runnable, ex) -> {
                    if (ex != null) {
                        if (SettingsManager.isDeveloperModeEnabled()) {
                            log.error(StringValues.ASYNC_LOADING_ERROR, ex);
                        } else {
                            log.error(StringValues.ASYNC_LOADING_ERROR);
                        }
                        SwingUtilities.invokeLater(() -> {
                            EventBus.publish(new LoadingActionFired(false));
                            FileLoading.setLoadingInProgress(false);
                        });
                    } else if (runnable != null) {
                        SwingUtilities.invokeLater(() -> {
                            runnable.run();
                            EventBus.publish(new LoadingActionFired(false));
                            FileLoading.setLoadingInProgress(false);
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
                        throw new RuntimeException(StringValues.RESOURCE_NOT_FOUND);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            spriteFile = new File(pathURI);
        } catch (URISyntaxException e) {
            String errorMsg = StringValues.IMAGE_RESOURCE_LOAD_FAILED.replace("{}", String.valueOf(spritePath));
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        errorMsg,
                        StringValues.FILE_LOADING_ERROR,
                        JOptionPane.ERROR_MESSAGE);
            } else {
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.error(errorMsg, e);
                } else {
                    log.error(errorMsg);
                }
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
            log.error(StringValues.FETCH_DATA_FILE_NULL);
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
            log.error(StringValues.FETCH_DATA_FILE_FAILED, filePath.getFileName());
        }
        return null;
    }

    public static HullSpecFile loadHullFile(File file) {
        HullSpecFile hullSpecFile = FileLoading.loadDataFile(file, StringConstants.SHIP_EXTENSION, HullSpecFile.class);
        if (hullSpecFile != null) {
            hullSpecFile.setFilePath(file.toPath());
            GameDataRepository.putSpec(hullSpecFile);
        }
        return hullSpecFile;
    }

    static WeaponSpecFile loadWeaponFile(File file) {
        WeaponSpecFile weaponSpecFile = FileLoading.loadDataFile(file, StringConstants.WEAPON_EXTENSION, WeaponSpecFile.class);
        if (weaponSpecFile != null) {
            weaponSpecFile.setWeaponSpecFilePath(file.toPath());

            if (weaponSpecFile.getType() == null) {
                log.error(StringValues.WEAPON_TYPE_NULL, file.getName());
            }

        }
        return weaponSpecFile;
    }

    public static SkinSpecFile loadSkinFile(File file) {
        SkinSpecFile skinSpecFile = FileLoading.loadDataFile(file, StringConstants.SKIN_EXTENSION, SkinSpecFile.class);
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
        ProjectileSpecFile projectileFile = FileLoading.loadDataFile(file, StringConstants.PROJECTILE_EXTENSION, ProjectileSpecFile.class);
        if (projectileFile != null) {
            projectileFile.setProjectileSpecFilePath(file.toPath());
        }
        return projectileFile;
    }

    private static <T> T loadDataFile(File file, String extension, Class<T> dataClass) {
        if (file == null || !file.exists()) {
            log.error(StringValues.DATA_FILE_NOT_EXIST, file != null ? file.getPath() : "null");
            return null;
        }
        String toString = file.getPath();
        if (!toString.endsWith(extension)) {
            throw new IllegalArgumentException(StringValues.INVALID_FILE_EXTENSION);
        }

        if (file.length() == 0) {
            log.warn(StringValues.DATA_FILE_EMPTY, file.getName());
            return null;
        }

        T dataFile;
        try {
            ObjectMapper objectMapper = FileUtilities.getConfigured();
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.trace(StringValues.OPENING_DATA_FILE, file.getName());
            }
            dataFile = objectMapper.readValue(file, dataClass);
        } catch (IOException e) {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.trace(StringValues.DATA_FILE_PARSE_FAILED_RETRY, file.getName());
            }

            dataFile = FileLoading.parseCorrectableJSON(file, dataClass);
            if (dataFile == null) {
                if (e.getClass().getSimpleName().equals("MismatchedInputException") &&
                    e.getMessage() != null && e.getMessage().contains(StringValues.NO_CONTENT_TO_MAP)) {
                    log.warn(StringValues.DATA_FILE_NO_CONTENT, file.getName());
                } else {
                    if (SettingsManager.isDeveloperModeEnabled()) {
                        log.error(StringValues.DATA_FILE_PARSE_FAILED, file.getName(), e);
                    } else {
                        log.error(StringValues.DATA_FILE_PARSE_FAILED, file.getName());
                    }
                    if (SettingsManager.isDeveloperModeEnabled()) {
                        Errors.printToStream(e);
                    }
                    if (SettingsManager.areFileErrorPopupsEnabled()) {
                        Errors.showFileError(StringValues.DATA_FILE_PARSE_EXCEPTION + file, e);
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
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.error(StringValues.CORRECTED_JSON_PARSE_FAILED, file.getName(), e);
                Errors.printToStream(e);
            } else {
                log.error(StringValues.CORRECTED_JSON_PARSE_FAILED, file.getName());
            }
            result = null;
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
            log.warn(StringValues.CSV_FILE_EMPTY, csvFile.getName());
            return null;
        }

        List<Map<String, String>> csvData = new ArrayList<>();
        try {
            csvData = FileLoading.readCSVWithCharset(csvFile, csvMapper, csvSchema, validationPredicate,
                    StandardCharsets.ISO_8859_1);
        } catch (Throwable exception) {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.warn(StringValues.CSV_ISO_LOAD_FAILED, csvFile.getAbsolutePath(), exception);
            } else {
                log.warn(StringValues.CSV_ISO_LOAD_FAILED, csvFile.getAbsolutePath());
            }
            try {
                csvData = FileLoading.readCSVWithCharset(csvFile, csvMapper, csvSchema, validationPredicate,
                        StandardCharsets.UTF_8);
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
                rawData.add(row);
                if (validationPredicate.test(row)) {
                    csvData.add(row);
                }
            }
            SettingsManager.getGameData().putCachedCSVData(csvFile.toPath(), rawData, parsedSchema);
        }
        return csvData;
    }

    private static Predicate<Map<String, String>> getNormalValidationPredicate() {
        return row -> {
            String id = row.get(StringConstants.ID);
            String name = row.get("name");
            boolean validID = id != null && !id.isEmpty();
            return validID && (name == null || !name.startsWith("#"));
        };
    }

    static Predicate<Map<String, String>> getWingValidationPredicate() {
        return row -> {
            String id = row.get(StringConstants.ID);
            return id != null && !id.isEmpty() && !id.startsWith("#");
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
        // Accept all rows — we need the complete raw data for saving.
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
