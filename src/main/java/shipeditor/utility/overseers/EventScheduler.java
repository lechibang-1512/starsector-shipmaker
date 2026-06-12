package shipeditor.utility.overseers;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.ViewerRepaintQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerUpdated;
import shipeditor.components.ComponentEnums.EditorInstrument;

import javax.swing.Timer;
import java.util.EnumMap;
import java.util.Map;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;

public class EventScheduler {

    private boolean viewerRepaintQueued;

    private boolean activeLayerUpdateQueued;

    private final Map<EditorInstrument, Boolean> instrumentRepaintStatus;

    EventScheduler() {
        instrumentRepaintStatus = new EnumMap<>(EditorInstrument.class);

        for (EditorInstrument instrument : EditorInstrument.values()) {
            instrumentRepaintStatus.put(instrument, false);
        }

        Timer repaintTimer = new Timer(16, e -> advanceEvents());
        repaintTimer.setRepeats(true);
        repaintTimer.start();
    }

    private void advanceEvents() {
        if (viewerRepaintQueued) {
            EventBus.publish(new ViewerRepaintQueued());
            viewerRepaintQueued = false;
        }
        if (activeLayerUpdateQueued) {
            EventBus.publish(new ActiveLayerUpdated(StaticController.getActiveLayer()));
            activeLayerUpdateQueued = false;
        }

        for (Map.Entry<EditorInstrument, Boolean> entry : instrumentRepaintStatus.entrySet()) {
            Boolean repaintQueued = entry.getValue();
            if (repaintQueued) {
                EditorInstrument editorInstrument = entry.getKey();
                EventBus.publish(new InstrumentRepaintQueued(editorInstrument));
                instrumentRepaintStatus.put(editorInstrument, false);
            }
        }
    }

    public void queueViewerRepaint() {
        this.viewerRepaintQueued = true;
    }

    public void queueLayerPropertiesRepaint() {
        this.instrumentRepaintStatus.put(EditorInstrument.LAYER, true);
    }

    public void queueCenterPanelsRepaint() {
        this.instrumentRepaintStatus.put(EditorInstrument.COLLISION, true);
        this.instrumentRepaintStatus.put(EditorInstrument.SHIELD, true);
    }

    public void queueBoundsPanelRepaint() {
        this.instrumentRepaintStatus.put(EditorInstrument.BOUNDS, true);
    }

    public void queueBaysPanelRepaint() {
        this.instrumentRepaintStatus.put(EditorInstrument.LAUNCH_BAYS, true);
    }

    public void queueEnginesPanelRepaint() {
        this.instrumentRepaintStatus.put(EditorInstrument.ENGINES, true);
    }

    public void queueSlotsPanelRepaint() {
        this.instrumentRepaintStatus.put(EditorInstrument.WEAPON_SLOTS, true);
    }

    public void queueBuiltInsRepaint() {
        this.instrumentRepaintStatus.put(EditorInstrument.BUILT_IN_MODS, true);
        this.instrumentRepaintStatus.put(EditorInstrument.BUILT_IN_WINGS, true);
    }

    public void queueVariantsRepaint() {
        this.instrumentRepaintStatus.put(EditorInstrument.VARIANT_DATA, true);
        this.instrumentRepaintStatus.put(EditorInstrument.VARIANT_WEAPONS, true);
    }

    public void queueVariantWeaponsRepaint() {
        this.instrumentRepaintStatus.put(EditorInstrument.VARIANT_WEAPONS, true);
    }

    public void queueModulesRepaint() {
        this.instrumentRepaintStatus.put(EditorInstrument.VARIANT_MODULES, true);
    }



    public void queueActiveLayerUpdate() {
        this.activeLayerUpdateQueued = true;
    }

    public void queueSlotRelatedRepaints() {
        this.viewerRepaintQueued = true;
        this.instrumentRepaintStatus.put(EditorInstrument.WEAPON_SLOTS, true);
        this.instrumentRepaintStatus.put(EditorInstrument.LAUNCH_BAYS, true);
    }

    public void queueEngineRelatedRepaints() {
        this.viewerRepaintQueued = true;
        this.instrumentRepaintStatus.put(EditorInstrument.ENGINES, true);
    }

    public void queueCenterRelatedRepaints() {
        this.viewerRepaintQueued = true;
        this.instrumentRepaintStatus.put(EditorInstrument.LAYER, true);
        this.instrumentRepaintStatus.put(EditorInstrument.COLLISION, true);
        this.instrumentRepaintStatus.put(EditorInstrument.SHIELD, true);
    }

    public void queueBuiltInRelatedRepaints() {
        this.viewerRepaintQueued = true;
        this.instrumentRepaintStatus.put(EditorInstrument.BUILT_IN_MODS, true);
        this.instrumentRepaintStatus.put(EditorInstrument.BUILT_IN_WINGS, true);
    }

}
