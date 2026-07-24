package shipeditor.components.viewer.painters.points.ship.features;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.control.ControlPredicates;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.ViewerEnums.PainterVisibility;
import shipeditor.components.viewer.painters.points.ship.WeaponSlotPainter;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;

import java.util.*;
import java.util.function.Predicate;

@Getter @Setter
@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public final class InstalledFeaturePainter {

    private PainterVisibility builtInsVisibility;

    private PainterVisibility decorativesVisibility;

    private WeaponSlotPoint cachedSelectCounterpart;

    /**
     * Has magic numbers: Integer.MIN_VALUE is for modules with UNDER_PARENT tag,
     * same number + 1 is for normal modules.
     */
    private Map<Integer, Set<InstalledFeature>> orderedRenderQueue;

    public InstalledFeaturePainter() {
        this.builtInsVisibility = PainterVisibility.ALWAYS_SHOWN;
        this.decorativesVisibility = PainterVisibility.ALWAYS_SHOWN;
    }

    private static boolean checkVisibility(ShipPainter painter, PainterVisibility visibility,
                                           EditorInstrument featureKind) {
        boolean layerActive = painter.isLayerActive();
        if (visibility == PainterVisibility.ALWAYS_HIDDEN) return false;

        EditorInstrument editorMode = StaticController.getEditorMode();

        boolean eligible = editorMode == featureKind && painter.isLayerActive();
        if (visibility == PainterVisibility.SHOWN_WHEN_EDITED && eligible) return true;
        if (visibility == PainterVisibility.SHOWN_WHEN_SELECTED && layerActive) return true;
        return visibility == PainterVisibility.ALWAYS_SHOWN;
    }

    private static boolean isInteractable(ShipPainter painter) {
        EditorInstrument editorMode = StaticController.getEditorMode();
        boolean eligibleMode = editorMode == EditorInstrument.VARIANT_WEAPONS
                || editorMode == EditorInstrument.VARIANT_MODULES;
        return eligibleMode && painter.isLayerActive();
    }

    public void updateRenderQueue(ShipPainter painter) {
        Map<Integer, Set<InstalledFeature>> result = new TreeMap<>();

        var slotPainter = painter.getWeaponSlotPainter();

        Map<String, InstalledFeature> toPrepare = new LinkedHashMap<>();

        var decoratives = painter.getBuiltInsWithSkin(true, false);
        if (InstalledFeaturePainter.checkVisibility(painter, this.decorativesVisibility,
                EditorInstrument.WEAPON_SLOTS)) {
            toPrepare.putAll(decoratives);
        }

        var builtIns = painter.getBuiltInsWithSkin(false, true);
        if (InstalledFeaturePainter.checkVisibility(painter, this.builtInsVisibility,
                EditorInstrument.WEAPON_SLOTS)) {
            builtIns.forEach(toPrepare::putIfAbsent);
        }

        var builtInModules = painter.getBuiltInModules();
        if (builtInModules != null) {
            builtInModules.forEach(toPrepare::putIfAbsent);
        }

        ShipVariant shipVariant = painter.getActiveVariant();
        if (shipVariant != null && !shipVariant.isEmpty()) {
            var modules = shipVariant.getFittedModules();
            if (modules != null) {
                modules.forEach(toPrepare::putIfAbsent);
            }
            var allWeapons = shipVariant.getAllFittedWeapons();
            if (allWeapons != null) {
                allWeapons.forEach(toPrepare::putIfAbsent);
            }
        }

        toPrepare.forEach((slotID, feature) -> this.prepareFeature(result, slotPainter, slotID, feature));

        if (cachedSelectCounterpart != null && ControlPredicates.isMirrorModeEnabled()) {
            result.forEach((integer, installedFeatures) -> installedFeatures.forEach(feature -> {
                String slotID = feature.getSlotID();
                if (cachedSelectCounterpart != null && slotID.equals(cachedSelectCounterpart.getId())) {
                    LayerPainter featurePainter = feature.getFeaturePainter();
                    featurePainter.setSpriteOpacity(0.75f);
                    cachedSelectCounterpart = null;
                }
            }));
            cachedSelectCounterpart = null;
        }

        this.orderedRenderQueue = result;
    }

    private void prepareFeature(Map<Integer, Set<InstalledFeature>> collection,
                                WeaponSlotPainter slotPainter, String slotID,
                                InstalledFeature feature) {
        int renderOrder = this.refreshSlotData(slotPainter, slotID, feature);
        if (renderOrder == Integer.MAX_VALUE) return;
        var renderLayer = collection.computeIfAbsent(renderOrder,
                k -> new LinkedHashSet<>());
        renderLayer.add(feature);
        collection.put(renderOrder, renderLayer);
    }

    /**
     * @return integer's max value as a magic number if the respective slot is not found or invalid.
     */
    private int refreshSlotData(WeaponSlotPainter slotPainter, String slotID,
                                InstalledFeature feature) {
        WeaponSlotPoint slotPoint = slotPainter.getSlotByID(slotID);

        if (slotPoint == null || !slotPoint.canFit(feature)) {
            feature.setInvalidated(true);
            return Integer.MAX_VALUE;
        }
        if (slotPoint.isPointSelected() && InstalledFeaturePainter.isInteractable(slotPainter.getParentLayer())) {
            cachedSelectCounterpart = (WeaponSlotPoint) slotPainter.getMirroredCounterpart(slotPoint);
        }

        feature.setInvalidated(false);
        feature.setAssociatedSlotSelected(slotPoint.isPointSelected());
        int renderOrder = feature.computeRenderOrder(slotPoint);
        feature.refreshPaintCircumstance(slotPoint);
        return renderOrder;
    }

    public void paintUnderParent(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        this.paintFeatures(spriteRenderer, shapeRenderer, projection, view, integer -> integer == Integer.MIN_VALUE);
    }

    public void paintNormal(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        this.paintFeatures(spriteRenderer, shapeRenderer, projection, view, integer -> integer != Integer.MIN_VALUE);
    }

    private void paintFeatures(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view,
                               Predicate<Integer> predicate) {
        if (orderedRenderQueue == null) return;
        this.orderedRenderQueue.forEach((integer, installedFeatures) -> {
            if (predicate.test(integer)) {
                installedFeatures.forEach(feature -> feature.paint(spriteRenderer, shapeRenderer, projection, view));
            }
        });
    }

    public java.awt.geom.Rectangle2D getVisualBounds() {
        if (orderedRenderQueue == null) return null;
        java.awt.geom.Rectangle2D totalBounds = null;
        for (Set<InstalledFeature> features : orderedRenderQueue.values()) {
            for (InstalledFeature feature : features) {
                LayerPainter painter = feature.getFeaturePainter();
                if (painter != null && !painter.isUninitialized()) {
                    java.awt.geom.Rectangle2D bounds = painter.getVisualBounds();
                    if (totalBounds == null) {
                        totalBounds = bounds;
                    } else {
                        java.awt.geom.Rectangle2D.union(totalBounds, bounds, totalBounds);
                    }
                }
            }
        }
        return totalBounds;
    }

}
