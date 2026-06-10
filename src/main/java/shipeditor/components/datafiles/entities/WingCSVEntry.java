package shipeditor.components.datafiles.entities;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.*;
import shipeditor.representation.ship.HullSize;
import shipeditor.representation.ship.ShipSpecFile;
import shipeditor.representation.ship.SkinSpecFile;
import shipeditor.representation.ship.VariantFile;
import shipeditor.utility.Utility;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;

@Log4j2
@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WingCSVEntry implements OrdnancedCSVEntry {

    private final Map<String, String> rowData;

    private final Path packageFolderPath;

    private final Path tableFilePath;

    private String wingID;

    @Setter
    private String displayedName;

    @Getter
    private ShipSpecFile wingMemberSpec;

    private Sprite memberSprite;

    public WingCSVEntry(Map<String, String> row, Path folder, Path tablePath) {
        this.rowData = row;
        packageFolderPath = folder;
        this.tableFilePath = tablePath;
        wingID = this.rowData.get("id");
    }

    @Override
    public String getID() {
        return wingID;
    }

    public void setWingID(String newID) {
        this.wingID = newID;
        this.rowData.put("id", newID);
    }

    @Override
    public String toString() {
        String name = rowData.get(StringConstants.ID);
        if (name.isEmpty()) {
            name = StringValues.UNTITLED;
        }
        return name;
    }

    public VariantFile retrieveMemberVariant() {
        String variantID = rowData.get(StringConstants.VARIANT);
        var gameData = SettingsManager.getGameData();
        var allVariants = gameData.getAllVariants();
        return allVariants.get(variantID);
    }

    @Override
    public String getMultilineTooltip() {
        String entryID = "Wing ID: " + this.getWingID();
        return Utility.getWithLinebreaks(entryID);
    }

    private ShipSpecFile retrieveSpec() {
        VariantFile variantFile = retrieveMemberVariant();

        String hullID = variantFile.getHullId();
        ShipSpecFile desiredSpec = GameDataRepository.retrieveSpecByID(hullID);

        this.wingMemberSpec = desiredSpec;
        return desiredSpec;
    }

    private Sprite getWingMemberSprite() {
        if (this.memberSprite != null) {
            return this.memberSprite;
        }

        ShipSpecFile specFile;
        if (this.wingMemberSpec == null) {
            specFile = this.retrieveSpec();
        } else {
            specFile = this.wingMemberSpec;
        }

        if (specFile != null) {
            String spriteName = specFile.getSpriteName();
            Path of = Path.of(spriteName);
            File spriteFile = FileLoading.fetchDataFile(of, packageFolderPath);
            Sprite result = FileLoading.loadSprite(spriteFile);
            this.memberSprite = result;
            return result;
        } else {
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    "Wing member sprite loading failed, exception thrown for: " + this.wingID,
                    StringValues.FILE_LOADING_ERROR,
                    JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException("Could not retrieve wing member sprite!");
        }
    }

    /**
     * @param size irrelevant, should be null.
     */
    @Override
    public int getOrdnanceCost(HullSize size) {
        String tableValue = this.rowData.get("op cost");
        return Utility.parseIntegerOrDefault(tableValue, 0);
    }

    @Override
    public String getEntryName() {
        if (this.displayedName != null) {
            return this.displayedName;
        }

        ShipSpecFile specFile;
        if (this.wingMemberSpec == null) {
            specFile = this.retrieveSpec();
        } else {
            specFile = this.wingMemberSpec;
        }

        if (specFile != null) {
            var variant = this.retrieveMemberVariant();
            ShipCSVEntry entry = GameDataRepository.retrieveShipCSVEntryByID(variant.getShipHullId());
            String result = entry.getShipName();

            String drone = "Drone";
            String displayName = variant.getDisplayName();
            if (!(result.endsWith(drone) && displayName.equals(drone))) {
                result = result + " " + displayName;
            }
            this.setDisplayedName(result);
            return result;
        }

        return this.getWingID();
    }

    @Override
    public JLabel getIconLabel() {
        return getIconLabel(32);
    }

    @Override
    public JLabel getIconLabel(int maxSize) {
        Sprite sprite = this.getWingMemberSprite();
        if (sprite != null) {
            String tooltip = Utility.getTooltipForSprite(sprite);
            return ComponentUtilities.createIconFromImage(sprite.getImage(), tooltip, maxSize);
        }
        return null;
    }

}
