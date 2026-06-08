package oth.shipeditor.components.datafiles.entities;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.components.viewer.layers.LayerManager;
import oth.shipeditor.components.viewer.layers.ship.ShipLayer;
import oth.shipeditor.components.viewer.layers.ship.ShipPainter;
import oth.shipeditor.components.viewer.layers.ship.data.ShipHull;
import oth.shipeditor.parsing.FileUtilities;
import oth.shipeditor.parsing.loading.FileLoading;
import oth.shipeditor.representation.ship.HullSize;
import oth.shipeditor.representation.ship.HullSpecFile;
import oth.shipeditor.representation.ship.ShipTypeHints;
import oth.shipeditor.representation.ship.SkinSpecFile;
import oth.shipeditor.utility.Utility;
import oth.shipeditor.utility.graphics.DrawUtilities;
import oth.shipeditor.utility.graphics.Sprite;
import oth.shipeditor.utility.text.StringConstants;
import oth.shipeditor.utility.text.StringValues;

import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.persistence.database.DatabaseQueryService;
import oth.shipeditor.persistence.database.IndexedFile;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class ShipCSVEntry implements LayerableEntry, InstallableEntry {

    @Getter
    private final Map<String, String> rowData;

    @lombok.Getter(lombok.AccessLevel.NONE)
    @lombok.Setter
    private HullSpecFile hullSpecFile;

    /**
     * Keys are simple names of skin files, e.g.: legion_xiv.skin.
     */
    @lombok.Getter(lombok.AccessLevel.NONE)
    private Map<String, SkinSpecFile> skins;

    @Getter
    private SkinSpecFile activeSkinSpecFile;

    @Getter
    private final String hullFileName;

    @Getter
    private String hullID;

    @Getter
    private final Path packageFolderPath;

    private Sprite entrySprite;

    private List<ShipTypeHints> cachedBaseHullHints;

    public ShipCSVEntry(Map<String, String> row, Map.Entry<HullSpecFile, Map<String, SkinSpecFile>> hullWithSkins,
                        Path folder, String fileName) {
        this.packageFolderPath = folder;
        this.hullSpecFile = hullWithSkins != null ? hullWithSkins.getKey() : null;
        this.skins = hullWithSkins != null ? hullWithSkins.getValue() : null;
        this.rowData = row;
        this.hullID = row.get(StringConstants.ID);
        this.hullFileName = fileName;
        this.activeSkinSpecFile = SkinSpecFile.empty();
        if (this.skins != null) {
            this.skins.put(SkinSpecFile.DEFAULT, activeSkinSpecFile);
        }
    }

    public HullSpecFile getHullSpecFile() {
        if (hullSpecFile == null) {
            lazyLoadSpecAndSkins();
        }
        return hullSpecFile;
    }

    public Map<String, SkinSpecFile> getSkins() {
        if (skins == null) {
            lazyLoadSpecAndSkins();
        }
        return skins;
    }

    private synchronized void lazyLoadSpecAndSkins() {
        if (hullSpecFile != null) return;

        log.info("Lazily loading spec and skins for base hull ID: {}", hullID);
        Path hullPath = DatabaseQueryService.getFilePathForEntity(this.hullID, "SHIP");
        if (hullPath != null) {
            this.hullSpecFile = FileLoading.loadHullFile(hullPath.toFile());
        } else {
            log.error("Failed to locate hull file path for ID: {}", this.hullID);
        }

        this.skins = new HashMap<>();
        this.activeSkinSpecFile = SkinSpecFile.empty();
        this.skins.put(SkinSpecFile.DEFAULT, this.activeSkinSpecFile);

        String modId = this.packageFolderPath.getFileName().toString();
        if (SettingsManager.isCoreFolder(this.packageFolderPath)) {
            modId = "starsector-core";
        }

        List<IndexedFile> skinFiles = DatabaseQueryService.getFilesByModAndTypeAsync(modId, "SKIN").join();
        for (IndexedFile skinFile : skinFiles) {
            SkinSpecFile skinSpec = FileLoading.loadSkinFile(skinFile.getFilePath().toFile());
            if (skinSpec != null && Objects.equals(skinSpec.getBaseHullId(), this.hullID)) {
                this.skins.put(skinFile.getFileName(), skinSpec);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public List<ShipTypeHints> getBaseHullHints() {
        if (cachedBaseHullHints != null) {
            return cachedBaseHullHints;
        }
        List<ShipTypeHints> result = new ArrayList<>();
        String cellData = rowData.get(StringConstants.HINTS);
        if (cellData != null && !cellData.isEmpty()) {
            Iterable<String> hintsText = new ArrayList<>(Arrays.asList(Utility.SPLIT_BY_COMMA.split(cellData)));
            hintsText.forEach(hintText -> {
                ShipTypeHints typeHint = ShipTypeHints.valueOf(hintText.trim());
                result.add(typeHint);
            });
        }
        cachedBaseHullHints = result;
        return result;
    }

    public HullSize getSize() {
        HullSpecFile specFile = this.getHullSpecFile();
        if (specFile == null) {
            return HullSize.DEFAULT;
        }
        String hullSize = specFile.getHullSize();
        if (hullSize == null) {
            return HullSize.DEFAULT;
        }
        try {
            return HullSize.valueOf(hullSize);
        } catch (IllegalArgumentException e) {
            return HullSize.DEFAULT;
        }
    }

    public String getShipID() {
        if (activeSkinSpecFile != null && !activeSkinSpecFile.isBase()) {
            return activeSkinSpecFile.getSkinHullId();
        }
        return getHullID();
    }

    @Override
    public String getMultilineTooltip() {
        return this.getMultilineTooltip(new String[0]);
    }

    @SuppressWarnings("OverloadedVarargsMethod")
    public String getMultilineTooltip(String... additional) {
        List<String> lines = new ArrayList<>(2);
        String entryID = "Hull ID: " + this.getHullID();
        String size =  "Hull size: " +  this.getSize();
        lines.add(entryID);
        lines.add(size);
        if (additional != null && additional.length > 0) {
            lines.addAll(List.of(additional));
        }
        return Utility.getWithLinebreaks(lines.toArray(new String[0]));
    }

    public String getShipName() {
        if (activeSkinSpecFile != null && !activeSkinSpecFile.isBase()) {
            return activeSkinSpecFile.getHullName();
        }
        return rowData.get(StringConstants.NAME);
    }

    public int getTotalOPWithSkin() {
        if (activeSkinSpecFile != null && !activeSkinSpecFile.isBase()) {
            return activeSkinSpecFile.getOrdnancePoints();
        }
        String ordnancePoints = rowData.get(StringConstants.ORDNANCE_POINTS_SPACED);
        return Utility.parseIntegerOrDefault(ordnancePoints, 0);
    }

    public int getBaseTotalOP() {
        String ordnancePoints = rowData.get(StringConstants.ORDNANCE_POINTS_SPACED);
        return Utility.parseIntegerOrDefault(ordnancePoints, -1);
    }

    public int getBayCount() {
        var entryRowData = this.getRowData();
        String fighterBays = entryRowData.get("fighter bays");
        return Utility.parseIntegerOrDefault(fighterBays, 0);
    }

    public String getShipSpriteName() {
        if (activeSkinSpecFile != null && !activeSkinSpecFile.isBase()) {
            String skinSpecFileSpriteName = activeSkinSpecFile.getSpriteName();
            if (skinSpecFileSpriteName != null && !skinSpecFileSpriteName.isEmpty()) {
                return skinSpecFileSpriteName;
            }
        }
        return hullSpecFile != null ? hullSpecFile.getSpriteName() : "";
    }

    @Override
    public String getID() {
        return hullID;
    }

    public void setHullID(String newID) {
        this.hullID = newID;
        this.rowData.put(StringConstants.ID, newID);
    }

    private final Map<SkinSpecFile, Sprite> skinSpriteCache = new HashMap<>();

    public Sprite getEntrySprite() {
        if (activeSkinSpecFile != null && !activeSkinSpecFile.isBase()) {
            Sprite skinSprite = skinSpriteCache.get(activeSkinSpecFile);
            if (skinSprite == null) {
                String spriteFileName = this.getShipSpriteName();
                File spriteFile = FileLoading.fetchDataFile(Path.of(spriteFileName), activeSkinSpecFile.getContainingPackage());
                if (spriteFile != null) {
                    skinSprite = FileLoading.loadSprite(spriteFile);
                    skinSpriteCache.put(activeSkinSpecFile, skinSprite);
                }
            }
            if (skinSprite != null) {
                return skinSprite;
            }
        }
        if (entrySprite == null) {
            String spriteFileName = this.getShipSpriteName();
            File spriteFile = FileLoading.fetchDataFile(Path.of(spriteFileName), this.getPackageFolderPath());

            if (spriteFile != null) {
                entrySprite = FileLoading.loadSprite(spriteFile);
            }
        }
        return entrySprite;
    }

    /**
     * @param rotation - in degrees.
     */
    public void paintEntry(Graphics2D g, AffineTransform worldToScreen,
                            double rotation, Point2D targetLocation) {
        DrawUtilities.paintInstallableGhost(g, worldToScreen,
                rotation, targetLocation, getEntrySprite());
    }

    @Override
    public Path getTableFilePath() {
        return hullSpecFile != null ? hullSpecFile.getTableFilePath() : null;
    }

    public void setActiveSkinSpecFile(SkinSpecFile input) {
        if (!skins.containsValue(input)) {
            throw new RuntimeException("Attempt to set incompatible skin on ship entry!");
        }
        this.activeSkinSpecFile = input;
    }

    @Override
    public String toString() {
        String displayedName = rowData.get(StringConstants.NAME);
        if (displayedName.isEmpty()) {
            displayedName = rowData.get(StringConstants.DESIGNATION);
        }
        return displayedName;
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public ShipLayer loadLayerFromEntry() {
        if (this.hullSpecFile == null) {
            log.error("Hull spec file not loaded, layer not created: {}", this.hullID);
            JOptionPane.showMessageDialog(oth.shipeditor.PrimaryWindow.getInstance(),
                    "Hull spec file not loaded, layer not created: " + this.hullID,
                    StringValues.FILE_LOADING_ERROR,
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        String spriteName = this.hullSpecFile.getSpriteName();
        Path spriteFilePath = Path.of(spriteName);
        File spriteFile = FileLoading.fetchDataFile(spriteFilePath, this.packageFolderPath);

        if (spriteFile == null) {
            log.error("Sprite file for ship not found: {}", spriteFilePath.toString());
            JOptionPane.showMessageDialog(oth.shipeditor.PrimaryWindow.getInstance(),
                    "Sprite file for ship not found, layer not created: " + spriteFilePath,
                    StringValues.FILE_LOADING_ERROR,
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        ShipLayer newLayer = FileUtilities.createShipLayerWithSprite(spriteFile);
        newLayer.initializeHullData(this.hullSpecFile);

        if (skins == null || skins.isEmpty()) return newLayer;

        Map<String, SkinSpecFile> eligibleSkins = new HashMap<>(skins);
        eligibleSkins.remove(SkinSpecFile.DEFAULT);
        if (eligibleSkins.isEmpty()) return newLayer;
        for (SkinSpecFile skinSpecFile : eligibleSkins.values()) {
            if (skinSpecFile == null || skinSpecFile.isBase()) continue;

            ShipHull data = newLayer.getHull();
            boolean setAsActive = skinSpecFile == this.activeSkinSpecFile;
            LayerManager.openSkinFile(newLayer, data, skinSpecFile, setAsActive);
        }
        return newLayer;
    }

    /**
     * @param layer can be null.
     */
    public ShipPainter createPainterFromEntry(ShipLayer layer) {
        if (this.hullSpecFile == null) {
            log.error("Hull spec file not loaded, painter not created: {}", this.hullID);
            return null;
        }
        ShipPainter shipPainter = new ShipPainter(layer);

        String spriteName = this.hullSpecFile.getSpriteName();
        Path spriteFilePath = Path.of(spriteName);
        File spriteFile = FileLoading.fetchDataFile(spriteFilePath, this.packageFolderPath);
        Sprite sprite = FileLoading.loadSprite(spriteFile);
        shipPainter.setSprite(sprite);
        shipPainter.setBaseHullSprite(sprite);

        shipPainter.initFromHullSpec(this.getHullSpecFile());

        return shipPainter;
    }

    public List<String> getBuiltInHullmods() {
        List<String> hullmodIDs = new ArrayList<>();
        if (hullSpecFile != null) {
            String[] fromHull = hullSpecFile.getBuiltInMods();
            if (fromHull != null) {
                hullmodIDs.addAll(List.of(fromHull));
            }
        }
        SkinSpecFile skinSpecFile = this.activeSkinSpecFile;
        if (skinSpecFile != null && !skinSpecFile.isBase()) {
            List<String> builtInMods = skinSpecFile.getBuiltInMods();
            if (builtInMods != null) {
                hullmodIDs.addAll(builtInMods);
            }
        }
        return hullmodIDs;
    }

}
