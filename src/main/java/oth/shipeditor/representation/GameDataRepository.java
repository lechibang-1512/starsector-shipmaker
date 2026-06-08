package oth.shipeditor.representation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.files.HullmodDataSet;
import oth.shipeditor.communication.events.files.WingDataSet;
import oth.shipeditor.components.datafiles.entities.*;
import oth.shipeditor.components.datafiles.trees.WeaponFilterPanel;
import oth.shipeditor.components.viewer.layers.ship.ShipLayer;
import oth.shipeditor.components.viewer.layers.ship.ShipPainter;
import oth.shipeditor.components.viewer.layers.ship.data.ActiveShipSpec;
import oth.shipeditor.components.viewer.layers.ship.data.ShipSkin;
import oth.shipeditor.components.viewer.layers.ship.data.Variant;
import oth.shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.representation.ship.*;
import oth.shipeditor.representation.weapon.ProjectileSpecFile;

import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods", "StaticMethodOnlyUsedInOneClass"})
@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class GameDataRepository {

    /**
     * All ship entries by their hull IDs.
     */
    private final Map<String, ShipCSVEntry> allShipEntries;

    /**
     * Base hull and skin entries by their ship hull IDs. Used when layer needs to be loaded from variant ID.
     */
    private final Map<String, ShipSpecFile> allSpecEntries;

    /**
     * All hullmod entries by their IDs.
     */
    private final Map<String, HullmodCSVEntry> allHullmodEntries;

    /**
     * All shipsystem entries by their IDs.
     */
    private final Map<String, ShipSystemCSVEntry> allShipsystemEntries;

    private final Map<String, WingCSVEntry> allWingEntries;

    private final Map<String, WeaponCSVEntry> allWeaponEntries;

    /**
     * Holds the same instances as id-entry collection, used for quick repopulating of entry tree with filtering.
     */
    private Map<Path, List<ShipCSVEntry>> shipEntriesByPackage;

    private Map<Path, List<WeaponCSVEntry>> weaponEntriesByPackage;

    private Map<Path, List<ProjectileSpecFile>> projectileEntriesByPackage;

    private Map<Path, List<HullmodCSVEntry>> hullmodEntriesByPackage;

    private Map<Path, List<ShipSystemCSVEntry>> shipSystemEntriesByPackage;

    private Map<Path, List<WingCSVEntry>> wingEntriesByPackage;

    @lombok.Getter(lombok.AccessLevel.NONE)
    private final Map<Path, SoftReference<List<Map<String, String>>>> rawCSVDataByPath = new ConcurrentHashMap<>();

    @lombok.Getter(lombok.AccessLevel.NONE)
    private final Map<Path, SoftReference<Object>> csvSchemasByPath = new ConcurrentHashMap<>();

    public void putRawCSVDataForPath(Path path, List<Map<String, String>> rawData) {
        rawCSVDataByPath.put(path, new SoftReference<>(rawData));
    }

    public void putCsvSchemaForPath(Path path, Object schema) {
        csvSchemasByPath.put(path, new SoftReference<>(schema));
    }

    /**
     * Retrieves raw CSV data for the given path. If the SoftReference was cleared by GC,
     * transparently re-parses the CSV from disk.
     */
    public List<Map<String, String>> getRawCSVDataForPath(Path path) {
        SoftReference<List<Map<String, String>>> ref = rawCSVDataByPath.get(path);
        if (ref != null) {
            List<Map<String, String>> data = ref.get();
            if (data != null) {
                return data;
            }
        }
        // Re-parse from disk on cache miss.
        List<Map<String, String>> reparsed = oth.shipeditor.parsing.loading.FileLoading.reparseCSVForPath(path);
        if (reparsed != null) {
            rawCSVDataByPath.put(path, new SoftReference<>(reparsed));
        }
        return reparsed;
    }

    /**
     * Retrieves the CSV schema for the given path. If the SoftReference was cleared by GC,
     * transparently re-parses to recover the schema.
     */
    public Object getCsvSchemaForPath(Path path) {
        SoftReference<Object> ref = csvSchemasByPath.get(path);
        if (ref != null) {
            Object schema = ref.get();
            if (schema != null) {
                return schema;
            }
        }
        // Re-parse from disk to recover the schema.
        oth.shipeditor.parsing.loading.FileLoading.reparseCSVForPath(path);
        SoftReference<Object> refreshedRef = csvSchemasByPath.get(path);
        return refreshedRef != null ? refreshedRef.get() : null;
    }

    /**
     * Hull styles by their IDs (field names in JSON).
     */
    @Setter
    private Map<String, HullStyle> allHullStyles;

    /**
     * Engine styles by their IDs (field names in JSON).
     */
    @Setter
    private Map<String, EngineStyle> allEngineStyles;

    /**
     * All variant files by variant IDs.
     */
    @Setter
    private Map<String, VariantFile> allVariants;

    /**
     * All projectile files by variant IDs.
     */
    @Setter
    private Map<String, ProjectileSpecFile> allProjectiles;

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
        this.allShipEntries = new HashMap<>();
        this.allHullmodEntries = new HashMap<>();
        this.allShipsystemEntries = new HashMap<>();
        this.allWingEntries = new HashMap<>();
        this.allWeaponEntries = new HashMap<>();
    }

    public void setShipEntriesByPackage(Map<Path, List<ShipCSVEntry>> shipEntries) {
        this.shipEntriesByPackage = shipEntries;
        SettingsManager.announcePackages(shipEntries);
    }

    public void setWeaponEntriesByPackage(Map<Path, List<WeaponCSVEntry>> weaponEntries) {
        this.weaponEntriesByPackage = weaponEntries;
        Map<Path, Boolean> filterEntries = new LinkedHashMap<>();
        weaponEntries.forEach((path, weaponCSVEntries) -> filterEntries.put(path, true));

        SettingsManager.announcePackages(weaponEntries);
        WeaponFilterPanel.setPackageFilters(filterEntries);
    }

    public void setProjectileEntriesByPackage(Map<Path, List<ProjectileSpecFile>> projectileEntries) {
        this.projectileEntriesByPackage = projectileEntries;
        SettingsManager.announcePackages(projectileEntries);
    }

    public void setHullmodEntriesByPackage(Map<Path, List<HullmodCSVEntry>> hullmodEntries) {
        this.hullmodEntriesByPackage = hullmodEntries;
        this.allHullmodEntries.clear();
        if (hullmodEntries != null) {
            hullmodEntries.values().forEach(list -> list.forEach(entry -> this.allHullmodEntries.put(entry.getID(), entry)));
        }
        SettingsManager.announcePackages(hullmodEntries);
    }

    public void setShipSystemEntriesByPackage(Map<Path, List<ShipSystemCSVEntry>> shipSystemEntries) {
        this.shipSystemEntriesByPackage = shipSystemEntries;
        this.allShipsystemEntries.clear();
        if (shipSystemEntries != null) {
            shipSystemEntries.values().forEach(list -> list.forEach(entry -> this.allShipsystemEntries.put(entry.getID(), entry)));
        }
        SettingsManager.announcePackages(shipSystemEntries);
    }

    public void setWingEntriesByPackage(Map<Path, List<WingCSVEntry>> wingEntries) {
        this.wingEntriesByPackage = wingEntries;
        this.allWingEntries.clear();
        if (wingEntries != null) {
            wingEntries.values().forEach(list -> list.forEach(entry -> this.allWingEntries.put(entry.getID(), entry)));
        }
        SettingsManager.announcePackages(wingEntries);
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
            Path filePath = oth.shipeditor.persistence.database.DatabaseQueryService.getFilePathForEntity(hullID, "SHIP");
            if (filePath != null) {
                spec = oth.shipeditor.parsing.loading.FileLoading.loadHullFile(filePath.toFile());
            } else {
                filePath = oth.shipeditor.persistence.database.DatabaseQueryService.getFilePathForEntity(hullID, "SKIN");
                if (filePath != null) {
                    spec = oth.shipeditor.parsing.loading.FileLoading.loadSkinFile(filePath.toFile());
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

    public static ShipLayer createLayerFromVariant(Variant variant) {
        if (variant == null) {
            return null;
        }
        String shipHullId = variant.getShipHullId();
        ShipSpecFile specFile = GameDataRepository.retrieveSpecByID(shipHullId);
        String baseHullId;
        SkinSpecFile skinSpec = null;
        if (specFile instanceof SkinSpecFile checkedSkin) {
            baseHullId = checkedSkin.getBaseHullId();
            skinSpec = checkedSkin;
        } else {
            baseHullId = specFile.getHullId();
        }
        ShipCSVEntry csvEntry = GameDataRepository.retrieveShipCSVEntryByID(baseHullId);
        ShipLayer shipLayer = csvEntry.loadLayerFromEntry();
        ShipPainter shipPainter = shipLayer.getPainter();

        if (skinSpec != null) {
            for (ShipSkin skin : shipLayer.getSkins()) {
                if (skin == null || skin.isBase()) continue;
                String skinHullId = skin.getSkinHullId();
                if (skinHullId.equals(skinSpec.getSkinHullId())) {
                    shipPainter.setActiveSpec(ActiveShipSpec.SKIN, skin);
                }
            }
        }

        shipPainter.selectVariant(variant);

        return shipLayer;
    }

    public static InstalledFeature createModuleFromVariant(String slotID, Variant variant) {
        if (variant == null) {
            return null;
        }
        String shipHullId = variant.getShipHullId();
        ShipSpecFile specFile = GameDataRepository.retrieveSpecByID(shipHullId);
        String baseHullId;
        SkinSpecFile skinSpec = null;
        if (specFile instanceof SkinSpecFile checkedSkin) {
            baseHullId = checkedSkin.getBaseHullId();
            skinSpec = checkedSkin;
        } else {
            baseHullId = specFile.getHullId();
        }
        ShipCSVEntry csvEntry = GameDataRepository.retrieveShipCSVEntryByID(baseHullId);
        ShipPainter modulePainter = csvEntry.createPainterFromEntry(null);

        if (skinSpec != null) {
            ShipSkin shipSkin = ShipSkin.createFromSpec(skinSpec);
            modulePainter.setActiveSpec(ActiveShipSpec.SKIN, shipSkin);
        }

        modulePainter.selectVariant(variant);
        return InstalledFeature.of(slotID, variant.getVariantId(), modulePainter, csvEntry);
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
        return dataRepository.allVariants.get(variantID);
    }

    public static ProjectileSpecFile getProjectileByID(String projectileID) {
        if (projectileID == null) {
            return null;
        }
        var dataRepository = SettingsManager.getGameData();
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
        var allVariants = dataRepository.getAllVariants();
        Map<String, VariantFile> result = new HashMap<>();
        for (Map.Entry<String, VariantFile> variantFileEntry : allVariants.entrySet()) {
            VariantFile variantFile = variantFileEntry.getValue();
            String variantHullId = variantFile.getHullId();
            if (variantHullId.equals(shipHullID)) {
                result.put(variantFileEntry.getKey(), variantFileEntry.getValue());
            }
        }
        return result;
    }

    // --- Re-indexing methods for CSV ID changes ---

    public void reindexShipEntry(String oldID, String newID, ShipCSVEntry entry) {
        allShipEntries.remove(oldID);
        allShipEntries.put(newID, entry);
    }

    public void reindexSpecEntry(String oldID, String newID) {
        ShipSpecFile spec = allSpecEntries.remove(oldID);
        if (spec != null) {
            allSpecEntries.put(newID, spec);
        }
    }

    public void reindexWeaponEntry(String oldID, String newID, WeaponCSVEntry entry) {
        allWeaponEntries.remove(oldID);
        allWeaponEntries.put(newID, entry);
    }

    public void reindexHullmodEntry(String oldID, String newID, HullmodCSVEntry entry) {
        allHullmodEntries.remove(oldID);
        allHullmodEntries.put(newID, entry);
    }

    public void reindexWingEntry(String oldID, String newID, WingCSVEntry entry) {
        allWingEntries.remove(oldID);
        allWingEntries.put(newID, entry);
    }

    public void reindexShipSystemEntry(String oldID, String newID, ShipSystemCSVEntry entry) {
        allShipsystemEntries.remove(oldID);
        allShipsystemEntries.put(newID, entry);
    }

}
