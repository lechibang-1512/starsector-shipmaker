package shipeditor.components.viewer.layers;

import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ActiveShipSpec;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.components.viewer.layers.ship.data.Variant;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import shipeditor.representation.GameDataRepository;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.representation.ship.ShipSpecFile;
import shipeditor.representation.ship.SkinSpecFile;

/**
 * Factory class to instantiate and set up ship layers and module painters.
 * Decouples the representation/repository layer from the viewer layers/painters.
 */
public final class LayerFactory {

    private LayerFactory() {}

    /**
     * Creates a ShipLayer instance from a Variant.
     */
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
        } else if (specFile != null) {
            baseHullId = specFile.getHullId();
        } else {
            baseHullId = shipHullId;
        }
        if (baseHullId == null) return null;
        ShipCSVEntry csvEntry = GameDataRepository.retrieveShipCSVEntryByID(baseHullId);
        if (csvEntry == null) return null;
        ShipLayer shipLayer = csvEntry.loadLayerFromEntry();
        if (shipLayer == null) return null;
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

    /**
     * Creates an InstalledFeature (module) from a Variant.
     */
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
        } else if (specFile != null) {
            baseHullId = specFile.getHullId();
        } else {
            baseHullId = shipHullId;
        }
        if (baseHullId == null) return null;
        ShipCSVEntry csvEntry = GameDataRepository.retrieveShipCSVEntryByID(baseHullId);
        if (csvEntry == null) return null;
        ShipPainter modulePainter = csvEntry.createPainterFromEntry(null);
        if (modulePainter == null) return null;

        if (skinSpec != null) {
            ShipSkin shipSkin = ShipSkin.createFromSpec(skinSpec);
            modulePainter.setActiveSpec(ActiveShipSpec.SKIN, shipSkin);
        }

        modulePainter.selectVariant(variant);
        return InstalledFeature.of(slotID, variant.getVariantId(), modulePainter, csvEntry);
    }
}
