package oth.shipeditor.components.viewer.layers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.files.*;
import oth.shipeditor.communication.events.viewer.layers.*;
import oth.shipeditor.communication.events.viewer.layers.ships.LayerShipDataInitialized;
import oth.shipeditor.communication.events.viewer.layers.ships.ShipLayerCreated;
import oth.shipeditor.communication.events.viewer.layers.ships.ShipLayerCreationQueued;
import oth.shipeditor.communication.events.viewer.layers.weapons.WeaponLayerCreated;
import oth.shipeditor.communication.events.viewer.layers.weapons.WeaponLayerCreationQueued;
import oth.shipeditor.components.datafiles.entities.ShipCSVEntry;
import oth.shipeditor.components.viewer.layers.ship.ShipLayer;
import oth.shipeditor.components.viewer.layers.ship.ShipPainter;
import oth.shipeditor.components.viewer.layers.ship.data.ActiveShipSpec;
import oth.shipeditor.components.viewer.layers.ship.data.ShipHull;
import oth.shipeditor.components.viewer.layers.ship.data.ShipSkin;
import oth.shipeditor.components.viewer.layers.weapon.WeaponLayer;
import oth.shipeditor.representation.GameDataRepository;
import oth.shipeditor.representation.ship.HullSpecFile;
import oth.shipeditor.representation.ship.SkinSpecFile;
import oth.shipeditor.utility.graphics.Sprite;
import oth.shipeditor.utility.overseers.StaticController;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import oth.shipeditor.components.viewer.layers.ship.data.ShipVariant;
import oth.shipeditor.communication.events.files.saving.HullSaveQueued;
import oth.shipeditor.communication.events.files.saving.VariantSaveQueued;
import oth.shipeditor.communication.events.components.LayerTabUpdated;

