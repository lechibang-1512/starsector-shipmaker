package shipeditor.representation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.extern.log4j.Log4j2;
import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.EventBus;
import shipeditor.components.datafiles.entities.*;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.ship.*;
import shipeditor.representation.weapon.ProjectileSpecFile;

import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import shipeditor.communication.events.files.FileEvents.HullmodDataSet;
import shipeditor.communication.events.files.FileEvents.WingDataSet;
import java.nio.file.Paths;

@Log4j2
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods", "StaticMethodOnlyUsedInOneClass"})
@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class GameDataRepository {

    /**
     * All ship entries by their hull IDs.
     */
    @Setter
    private volatile Map<String, ShipCSVEntry> allShipEntries;

    /**
     * Base hull and skin entries by their ship hull IDs. Used when layer needs to be loaded from variant ID.
     */
    private final Map<String, ShipSpecFile> allSpecEntries;

    /**
     * All hullmod entries by their IDs.
     */
    private volatile Map<String, HullmodCSVEntry> allHullmodEntries;

    /**
     * All shipsystem entries by their IDs.
     */
    private volatile Map<String, ShipSystemCSVEntry> allShipsystemEntries;

    private volatile Map<String, WingCSVEntry> allWingEntries;

    @Setter
    private volatile Map<String, WeaponCSVEntry> allWeaponEntries;

    /**
     * Holds the same instances as id-entry collection, used for quick repopulating of entry tree with filtering.
     */
    private volatile Map<Path, List<ShipCSVEntry>> shipEntriesByPackage;

    private volatile Map<Path, List<WeaponCSVEntry>> weaponEntriesByPackage;

    private volatile Map<Path, List<ProjectileSpecFile>> projectileEntriesByPackage;

    private volatile Map<Path, List<HullmodCSVEntry>> hullmodEntriesByPackage;

    private volatile Map<Path, List<ShipSystemCSVEntry>> shipSystemEntriesByPackage;

    private volatile Map<Path, List<WingCSVEntry>> wingEntriesByPackage;

    public static class CachedCSVData {
        private final List<Map<String, String>> rawData;
        private final Object schema;

        public CachedCSVData(List<Map<String, String>> rawData, Object schema) {
            this.rawData = rawData;
            this.schema = schema;
        }

        public List<Map<String, String>> getRawData() {
            return rawData;
        }

        public Object getSchema() {
            return schema;
        }
    }

    @lombok.Getter(lombok.AccessLevel.NONE)
    private final Map<Path, SoftReference<CachedCSVData>> csvCacheByPath = new ConcurrentHashMap<>();

    public void putRawCSVDataForPath(Path path, List<Map<String, String>> rawData) {
        csvCacheByPath.compute(path, (k, ref) -> {
            CachedCSVData existing = ref != null ? ref.get() : null;
            Object schema = existing != null ? existing.getSchema() : null;
            return new SoftReference<>(new CachedCSVData(rawData, schema));
        });
    }

    public void putCsvSchemaForPath(Path path, Object schema) {
        csvCacheByPath.compute(path, (k, ref) -> {
            CachedCSVData existing = ref != null ? ref.get() : null;
            List<Map<String, String>> rawData = existing != null ? existing.getRawData() : null;
            return new SoftReference<>(new CachedCSVData(rawData, schema));
        });
    }

    public void putCachedCSVData(Path path, List<Map<String, String>> rawData, Object schema) {
        csvCacheByPath.put(path, new SoftReference<>(new CachedCSVData(rawData, schema)));
    }

    /**
     * Retrieves raw CSV data for the given path. If the SoftReference was cleared by GC,
     * transparently re-parses the CSV from disk.
     */
    public List<Map<String, String>> getRawCSVDataForPath(Path path) {
        SoftReference<CachedCSVData> ref = csvCacheByPath.get(path);
        if (ref != null) {
            CachedCSVData cached = ref.get();
            if (cached != null && cached.getRawData() != null) {
                return cached.getRawData();
            }
        }
        // Re-parse from disk on cache miss.
        List<Map<String, String>> reparsed = shipeditor.parsing.loading.FileLoading.reparseCSVForPath(path);
        if (reparsed != null) {
            SoftReference<CachedCSVData> refreshedRef = csvCacheByPath.get(path);
            CachedCSVData refreshed = refreshedRef != null ? refreshedRef.get() : null;
            if (refreshed != null && refreshed.getRawData() != null) {
                return refreshed.getRawData();
            }
        }
        return reparsed;
    }

    /**
     * Retrieves the CSV schema for the given path. If the SoftReference was cleared by GC,
     * transparently re-parses to recover the schema.
     */
    public Object getCsvSchemaForPath(Path path) {
        SoftReference<CachedCSVData> ref = csvCacheByPath.get(path);
        if (ref != null) {
            CachedCSVData cached = ref.get();
            if (cached != null && cached.getSchema() != null) {
                return cached.getSchema();
            }
        }
        // Re-parse from disk to recover the schema.
        shipeditor.parsing.loading.FileLoading.reparseCSVForPath(path);
        SoftReference<CachedCSVData> refreshedRef = csvCacheByPath.get(path);
        CachedCSVData refreshed = refreshedRef != null ? refreshedRef.get() : null;
        return refreshed != null ? refreshed.getSchema() : null;
    }

    /**
     * Hull styles by their IDs (field names in JSON).
     */
    @Setter
    private volatile Map<String, HullStyle> allHullStyles;

    /**
     * Engine styles by their IDs (field names in JSON).
     */
    @Setter
    private volatile Map<String, EngineStyle> allEngineStyles;

    /**
     * All variant files by variant IDs.
     */
    @Setter
    private volatile Map<String, VariantFile> allVariants;

    /**
     * Reverse index: hull ID → map of variant ID → VariantFile. Rebuilt when allVariants is set.
     */
    private volatile Map<String, Map<String, VariantFile>> variantsByHullID = new ConcurrentHashMap<>();

    /**
     * All projectile files by variant IDs.
     */
    @Setter
    private volatile Map<String, ProjectileSpecFile> allProjectiles;

    @Setter
    private volatile boolean shipDataLoaded;

    private volatile boolean hullmodDataLoaded;

    @Setter
    private volatile boolean shipsystemDataLoaded;

    private volatile boolean wingDataLoaded;

    @Setter
    private volatile boolean weaponsDataLoaded;

    public GameDataRepository() {
        this.allSpecEntries = new ConcurrentHashMap<>();
    }



    private static final com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>> LIST_MAP_TYPE =
            new com.fasterxml.jackson.core.type.TypeReference<>() {};

    public <T extends CSVEntry> Map<Path, List<T>> loadCsvEntriesByPackage(Path relativePath, CsvEntryFactory<T> factory) {
        Map<Path, List<T>> result = new LinkedHashMap<>();
        List<Path> searchFolders = new java.util.ArrayList<>();
        Path coreFolder = SettingsManager.getCoreFolderPath();
        if (coreFolder != null) {
            searchFolders.add(coreFolder);
        }
        searchFolders.addAll(SettingsManager.getAllModFolders());
        com.fasterxml.jackson.databind.ObjectMapper mapper = SettingsManager.getMapperForSettingsFile();
        for (Path modFolder : searchFolders) {
            Path modFolderName = modFolder.getFileName();
            if (modFolderName == null) {
                continue;
            }
            String modId = modFolderName.toString();
            if (!SettingsManager.isModActive(modId) && !SettingsManager.isCoreFolder(modFolder)) {
                continue;
            }
            if (shipeditor.parsing.loading.LibModFilter.isLibMod(modFolder)) {
                continue;
            }
            Path csvPath = modFolder.resolve(relativePath);
            if (java.nio.file.Files.exists(csvPath)) {
                List<Map<String, String>> rows = loadCsvRowsWithCache(csvPath, modId, mapper);
                if (rows != null) {
                    List<T> entries = new java.util.ArrayList<>();
                    for (Map<String, String> row : rows) {
                        entries.add(factory.create(row, modFolder, csvPath));
                    }
                    if (!entries.isEmpty()) {
                        result.put(modFolder, entries);
                    }
                }
            }
        }
        return result;
    }

    private List<Map<String, String>> loadCsvRowsWithCache(Path csvPath, String modId,
                                                            com.fasterxml.jackson.databind.ObjectMapper mapper) {
        long diskModified = csvPath.toFile().lastModified();
        shipeditor.persistence.database.DatabaseQueryService.CsvCacheRow cached =
                shipeditor.persistence.database.DatabaseQueryService.getCsvCache(csvPath);

        if (cached != null && cached.lastModified() == diskModified) {
            // Cache hit — deserialize JSON rows from SQLite instead of re-parsing CSV from disk.
            try {
                return mapper.readValue(cached.rowsJson(), LIST_MAP_TYPE);
            } catch (Exception e) {
                log.warn("Failed to deserialize CSV cache for {}, falling back to disk", csvPath, e);
            }
        }

        // Cache miss or stale — parse CSV from disk and persist to cache.
        List<Map<String, String>> rows = getRawCSVDataForPath(csvPath);
        if (rows != null) {
            try {
                String rowsJson = mapper.writeValueAsString(rows);
                Path parent = csvPath.getParent();
                String parentStr = parent != null ? parent.toAbsolutePath().toString() : "";
                shipeditor.persistence.database.DatabaseQueryService.ensureModExists(modId, modId, parentStr);
                shipeditor.persistence.database.DatabaseQueryService.upsertCsvCache(csvPath, modId, diskModified, rowsJson);
            } catch (Exception e) {
                log.warn("Failed to write CSV cache for {}", csvPath, e);
            }
        }
        return rows;
    }

    public Map<String, ShipCSVEntry> getAllShipEntries() {
        if (allShipEntries == null) {
            synchronized(this) {
                if (allShipEntries == null) {
                    Map<String, ShipCSVEntry> local = new ConcurrentHashMap<>();
                    for (List<ShipCSVEntry> list : getShipEntriesByPackage().values()) {
                        for (ShipCSVEntry entry : list) {
                            if (entry.getID() != null && !entry.getID().isEmpty()) {
                                local.put(entry.getID(), entry);
                            }
                        }
                    }
                    allShipEntries = local;
                }
            }
        }
        return allShipEntries;
    }

    public Map<Path, List<ShipCSVEntry>> getShipEntriesByPackage() {
        if (shipEntriesByPackage == null) {
            synchronized(this) {
                if (shipEntriesByPackage == null) {
                    shipEntriesByPackage = loadCsvEntriesByPackage(Paths.get("data", "hulls", "ship_data.csv"), (r, f, p) -> new ShipCSVEntry(r, null, f, "ship_data.csv", p));
                }
            }
        }
        return shipEntriesByPackage;
    }

    public Map<String, WeaponCSVEntry> getAllWeaponEntries() {
        if (allWeaponEntries == null) {
            synchronized(this) {
                if (allWeaponEntries == null) {
                    Map<String, WeaponCSVEntry> local = new ConcurrentHashMap<>();
                    for (List<WeaponCSVEntry> list : getWeaponEntriesByPackage().values()) {
                        for (WeaponCSVEntry entry : list) {
                            if (entry.getID() != null && !entry.getID().isEmpty()) {
                                local.put(entry.getID(), entry);
                            }
                        }
                    }
                    allWeaponEntries = local;
                }
            }
        }
        return allWeaponEntries;
    }

    public Map<Path, List<WeaponCSVEntry>> getWeaponEntriesByPackage() {
        if (weaponEntriesByPackage == null) {
            synchronized(this) {
                if (weaponEntriesByPackage == null) {
                    weaponEntriesByPackage = loadCsvEntriesByPackage(Paths.get("data", "weapons", "weapon_data.csv"), (r, f, p) -> new WeaponCSVEntry(r, f, p));
                }
            }
        }
        return weaponEntriesByPackage;
    }

    public Map<String, HullmodCSVEntry> getAllHullmodEntries() {
        if (allHullmodEntries == null) {
            synchronized(this) {
                if (allHullmodEntries == null) {
                    Map<String, HullmodCSVEntry> local = new ConcurrentHashMap<>();
                    for (List<HullmodCSVEntry> list : getHullmodEntriesByPackage().values()) {
                        for (HullmodCSVEntry entry : list) {
                            if (entry.getID() != null && !entry.getID().isEmpty()) {
                                local.put(entry.getID(), entry);
                            }
                        }
                    }
                    allHullmodEntries = local;
                }
            }
        }
        return allHullmodEntries;
    }

    public Map<Path, List<HullmodCSVEntry>> getHullmodEntriesByPackage() {
        if (hullmodEntriesByPackage == null) {
            synchronized(this) {
                if (hullmodEntriesByPackage == null) {
                    hullmodEntriesByPackage = loadCsvEntriesByPackage(Paths.get("data", "hullmods", "hull_mods.csv"), (r, f, p) -> new HullmodCSVEntry(r, f, p));
                }
            }
        }
        return hullmodEntriesByPackage;
    }

    public Map<String, ShipSystemCSVEntry> getAllShipsystemEntries() {
        if (allShipsystemEntries == null) {
            synchronized(this) {
                if (allShipsystemEntries == null) {
                    Map<String, ShipSystemCSVEntry> local = new ConcurrentHashMap<>();
                    for (List<ShipSystemCSVEntry> list : getShipSystemEntriesByPackage().values()) {
                        for (ShipSystemCSVEntry entry : list) {
                            if (entry.getID() != null && !entry.getID().isEmpty()) {
                                local.put(entry.getID(), entry);
                            }
                        }
                    }
                    allShipsystemEntries = local;
                }
            }
        }
        return allShipsystemEntries;
    }

    public Map<Path, List<ShipSystemCSVEntry>> getShipSystemEntriesByPackage() {
        if (shipSystemEntriesByPackage == null) {
            synchronized(this) {
                if (shipSystemEntriesByPackage == null) {
                    shipSystemEntriesByPackage = loadCsvEntriesByPackage(Paths.get("data", "shipsystems", "ship_systems.csv"), (r, f, p) -> new ShipSystemCSVEntry(r, f, p));
                }
            }
        }
        return shipSystemEntriesByPackage;
    }

    public Map<String, WingCSVEntry> getAllWingEntries() {
        if (allWingEntries == null) {
            synchronized(this) {
                if (allWingEntries == null) {
                    Map<String, WingCSVEntry> local = new ConcurrentHashMap<>();
                    for (List<WingCSVEntry> list : getWingEntriesByPackage().values()) {
                        for (WingCSVEntry entry : list) {
                            if (entry.getID() != null && !entry.getID().isEmpty()) {
                                local.put(entry.getID(), entry);
                            }
                        }
                    }
                    allWingEntries = local;
                }
            }
        }
        return allWingEntries;
    }

    public Map<Path, List<WingCSVEntry>> getWingEntriesByPackage() {
        if (wingEntriesByPackage == null) {
            synchronized(this) {
                if (wingEntriesByPackage == null) {
                    wingEntriesByPackage = loadCsvEntriesByPackage(Paths.get("data", "hulls", "wing_data.csv"), (r, f, p) -> new WingCSVEntry(r, f, p));
                }
            }
        }
        return wingEntriesByPackage;
    }

    public void reset() {
        if (allSpecEntries != null) allSpecEntries.clear();
        if (allShipEntries != null) allShipEntries.clear();
        if (allHullmodEntries != null) allHullmodEntries.clear();
        if (allShipsystemEntries != null) allShipsystemEntries.clear();
        if (allWingEntries != null) allWingEntries.clear();
        if (allWeaponEntries != null) allWeaponEntries.clear();
        allVariants = null;
        if (allProjectiles != null) allProjectiles.clear();

        shipEntriesByPackage = null;
        weaponEntriesByPackage = null;
        projectileEntriesByPackage = null;
        hullmodEntriesByPackage = null;
        shipSystemEntriesByPackage = null;
        wingEntriesByPackage = null;

        shipDataLoaded = false;
        hullmodDataLoaded = false;
        shipsystemDataLoaded = false;
        wingDataLoaded = false;
        weaponsDataLoaded = false;
    }

    public void setShipEntriesByPackage(Map<Path, List<ShipCSVEntry>> shipEntries) {
        this.shipEntriesByPackage = shipEntries;
        if (shipEntries != null) {
            SettingsManager.announcePackages(shipEntries);
        }
    }

    public void setWeaponEntriesByPackage(Map<Path, List<WeaponCSVEntry>> weaponEntries) {
        this.weaponEntriesByPackage = weaponEntries;
        if (weaponEntries != null) {
            SettingsManager.announcePackages(weaponEntries);
        }
    }

    public void setProjectileEntriesByPackage(Map<Path, List<ProjectileSpecFile>> projectileEntries) {
        this.projectileEntriesByPackage = projectileEntries;
        if (projectileEntries != null) {
            SettingsManager.announcePackages(projectileEntries);
        }
    }

    public void setHullmodEntriesByPackage(Map<Path, List<HullmodCSVEntry>> hullmodEntries) {
        this.hullmodEntriesByPackage = hullmodEntries;
        Map<String, HullmodCSVEntry> newAllHullmodEntries = new ConcurrentHashMap<>();
        if (hullmodEntries != null) {
            hullmodEntries.values().forEach(list -> list.forEach(entry -> newAllHullmodEntries.put(entry.getID(), entry)));
            SettingsManager.announcePackages(hullmodEntries);
        }
        this.allHullmodEntries = newAllHullmodEntries;
    }

    public void setShipSystemEntriesByPackage(Map<Path, List<ShipSystemCSVEntry>> shipSystemEntries) {
        this.shipSystemEntriesByPackage = shipSystemEntries;
        Map<String, ShipSystemCSVEntry> newAllShipsystemEntries = new ConcurrentHashMap<>();
        if (shipSystemEntries != null) {
            shipSystemEntries.values().forEach(list -> list.forEach(entry -> newAllShipsystemEntries.put(entry.getID(), entry)));
            SettingsManager.announcePackages(shipSystemEntries);
        }
        this.allShipsystemEntries = newAllShipsystemEntries;
    }

    public void setWingEntriesByPackage(Map<Path, List<WingCSVEntry>> wingEntries) {
        this.wingEntriesByPackage = wingEntries;
        Map<String, WingCSVEntry> newAllWingEntries = new ConcurrentHashMap<>();
        if (wingEntries != null) {
            wingEntries.values().forEach(list -> list.forEach(entry -> newAllWingEntries.put(entry.getID(), entry)));
            SettingsManager.announcePackages(wingEntries);
        }
        this.allWingEntries = newAllWingEntries;
    }

    public static ShipCSVEntry retrieveShipCSVEntryByID(String baseHullID) {
        if (baseHullID == null) {
            return null;
        }
        GameDataRepository dataRepository = SettingsManager.getGameData();
        if (dataRepository == null) return null;
        var shipEntries = dataRepository.getAllShipEntries();
        return shipEntries != null ? shipEntries.get(baseHullID) : null;
    }

    public static HullmodCSVEntry retrieveHullmodCSVEntryByID(String hullmodID) {
        if (hullmodID == null) {
            return null;
        }
        GameDataRepository dataRepository = SettingsManager.getGameData();
        if (dataRepository == null) return null;
        var hullmodEntries = dataRepository.getAllHullmodEntries();
        return hullmodEntries != null ? hullmodEntries.get(hullmodID) : null;
    }

    public static WeaponCSVEntry retrieveWeaponCSVEntryByID(String weaponID) {
        if (weaponID == null) {
            return null;
        }
        GameDataRepository dataRepository = SettingsManager.getGameData();
        if (dataRepository == null) return null;
        var weaponEntries = dataRepository.getAllWeaponEntries();
        return weaponEntries != null ? weaponEntries.get(weaponID) : null;
    }

    public static ShipSpecFile retrieveSpecByID(String hullID) {
        if (hullID == null) {
            return null;
        }
        GameDataRepository dataRepository = SettingsManager.getGameData();
        if (dataRepository == null) return null;
        var allSpecs = dataRepository.getAllSpecEntries();
        if (allSpecs == null) return null;
        ShipSpecFile spec = allSpecs.get(hullID);
        if (spec == null) {
            Path filePath = shipeditor.persistence.database.DatabaseQueryService.getFilePathForEntity(hullID, "SHIP");
            if (filePath != null) {
                spec = shipeditor.parsing.loading.FileLoading.loadHullFile(filePath.toFile());
            } else {
                filePath = shipeditor.persistence.database.DatabaseQueryService.getFilePathForEntity(hullID, "SKIN");
                if (filePath != null) {
                    spec = shipeditor.parsing.loading.FileLoading.loadSkinFile(filePath.toFile());
                }
            }
            if (spec != null) {
                putSpec(spec);
            }
        }
        return spec;
    }

    /**
     * @param shipHullID ship ID, whether base or skin.
     * @return base hull ID.
     */
    public static String getBaseHullID(String shipHullID) {
        if (shipHullID == null) return null;
        ShipSpecFile specFile = GameDataRepository.retrieveSpecByID(shipHullID);
        if (specFile == null) return null;
        String baseHullId;
        if (specFile instanceof SkinSpecFile checkedSkin) {
            baseHullId = checkedSkin.getBaseHullId();
        } else {
            baseHullId = specFile.getHullId();
        }
        return baseHullId;
    }

    public static void putSpec(ShipSpecFile specFile) {
        if (specFile == null || specFile.getHullId() == null) {
            return;
        }
        GameDataRepository dataRepository = SettingsManager.getGameData();
        if (dataRepository != null) {
            var allSpecs = dataRepository.getAllSpecEntries();
            if (allSpecs != null) {
                allSpecs.put(specFile.getHullId(), specFile);
            }
        }
    }

    public static void putVariant(VariantFile variantFile) {
        if (variantFile == null || variantFile.getVariantId() == null) {
            return;
        }
        GameDataRepository dataRepository = SettingsManager.getGameData();
        if (dataRepository != null && dataRepository.getAllVariants() != null) {
            dataRepository.getAllVariants().put(variantFile.getVariantId(), variantFile);
            // Update reverse index.
            String hullId = variantFile.getHullId();
            if (hullId != null) {
                dataRepository.variantsByHullID
                        .computeIfAbsent(hullId, k -> new ConcurrentHashMap<>())
                        .put(variantFile.getVariantId(), variantFile);
            }
        }
    }

    public void setHullmodDataLoaded(boolean hullmodsLoaded) {
        this.hullmodDataLoaded = hullmodsLoaded;
        EventBus.publish(new HullmodDataSet());
    }

    public void setWingDataLoaded(boolean wingsLoaded) {
        this.wingDataLoaded = wingsLoaded;
        EventBus.publish(new WingDataSet());
    }

    public Map<String, HullStyle> getAllHullStyles() {
        if (allHullStyles == null) {
            synchronized(this) {
                if (allHullStyles == null) {
                    shipeditor.parsing.loading.FileLoading.loadHullStyles().run();
                }
            }
        }
        return allHullStyles;
    }

    public Map<String, EngineStyle> getAllEngineStyles() {
        if (allEngineStyles == null) {
            synchronized(this) {
                if (allEngineStyles == null) {
                    shipeditor.parsing.loading.FileLoading.loadEngineStyles().run();
                }
            }
        }
        return allEngineStyles;
    }

    public static HullStyle fetchStyleByID(String styleID) {
        if (styleID == null) {
            return null;
        }
        var dataRepository = SettingsManager.getGameData();
        if (dataRepository == null) return null;
        Map<String, HullStyle> allStyles = dataRepository.getAllHullStyles();
        return allStyles != null ? allStyles.get(styleID) : null;
    }

    public Map<String, VariantFile> getAllVariants() {
        if (allVariants == null) {
            synchronized(this) {
                if (allVariants == null) {
                    Map<String, VariantFile> loadedVariants = new java.util.concurrent.ConcurrentHashMap<>();
                    java.util.List<shipeditor.persistence.database.IndexedFile> dbFiles = shipeditor.persistence.database.DatabaseQueryService.getFilesByType("VARIANT");
                    dbFiles.parallelStream().forEach(dbFile -> {
                        if (dbFile != null && dbFile.getFilePath() != null) {
                            java.io.File variantFile = dbFile.getFilePath().toFile();
                            VariantFile mapped = shipeditor.parsing.loading.FileLoading.loadVariantFile(variantFile);
                            if (mapped != null && mapped.getVariantId() != null) {
                                loadedVariants.put(mapped.getVariantId(), mapped);
                            }
                        }
                    });
                    this.allVariants = loadedVariants;
                    rebuildVariantsByHullIndex();
                }
            }
        }
        return allVariants;
    }

    public static VariantFile getVariantByID(String variantID) {
        if (variantID == null) {
            return null;
        }
        var dataRepository = SettingsManager.getGameData();
        if (dataRepository == null) return null;
        Map<String, VariantFile> variants = dataRepository.getAllVariants();
        return variants != null ? variants.get(variantID) : null;
    }

    public static ProjectileSpecFile getProjectileByID(String projectileID) {
        if (projectileID == null) {
            return null;
        }
        var dataRepository = SettingsManager.getGameData();
        if (dataRepository == null) return null;
        ProjectileSpecFile spec = dataRepository.allProjectiles != null ? dataRepository.allProjectiles.get(projectileID) : null;
        if (spec == null) {
            Path filePath = shipeditor.persistence.database.DatabaseQueryService.getFilePathForEntity(projectileID, shipeditor.utility.text.StringConstants.PROJECTILE_TYPE);
            if (filePath != null) {
                spec = shipeditor.parsing.loading.FileLoading.loadProjectileFile(filePath.toFile());
                if (spec != null) {
                    if (dataRepository.allProjectiles == null) {
                        dataRepository.allProjectiles = new ConcurrentHashMap<>();
                    }
                    dataRepository.allProjectiles.put(projectileID, spec);
                }
            }
        }
        return spec;
    }

    public static WeaponCSVEntry getWeaponByID(String weaponID) {
        if (weaponID == null) {
            return null;
        }
        var dataRepository = SettingsManager.getGameData();
        if (dataRepository == null) return null;
        Map<String, WeaponCSVEntry> weapons = dataRepository.getAllWeaponEntries();
        return weapons != null ? weapons.get(weaponID) : null;
    }

    public static Map<String, VariantFile> getMatchingForHullID(String shipHullID) {
        if (shipHullID == null) {
            return new HashMap<>();
        }
        var dataRepository = SettingsManager.getGameData();
        if (dataRepository == null) {
            return new HashMap<>();
        }
        dataRepository.getAllVariants();
        if (dataRepository.variantsByHullID != null) {
            Map<String, VariantFile> indexed = dataRepository.variantsByHullID.get(shipHullID);
            if (indexed != null) {
                return new HashMap<>(indexed);
            }
        }
        return new HashMap<>();
    }

    /**
     * Rebuilds the reverse hull-ID → variants index from the full allVariants map.
     * Should be called after allVariants is fully populated.
     */
    public void rebuildVariantsByHullIndex() {
        Map<String, Map<String, VariantFile>> index = new ConcurrentHashMap<>();
        if (allVariants != null) {
            allVariants.forEach((variantId, variantFile) -> {
                if (variantId != null && variantFile != null) {
                    String hullId = variantFile.getHullId();
                    if (hullId != null) {
                        index.computeIfAbsent(hullId, k -> new ConcurrentHashMap<>())
                                .put(variantId, variantFile);
                    }
                }
            });
        }
        this.variantsByHullID = index;
    }

    // --- Re-indexing methods for CSV ID changes ---

    private <T> void reindexEntry(Map<String, T> map, String oldID, String newID, T entry) {
        if (map == null || newID == null || entry == null) {
            return;
        }
        if (oldID != null) {
            map.remove(oldID);
        }
        map.put(newID, entry);
    }

    public void reindexShipEntry(String oldID, String newID, ShipCSVEntry entry) {
        reindexEntry(allShipEntries, oldID, newID, entry);
    }

    public void reindexSpecEntry(String oldID, String newID) {
        if (oldID == null || newID == null) {
            return;
        }
        ShipSpecFile spec = allSpecEntries.remove(oldID);
        if (spec != null) {
            allSpecEntries.put(newID, spec);
        }
    }

    public void reindexWeaponEntry(String oldID, String newID, WeaponCSVEntry entry) {
        reindexEntry(allWeaponEntries, oldID, newID, entry);
    }

    public void reindexHullmodEntry(String oldID, String newID, HullmodCSVEntry entry) {
        reindexEntry(allHullmodEntries, oldID, newID, entry);
    }

    public void reindexWingEntry(String oldID, String newID, WingCSVEntry entry) {
        reindexEntry(allWingEntries, oldID, newID, entry);
    }

    public void reindexShipSystemEntry(String oldID, String newID, ShipSystemCSVEntry entry) {
        reindexEntry(allShipsystemEntries, oldID, newID, entry);
    }

}
