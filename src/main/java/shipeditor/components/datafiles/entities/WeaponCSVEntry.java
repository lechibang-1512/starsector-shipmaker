package shipeditor.components.datafiles.entities;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerUpdated;
import shipeditor.components.viewer.entities.weapon.OffsetPoint;
import shipeditor.components.viewer.layers.ship.ShipPainterInitialization;
import shipeditor.components.viewer.layers.weapon.*;
import shipeditor.components.viewer.ViewerEnums.WeaponRenderOrdering;
import shipeditor.components.viewer.painters.points.weapon.ProjectilePainter;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.weapon.*;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponMount;
import shipeditor.representation.weapon.WeaponEnums.WeaponRenderHints;
import shipeditor.utility.Utility;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.graphics.DrawUtilities;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.objects.Size2D;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;

@SuppressWarnings("OverlyCoupledClass")
@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WeaponCSVEntry implements LayerableEntry, InstallableEntry {

    private final Map<String, String> rowData;

    private final Path packageFolderPath;

    private final Path tableFilePath;

    private String weaponID;

    private WeaponSpecFile specFile;

    private WeaponSprites sprites;

    private Sprite weaponImage;

    public WeaponCSVEntry(Map<String, String> row, Path folder, Path tablePath) {
        this.rowData = row;
        packageFolderPath = folder;
        this.tableFilePath = tablePath;
        weaponID = this.rowData.get("id");
    }

    @Override
    public String getID() {
        return weaponID;
    }

    public void setWeaponID(String newID) {
        this.weaponID = newID;
        this.rowData.put("id", newID);
    }

    public void setSpecFile(WeaponSpecFile weaponSpecFile) {
        this.specFile = weaponSpecFile;
    }

    @Override
    public String getMultilineTooltip() {
        return getMultilineTooltip(null);
    }

    public String getMultilineTooltip(String appendHint) {
        String entryID = StringValues.WEAPON_ID + this.getWeaponID();
        WeaponType weaponType = this.getType();
        String type =  "Weapon type: " + weaponType.getDisplayedName();
        WeaponSize weaponSize = this.getSize();
        String size =  "Weapon size: " + weaponSize.getDisplayedName();
        if (appendHint != null) {
            return Utility.getWithLinebreaks(entryID, type, size, appendHint);
        }
        return Utility.getWithLinebreaks(entryID, type, size);
    }

    public WeaponType getType() {
        WeaponType mountTypeOverride = specFile.getMountTypeOverride();
        if (mountTypeOverride != null) {
            return mountTypeOverride;
        } else {
            return specFile.getType();
        }
    }

    public int getOPCost() {
        var data = this.getRowData();
        String costText = data.get("OPs");
        return Utility.parseIntegerOrDefault(costText, 0);
    }

    public int getDrawOrder() {
        WeaponSize specFileSize = getSize();
        return specFileSize.getNumericSize();
    }

    public WeaponSize getSize() {
        return specFile.getSize();
    }

    public WeaponSprites getSprites() {
        if (sprites == null) {
            WeaponSprites spriteHolder = new WeaponSprites();
            WeaponSpecFile weaponSpecFile = this.getSpecFile();

            String turretSprite = weaponSpecFile.getTurretSprite();
            setSpecSpriteFromPath(turretSprite, spriteHolder::setTurretSprite);
            String turretGunSprite = weaponSpecFile.getTurretGunSprite();
            setSpecSpriteFromPath(turretGunSprite, spriteHolder::setTurretGunSprite);
            String turretGlowSprite = weaponSpecFile.getTurretGlowSprite();
            setSpecSpriteFromPath(turretGlowSprite, spriteHolder::setTurretGlowSprite);
            String turretUnderSprite = weaponSpecFile.getTurretUnderSprite();
            setSpecSpriteFromPath(turretUnderSprite, spriteHolder::setTurretUnderSprite);

            String hardpointSprite = weaponSpecFile.getHardpointSprite();
            setSpecSpriteFromPath(hardpointSprite, spriteHolder::setHardpointSprite);
            String hardpointGunSprite = weaponSpecFile.getHardpointGunSprite();
            setSpecSpriteFromPath(hardpointGunSprite, spriteHolder::setHardpointGunSprite);
            String hardpointGlowSprite = weaponSpecFile.getHardpointGlowSprite();
            setSpecSpriteFromPath(hardpointGlowSprite, spriteHolder::setHardpointGlowSprite);
            String hardpointUnderSprite = weaponSpecFile.getHardpointUnderSprite();
            setSpecSpriteFromPath(hardpointUnderSprite, spriteHolder::setHardpointUnderSprite);

            sprites = spriteHolder;
        }
        return sprites;
    }

    public Sprite getWeaponImage() {
        if (weaponImage == null) {
            WeaponSprites spriteHolder = this.getSprites();

            var turretSprite = spriteHolder.getTurretSprite();
            if (turretSprite == null) return null;
            BufferedImage turretMain = turretSprite.getImage();

            var turretGunSprite = spriteHolder.getTurretGunSprite();
            BufferedImage turretGun = null;
            if (turretGunSprite != null) {
                turretGun = turretGunSprite.getImage();
            }

            BufferedImage combinedImage = new BufferedImage(turretMain.getWidth(), turretMain.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2d = combinedImage.createGraphics();

            boolean barrelsBelow = false;
            var renderHints = specFile.getRenderHints();
            if (renderHints != null && renderHints.contains(StringConstants.RENDER_BARREL_BELOW)) {
                barrelsBelow = true;
            }

            if (barrelsBelow) {
                if (turretGun != null) {
                    g2d.drawImage(turretGun, 0, 0, null);
                }
                g2d.drawImage(turretMain, 0, 0, null);
            } else {
                g2d.drawImage(turretMain, 0, 0, null);
                if (turretGun != null) {
                    g2d.drawImage(turretGun, 0, 0, null);
                }
            }

            g2d.dispose();
            weaponImage = new Sprite(combinedImage, turretSprite.getPath(), turretSprite.getFilename());
        }
        return weaponImage;
    }

    @Override
    public Sprite getEntrySprite() {
        return getWeaponImage();
    }

    /**
     * @param rotation - in degrees.
     */
    public void paintEntry(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer,
                           Matrix4f projection, Matrix4f view,
                           double rotation, Point2D targetLocation, WeaponMount mount) {
        WeaponSprites weaponSprites = getSprites();
        Sprite neededSprite = weaponSprites.getMainSprite(mount);
        DrawUtilities.paintInstallableGhostGL(spriteRenderer, projection, view,
                rotation, targetLocation, neededSprite);
    }


    @Override
    public WeaponLayer loadLayerFromEntry() {
        String turretSprite = this.specFile.getTurretSprite();

        Sprite sprite = null;

        if (turretSprite == null || turretSprite.isEmpty()) {
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    "Layer initialization warning, sprite file not defined for: " + this.getWeaponID() + ".\nIt will be loaded with missing graphics.",
                    StringValues.FILE_LOADING_ERROR,
                    JOptionPane.WARNING_MESSAGE);
        } else {
            Path spriteFilePath = Path.of(turretSprite);
            File spriteFile = FileLoading.fetchDataFile(spriteFilePath, this.packageFolderPath);

            if (spriteFile == null) {
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        "Layer initialization warning, sprite file not found for: " + this.getWeaponID() + ".\nIt will be loaded with missing graphics.",
                        StringValues.FILE_LOADING_ERROR,
                        JOptionPane.WARNING_MESSAGE);
            } else {
                sprite = FileLoading.loadSprite(spriteFile);
            }
        }

        var manager = StaticController.getLayerManager();
        if (manager == null) {
            throw new IllegalStateException("Layer manager is not found during runtime!");
        }
        WeaponLayer newLayer = manager.createWeaponLayer();
        newLayer.setSpecFile(specFile);

        WeaponPainter weaponPainter = createPainterFromEntry(newLayer, specFile);
        newLayer.setPainter(weaponPainter);

        manager.setActiveLayer(newLayer);
        var viewer = StaticController.getViewer();
        viewer.loadLayer(newLayer, sprite);

        EventBus.publish(new ActiveLayerUpdated(newLayer));

        return newLayer;
    }

    /**
     * @param layer can be null.
     */
    @SuppressWarnings("WeakerAccess")
    public WeaponPainter createPainterFromEntry(WeaponLayer layer, WeaponSpecFile weaponSpecFile) {
        WeaponPainter weaponPainter = new WeaponPainter(layer);
        var spriteHolder = this.getSprites();
        weaponPainter.setWeaponSprites(spriteHolder);
        weaponPainter.setWeaponID(weaponSpecFile.getId());

        if (weaponSpecFile.isRenderBelowAllWeapons()) {
            weaponPainter.setRenderOrderType(WeaponRenderOrdering.BELOW_ALL);
        } else if (weaponSpecFile.isRenderAboveAllWeapons()) {
            weaponPainter.setRenderOrderType(WeaponRenderOrdering.ABOVE_ALL);
        } else {
            weaponPainter.setRenderOrderType(WeaponRenderOrdering.NORMAL);
        }

        var hardpointOffsets = weaponSpecFile.getHardpointOffsets();
        var hardpointAngles = weaponSpecFile.getHardpointAngleOffsets();
        WeaponCSVEntry.initializeOffsets(weaponPainter, WeaponMount.HARDPOINT,
                hardpointOffsets, hardpointAngles);

        var turretOffsets = weaponSpecFile.getTurretOffsets();
        var turretAngles = weaponSpecFile.getTurretAngleOffsets();
        WeaponCSVEntry.initializeOffsets(weaponPainter, WeaponMount.TURRET,
                turretOffsets, turretAngles);

        var hiddenOffsets = weaponSpecFile.getHiddenOffsets();
        var hiddenAngles = weaponSpecFile.getHiddenAngleOffsets();
        WeaponCSVEntry.initializeOffsets(weaponPainter, WeaponMount.HIDDEN,
                hiddenOffsets, hiddenAngles);

        var renderHints = specFile.getRenderHints();
        if (renderHints != null && !renderHints.isEmpty()) {
            List<WeaponRenderHints> hintEnums = new ArrayList<>();
            renderHints.forEach(hintText -> {
                try {
                    hintEnums.add(WeaponRenderHints.valueOf(hintText));
                } catch (IllegalArgumentException ignored) {
                    // Unknown render hint from mod data; skip silently.
                }
            });
            weaponPainter.setRenderHints(hintEnums);
        }

        ProjectileSpecFile projectileSpec = GameDataRepository.getProjectileByID(specFile.getProjectileSpecId());
        if (projectileSpec != null) {
            String sprite = projectileSpec.getSprite();
            if (sprite != null && !sprite.isEmpty()) {
                Path projectileSpecSpritePath = Path.of(sprite);
                Path containingPackage = projectileSpec.getContainingPackage();
                File file = FileLoading.fetchDataFile(projectileSpecSpritePath, containingPackage);
                Sprite projectileSprite = FileLoading.loadSprite(file);

                double spriteWidth = projectileSpec.getSize()[0];
                double spriteHeight = projectileSpec.getSize()[1];
                ProjectilePainter projectilePainter = new ProjectilePainter(projectileSprite,
                        projectileSpec.getCenter(), new Size2D(spriteWidth, spriteHeight));
                weaponPainter.setProjectilePainter(projectilePainter);
            }
        }

        Sprite loadedTurretSprite = spriteHolder.getTurretSprite();
        if (loadedTurretSprite != null) {
            weaponPainter.setSprite(loadedTurretSprite);
        }
        return weaponPainter;
    }

    private static void initializeOffsets(WeaponPainter painter, WeaponMount mount,
                                          Point2D[] offsetPoints, double[] offsetAngles) {
        if (offsetPoints == null || offsetPoints.length == 0) return;
        int length = offsetPoints.length;
        painter.setMount(mount);
        var offsetPainter = painter.getOffsetPainter();
        for (int i = 0; i < length; i++) {
            Point2D offset = offsetPoints[i];
            Point2D rotated = ShipPainterInitialization.rotatePointByCenter(offset,
                    painter.getEntityCenter());
            OffsetPoint initialized = new OffsetPoint(rotated, painter);
            if (offsetAngles != null && offsetAngles.length > i) {
                initialized.setAngle(offsetAngles[i]);
            }

            offsetPainter.addPoint(initialized);
        }
    }

    private void setSpecSpriteFromPath(String pathInPackage, Consumer<Sprite> setter) {
        if (pathInPackage == null || pathInPackage.isEmpty()) {
            return;
        }
        Sprite sprite = Utility.loadSpriteFromPath(pathInPackage, this.packageFolderPath);
        setter.accept(sprite);
    }

    public JPanel createPickedWeaponPanel() {
        JPanel weaponPickPanel = new JPanel();
        weaponPickPanel.setLayout(new BoxLayout(weaponPickPanel, BoxLayout.LINE_AXIS));
        weaponPickPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        WeaponSize weaponSize = this.getSize();
        JLabel sizeLabel = new JLabel("[" + weaponSize.getDisplayedName() + "]");
        sizeLabel.setToolTipText(weaponSize.getDisplayedName());
        weaponPickPanel.add(sizeLabel);

        WeaponType weaponType = this.getType();
        JLabel typeLabel = new JLabel("[" + weaponType.getDisplayedName() + "]");
        typeLabel.setForeground(weaponType.getColor());
        typeLabel.setToolTipText(weaponType.getDisplayedName());
        weaponPickPanel.add(typeLabel);

        JLabel text = new JLabel(this.toString());
        text.setBorder(new EmptyBorder(0, 4, 0, 0));
        weaponPickPanel.add(text);

        Insets insets = new Insets(1, 0, 0, 0);
        ComponentUtilities.outfitPanelWithTitle(weaponPickPanel, insets, StringValues.PICKED_FOR_INSTALL);

        return weaponPickPanel;
    }

    @Override
    public String toString() {
        String displayedName = rowData.get(StringConstants.NAME);
        if (displayedName.isEmpty()) {
            displayedName = this.getWeaponID();
        }
        return displayedName;
    }

}