@Getter
@SuppressWarnings("OverlyCoupledClass")
@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class LayerManager {

    @Setter
    private List<ViewerLayer> layers = new ArrayList<>();

    private ViewerLayer activeLayer;

    private final Map<ViewerLayer, List<String>> unsavedChangesRegistry = new HashMap<>();

    public void markUnsaved(ViewerLayer layer, String changeType) {
        List<String> types = unsavedChangesRegistry.computeIfAbsent(layer, k -> new ArrayList<>());
        if (!types.contains(changeType)) {
            types.add(changeType);
            EventBus.publish(new LayerTabUpdated(layer));
        }
    }

    public void markSaved(ViewerLayer layer, String changeType) {
        List<String> types = unsavedChangesRegistry.get(layer);
        if (types != null) {
            types.remove(changeType);
            if (types.isEmpty()) {
                unsavedChangesRegistry.remove(layer);
            }
            EventBus.publish(new LayerTabUpdated(layer));
        }
    }

    public boolean isLayerDirty(ViewerLayer layer) {
        List<String> types = unsavedChangesRegistry.get(layer);
        return types != null && !types.isEmpty();
    }

    public boolean isHullDirty(ViewerLayer layer) {
        List<String> types = unsavedChangesRegistry.get(layer);
        return types != null && types.contains("hull");
    }

    public boolean isVariantDirty(ViewerLayer layer) {
        List<String> types = unsavedChangesRegistry.get(layer);
        return types != null && types.contains("variant");
    }

    public void initListeners() {
        this.initLayerListening();
        this.initOpenSpriteListener();
        this.initOpenHullListener();
    }

    public boolean isEmpty() {
        return layers.isEmpty();
    }

    public void activateLastLayer() {
        ViewerLayer next = layers.get(layers.size() - 1);
        this.setActiveLayer(next);
    }

    public void setActiveLayer(ViewerLayer newlySelected) {
        ViewerLayer old = this.activeLayer;
        this.activeLayer = newlySelected;
        StaticController.setActiveLayer(activeLayer);
        EventBus.publish(new LayerWasSelected(old, newlySelected));
    }

    public ShipLayer createShipLayer() {
        ShipLayer newLayer = new ShipLayer();
        layers.add(newLayer);
        EventBus.publish(new ShipLayerCreated(newLayer));
        return newLayer;
    }

    public WeaponLayer createWeaponLayer() {
        WeaponLayer newLayer = new WeaponLayer();
        layers.add(newLayer);
        EventBus.publish(new WeaponLayerCreated(newLayer));
        return newLayer;
    }

    @SuppressWarnings({"OverlyCoupledMethod", "ChainOfInstanceofChecks", "OverlyComplexMethod"})
    private void initLayerListening() {
        EventBus.subscribe(this, event -> {
            if (event instanceof ShipLayerCreationQueued) {
                this.createShipLayer();
            } else if (event instanceof WeaponLayerCreationQueued) {
                this.createWeaponLayer();
            }
        });
        // It is implicitly assumed that the last layer in list is also the one that was just created.
        EventBus.subscribe(this, event -> {
            if (event instanceof LastLayerSelectQueued) {
                activateLastLayer();
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof ActiveLayerRemovalQueued) {
                ViewerLayer selected = this.activeLayer;
                publishLayerRemoval(selected);
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerRemovalQueued checked) {
                ViewerLayer layer = checked.layer();
                publishLayerRemoval(layer);
            }
        });
        
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerOpacityChangeQueued checked) {
                if (activeLayer == null) return;
                LayerPainter painter = activeLayer.getPainter();
                if (painter == null) return;
                painter.setSpriteOpacity(checked.changedValue());
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerShipDataInitialized checked) {
                ShipPainter source = checked.source();
                ShipLayer parentLayer = source.getParentLayer();
                if (parentLayer != null) {
                    this.setActiveLayer(parentLayer);
                    EventBus.publish(new ActiveLayerUpdated(this.getActiveLayer()));
                }
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof ActiveLayerUpdated checked) {
                setActiveLayer(checked.updated());
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof HullmodDataSet) {
                actOnAllLayerHulls(ShipHull::loadBuiltInMods);
            } else if (event instanceof WingDataSet) {
                actOnAllLayerHulls(ShipHull::loadBuiltInWings);
            }
        });
    }

    private void actOnAllLayerHulls(BiConsumer<ShipHull, HullSpecFile> action) {
        layers.forEach(layer -> {
            if (layer instanceof ShipLayer checkedLayer) {
                ShipHull hull = checkedLayer.getHull();
                if (hull != null) {
                    ShipCSVEntry shipEntry = GameDataRepository.retrieveShipCSVEntryByID(hull.getHullID());
                    if (shipEntry != null) {
                        HullSpecFile hullSpec = shipEntry.getHullSpecFile();
                        if (hullSpec != null) {
                            action.accept(hull, hullSpec);
                        }
                    }
                }
            }
        });
        setActiveLayer(this.getActiveLayer());
    }

    public boolean confirmLayerRemoval(ViewerLayer layer) {
        if (!(layer instanceof ShipLayer shipLayer)) {
            return true;
        }

        boolean hullDirty = isHullDirty(shipLayer);
        boolean variantDirty = isVariantDirty(shipLayer);

        if (!hullDirty && !variantDirty) {
            return true;
        }

        setActiveLayer(shipLayer);

        String title = "Unsaved Changes";
        String message = "Layer has unsaved changes. Do you want to save them before closing?";

        if (hullDirty && variantDirty) {
            Object[] options = {"Save Both", "Save Hull", "Save Variant", "Don't Save", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                    oth.shipeditor.PrimaryWindow.getInstance(),
                    message,
                    title,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == 0) { // Save Both
                EventBus.publish(new HullSaveQueued(shipLayer));
                ShipVariant variant = shipLayer.getActiveVariant();
                if (variant != null && !variant.isEmpty()) {
                    EventBus.publish(new VariantSaveQueued(variant));
                }
                return !isHullDirty(shipLayer) && !isVariantDirty(shipLayer);
            } else if (choice == 1) { // Save Hull
                EventBus.publish(new HullSaveQueued(shipLayer));
                return !isHullDirty(shipLayer);
            } else if (choice == 2) { // Save Variant
                ShipVariant variant = shipLayer.getActiveVariant();
                if (variant != null && !variant.isEmpty()) {
                    EventBus.publish(new VariantSaveQueued(variant));
                }
                return !isVariantDirty(shipLayer);
            } else if (choice == 3) { // Don't Save
                unsavedChangesRegistry.remove(shipLayer);
                return true;
            } else { // Cancel
                return false;
            }
        } else if (hullDirty) {
            Object[] options = {"Save Hull", "Don't Save", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                    oth.shipeditor.PrimaryWindow.getInstance(),
                    "Hull has unsaved changes. Do you want to save?",
                    title,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == 0) { // Save Hull
                EventBus.publish(new HullSaveQueued(shipLayer));
                return !isHullDirty(shipLayer);
            } else if (choice == 1) { // Don't Save
                unsavedChangesRegistry.remove(shipLayer);
                return true;
            } else { // Cancel
                return false;
            }
        } else { // variantDirty
            Object[] options = {"Save Variant", "Don't Save", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                    oth.shipeditor.PrimaryWindow.getInstance(),
                    "Variant has unsaved changes. Do you want to save?",
                    title,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == 0) { // Save Variant
                ShipVariant variant = shipLayer.getActiveVariant();
                if (variant != null && !variant.isEmpty()) {
                    EventBus.publish(new VariantSaveQueued(variant));
                }
                return !isVariantDirty(shipLayer);
            } else if (choice == 1) { // Don't Save
                unsavedChangesRegistry.remove(shipLayer);
                return true;
            } else { // Cancel
                return false;
            }
        }
    }

    private void publishLayerRemoval(ViewerLayer layer) {
        if (!confirmLayerRemoval(layer)) {
            return;
        }
        if (layers.size() >= 2) {
            ViewerLayer other = null;
            for (ViewerLayer checked : layers) {
                if (checked != layer) {
                    other = checked;
                }
            }
            this.setActiveLayer(other);
        } else {
            this.setActiveLayer(null);
        }
        layers.remove(layer);
        unsavedChangesRegistry.remove(layer);
        EventBus.publish(new ViewerLayerRemovalConfirmed(layer));
    }

    private void initOpenSpriteListener() {
        EventBus.subscribe(this, event -> {
            if (event instanceof SpriteOpened checked) {
                Sprite sprite = checked.sprite();
                if (activeLayer == null) return;
                EventBus.publish(new LayerSpriteLoadQueued(activeLayer, sprite));
            }
        });
    }

    private void initOpenHullListener() {
        EventBus.subscribe(this, event -> {
            if (event instanceof HullFileOpened checked) {
                HullSpecFile hullSpecFile = checked.hullSpecFile();
                if (activeLayer != null && activeLayer instanceof ShipLayer checkedLayer) {
                    checkedLayer.initializeHullData(hullSpecFile);
                } else {
                    throw new IllegalStateException("Hull file loaded onto invalid layer!");
                }
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof SkinFileOpened checked) {
                SkinSpecFile skinSpecFile = checked.skinSpecFile();
                if (activeLayer != null && activeLayer instanceof ShipLayer checkedLayer) {
                    ShipHull data = checkedLayer.getHull();
                    if (data != null) {
                        LayerManager.openSkinFile(checkedLayer, data, skinSpecFile, checked.setAsActive());
                    } else {
                        throw new IllegalStateException("Skin file loaded onto a null ship data!");
                    }
                    EventBus.publish(new ActiveLayerUpdated(checkedLayer));
                } else {
                    throw new IllegalStateException("Skin file loaded onto invalid layer!");
                }
            }
        });
    }

    @SuppressWarnings("BooleanParameter")
    public static void openSkinFile(ShipLayer checkedLayer, ShipHull data,
                                    SkinSpecFile skinSpecFile, boolean setAsActive) {
        String hullID = data.getHullID();
        if (!hullID.equals(skinSpecFile.getBaseHullId())) {
            Path skinFilePath = skinSpecFile.getFilePath();
            JOptionPane.showMessageDialog(oth.shipeditor.PrimaryWindow.getInstance(),
                    "Hull ID of active layer does not equal base hull ID of skin: " +
                            Optional.of(skinFilePath.toString()).orElse(skinSpecFile.toString()),
                    "Ship ID mismatch!",
                    JOptionPane.ERROR_MESSAGE);
            throw new IllegalStateException("Illegal skin file opening operation!");
        }
        ShipSkin created = checkedLayer.addSkin(skinSpecFile);
        if (setAsActive) {
            ShipPainter shipPainter = checkedLayer.getPainter();
            shipPainter.setActiveSpec(ActiveShipSpec.SKIN, created);
        }
    }

}
