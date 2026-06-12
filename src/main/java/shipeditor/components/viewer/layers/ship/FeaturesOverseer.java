package shipeditor.components.viewer.layers.ship;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import org.apache.commons.collections4.map.ListOrderedMap;
import shipeditor.communication.BusEventListener;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.control.ControlEvents.FeatureInstallQueued;
import shipeditor.components.datafiles.entities.InstallableEntry;
import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.entities.weapon.SlotData;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.layers.ship.data.Variant;
import shipeditor.components.viewer.layers.weapon.WeaponPainter;
import shipeditor.components.viewer.painters.points.ship.WeaponSlotPainter;
import shipeditor.components.viewer.ViewerEnums.FeatureOverrideState;
import shipeditor.components.viewer.ViewerEnums.FireMode;
import shipeditor.components.viewer.painters.points.ship.features.FittedWeaponGroup;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import shipeditor.representation.ship.VariantFile;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.overseers.StaticController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import shipeditor.communication.events.components.ComponentEvents.DeleteButtonPressed;
import shipeditor.communication.events.components.ComponentEvents.ShipEntryPicked;
import shipeditor.communication.events.components.ComponentEvents.WeaponEntryPicked;

/** * Responsible for all non-rendering interactions with installed features of layer,
 * be it base hull or skin built-in, or variant fits.*/
