package shipeditor.representation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.EventBus;
import shipeditor.components.datafiles.entities.*;
import shipeditor.components.datafiles.trees.ShipFilterPanel;
import shipeditor.components.datafiles.trees.WeaponFilterPanel;

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
    private Map<Path, List<ShipCSVEntry>> shipEntriesByPackage;

    private Map<Path, List<WeaponCSVEntry>> weaponEntriesByPackage;

    private Map<Path, List<ProjectileSpecFile>> projectileEntriesByPackage;

    private Map<Path, List<HullmodCSVEntry>> hullmodEntriesByPackage;

    private Map<Path, List<ShipSystemCSVEntry>> shipSystemEntriesByPackage;

    private Map<Path, List<WingCSVEntry>> wingEntriesByPackage;

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
    private boolean shipDataLoaded;

    private boolean hullmodDataLoaded;

    @Setter
    private boolean shipsystemDataLoaded;

    private boolean wingDataLoaded;

    @Setter
    private boolean weaponsDataLoaded;

    public GameDataRepository() {
        this.allSpecEntries = new ConcurrentHashMap<>();
        this.allShipEntries = new ConcurrentHashMap<>();
        this.allHullmodEntries = new ConcurrentHashMap<>();
        this.allShipsystemEntries = new ConcurrentHashMap<>();
        this.allWingEntries = new ConcurrentHashMap<>();
        this.allWeaponEntries = new ConcurrentHashMap<>();
        this.allVariants = new ConcurrentHashMap<>();
        this.allProjectiles = new ConcurrentHashMap<>();
    }

    public void setShipEntriesByPackage(Map<Path, List<ShipCSVEntry>> shipEntries) {
        this.shipEntriesByPackage = shipEntries;
        Map<Path, Boolean> filterEntries = new LinkedHashMap<>();
        if (shipEntries != null) {
            shipEntries.forEach((path, shipCSVEntries) -> filterEntries.put(path, true));
            SettingsManager.announcePackages(shipEntries);
        }
        ShipFilterPanel.setFactionFilters(filterEntries);
    }

    public void setWeaponEntriesByPackage(Map<Path, List<WeaponCSVEntry>> weaponEntries) {
        this.weaponEntriesByPackage = weaponEntries;
        Map<Path, Boolean> filterEntries = new LinkedHashMap<>();
        if (weaponEntries != null) {
            weaponEntries.forEach((path, weaponCSVEntries) -> filterEntries.put(path, true));
            SettingsManager.announcePackages(weaponEntries);
        }
        WeaponFilterPanel.setPackageFilters(filterEntries);
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
        var shipEntries = dataRepository.getAllShipEntries();
        return shipEntries.get(baseHullID);
    }

    public static HullmodCSVEntry retrieveHullmodCSVEntryByID(String hullmodID) {
        if (hullmodID == null) {
            return null;
        }
        GameDataRepository dataRepository = SettingsManager.getGameData();
        var hullmodEntries = dataRepository.getAllHullmodEntries();
        return hullmodEntries.get(hullmodID);
    }

    public static WeaponCSVEntry retrieveWeaponCSVEntryByID(String weaponID) {
        if (weaponID == null) {
            return null;
        }
        GameDataRepository dataRepository = SettingsManager.getGameData();
        var weaponEntries = dataRepository.getAllWeaponEntries();
        return weaponEntries.get(weaponID);
    }

    public static ShipSpecFile retrieveSpecByID(String hullID) {
        if (hullID == null) {
            return null;
        }
        GameDataRepository dataRepository = SettingsManager.getGameData();
        var allSpecs = dataRepository.getAllSpecEntries();
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
        }
        return spec;
    }

    /**
     * @param shipHullID ship ID, whether base or skin.
     * @return base hull ID.
     */
    public static String getBaseHullID(String shipHullID) {
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
        GameDataRepository dataRepository = SettingsManager.getGameData();
        var allSpecs = dataRepository.getAllSpecEntries();
        allSpecs.put(specFile.getHullId(), specFile);
    }

    public static void putVariant(VariantFile variantFile) {
        GameDataRepository dataRepository = SettingsManager.getGameData();
        if (dataRepository.getAllVariants() != null) {
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

    public static HullStyle fetchStyleByID(String styleID) {
        if (styleID == null) {
            return null;
        }
        var dataRepository = SettingsManager.getGameData();
        Map<String, HullStyle> allHullStyles = dataRepository.getAllHullStyles();
        HullStyle style = null;
        if (allHullStyles != null) {
            style = allHullStyles.get(styleID);
        }
        return style;
    }

    public static VariantFile getVariantByID(String variantID) {
        if (variantID == null) {
            return null;
        }
        var dataRepository = SettingsManager.getGameData();
        if (dataRepository.allVariants == null) {
            return null;
        }
        return dataRepository.allVariants.get(variantID);
    }

    public static ProjectileSpecFile getProjectileByID(String projectileID) {
        if (projectileID == null) {
            return null;
        }
        var dataRepository = SettingsManager.getGameData();
        if (dataRepository.allProjectiles == null) {
            return null;
        }
        return dataRepository.allProjectiles.get(projectileID);
    }

    public static WeaponCSVEntry getWeaponByID(String weaponID) {
        if (weaponID == null) {
            return null;
        }
        var dataRepository = SettingsManager.getGameData();
        return dataRepository.allWeaponEntries.get(weaponID);
    }

    public static Map<String, VariantFile> getMatchingForHullID(String shipHullID) {
        var dataRepository = SettingsManager.getGameData();
        Map<String, VariantFile> indexed = dataRepository.variantsByHullID.get(shipHullID);
        if (indexed != null) {
            return new HashMap<>(indexed);
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
                String hullId = variantFile.getHullId();
                if (hullId != null) {
                    index.computeIfAbsent(hullId, k -> new ConcurrentHashMap<>())
                            .put(variantId, variantFile);
                }
            });
        }
        this.variantsByHullID = index;
    }

    // --- Re-indexing methods for CSV ID changes ---

    private <T> void reindexEntry(Map<String, T> map, String oldID, String newID, T entry) {
        map.remove(oldID);
        map.put(newID, entry);
    }

    public void reindexShipEntry(String oldID, String newID, ShipCSVEntry entry) {
        reindexEntry(allShipEntries, oldID, newID, entry);
    }

    public void reindexSpecEntry(String oldID, String newID) {
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
