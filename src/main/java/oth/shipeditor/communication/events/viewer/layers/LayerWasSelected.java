package oth.shipeditor.communication.events.viewer.layers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.layers.ViewerLayer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record LayerWasSelected(ViewerLayer old, ViewerLayer selected) implements LayerEvent {
}
