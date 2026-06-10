package shipeditor.parsing.loading;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.WeaponTreeReloadQueued;
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
        return "Weapons";
    }

    private static Runnable collectWeapons() {
        // Query weapon CSV files from DB, grouped by mod
        Map<String, List<IndexedFile>> weaponCsvsByMod = DatabaseQueryService.getFilesByTypeGroupedByMod("WEAPON_CSV");

        GameDataRepository gameData = SettingsManager.getGameData();
        Map<String, WeaponCSVEntry> allWeapons = gameData.getAllWeaponEntries();
        Map<Path, List<WeaponCSVEntry>> entryListsByPackage = new HashMap<>();

        for (Map.Entry<String, List<IndexedFile>> entry : weaponCsvsByMod.entrySet()) {
            Path folder = SettingsManager.getFolderForModId(entry.getKey());
            if (folder == null) continue;

            Settings settings = SettingsManager.getSettings();
            GameDataPackage dataPackage = settings.getPackage(folder);
            if (dataPackage != null && dataPackage.isDisabled()) continue;
            if (LibModFilter.isLibMod(folder)) continue;

            Map<String, WeaponCSVEntry> weaponsFromPackage = LoadWeaponsDataAction.walkWeaponsFolder(folder);

            if (weaponsFromPackage != null) {
                allWeapons.putAll(weaponsFromPackage);
                List<WeaponCSVEntry> entries = new ArrayList<>(weaponsFromPackage.values());
                entryListsByPackage.put(folder, entries);
            }
        }

        return () -> {
            gameData.setWeaponsDataLoaded(true);
            gameData.setWeaponEntriesByPackage(entryListsByPackage);
        };
    }

    private static Runnable collectProjectiles() {
        // Query all projectile files from DB grouped by mod
        Map<String, List<IndexedFile>> projectilesByMod = DatabaseQueryService.getFilesByTypeGroupedByMod("PROJECTILE");

        Map<String, ProjectileSpecFile> allProjectiles = new java.util.concurrent.ConcurrentHashMap<>();
        Map<Path, List<ProjectileSpecFile>> entryListsByPackage = new java.util.concurrent.ConcurrentHashMap<>();

        projectilesByMod.entrySet().parallelStream().forEach(entry -> {
            Path directory = SettingsManager.getFolderForModId(entry.getKey());
            if (directory != null) {
                Settings settings = SettingsManager.getSettings();
                GameDataPackage dataPackage = settings.getPackage(directory);
                if (dataPackage == null || !dataPackage.isDisabled()) {
                    if (!LibModFilter.isLibMod(directory)) {
                        log.trace("Fetching projectile files from database index for package: {}", directory);

                        List<ProjectileSpecFile> packageProjectiles = new ArrayList<>();
                        entry.getValue().forEach(dbFile -> {
                            File projectileFile = dbFile.getFilePath().toFile();
                            ProjectileSpecFile mapped = FileLoading.loadProjectileFile(projectileFile);
                            if (mapped != null) {
                                mapped.setContainingPackage(directory);
                                allProjectiles.put(mapped.getId(), mapped);
                                packageProjectiles.add(mapped);
                            } else if (projectileFile.length() > 0) {
                                log.error("Failure to load spec, omitting from result data: {}", projectileFile);
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

    private static Map<String, WeaponCSVEntry> walkWeaponsFolder(Path folder) {
        Path weaponTablePath = Paths.get(folder.toString(), "data", StringConstants.WEAPONS, "weapon_data.csv");

        log.trace("Parsing weapon CSV data at: {}..", weaponTablePath);
        List<Map<String, String>> csvData = FileLoading.parseCSVTable(weaponTablePath);
        log.trace("Weapon CSV data at {} retrieved successfully.", weaponTablePath);

        if (csvData == null) {
            log.info("Weapon folder without CSV table at: {}", folder.toString());
            return null;
        }

        log.trace("Fetching weapon files from database index for package: {}", folder);

        Path fileNamePath = folder.getFileName();
        if (fileNamePath == null) return null;
        String modId = fileNamePath.toString();
        if (SettingsManager.isCoreFolder(folder)) {
            modId = "starsector-core";
        }

        List<IndexedFile> dbFiles = DatabaseQueryService.getFilesByModAndTypeAsync(modId, "WEAPON").join();
        Map<String, WeaponSpecFile> mappedWeaponSpecs = new java.util.concurrent.ConcurrentHashMap<>();

        dbFiles.parallelStream().forEach(dbFile -> {
            File weaponFile = dbFile.getFilePath().toFile();
            WeaponSpecFile mapped = FileLoading.loadWeaponFile(weaponFile);
            if (mapped != null) {
                mapped.setTableFilePath(weaponTablePath);
                mapped.setContainingPackage(folder);
                mappedWeaponSpecs.put(mapped.getId(), mapped);
            }
        });
        log.trace("Fetched and mapped {} weapon files.", mappedWeaponSpecs.size());

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
                    log.trace("Weapon CSV entry does not have matching spec file, omitting from data repository. " +
                            "ID: {}", rowId);
                }
            }
        });

        return weaponEntries;
    }

}
