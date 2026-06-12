package shipeditor;

import lombok.extern.log4j.Log4j2;
import shipeditor.components.datafiles.entities.*;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.parsing.loading.IndexScannerTask;
import shipeditor.persistence.Initializations;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.ship.VariantFile;
import shipeditor.utility.Errors;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * CLI test script that validates the full database loading pipeline into UI data structures.
 * <p>
 * This tests the entire chain:
 * <ol>
 *   <li>Settings initialization and game folder detection</li>
 *   <li>Database indexing via {@link IndexScannerTask}</li>
 *   <li>Data loading via each {@code DataLoadingAction} (ships, weapons, hullmods, wings, etc.)</li>
 *   <li>Population of {@link GameDataRepository} (the data structures that feed UI trees/panels)</li>
 * </ol>
 * <p>
 * Run: {@code mvn exec:java -Dexec.mainClass="shipeditor.CliLoadingTest"}
 * <br>Or: {@code java -Djava.awt.headless=true -cp ship_editor.jar shipeditor.CliLoadingTest}
 */
@Log4j2
public class CliLoadingTest {

    private static int totalChecks = 0;
    private static int passedChecks = 0;
    private static int failedChecks = 0;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        log.info("================================================================");
        log.info("  CLI Loading Test — Database → GameDataRepository → UI Data");
        log.info("================================================================");

