package shipeditor.communication.events.viewer.layers;

import shipeditor.communication.events.BusEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.components.viewer.layers.ViewerLayer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record ViewerLayerRemovalConfirmed(ViewerLayer removed) implements BusEvent {

}
