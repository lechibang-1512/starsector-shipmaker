package shipeditor.parsing.loading;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.weapon.ProjectileSpecFile;
import shipeditor.representation.weapon.WeaponSpecFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;
import shipeditor.communication.events.files.FileEvents.WeaponTreeReloadQueued;

@Log4j2
public class LoadWeaponsDataAction extends DataLoadingAction {

    @Override
    public Runnable perform() {
        CompletableFuture<Runnable> weaponData = CompletableFuture.supplyAsync(LoadWeaponsDataAction::collectWeapons);
        CompletableFuture<Runnable> projectileData = CompletableFuture.supplyAsync(LoadWeaponsDataAction::collectProjectiles);

        CompletableFuture<Runnable> combinedResult = weaponData.thenCombine(projectileData,
                (weaponsRunnable, projectilesRunnable) ->
                        () -> {
                            weaponsRunnable.run();
                            projectilesRunnable.run();
                        }
        );

        return combinedResult.join();
    }

    @Override
    public String getTaskName() {
        return StringValues.TASK_WEAPONS;
    }

    private static Runnable collectWeapons() {
        // Query weapon CSV files from DB, grouped by mod
        Map<String, List<IndexedFile>> weaponCsvsByMod = DatabaseQueryService.getFilesByTypeGroupedByMod(StringConstants.WEAPON_CSV_TYPE);

        Map<String, WeaponCSVEntry> allWeapons = new java.util.concurrent.ConcurrentHashMap<>();
        Map<Path, List<WeaponCSVEntry>> entryListsByPackage = new HashMap<>();

        for (Map.Entry<String, List<IndexedFile>> entry : weaponCsvsByMod.entrySet()) {
            Path folder = SettingsManager.getFolderForModId(entry.getKey());
            if (folder == null) continue;

            Settings settings = SettingsManager.getSettings();
            GameDataPackage dataPackage = settings.getPackage(folder);
            if (dataPackage != null && dataPackage.isDisabled()) continue;
            if (LibModFilter.isLibMod(folder)) continue;

            IndexedFile weaponCsvFile = entry.getValue().isEmpty() ? null : entry.getValue().get(0);
            Map<String, WeaponCSVEntry> weaponsFromPackage = LoadWeaponsDataAction.walkWeaponsFolder(folder, weaponCsvFile, entry.getKey());

            if (weaponsFromPackage != null) {
                allWeapons.putAll(weaponsFromPackage);
                List<WeaponCSVEntry> entries = new ArrayList<>(weaponsFromPackage.values());
                entryListsByPackage.put(folder, entries);
            }
        }

        return () -> {
            GameDataRepository gameData = SettingsManager.getGameData();
            gameData.setAllWeaponEntries(allWeapons);
            gameData.setWeaponsDataLoaded(true);
            gameData.setWeaponEntriesByPackage(entryListsByPackage);
        };
    }

    private static Runnable collectProjectiles() {
        // Query all projectile files from DB grouped by mod
        Map<String, List<IndexedFile>> projectilesByMod = DatabaseQueryService.getFilesByTypeGroupedByMod(StringConstants.PROJECTILE_TYPE);

        Map<String, ProjectileSpecFile> allProjectiles = new java.util.concurrent.ConcurrentHashMap<>();
        Map<Path, List<ProjectileSpecFile>> entryListsByPackage = new java.util.concurrent.ConcurrentHashMap<>();

        projectilesByMod.entrySet().parallelStream().forEach(entry -> {
            Path directory = SettingsManager.getFolderForModId(entry.getKey());
            if (directory != null) {
                Settings settings = SettingsManager.getSettings();
                GameDataPackage dataPackage = settings.getPackage(directory);
                if (dataPackage == null || !dataPackage.isDisabled()) {
                    if (!LibModFilter.isLibMod(directory)) {
                        if (SettingsManager.isDeveloperModeEnabled()) {
                            log.trace(StringValues.FETCHING_PROJECTILE_FILES_DB, directory);
                        }

                        List<ProjectileSpecFile> packageProjectiles = new ArrayList<>();
                        entry.getValue().forEach(dbFile -> {
                            File projectileFile = dbFile.getFilePath().toFile();
                            ProjectileSpecFile mapped = null;
                            if (dbFile.getParsedData() != null) {
                                try {
                                    mapped = shipeditor.parsing.FileUtilities.getConfigured().readValue(dbFile.getParsedData(), ProjectileSpecFile.class);
                                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                                    if (SettingsManager.isDeveloperModeEnabled()) {
                                        log.error(StringValues.FAILED_DESERIALIZE_PROJECTILE_DB, projectileFile.getName(), e);
                                    } else {
                                        log.error(StringValues.FAILED_DESERIALIZE_PROJECTILE_DB, projectileFile.getName());
                                    }
                                }
                            }
                            if (mapped == null) {
                                mapped = FileLoading.loadProjectileFile(projectileFile);
                            }
                            if (mapped != null) {
                                mapped.setContainingPackage(directory);
                                String projId = mapped.getId();
                                if (projId == null || projId.isEmpty()) {
                                    projId = dbFile.getEntityId();
                                    mapped.setId(projId);
                                }
                                if (projId != null && !projId.isEmpty()) {
                                    allProjectiles.put(projId, mapped);
                                    packageProjectiles.add(mapped);
                                } else {
                                    log.warn(StringValues.FAILURE_TO_LOAD_SPEC + " (Missing projectile ID for " + projectileFile.getName() + ")");
                                }
                            } else if (projectileFile.length() > 0) {
                                log.error(StringValues.FAILURE_TO_LOAD_SPEC, projectileFile);
                            }
                        });
                        if (!packageProjectiles.isEmpty()) {
                            entryListsByPackage.put(directory, packageProjectiles);
                        }
                    }
                }
            }
        });

        return () -> {
            GameDataRepository gameData = SettingsManager.getGameData();
            gameData.setAllProjectiles(allProjectiles);
            gameData.setProjectileEntriesByPackage(entryListsByPackage);

            EventBus.publish(new WeaponTreeReloadQueued());
        };
    }

