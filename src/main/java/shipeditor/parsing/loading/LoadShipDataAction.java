package shipeditor.parsing.loading;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.*;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.representation.ship.SkinSpecFile;
import shipeditor.representation.ship.VariantFile;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.utility.objects.Pair;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import shipeditor.communication.events.files.FileEvents.HullTreeEntryCleared;
import shipeditor.communication.events.files.FileEvents.HullTreeReloadQueued;

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
        return StringValues.TASK_SHIPS;
    }

    private static Runnable collectShips() {
        SettingsManager.getGameData().getAllSpecEntries().clear();
        // Load skins from DB index, grouped by mod
        Map<String, SkinSpecFile> allSkins = new HashMap<>();
        Map<String, List<IndexedFile>> skinsByMod = DatabaseQueryService.getFilesByTypeGroupedByMod(StringConstants.SKIN_TYPE);
        for (Map.Entry<String, List<IndexedFile>> entry : skinsByMod.entrySet()) {
            Path folderPath = SettingsManager.getFolderForModId(entry.getKey());
            if (folderPath == null) continue;
            Settings settings = SettingsManager.getSettings();
            GameDataPackage dataPackage = settings.getPackage(folderPath);
            if (dataPackage != null && dataPackage.isDisabled()) continue;
            if (LibModFilter.isLibMod(folderPath)) continue;

            if (SettingsManager.isDeveloperModeEnabled()) {
                log.trace(StringValues.LOADING_SKINS_DB_INDEX, folderPath);
            }
            Map<String, SkinSpecFile> containedSkins = LoadShipDataAction.walkSkinFolder(folderPath);
            allSkins.putAll(containedSkins);
        }

        // Load ships from DB-indexed CSV files, grouped by mod
        Map<String, ShipCSVEntry> allShipEntries = new java.util.concurrent.ConcurrentHashMap<>();
        Map<Path, List<ShipCSVEntry>> allEntriesByPackage = new HashMap<>();

        Map<String, List<IndexedFile>> shipCsvsByMod = DatabaseQueryService.getFilesByTypeGroupedByMod(StringConstants.SHIP_CSV_TYPE);
        for (Map.Entry<String, List<IndexedFile>> entry : shipCsvsByMod.entrySet()) {
            Path folderPath = SettingsManager.getFolderForModId(entry.getKey());
            if (folderPath == null) continue;
            Settings settings = SettingsManager.getSettings();
            GameDataPackage dataPackage = settings.getPackage(folderPath);
            if (dataPackage != null && dataPackage.isDisabled()) continue;
            if (LibModFilter.isLibMod(folderPath)) continue;

            IndexedFile shipCsvFile = entry.getValue().isEmpty() ? null : entry.getValue().get(0);
            Pair<Path, List<ShipCSVEntry>> packageShipData = LoadShipDataAction.walkHullFolder(
                    entry.getKey(), folderPath.toString(), allSkins, shipCsvFile, allShipEntries);
            if (packageShipData != null) {
                allEntriesByPackage.put(packageShipData.getFirst(), packageShipData.getSecond());
            }
        }

        return () -> {
            GameDataRepository gameData = SettingsManager.getGameData();
            gameData.setAllShipEntries(allShipEntries);
            gameData.setShipEntriesByPackage(allEntriesByPackage);
            gameData.setShipDataLoaded(true);
            EventBus.publish(new HullTreeEntryCleared());
            EventBus.publish(new HullTreeReloadQueued());
        };
    }

    private static Runnable collectVariants() {
        // Query all variants from DB grouped by mod — no need to enumerate packages via filesystem
        Map<String, List<IndexedFile>> variantsByMod = DatabaseQueryService.getFilesByTypeGroupedByMod(StringConstants.VARIANT_TYPE);

        Map<String, VariantFile> allVariants = new java.util.concurrent.ConcurrentHashMap<>();
        variantsByMod.entrySet().parallelStream().forEach(entry -> {
            Path directory = SettingsManager.getFolderForModId(entry.getKey());
            if (directory != null) {
                Settings settings = SettingsManager.getSettings();
                GameDataPackage dataPackage = settings.getPackage(directory);
                if (dataPackage == null || !dataPackage.isDisabled()) {
                    if (!LibModFilter.isLibMod(directory)) {
                        if (SettingsManager.isDeveloperModeEnabled()) {
                            log.trace(StringValues.FETCHING_VARIANT_FILES_DB, directory);
                        }

                        entry.getValue().parallelStream().forEach(dbFile -> {
                            File variantFile = dbFile.getFilePath().toFile();
                            VariantFile mapped = null;
                            if (dbFile.getParsedData() != null) {
                                try {
                                    mapped = shipeditor.parsing.FileUtilities.getConfigured().readValue(dbFile.getParsedData(), VariantFile.class);
                                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                                    if (SettingsManager.isDeveloperModeEnabled()) {
                                        log.error(StringValues.FAILED_DESERIALIZE_VARIANT_DB, variantFile.getName(), e);
                                    } else {
                                        log.error(StringValues.FAILED_DESERIALIZE_VARIANT_DB, variantFile.getName());
                                    }
                                }
                            }
                            if (mapped == null) {
                                mapped = FileLoading.loadVariantFile(variantFile);
                            }
                            
                            if (mapped != null) {
                                mapped.setContainingPackage(directory);
                                String variantId = mapped.getVariantId();
                                if (variantId == null || variantId.isEmpty()) {
                                    variantId = dbFile.getEntityId();
                                    mapped.setVariantId(variantId);
                                }
                                if (variantId != null && !variantId.isEmpty()) {
                                    allVariants.put(variantId, mapped);
                                } else {
                                    log.warn(StringValues.FAILURE_TO_LOAD_VARIANT + " (Missing variant ID for " + variantFile.getName() + ")");
                                }
                            } else if (variantFile.length() > 0) {
                                log.error(StringValues.FAILURE_TO_LOAD_VARIANT, variantFile);
                            }
                        });
                    }
                }
            }
        });

        return () -> {
            GameDataRepository gameData = SettingsManager.getGameData();
            gameData.setAllVariants(allVariants);
            gameData.rebuildVariantsByHullIndex();
        };
    }

    private static Pair<Path, List<ShipCSVEntry>> walkHullFolder(String modId, String folderPath, Map<String, SkinSpecFile> skins, IndexedFile shipCsvFile, Map<String, ShipCSVEntry> allShipEntries) {
        Path shipTablePath = Paths.get(folderPath, "data", StringConstants.HULLS, StringConstants.SHIP_DATA_CSV);
        List<Map<String, String>> csvData = null;
        if (shipCsvFile != null && shipCsvFile.getParsedData() != null) {
            try {
                csvData = shipeditor.parsing.FileUtilities.getConfigured().readValue(
                        shipCsvFile.getParsedData(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {});
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.trace(StringValues.LOADED_SHIP_CSV_DB);
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.error(StringValues.FAILED_DESERIALIZE_SHIP_CSV_DB, e);
                } else {
                    log.error(StringValues.FAILED_DESERIALIZE_SHIP_CSV_DB);
                }
            }
        }
        
        if (csvData == null) {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.trace(StringValues.PARSING_SHIP_CSV, shipTablePath);
            }
            csvData = FileLoading.parseCSVTable(shipTablePath);
        }
        
        if (csvData == null) {
            log.info(StringValues.HULL_FOLDER_NO_CSV, folderPath);
            return null;
        }
        if (SettingsManager.isDeveloperModeEnabled()) {
            log.trace(StringValues.SHIP_CSV_RETRIEVED_OK, shipTablePath);
        }

        Path packagePath = Paths.get(folderPath, "");
        List<ShipCSVEntry> entriesFromPackage = Collections.synchronizedList(new ArrayList<>());

        List<IndexedFile> dbFiles = DatabaseQueryService.getFilesByModAndTypeAsync(modId, StringConstants.SHIP_TYPE).join();
        Map<String, HullSpecFile> mappedHullSpecs = new java.util.concurrent.ConcurrentHashMap<>();
        Map<String, String> shipFiles = new java.util.concurrent.ConcurrentHashMap<>();
        dbFiles.parallelStream().forEach(dbFile -> {
            String entityId = dbFile.getEntityId();
            if (entityId != null && !entityId.isEmpty()) {
                shipFiles.put(entityId, dbFile.getFileName());
            } else {
                log.warn("Missing entity ID for hull file: {}", dbFile.getFileName());
                return;
            }
            
            File hullFile = dbFile.getFilePath().toFile();
            HullSpecFile mapped = null;
            if (dbFile.getParsedData() != null) {
                try {
                    mapped = shipeditor.parsing.FileUtilities.getConfigured().readValue(dbFile.getParsedData(), HullSpecFile.class);
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    if (SettingsManager.isDeveloperModeEnabled()) {
                        log.error(StringValues.FAILED_DESERIALIZE_HULL_SPEC_DB, hullFile.getName(), e);
                    } else {
                        log.error(StringValues.FAILED_DESERIALIZE_HULL_SPEC_DB, hullFile.getName());
                    }
                }
            }
            if (mapped == null) {
                mapped = FileLoading.loadHullFile(hullFile);
            }
            if (mapped != null) {
                mapped.setFilePath(hullFile.toPath());
                mapped.setTableFilePath(shipTablePath);
                mappedHullSpecs.put(entityId, mapped);
                GameDataRepository.putSpec(mapped);
            }
        });

        csvData.parallelStream().forEach(row -> {
            String rowId = row.get("id");
            if (rowId != null && !rowId.isEmpty() && !rowId.startsWith("#")) {
                String fileName = shipFiles.get(rowId);
                if (fileName != null && !fileName.isEmpty()) {
                    HullSpecFile hullSpec = mappedHullSpecs.get(rowId);
                    
                    Map<String, SkinSpecFile> entrySkins = new HashMap<>();
                    if (hullSpec != null) {
                        for (Map.Entry<String, SkinSpecFile> skinEntry : skins.entrySet()) {
                            SkinSpecFile skinSpec = skinEntry.getValue();
                            if (Objects.equals(skinSpec.getBaseHullId(), rowId)) {
                                entrySkins.put(skinEntry.getKey(), skinSpec);
                            }
                        }
                    }
                    
                    Map.Entry<HullSpecFile, Map<String, SkinSpecFile>> hullWithSkins = null;
                    if (hullSpec != null) {
                        hullWithSkins = new java.util.AbstractMap.SimpleEntry<>(hullSpec, entrySkins);
                    }
                    
                    ShipCSVEntry newEntry = new ShipCSVEntry(row, hullWithSkins, packagePath, fileName, shipTablePath);
                    entriesFromPackage.add(newEntry);
                    allShipEntries.put(rowId, newEntry);
                }
            }
        });

        return new Pair<>(packagePath, new ArrayList<>(entriesFromPackage));
    }

    private static Map<String, SkinSpecFile> walkSkinFolder(Path skinFolder) {
        if (SettingsManager.isDeveloperModeEnabled()) {
            log.trace(StringValues.FETCHING_SKIN_FILES_DB, skinFolder);
        }
        
        Path fileNamePath = skinFolder.getFileName();
        if (fileNamePath == null) return new java.util.concurrent.ConcurrentHashMap<>();
        String modId = fileNamePath.toString();
        if (SettingsManager.isCoreFolder(skinFolder)) {
            modId = "starsector-core";
        }
        
        List<IndexedFile> dbFiles = DatabaseQueryService.getFilesByModAndTypeAsync(modId, StringConstants.SKIN_TYPE).join();
        Map<String, SkinSpecFile> mappedSkins = new java.util.concurrent.ConcurrentHashMap<>();
        
        dbFiles.parallelStream().forEach(dbFile -> {
            File skinFile = dbFile.getFilePath().toFile();
            SkinSpecFile mapped = null;
            if (dbFile.getParsedData() != null) {
                try {
                    mapped = shipeditor.parsing.FileUtilities.getConfigured().readValue(dbFile.getParsedData(), SkinSpecFile.class);
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    if (SettingsManager.isDeveloperModeEnabled()) {
                        log.error(StringValues.FAILED_DESERIALIZE_SKIN_SPEC_DB, skinFile.getName(), e);
                    } else {
                        log.error(StringValues.FAILED_DESERIALIZE_SKIN_SPEC_DB, skinFile.getName());
                    }
                }
            }
            if (mapped == null) {
                mapped = FileLoading.loadSkinFile(skinFile);
            }

            if (mapped != null) {
                mapped.setContainingPackage(skinFolder);
                mappedSkins.put(skinFile.getName(), mapped);
                GameDataRepository.putSpec(mapped);
            } else if (skinFile.length() > 0) {
                log.error(StringValues.FAILURE_TO_LOAD_SKIN, skinFile);
            }
        });
        if (SettingsManager.isDeveloperModeEnabled()) {
            log.trace(StringValues.FETCHED_AND_MAPPED_SKINS, mappedSkins.size());
        }
        return mappedSkins;
    }

}