@SuppressWarnings({"WeakerAccess", "OverlyCoupledClass", "OverlyComplexClass", "ClassWithTooManyMethods"})
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class FeaturesOverseer {

    @SuppressWarnings("StaticNonFinalField")
    @Getter
    private static WeaponCSVEntry weaponForInstall;

    @SuppressWarnings("StaticNonFinalField")
    @Getter
    private static VariantFile moduleVariantForInstall;
    private final ShipLayer parent;

    FeaturesOverseer(ShipLayer layer) {
        this.parent = layer;
        this.initInstallListeners();
    }

    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static void setWeaponForInstall(WeaponCSVEntry entry) {
        FeaturesOverseer.weaponForInstall = entry;
        FeaturesOverseer.moduleVariantForInstall = null;
        EventBus.publish(new WeaponEntryPicked());
    }

    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static void setModuleForInstall(VariantFile variant) {
        FeaturesOverseer.weaponForInstall = null;
        FeaturesOverseer.moduleVariantForInstall = variant;
        EventBus.publish(new ShipEntryPicked());
    }

    
    // However, given the constraints, best to let it be and move on to finish the project.

    public Map<String, InstalledFeature> getBuiltInsFromBaseHull() {
        var painter = parent.getPainter();
        Supplier<Map<String, InstalledFeature>> getter = painter::getBuiltInWeapons;
        Map<String, InstalledFeature> result = getFilteredInstallables(getter, (slotPainter, featureEntry) -> {
            InstalledFeature installedFeature = featureEntry.getValue();
            return installedFeature.isNormalWeapon();
        });
        stampOverrideStates(result);
        return result;
    }

    public Map<String, InstalledFeature> getDecorativesFromBaseHull() {
        var painter = parent.getPainter();
        Supplier<Map<String, InstalledFeature>> getter = painter::getBuiltInWeapons;
        Map<String, InstalledFeature> result = getFilteredInstallables(getter, (slotPainter, featureEntry) -> {
            InstalledFeature installedFeature = featureEntry.getValue();
            return installedFeature.isDecoWeapon();
        });
        stampOverrideStates(result);
        return result;
    }

    /**
     * Eagerly computes and sets the override state on each feature so that
     * the cell renderer can read it directly without reaching into domain objects.
     */
    private void stampOverrideStates(Map<String, InstalledFeature> features) {
        if (features == null) return;
        var painter = parent.getPainter();
        if (painter == null) return;
        var slotPainter = painter.getWeaponSlotPainter();
        if (slotPainter == null) return;

        features.forEach((slotID, feature) -> {
            var slotPoint = slotPainter.getSlotByID(slotID);
            if (slotPoint != null) {
                feature.setOverrideState(getOverrideState(slotPoint));
            } else {
                feature.setOverrideState(FeatureOverrideState.NORMAL);
            }
        });
    }

    public List<String> getBuiltInsRemovedBySkin() {
        ShipPainter painter = parent.getPainter();
        if (painter == null) return null;
        ShipSkin activeSkin = painter.getActiveSkin();

        if (activeSkin != null && !activeSkin.isBase()) {
            return activeSkin.getRemoveBuiltInWeapons();

        }
        return null;
    }

    public Map<String, InstalledFeature> getBuiltInsFromSkin() {
        var painter = parent.getPainter();
        ShipSkin activeSkin = painter.getActiveSkin();

        if (activeSkin != null && !activeSkin.isBase()) {
            Supplier<Map<String, InstalledFeature>> getter = activeSkin::getInitializedBuiltIns;
            return getFilteredInstallables(getter, (slotPainter, featureEntry) -> {
                InstalledFeature installedFeature = featureEntry.getValue();
                return installedFeature.isNormalWeapon();
            });
        }
        return null;
    }

    public Map<String, InstalledFeature> getDecorativesFromSkin() {
        var painter = parent.getPainter();
        ShipSkin activeSkin = painter.getActiveSkin();

        if (activeSkin != null && !activeSkin.isBase()) {
            Supplier<Map<String, InstalledFeature>> getter = activeSkin::getInitializedBuiltIns;
            return getFilteredInstallables(getter, (slotPainter, featureEntry) -> {
                InstalledFeature installedFeature = featureEntry.getValue();
                return installedFeature.isDecoWeapon();
            });
        }
        return null;
    }

    public FeatureOverrideState getOverrideState(SlotData slotPoint) {
        var shipPainter = parent.getPainter();
        var skin = shipPainter.getActiveSkin();
        if (skin == null || skin.isBase()) return FeatureOverrideState.NORMAL;
        
        var removals = this.getBuiltInsRemovedBySkin();
        if (removals != null && removals.contains(slotPoint.getId())) {
            return FeatureOverrideState.REMOVED;
        }
        
        var skinBuiltIns = skin.getInitializedBuiltIns();
        if (skinBuiltIns != null && skinBuiltIns.containsKey(slotPoint.getId())) {
            return FeatureOverrideState.OVERRIDDEN;
        }
        
        return FeatureOverrideState.NORMAL;
    }

    Map<String, InstalledFeature> getFilteredInstallables(
            Supplier<Map<String, InstalledFeature>> getter,
            BiFunction<WeaponSlotPainter, Map.Entry<String, InstalledFeature>, Boolean> filter) {
        var painter = parent.getPainter();
        if (painter == null) return null;
        var installedFeatureMap = getter.get();
        if (installedFeatureMap == null) return null;
        var slotPainter = painter.getWeaponSlotPainter();
        if (slotPainter == null) return null;

        Map<String, InstalledFeature> result = new ListOrderedMap<>();
        Set<Map.Entry<String, InstalledFeature>> entries = installedFeatureMap.entrySet();
        Stream<Map.Entry<String, InstalledFeature>> stream = entries.stream();
        stream.forEach(featureEntry -> {
            if (filter.apply(slotPainter, featureEntry)) {
                result.put(featureEntry.getKey(), featureEntry.getValue());
            }
        });
        return result;
    }

    
    

    private Map<String, InstalledFeature> combineBuiltIns(Map<String, InstalledFeature> first, Map<String, InstalledFeature> second) {
        Map<String, InstalledFeature> combined = new ListOrderedMap<>();
        if (first != null) combined.putAll(first);
        if (second != null) combined.putAll(second);
        return combined;
    }

    private void updateBaseBuiltIns(Map<String, InstalledFeature> first, Map<String, InstalledFeature> second) {
        Map<String, InstalledFeature> combined = combineBuiltIns(first, second);
        ShipPainter shipPainter = parent.getPainter();

        combined.forEach((slotID, feature) -> feature.setContainedInBuiltIns(true));

        var oldCollection = shipPainter.getBuiltInWeapons();
        EditDispatch.postBuiltInFeaturesSorted(shipPainter::setBuiltInWeapons, oldCollection, combined);
    }

    private void updateSkinBuiltIns(Map<String, InstalledFeature> first, Map<String, InstalledFeature> second) {
        Map<String, InstalledFeature> combined = combineBuiltIns(first, second);
        ShipPainter shipPainter = parent.getPainter();

        ShipSkin activeSkin = shipPainter.getActiveSkin();

        var reconstructed = ShipSkin.reconstructAsEntries(combined);

        var oldCollection = activeSkin.getBuiltInWeapons();
        EditDispatch.postBuiltInFeaturesSorted(activeSkin::setBuiltInWeapons, oldCollection, reconstructed);
    }

    public void setBaseBuiltInsWithNewNormal(Map<String, InstalledFeature> rearrangedNormal) {
        updateBaseBuiltIns(rearrangedNormal, this.getDecorativesFromBaseHull());
    }

    public void setSkinBuiltInsWithNewNormal(Map<String, InstalledFeature> rearrangedNormal) {
        updateSkinBuiltIns(rearrangedNormal, this.getDecorativesFromSkin());
    }

    public void setBaseBuiltInsWithNewDecos(Map<String, InstalledFeature> rearrangedDecos) {
        updateBaseBuiltIns(this.getBuiltInsFromBaseHull(), rearrangedDecos);
    }

    public void setSkinBuiltInsWithNewDecos(Map<String, InstalledFeature> rearrangedDecos) {
        updateSkinBuiltIns(this.getBuiltInsFromSkin(), rearrangedDecos);
    }

    public void cleanupListeners() {
        EventBus.unsubscribeByParent(this);
    }

    private void initInstallListeners() {
        BusEventListener installListener = event -> {
            if (event instanceof FeatureInstallQueued) {
                if (StaticController.getActiveLayer() != parent) return;
                if (moduleVariantForInstall != null) {
                    addModuleToSelectedSlot(moduleVariantForInstall);
                }
                if (weaponForInstall != null) {
                    addWeaponToSelectedSlot(weaponForInstall);
                }
            }
        };
        EventBus.subscribe(this, installListener);

        BusEventListener uninstallListener = event -> {
            if (event instanceof DeleteButtonPressed) {
                if (StaticController.getActiveLayer() != parent) return;
                var shipPainter = parent.getPainter();
                if (shipPainter == null || shipPainter.isUninitialized()) return;
                var slotPainter = shipPainter.getWeaponSlotPainter();

                var mode = StaticController.getEditorMode();
                WeaponSlotPoint selected = slotPainter.getSelected();
                var eligibleSlots = slotPainter.getEligibleForSelection();

                if (selected == null || !eligibleSlots.contains(selected)) return;

                FeaturesOverseer.handleFeatureRemoval(selected, mode, shipPainter);
            }
        };
        EventBus.subscribe(this, uninstallListener);
    }

    public void addModuleToSelectedSlot(VariantFile moduleVariant) {
        chooseWeaponPointAndInstall((editorInstrument, slotPoint) -> {
            boolean choseVariant = moduleVariant != null && !moduleVariant.isEmpty();
            if (editorInstrument == EditorInstrument.VARIANT_MODULES && choseVariant) {
                if (slotPoint.isModule()) {
                    installModule(slotPoint, moduleVariant);
                }
            }
        });
    }

    public void addWeaponToSelectedSlot(WeaponCSVEntry weaponEntry) {
        chooseWeaponPointAndInstall((editorInstrument, slotPoint) -> {
            if (!WeaponType.isWeaponFitting(slotPoint, weaponEntry)) return;
            switch (editorInstrument) {
                case VARIANT_WEAPONS -> {
                    if (slotPoint.isFittable()) {
                        installWeapon(slotPoint, weaponEntry);
                    }
                }
                default -> {}
            }
        });
    }

    private void chooseWeaponPointAndInstall(BiConsumer<EditorInstrument,
            WeaponSlotPoint> installAction) {
        var shipPainter = parent.getPainter();
        if (shipPainter == null || shipPainter.isUninitialized()) return;
        var slotPainter = shipPainter.getWeaponSlotPainter();

        WeaponSlotPoint selected = slotPainter.getSelected();
        var eligibleSlots = slotPainter.getEligibleForSelection();

        if (selected == null || !eligibleSlots.contains(selected)) return;
        var mode = StaticController.getEditorMode();

        installAction.accept(mode, selected);
    }

    @SuppressWarnings({"MethodWithMultipleReturnPoints", "OverlyComplexMethod"})
    private static void handleFeatureRemoval(SlotData selected, EditorInstrument mode,
                                             ShipPainter shipPainter) {
        String slotID = selected.getId();
        switch (mode) {
            case VARIANT_WEAPONS -> {
                var variant = shipPainter.getActiveVariant();
                if (variant == null || variant.isEmpty()) return;
                FittedWeaponGroup targetGroup = variant.getGroupWithExistingMapping(slotID);

                if (targetGroup != null) {
                    targetGroup.removeBySlotID(slotID);
                }
            }
            case VARIANT_MODULES -> {
                var variant = shipPainter.getActiveVariant();
                if (variant == null || variant.isEmpty()) return;
                var modules = variant.getFittedModules();
                InstalledFeature toRemove = modules.get(slotID);
                if (toRemove == null) return;
                EditDispatch.postFeatureUninstalled(modules, slotID,
                        toRemove, null);
            }
            default -> {}
        }
    }

    private void installWeapon(SlotData selected, WeaponCSVEntry forInstall) {
        var shipPainter = parent.getPainter();
        var activeVariant = shipPainter.getActiveVariant();
        if (activeVariant == null || activeVariant.isEmpty()) return;

        String slotID = selected.getId();

        List<FittedWeaponGroup> weaponGroups = activeVariant.getWeaponGroups();

        WeaponSpecFile specFile = forInstall.getSpecFile();
        WeaponPainter weaponPainter = forInstall.createPainterFromEntry(null, specFile);
        InstalledFeature feature = InstalledFeature.of(slotID, forInstall.getWeaponID(),
                weaponPainter, forInstall);

        FittedWeaponGroup targetGroup = activeVariant.getGroupWithExistingMapping(slotID);
        Map<String, InstalledFeature> groupWeapons;
        if (targetGroup != null) {
            groupWeapons = targetGroup.getWeapons();
            InstalledFeature existing = groupWeapons.get(slotID);
            EditDispatch.postFeatureUninstalled(groupWeapons, slotID, existing, null);
        } else {
            if (weaponGroups.isEmpty()) {
                FittedWeaponGroup newGroup = new FittedWeaponGroup(activeVariant,
                        false, FireMode.ALTERNATING);
                weaponGroups.add(newGroup);
                targetGroup = newGroup;
            } else {
                targetGroup = weaponGroups.get(0);
            }
        }

        if (targetGroup == null) {
            targetGroup = new FittedWeaponGroup(activeVariant,
                    false, FireMode.LINKED);
            weaponGroups.add(targetGroup);
        }
        groupWeapons = targetGroup.getWeapons();
        feature.setParentGroup(targetGroup);
        FeaturesOverseer.commenceInstall(slotID, feature, groupWeapons, null);
    }

    private void installModule(SlotData selected, Variant variantFile) {
        var shipPainter = parent.getPainter();
        var activeVariant = shipPainter.getActiveVariant();
        if (activeVariant == null || activeVariant.isEmpty()) return;

        String slotID = selected.getId();
        var modules = activeVariant.getFittedModules();

        InstalledFeature moduleFeature = shipeditor.components.viewer.layers.LayerFactory.createModuleFromVariant(slotID, variantFile);

        InstalledFeature existing = modules.get(slotID);
        if (existing != null) {
            EditDispatch.postFeatureUninstalled(modules, slotID, existing, null);
        }

        FeaturesOverseer.commenceInstall(slotID, moduleFeature, modules, null);
    }

    public void uninstallBuiltIn(SlotData selected) {
        var shipPainter = parent.getPainter();
        if (shipPainter == null || shipPainter.isUninitialized()) return;
        String slotID = selected.getId();
        ShipSkin activeSkin = shipPainter.getActiveSkin();
        if (activeSkin != null && !activeSkin.isBase()) {
            var addedBySkin = activeSkin.getBuiltInWeapons();
            WeaponCSVEntry toRemove = addedBySkin.get(slotID);
            if (toRemove != null) {
                EditDispatch.postFeatureUninstalled(addedBySkin, slotID,
                        toRemove, activeSkin::invalidateBuiltIns);
            }
        } else {
            var weapons = shipPainter.getBuiltInWeapons();
            InstalledFeature toRemove = weapons.get(slotID);
            if (toRemove != null) {
                EditDispatch.postFeatureUninstalled(weapons, slotID,
                        toRemove, null);
            }
        }
    }

    public void installBuiltIn(SlotData selected, WeaponCSVEntry forInstall) {
        var shipPainter = parent.getPainter();
        var activeSkin = shipPainter.getActiveSkin();
        String slotID = selected.getId();

        ShipVariant activeVariant = shipPainter.getActiveVariant();

        if (activeSkin != null && !activeSkin.isBase()) {
            var skinBuiltIns = activeSkin.getBuiltInWeapons();

            Runnable invalidator = activeSkin::invalidateBuiltIns;
            FeaturesOverseer.removeExistingBeforeInstall(skinBuiltIns, activeVariant, slotID, invalidator);

            FeaturesOverseer.commenceInstall(slotID, forInstall, skinBuiltIns,
                    invalidator);
        } else {
            
            var baseBuiltIns = shipPainter.getBuiltInWeapons();

            FeaturesOverseer.removeExistingBeforeInstall(baseBuiltIns, activeVariant, slotID, null);

            WeaponSpecFile specFile = forInstall.getSpecFile();
            WeaponPainter weaponPainter = forInstall.createPainterFromEntry(null, specFile);
            InstalledFeature feature = InstalledFeature.of(slotID, forInstall.getWeaponID(),
                    weaponPainter, forInstall);
            feature.setContainedInBuiltIns(true);

            FeaturesOverseer.commenceInstall(slotID, feature, baseBuiltIns, null);
        }
    }

    private static <T extends InstallableEntry> void removeExistingBeforeInstall(Map<String, T> collection,
                                                                                 ShipVariant activeVariant,
                                                                                 String slotID, Runnable builtInInvalidator) {
        T existingBuiltIn = collection.get(slotID);
        if (existingBuiltIn != null) {
            EditDispatch.postFeatureUninstalled(collection, slotID, existingBuiltIn, builtInInvalidator);
        } else if (activeVariant != null) {
            FittedWeaponGroup targetGroup = activeVariant.getGroupWithExistingMapping(slotID);
            Map<String, InstalledFeature> groupWeapons;
            if (targetGroup != null) {
                groupWeapons = targetGroup.getWeapons();
                InstalledFeature existing = groupWeapons.get(slotID);
                EditDispatch.postFeatureUninstalled(groupWeapons, slotID, existing, null);
            }
        }
    }

    private static <T extends InstallableEntry> void commenceInstall(String slotID, T entry,
                                                                     Map<String, T> collection,
                                                                     Runnable invalidator) {
        EditDispatch.postFeatureInstalled(collection, slotID, entry, invalidator);
    }

}
