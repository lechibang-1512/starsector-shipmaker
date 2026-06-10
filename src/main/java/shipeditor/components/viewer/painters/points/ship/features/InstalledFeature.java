package shipeditor.components.viewer.painters.points.ship.features;

import shipeditor.utility.graphics.opengl.OpenGLPainter;
import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import shipeditor.components.datafiles.entities.CSVEntry;
import shipeditor.components.datafiles.entities.InstallableEntry;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import java.util.Comparator;
import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.layers.weapon.WeaponPainter;
import shipeditor.components.viewer.painters.points.AbstractPointPainter;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.SizeEnum;
import shipeditor.representation.ship.VariantFile;
import shipeditor.representation.weapon.WeaponMount;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.representation.weapon.WeaponType;
import shipeditor.utility.Utility;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.text.StringConstants;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;

@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public final class InstalledFeature implements InstallableEntry {

    /**
     * Comparator for serialization output: normal built-in weapons first, decoratives last,
     * with secondary alphabetical sort by slot ID to match vanilla conventions.
     */
    public static final Comparator<Map.Entry<String, InstalledFeature>> SERIALIZATION_ORDER =
            Comparator.<Map.Entry<String, InstalledFeature>, Boolean>comparing(
                            entry -> entry.getValue().isDecoWeapon())
                    .thenComparing(Map.Entry::getKey);

    private final String slotID;

    private final String featureID;

    private final LayerPainter featurePainter;

    private final CSVEntry dataEntry;

    @Setter
    private boolean invalidated;

    @Setter
    private boolean containedInBuiltIns;

    /**
     * Override state relative to the active skin; computed by {@code FeaturesOverseer}
     * and consumed by the cell renderer. Defaults to {@code NORMAL}.
     */
    @Setter
    private FeatureOverrideState overrideState = FeatureOverrideState.NORMAL;

    /**
     * Can be null, only relevant for variant interaction.
     */
    @Setter
    private FittedWeaponGroup parentGroup;

    private final AffineTransform cachedTransform = new AffineTransform();

    private InstalledFeature(String slot, String id, LayerPainter painter, CSVEntry entry) {
        this.slotID = slot;
        this.featureID = id;
        this.featurePainter = painter;
        this.dataEntry = entry;
        featurePainter.setShouldDrawPainter(false);
    }

    public void cleanupForRemoval() {
        featurePainter.cleanupForRemoval();
    }

    public static InstalledFeature of(String slot, String id, LayerPainter painter, CSVEntry entry) {
        if (entry instanceof InstallableEntry) {
            return new InstalledFeature(slot, id, painter, entry);
        } else throw new IllegalArgumentException("Illegal data entry passed for installable feature!");
    }

    public WeaponType getWeaponType() {
        if (dataEntry instanceof WeaponCSVEntry weaponEntry) {
            return weaponEntry.getType();
        } else {
            return WeaponType.STATION_MODULE;
        }
    }

    public SizeEnum getSize() {
        if (dataEntry instanceof WeaponCSVEntry weaponEntry) {
            return weaponEntry.getSize();
        } else {
            ShipCSVEntry shipEntry = (ShipCSVEntry) dataEntry;
            return shipEntry.getSize();
        }
    }

    public int getOPCost() {
        if (dataEntry instanceof WeaponCSVEntry weaponEntry) {
            if (containedInBuiltIns) return 0;
            return weaponEntry.getOPCost();
        } else {
            return 0;
        }
    }

    public boolean isDecoWeapon() {
        if (dataEntry instanceof WeaponCSVEntry weaponEntry) {
            return weaponEntry.getType() == WeaponType.DECORATIVE;
        }
        return false;
    }

    public boolean isNormalWeapon() {
        if (dataEntry instanceof WeaponCSVEntry weaponEntry) {
            return weaponEntry.getType() != WeaponType.DECORATIVE;
        }
        return false;
    }

    public String getName() {
        return dataEntry.toString();
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    int computeRenderOrder(WeaponSlotPoint slotPoint) {
        int result = Integer.MIN_VALUE;
        if (featurePainter instanceof WeaponPainter weaponPainter) {
            double slotOffset = slotPoint.getOffsetRelativeToAxis();
            double rawResult;
            switch (weaponPainter.getRenderOrderType()) {
                case BELOW_ALL -> rawResult = 0 - slotOffset + slotPoint.getRenderOrderMod();
                case ABOVE_ALL -> rawResult  = 100000 - slotOffset + slotPoint.getRenderOrderMod();
                default -> {
                    WeaponCSVEntry weaponCSVEntry = (WeaponCSVEntry) this.getDataEntry();

                    rawResult = weaponCSVEntry.getDrawOrder() * 2;
                    boolean weaponIsMissile = weaponCSVEntry.getType() == WeaponType.MISSILE;

                    WeaponSpecFile specFile = weaponCSVEntry.getSpecFile();
                    List<String> renderHints = specFile.getRenderHints();
                    boolean hasTargetHint = false;
                    if (renderHints != null && !renderHints.isEmpty()) {
                        hasTargetHint = renderHints.contains(StringConstants.RENDER_LOADED_MISSILES)
                                || renderHints.contains(StringConstants.RENDER_LOADED_MISSILES_UNLESS_HIDDEN);
                    }
                    if (weaponIsMissile && hasTargetHint) {
                        rawResult -= 1;
                    }
                    if (slotPoint.getWeaponMount() != WeaponMount.HARDPOINT) {
                        rawResult += 20;
                    }
                    rawResult += slotOffset + slotPoint.getRenderOrderMod();
                }
            }
            result = (int) Math.ceil(rawResult);
        } else if (featurePainter instanceof ShipPainter) {
            CSVEntry entry = this.getDataEntry();
            Map<String, String> rowData = entry.getRowData();
            String hints = rowData.get(StringConstants.HINTS);
            if (!hints.contains("UNDER_PARENT")) {
                result = Integer.MIN_VALUE + 1;
            }
        }
        return result;
    }

    void refreshPaintCircumstance(WeaponSlotPoint slotPoint) {
        LayerPainter painter = this.getFeaturePainter();
        painter.setShouldDrawPainter(false);
        if (slotPoint == null) {
            return;
        }
        painter.setShouldDrawPainter(true);

        InstalledFeature.configurePainterBySlot(slotPoint, painter);
    }

    private static void configurePainterBySlot(WeaponSlotPoint slotPoint, LayerPainter painter) {
        Point2D position = slotPoint.getPosition();
        Point2D entityCenter = painter.getCenterAnchorDifference();
        if (painter instanceof WeaponPainter weaponPainter) {
            weaponPainter.setMount(slotPoint.getWeaponMount());
        }
        double x = position.getX() - entityCenter.getX();
        double y = position.getY() - entityCenter.getY();
        Point2D newAnchor = new Point2D.Double(x, y);
        Point2D painterAnchor = painter.getAnchor();
        if (!painterAnchor.equals(newAnchor)) {
            painter.setAnchor(newAnchor);
        }

        double transformedAngle = Utility.transformAngle(slotPoint.getAngle());
        double rotationRadians = Math.toRadians(transformedAngle + 90);
        painter.setRotationRadians(rotationRadians + slotPoint.getParent().getRotationRadians());
    }

    public void loadAsSeparateLayer() {
        LayerPainter layerPainter = this.getFeaturePainter();
        if (layerPainter instanceof ShipPainter shipPainter) {
            float opacity = 0.3f;
            layerPainter.setSpriteOpacity(opacity);

            ShipVariant variant = shipPainter.getActiveVariant();
            variant.setOpacityForAllFitted(opacity);

            VariantFile rawVariant = GameDataRepository.getVariantByID(variant.getVariantId());
            if (rawVariant != null && !rawVariant.isEmpty()) {
                ShipLayer loadedFromModule = shipeditor.components.viewer.layers.LayerFactory.createLayerFromVariant(rawVariant);

                ShipPainter newPainter = loadedFromModule.getPainter();

                Point2D anchor = layerPainter.getAnchor();
                newPainter.setAnchor(new Point2D.Double(anchor.getX(), anchor.getY()));

                double rotationDegrees = Math.toDegrees(shipPainter.getRotationRadians());
                newPainter.rotateLayer(rotationDegrees);
            }
        } else {
            throw new IllegalStateException("Can only load modules as separate layers!");
        }
    }

    @Override
    public Sprite getEntrySprite() {
        LayerPainter painter = getFeaturePainter();
        return painter.getSprite();
    }

    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        LayerPainter layerPainter = this.getFeaturePainter();
        if (layerPainter == null) return;
        layerPainter.paint(spriteRenderer, shapeRenderer, projection, view);

        List<AbstractPointPainter> allPainters = layerPainter.getAllPainters();
        for (AbstractPointPainter pointPainter : allPainters) {
            pointPainter.paint(spriteRenderer, shapeRenderer, projection, view);
        }
    }

    @Override
    public String getID() {
        return featureID;
    }

}