        try {
            // Phase 1: Initialize settings and detect game folder
            phase("Settings Initialization", () -> {
                Errors.initGlobalHandler();
                Initializations.initializeSettingsFile();
                Initializations.selectGameFolder();
            });

            check("Settings loaded", SettingsManager.getSettings() != null);
            check("Core folder configured",
                    SettingsManager.getSettings().getCoreFolderPath() != null
                            && !SettingsManager.getSettings().getCoreFolderPath().isEmpty());
            check("Core folder name set",
                    SettingsManager.getCoreFolderName() != null
                            && !SettingsManager.getCoreFolderName().isEmpty());

            log.info("  Core folder: {}", SettingsManager.getSettings().getCoreFolderPath());
            log.info("  Mod folder:  {}", SettingsManager.getSettings().getModFolderPath());

            // Phase 2: Database indexing
            phase("Database Indexing", () -> IndexScannerTask.scanAndIndexAll(false));

            // Phase 3: Full data loading (same as CliMain --validate, but we inspect the results)
            phase("Data Loading (loadGameData)", () -> {
                CompletableFuture<?> future = FileLoading.loadGameData();
                future.join();
            });

            // Phase 4: Inspect GameDataRepository
            GameDataRepository gameData = SettingsManager.getGameData();

            log.info("");
            log.info("================================================================");
            log.info("  Inspecting GameDataRepository (UI Data Structures)");
            log.info("================================================================");

            // --- Ships ---
            inspectShips(gameData);

            // --- Weapons ---
            inspectWeapons(gameData);

            // --- Hullmods ---
            inspectHullmods(gameData);

            // --- Wings ---
            inspectWings(gameData);

            // --- Ship Systems ---
            inspectShipSystems(gameData);

            // --- Variants ---
            inspectVariants(gameData);

            // --- Hull Styles ---
            inspectHullStyles(gameData);

            // --- Engine Styles ---
            inspectEngineStyles(gameData);

            // --- Loading Flags ---
            log.info("");
            log.info("── Loading Flags ──");
            check("shipDataLoaded flag", gameData.isShipDataLoaded());
            check("weaponsDataLoaded flag", gameData.isWeaponsDataLoaded());
            check("hullmodDataLoaded flag", gameData.isHullmodDataLoaded());
            check("wingDataLoaded flag", gameData.isWingDataLoaded());
            check("shipsystemDataLoaded flag", gameData.isShipsystemDataLoaded());

            // Print summary
            log.info("");
            log.info("================================================================");
            log.info("  RESULTS: {} passed, {} failed, {} total",
                    passedChecks, failedChecks, totalChecks);
            log.info("================================================================");

            if (failedChecks > 0) {
                log.error("SOME CHECKS FAILED!");
                System.exit(1);
            } else {
                log.info("ALL CHECKS PASSED ✓");
                System.exit(0);
            }

        } catch (Throwable t) {
            log.error("CLI Loading Test CRASHED with exception:", t);
            System.exit(2);
        }
    }

    // ========================================================================
    // Inspection Methods
    // ========================================================================

    private static void inspectShips(GameDataRepository gameData) {
        log.info("");
        log.info("── Ships ──");

        Map<String, ShipCSVEntry> allShips = gameData.getAllShipEntries();
        int shipCount = allShips != null ? allShips.size() : 0;
        check("Ship entries loaded (allShipEntries)", shipCount > 0);
        log.info("  Total ship entries: {}", shipCount);

        Map<Path, List<ShipCSVEntry>> byPackage = gameData.getShipEntriesByPackage();
        check("Ship entries by package populated", byPackage != null && !byPackage.isEmpty());
        if (byPackage != null) {
            log.info("  Packages with ships: {}", byPackage.size());
            for (Map.Entry<Path, List<ShipCSVEntry>> entry : byPackage.entrySet()) {
                log.info("    {} → {} ships", entry.getKey().getFileName(), entry.getValue().size());
            }
        }

        // Spot-check: verify a known core ship has valid data
        if (allShips != null && !allShips.isEmpty()) {
            ShipCSVEntry firstShip = allShips.values().iterator().next();
            check("First ship has ID", firstShip.getID() != null && !firstShip.getID().isEmpty());
            check("First ship has row data", firstShip.getRowData() != null && !firstShip.getRowData().isEmpty());
            log.info("  Sample ship: id='{}', name='{}', designation='{}'",
                    firstShip.getID(),
                    firstShip.getRowData().get("name"),
                    firstShip.getRowData().get("designation"));
        }

        // Verify spec files were loaded
        Map<String, ?> allSpecs = gameData.getAllSpecEntries();
        int specCount = allSpecs != null ? allSpecs.size() : 0;
        check("Ship spec files loaded (allSpecEntries)", specCount > 0);
        log.info("  Total spec files (hull + skin): {}", specCount);
    }

    private static void inspectWeapons(GameDataRepository gameData) {
        log.info("");
        log.info("── Weapons ──");

        Map<String, WeaponCSVEntry> allWeapons = gameData.getAllWeaponEntries();
        int weaponCount = allWeapons != null ? allWeapons.size() : 0;
        check("Weapon entries loaded (allWeaponEntries)", weaponCount > 0);
        log.info("  Total weapon entries: {}", weaponCount);

        Map<Path, List<WeaponCSVEntry>> byPackage = gameData.getWeaponEntriesByPackage();
        check("Weapon entries by package populated", byPackage != null && !byPackage.isEmpty());
        if (byPackage != null) {
            log.info("  Packages with weapons: {}", byPackage.size());
            for (Map.Entry<Path, List<WeaponCSVEntry>> entry : byPackage.entrySet()) {
                log.info("    {} → {} weapons", entry.getKey().getFileName(), entry.getValue().size());
            }
        }

        if (allWeapons != null && !allWeapons.isEmpty()) {
            WeaponCSVEntry firstWeapon = allWeapons.values().iterator().next();
            check("First weapon has ID", firstWeapon.getID() != null && !firstWeapon.getID().isEmpty());
            check("First weapon has row data", firstWeapon.getRowData() != null && !firstWeapon.getRowData().isEmpty());
            log.info("  Sample weapon: id='{}', name='{}', type='{}'",
                    firstWeapon.getID(),
                    firstWeapon.getRowData().get("name"),
                    firstWeapon.getRowData().get("type"));
        }
    }

    private static void inspectHullmods(GameDataRepository gameData) {
        log.info("");
        log.info("── Hullmods ──");

        Map<String, HullmodCSVEntry> allHullmods = gameData.getAllHullmodEntries();
        int hullmodCount = allHullmods != null ? allHullmods.size() : 0;
        check("Hullmod entries loaded (allHullmodEntries)", hullmodCount > 0);
        log.info("  Total hullmod entries: {}", hullmodCount);

        Map<Path, List<HullmodCSVEntry>> byPackage = gameData.getHullmodEntriesByPackage();
        check("Hullmod entries by package populated", byPackage != null && !byPackage.isEmpty());
        if (byPackage != null) {
            log.info("  Packages with hullmods: {}", byPackage.size());
            for (Map.Entry<Path, List<HullmodCSVEntry>> entry : byPackage.entrySet()) {
                log.info("    {} → {} hullmods", entry.getKey().getFileName(), entry.getValue().size());
            }
        }

        if (allHullmods != null && !allHullmods.isEmpty()) {
            HullmodCSVEntry firstMod = allHullmods.values().iterator().next();
            check("First hullmod has ID", firstMod.getID() != null && !firstMod.getID().isEmpty());
            log.info("  Sample hullmod: id='{}', name='{}'",
                    firstMod.getID(), firstMod.getRowData().get("name"));
        }
    }

    private static void inspectWings(GameDataRepository gameData) {
        log.info("");
        log.info("── Fighter Wings ──");

        Map<String, WingCSVEntry> allWings = gameData.getAllWingEntries();
        int wingCount = allWings != null ? allWings.size() : 0;
        check("Wing entries loaded (allWingEntries)", wingCount > 0);
        log.info("  Total wing entries: {}", wingCount);

        Map<Path, List<WingCSVEntry>> byPackage = gameData.getWingEntriesByPackage();
        check("Wing entries by package populated", byPackage != null && !byPackage.isEmpty());
        if (byPackage != null) {
            log.info("  Packages with wings: {}", byPackage.size());
            for (Map.Entry<Path, List<WingCSVEntry>> entry : byPackage.entrySet()) {
                log.info("    {} → {} wings", entry.getKey().getFileName(), entry.getValue().size());
            }
        }

        if (allWings != null && !allWings.isEmpty()) {
            WingCSVEntry firstWing = allWings.values().iterator().next();
            check("First wing has ID", firstWing.getID() != null && !firstWing.getID().isEmpty());
            log.info("  Sample wing: id='{}', variant='{}'",
                    firstWing.getID(), firstWing.getRowData().get("variant"));
        }
    }

    private static void inspectShipSystems(GameDataRepository gameData) {
        log.info("");
        log.info("── Ship Systems ──");

        Map<String, ShipSystemCSVEntry> allSystems = gameData.getAllShipsystemEntries();
        int systemCount = allSystems != null ? allSystems.size() : 0;
        check("Ship system entries loaded (allShipsystemEntries)", systemCount > 0);
        log.info("  Total ship system entries: {}", systemCount);

        Map<Path, List<ShipSystemCSVEntry>> byPackage = gameData.getShipSystemEntriesByPackage();
        check("Ship system entries by package populated", byPackage != null && !byPackage.isEmpty());
        if (byPackage != null) {
            log.info("  Packages with ship systems: {}", byPackage.size());
            for (Map.Entry<Path, List<ShipSystemCSVEntry>> entry : byPackage.entrySet()) {
                log.info("    {} → {} systems", entry.getKey().getFileName(), entry.getValue().size());
            }
        }

        if (allSystems != null && !allSystems.isEmpty()) {
            ShipSystemCSVEntry firstSystem = allSystems.values().iterator().next();
            check("First ship system has ID", firstSystem.getID() != null && !firstSystem.getID().isEmpty());
            log.info("  Sample system: id='{}', name='{}'",
                    firstSystem.getID(), firstSystem.getRowData().get("name"));
        }
    }

    private static void inspectVariants(GameDataRepository gameData) {
        log.info("");
        log.info("── Variants ──");

        Map<String, VariantFile> allVariants = gameData.getAllVariants();
        int variantCount = allVariants != null ? allVariants.size() : 0;
        check("Variant files loaded (allVariants)", variantCount > 0);
        log.info("  Total variant files: {}", variantCount);

        if (allVariants != null && !allVariants.isEmpty()) {
            VariantFile firstVariant = allVariants.values().iterator().next();
            check("First variant has variantId",
                    firstVariant.getVariantId() != null && !firstVariant.getVariantId().isEmpty());
            check("First variant has hullId",
                    firstVariant.getHullId() != null && !firstVariant.getHullId().isEmpty());
            log.info("  Sample variant: id='{}', hullId='{}', displayName='{}'",
                    firstVariant.getVariantId(),
                    firstVariant.getHullId(),
                    firstVariant.getDisplayName());
        }
    }

    private static void inspectHullStyles(GameDataRepository gameData) {
        log.info("");
        log.info("── Hull Styles ──");

        var allHullStyles = gameData.getAllHullStyles();
        int styleCount = allHullStyles != null ? allHullStyles.size() : 0;
        check("Hull styles loaded (allHullStyles)", styleCount > 0);
        log.info("  Total hull styles: {}", styleCount);

        if (allHullStyles != null && !allHullStyles.isEmpty()) {
            String firstStyleKey = allHullStyles.keySet().iterator().next();
            log.info("  Sample hull style: '{}'", firstStyleKey);
        }
    }

    private static void inspectEngineStyles(GameDataRepository gameData) {
        log.info("");
        log.info("── Engine Styles ──");

        var allEngineStyles = gameData.getAllEngineStyles();
        int styleCount = allEngineStyles != null ? allEngineStyles.size() : 0;
        check("Engine styles loaded (allEngineStyles)", styleCount > 0);
        log.info("  Total engine styles: {}", styleCount);

        if (allEngineStyles != null && !allEngineStyles.isEmpty()) {
            String firstStyleKey = allEngineStyles.keySet().iterator().next();
            log.info("  Sample engine style: '{}'", firstStyleKey);
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private static void phase(String name, Runnable action) {
        log.info("");
        log.info("── Phase: {} ──", name);
        long start = System.currentTimeMillis();
        try {
            action.run();
            long elapsed = System.currentTimeMillis() - start;
            log.info("  ✓ {} completed in {} ms", name, elapsed);
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("  ✗ {} FAILED after {} ms: {}", name, elapsed, t.getMessage(), t);
            throw t;
        }
    }

    private static void check(String description, boolean condition) {
        totalChecks++;
        if (condition) {
            passedChecks++;
            log.info("  [PASS] {}", description);
        } else {
            failedChecks++;
            log.error("  [FAIL] {}", description);
        }
    }
}
