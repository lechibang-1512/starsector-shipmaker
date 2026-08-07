package shipeditor.communication.events.components;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.Dimension;
import shipeditor.communication.events.BusEvent;
import shipeditor.components.datafiles.entities.CSVEntry;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.components.instrument.AbstractInstrumentsPane;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.ComponentEnums.VariantDataTab;
import shipeditor.components.viewer.layers.ViewerLayer;

public class ComponentEvents {
    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record SelectWeaponDataEntry(WeaponCSVEntry entry) implements ComponentEvent {

    }

    public static record LoadingTaskCompleted(String taskName) implements ComponentEvent {

    }

    public static record DataTreesReloadQueued() implements ComponentEvent {

    }

    public static record InstrumentRepaintQueued(EditorInstrument editorMode) implements ComponentEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record SelectShipDataEntry(ShipCSVEntry entry) implements ComponentEvent{

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record LayerTabUpdated(ViewerLayer layer) implements ComponentEvent {

    }

    public static record LoadingTaskStarted(String taskName) implements ComponentEvent {

    }

    public static record WindowGUIShowConfirmed() implements ComponentEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record InstrumentSplitterResized(AbstractInstrumentsPane source, boolean minimized) implements ComponentEvent {

    }

    public static record LoadingActionFired(boolean started) implements ComponentEvent {

    }

    public static record DeleteButtonPressed() implements ComponentEvent{

    }

    public static record ShipEntryPicked(shipeditor.representation.ship.VariantFile variant) implements ComponentEvent {

    }

    public static record ViewerFocusRequestQueued() implements ComponentEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record GameDataPanelResized(Dimension newMinimum) implements ComponentEvent {
    }

    public static record WeaponEntryPicked(WeaponCSVEntry weapon) implements ComponentEvent {

    }

    public static record VariantDataTabSelected(VariantDataTab selected) implements ComponentEvent {

    }

    public static record WindowRepaintQueued() implements ComponentEvent {

    }

    public static record CSVEntryIDChanged(String oldID, String newID, CSVEntry entry) implements ComponentEvent {

    }


    public static record SelectWingsDataTab() implements ComponentEvent {

    }



    @SuppressWarnings("MarkerInterface")
    public static interface ComponentEvent extends BusEvent {

    }
}
