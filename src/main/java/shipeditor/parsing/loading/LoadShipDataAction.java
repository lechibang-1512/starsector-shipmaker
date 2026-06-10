package shipeditor.parsing.loading;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.HullTreeEntryCleared;
import shipeditor.communication.events.files.HullTreeReloadQueued;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.*;
import shipeditor.representation.ship.SkinSpecFile;
import shipeditor.representation.ship.VariantFile;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.utility.objects.Pair;
import shipeditor.utility.text.StringConstants;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class LoadShipDataAction extends DataLoadingAction {

    @Override
    public Runnable perform() {
        CompletableFuture<Runnable> shipData = CompletableFuture.supplyAsync(LoadShipDataAction::collectShips);
        CompletableFuture<Runnable> variantData = CompletableFuture.supplyAsync(LoadShipDataAction::collectVariants);

        CompletableFuture<Runnable> combinedResult = shipData.thenCombine(variantData,
                (shipRunnable, variantRunnable) -> () -> {
                    shipRunnable.run();
                    variantRunnable.run();
                });
        return combinedResult.join();
    }

    @Override
    public String getTaskName() {
        return "Ship Data";
    }

    private static Runnable collectShips() {
        // Load skins from DB index, grouped by mod
        Map<String, SkinSpecFile> allSkins = new HashMap<>();
        Map<String, List<IndexedFile>> skinsByMod = DatabaseQueryService.getFilesByTypeGroupedByMod("SKIN");
        for (Map.Entry<String, List<IndexedFile>> entry : skinsByMod.entrySet()) {
            Path folderPath = SettingsManager.getFolderForModId(entry.getKey());
            if (folderPath == null) continue;
            Settings settings = SettingsManager.getSettings();
            GameDataPackage dataPackage = settings.getPackage(folderPath);
            if (dataPackage != null && dataPackage.isDisabled()) continue;
            if (LibModFilter.isLibMod(folderPath)) continue;

            log.trace("Loading skins from DB index for package: {}", folderPath);
            Map<String, SkinSpecFile> containedSkins = LoadShipDataAction.walkSkinFolder(folderPath);
            allSkins.putAll(containedSkins);
        }

        // Load ships from DB-indexed CSV files, grouped by mod
        GameDataRepository gameData = SettingsManager.getGameData();
        Map<String, ShipCSVEntry> allShipEntries = gameData.getAllShipEntries();
        allShipEntries.clear();
        Map<Path, List<ShipCSVEntry>> allEntriesByPackage = new HashMap<>();

        Map<String, List<IndexedFile>> shipCsvsByMod = DatabaseQueryService.getFilesByTypeGroupedByMod("SHIP_CSV");
        for (Map.Entry<String, List<IndexedFile>> entry : shipCsvsByMod.entrySet()) {
            Path folderPath = SettingsManager.getFolderForModId(entry.getKey());
            if (folderPath == null) continue;
            Settings settings = SettingsManager.getSettings();
            GameDataPackage dataPackage = settings.getPackage(folderPath);
            if (dataPackage != null && dataPackage.isDisabled()) continue;
            if (LibModFilter.isLibMod(folderPath)) continue;

            Pair<Path, List<ShipCSVEntry>> packageShipData = LoadShipDataAction.walkHullFolder(
                    entry.getKey(), folderPath.toString(), allSkins);
            if (packageShipData != null) {
                allEntriesByPackage.put(packageShipData.getFirst(), packageShipData.getSecond());
            }
        }

        return () -> {
            gameData.setShipEntriesByPackage(allEntriesByPackage);
            gameData.setShipDataLoaded(true);
            EventBus.publish(new HullTreeEntryCleared());
            EventBus.publish(new HullTreeReloadQueued());
        };
    }

    private static Runnable collectVariants() {
        // Query all variants from DB grouped by mod — no need to enumerate packages via filesystem
        Map<String, List<IndexedFile>> variantsByMod = DatabaseQueryService.getFilesByTypeGroupedByMod("VARIANT");

        Map<String, VariantFile> allVariants = new java.util.concurrent.ConcurrentHashMap<>();
        variantsByMod.entrySet().parallelStream().forEach(entry -> {
            Path directory = SettingsManager.getFolderForModId(entry.getKey());
            if (directory != null) {
                Settings settings = SettingsManager.getSettings();
                GameDataPackage dataPackage = settings.getPackage(directory);
                if (dataPackage == null || !dataPackage.isDisabled()) {
                    if (!LibModFilter.isLibMod(directory)) {
                        log.trace("Fetching variant files from database index for package: {}", directory);

                        entry.getValue().parallelStream().forEach(dbFile -> {
                            File variantFile = dbFile.getFilePath().toFile();
                            VariantFile mapped = FileLoading.loadVariantFile(variantFile);
                            if (mapped != null) {
                                mapped.setContainingPackage(directory);
                                allVariants.put(mapped.getVariantId(), mapped);
                            } else if (variantFile.length() > 0) {
                                log.error("Failure to load variant, omitting from result data: {}", variantFile);
                            }
                        });
                    }
                }
            }
        });

        return () -> {
            GameDataRepository gameData = SettingsManager.getGameData();
            gameData.setAllVariants(allVariants);
        };
    }

    private static Pair<Path, List<ShipCSVEntry>> walkHullFolder(String modId, String folderPath, Map<String, SkinSpecFile> skins) {
        Path shipTablePath = Paths.get(folderPath, "data", StringConstants.HULLS, StringConstants.SHIP_DATA_CSV);

        log.trace("Parsing ship CSV data at: {}..", shipTablePath);
        List<Map<String, String>> csvData = FileLoading.parseCSVTable(shipTablePath);

        if (csvData == null) {
            log.info("Hull folder without CSV table at: {}", folderPath.toString());
            return null;
        }
        log.trace("Ship CSV data at {} retrieved successfully.", shipTablePath);

        Path packagePath = Paths.get(folderPath, "");
        List<ShipCSVEntry> entriesFromPackage = Collections.synchronizedList(new ArrayList<>());

        GameDataRepository gameData = SettingsManager.getGameData();
        Map<String, ShipCSVEntry> allShipEntries = gameData.getAllShipEntries();

        List<IndexedFile> dbFiles = DatabaseQueryService.getFilesByModAndTypeAsync(modId, "SHIP").join();
        Map<String, String> shipFiles = new java.util.concurrent.ConcurrentHashMap<>();
        dbFiles.parallelStream().forEach(dbFile -> {
            shipFiles.put(dbFile.getEntityId(), dbFile.getFileName());
        });

        csvData.parallelStream().forEach(row -> {
            String rowId = row.get("id");
            if (rowId != null && !rowId.isEmpty() && !rowId.startsWith("#")) {
                String fileName = shipFiles.get(rowId);
                if (fileName != null && !fileName.isEmpty()) {
                    ShipCSVEntry newEntry = new ShipCSVEntry(row, null, packagePath, fileName, shipTablePath);
                    entriesFromPackage.add(newEntry);
                    synchronized (allShipEntries) {
                        allShipEntries.put(rowId, newEntry);
                    }
                }
            }
        });

        return new Pair<>(packagePath, new ArrayList<>(entriesFromPackage));
    }

    private static Map<String, SkinSpecFile> walkSkinFolder(Path skinFolder) {
        log.trace("Fetching skin files from database index for package: {}...", skinFolder);
        
        Path fileNamePath = skinFolder.getFileName();
        if (fileNamePath == null) return new java.util.concurrent.ConcurrentHashMap<>();
        String modId = fileNamePath.toString();
        if (SettingsManager.isCoreFolder(skinFolder)) {
            modId = "starsector-core";
        }
        
        List<IndexedFile> dbFiles = DatabaseQueryService.getFilesByModAndTypeAsync(modId, "SKIN").join();
        Map<String, SkinSpecFile> mappedSkins = new java.util.concurrent.ConcurrentHashMap<>();
        
        dbFiles.parallelStream().forEach(dbFile -> {
            File skinFile = dbFile.getFilePath().toFile();
            SkinSpecFile mapped = FileLoading.loadSkinFile(skinFile);
            if (mapped != null) {
                mapped.setContainingPackage(skinFolder);
                mappedSkins.put(skinFile.getName(), mapped);
            } else if (skinFile.length() > 0) {
                log.error("Failure to load skin, omitting from result data: {}", skinFile);
            }
        });
        log.trace("Fetched and mapped {} skin files.", mappedSkins.size());
        return mappedSkins;
    }

}