    private static Map<String, WeaponCSVEntry> walkWeaponsFolder(Path folder, IndexedFile weaponCsvFile, String modId) {
        Path weaponTablePath = Paths.get(folder.toString(), "data", StringConstants.WEAPONS, "weapon_data.csv");

        List<Map<String, String>> csvData = null;
        if (weaponCsvFile != null && weaponCsvFile.getParsedData() != null) {
            try {
                csvData = shipeditor.parsing.FileUtilities.getConfigured().readValue(
                        weaponCsvFile.getParsedData(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {});
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.trace(StringValues.CSV_LOADED_DB_CACHE);
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.error(StringValues.CSV_DESERIALIZE_DB_CACHE_FAILED, e);
                } else {
                    log.error(StringValues.CSV_DESERIALIZE_DB_CACHE_FAILED);
                }
            }
        }

        if (csvData == null) {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.trace(StringValues.PARSING_SHIP_CSV, weaponTablePath);
            }
            csvData = FileLoading.parseCSVTable(weaponTablePath);
        }
        if (SettingsManager.isDeveloperModeEnabled()) {
            log.trace(StringValues.WEAPON_CSV_RETRIEVED_OK, weaponTablePath);
        }

        if (csvData == null) {
            log.info(StringValues.WEAPON_FOLDER_NO_CSV, folder.toString());
            return null;
        }

        if (SettingsManager.isDeveloperModeEnabled()) {
            log.trace(StringValues.FETCHING_WEAPON_FILES_DB, folder);
        }


        List<IndexedFile> dbFiles = DatabaseQueryService.getFilesByModAndTypeAsync(modId, StringConstants.WEAPON_TYPE).join();
        Map<String, WeaponSpecFile> mappedWeaponSpecs = new java.util.concurrent.ConcurrentHashMap<>();

        dbFiles.parallelStream().forEach(dbFile -> {
            File weaponFile = dbFile.getFilePath().toFile();
            WeaponSpecFile mapped = null;
            if (dbFile.getParsedData() != null) {
                try {
                    mapped = shipeditor.parsing.FileUtilities.getConfigured().readValue(dbFile.getParsedData(), WeaponSpecFile.class);
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    if (SettingsManager.isDeveloperModeEnabled()) {
                        log.error(StringValues.FAILED_DESERIALIZE_WEAPON_SPEC_DB, weaponFile.getName(), e);
                    } else {
                        log.error(StringValues.FAILED_DESERIALIZE_WEAPON_SPEC_DB, weaponFile.getName());
                    }
                }
            }
            if (mapped == null) {
                mapped = FileLoading.loadWeaponFile(weaponFile);
            }
            
            if (mapped != null) {
                mapped.setTableFilePath(weaponTablePath);
                mapped.setContainingPackage(folder);
                String weaponId = mapped.getId();
                if (weaponId == null || weaponId.isEmpty()) {
                    weaponId = dbFile.getEntityId();
                    mapped.setId(weaponId);
                }
                if (weaponId != null && !weaponId.isEmpty()) {
                    mappedWeaponSpecs.put(weaponId, mapped);
                } else {
                    log.warn(StringValues.FAILED_DESERIALIZE_WEAPON_SPEC_DB + " (Missing weapon ID for " + weaponFile.getName() + ")");
                }
            }
        });
        if (SettingsManager.isDeveloperModeEnabled()) {
            log.trace(StringValues.FETCHED_AND_MAPPED_WEAPONS, mappedWeaponSpecs.size());
        }

        Map<String, WeaponCSVEntry> weaponEntries = new java.util.concurrent.ConcurrentHashMap<>();
        csvData.parallelStream().forEach(row -> {
            String rowId = row.get("id");
            if (rowId != null && !rowId.isEmpty()) {
                WeaponCSVEntry newEntry = new WeaponCSVEntry(row, folder, weaponTablePath);
                WeaponSpecFile matching = mappedWeaponSpecs.get(rowId);
                if (matching != null) {
                    newEntry.setSpecFile(matching);
                    weaponEntries.put(rowId, newEntry);
                } else {
                    if (SettingsManager.isDeveloperModeEnabled()) {
                        log.trace(StringValues.WEAPON_CSV_ENTRY_NO_SPEC, rowId);
                    }
                }
            }
        });

        return weaponEntries;
    }

}
