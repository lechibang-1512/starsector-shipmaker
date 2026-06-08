package oth.shipeditor.undo.edits.features;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.viewer.layers.ActiveLayerUpdated;
import oth.shipeditor.components.viewer.layers.ViewerLayer;
import oth.shipeditor.components.viewer.layers.ship.data.ShipVariant;
import oth.shipeditor.undo.AbstractEdit;

import java.util.List;


@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class SuppressedModsEdit extends AbstractEdit {

    private final ShipVariant variant;
    private final ViewerLayer layer;
    private final List<String> oldMods;
    private final List<String> newMods;

    public SuppressedModsEdit(ShipVariant variant, ViewerLayer layer, List<String> oldMods, List<String> newMods) {
        this.variant = variant;
        this.layer = layer;
        this.oldMods = oldMods;
        this.newMods = newMods;
    }

    @Override
    public void undo() {
        undoSubEdits();
        variant.setSuppressedMods(oldMods);
        EventBus.publish(new ActiveLayerUpdated(layer));
    }

    @Override
    public void redo() {
        variant.setSuppressedMods(newMods);
        EventBus.publish(new ActiveLayerUpdated(layer));
        redoSubEdits();
    }

    @Override
    public String getName() {
        return "Suppressed Mods Change";
    }

}
