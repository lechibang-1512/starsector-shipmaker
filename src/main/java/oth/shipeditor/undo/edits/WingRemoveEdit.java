package oth.shipeditor.undo.edits;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.AllArgsConstructor;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.viewer.layers.ActiveLayerUpdated;
import oth.shipeditor.components.datafiles.entities.WingCSVEntry;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.components.viewer.layers.ship.ShipLayer;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

import java.util.List;

@AllArgsConstructor
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WingRemoveEdit extends AbstractEdit implements LayerEdit {

    private final List<WingCSVEntry> wingIndex;

    private ShipLayer layer;

    private final WingCSVEntry entry;

    private int positionIndex;

    @Override
    public void undo() {
        wingIndex.add(positionIndex, entry);
        if (StaticController.getActiveLayer() == layer) {
            EventBus.publish(new ActiveLayerUpdated(layer));
        }
    }

    @Override
    public void redo() {
        wingIndex.remove(positionIndex);
        if (StaticController.getActiveLayer() == layer) {
            EventBus.publish(new ActiveLayerUpdated(layer));
        }
    }

    @Override
    public String getName() {
        return "Remove Wing";
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
