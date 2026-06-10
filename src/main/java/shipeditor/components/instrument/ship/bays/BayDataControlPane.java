package shipeditor.components.instrument.ship.bays;

import shipeditor.components.instrument.ship.shared.AbstractSlotValuesPanel;
import shipeditor.components.viewer.entities.WorldPoint;
import shipeditor.components.viewer.entities.weapon.SlotData;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.representation.weapon.WeaponMount;
import shipeditor.representation.weapon.WeaponSize;
import shipeditor.representation.weapon.WeaponType;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.overseers.StaticController;

import java.util.function.Consumer;

public class BayDataControlPane extends AbstractSlotValuesPanel {

    BayDataControlPane() {
        super(false);
    }

    @Override
    protected String getEntityName() {
        return "Bay";
    }

    @Override
    protected boolean shouldIncludeTypeSelector() {
        return false;
    }

    @Override
    protected SlotData getSelectedFromLayer(LayerPainter layerPainter) {
        if (layerPainter instanceof ShipPainter shipPainter) {
            if (shipPainter.isUninitialized()) return null;
            var bayPainter = shipPainter.getBayPainter();

            WorldPoint selected = bayPainter.getSelected();
            if (selected instanceof SlotData slotData) {
                return slotData;
            }
        }
        return null;
    }

    @Override
    protected String getNextUniqueID() {
        var layer = StaticController.getActiveLayer();
        if (!(layer instanceof ShipLayer shipLayer)) return null;
        var shipPainter = shipLayer.getPainter();
        if (shipPainter == null || shipPainter.isUninitialized()) return null;

        var bayPainter = shipPainter.getBayPainter();
        return bayPainter.generateUniqueBayID();
    }

    @Override
    protected Consumer<String> getIDSetter() {
        return newID -> {
            SlotData selectedPort = getSelectedPort();
            if (selectedPort != null) {
                EditDispatch.postSlotIDChanged(selectedPort, newID);
            }
        };
    }

    @Override
    protected Consumer<WeaponType> getTypeSetter() {
        throw new UnsupportedOperationException("Type selection is not relevant for launch bays!");
    }

    @Override
    protected Consumer<WeaponMount> getMountSetter() {
        return mount -> EditDispatch.postSlotMountChanged( getSelectedPort(), mount);
    }

    @Override
    protected Consumer<WeaponSize> getSizeSetter() {
        return weaponSize -> EditDispatch.postSlotSizeChanged( getSelectedPort(), weaponSize);
    }

    @Override
    protected Consumer<Double> getAngleSetter() {
        return angle -> {
            SlotData selectedPort = getSelectedPort();
            double oldValue = selectedPort.getAngle();
            EditDispatch.postSlotAngleSet(selectedPort, oldValue, angle);
        };
    }

    @Override
    protected Consumer<Double> getArcSetter() {
        return arc -> {
            SlotData selectedPort = getSelectedPort();
            double oldValue = selectedPort.getArc();
            EditDispatch.postSlotArcSet(selectedPort, oldValue, arc);
        };
    }

    @Override
    protected Consumer<Double> getRenderOrderSetter() {
        return renderOrder -> {
            SlotData selectedPort = getSelectedPort();
            int oldValue = selectedPort.getRenderOrderMod();
            EditDispatch.postRenderOrderChanged(selectedPort, oldValue, renderOrder.intValue());
        };
    }

    private SlotData getSelectedPort() {
        ShipPainter cachedLayerPainter = getCachedLayerPainter();
        return getSelectedFromLayer(cachedLayerPainter);
    }

}
