package oth.shipeditor.undo.edits;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.viewer.layers.ActiveLayerUpdated;
import oth.shipeditor.components.datafiles.entities.HullmodCSVEntry;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.components.viewer.layers.ship.ShipLayer;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

import java.util.List;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class HullmodAddEdit extends AbstractEdit implements LayerEdit {

    private final List<HullmodCSVEntry> hullmodIndex;

    private ShipLayer layer;

    private final HullmodCSVEntry entry;

    public HullmodAddEdit(List<HullmodCSVEntry> index, ShipLayer shipLayer, HullmodCSVEntry hullmod) {
        this.hullmodIndex = index;
        this.layer = shipLayer;
        this.entry = hullmod;
    }

    @Override
    public void undo() {
        hullmodIndex.remove(entry);
        if (StaticController.getActiveLayer() == layer) {
            EventBus.publish(new ActiveLayerUpdated(layer));
        }
    }

    @Override
    public void redo() {
        hullmodIndex.add(entry);
        if (StaticController.getActiveLayer() == layer) {
            EventBus.publish(new ActiveLayerUpdated(layer));
        }
    }

    @Override
    public String getName() {
        return "Add Hullmod";
    }

    @Override
    public LayerPainter getLayerPainter() {
        return layer.getPainter();
    }

    @Override
    public void cleanupReferences() {
        layer = null;
    }

}
