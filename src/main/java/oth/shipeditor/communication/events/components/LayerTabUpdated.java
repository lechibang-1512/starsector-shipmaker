package oth.shipeditor.communication.events.components;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.layers.ViewerLayer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record LayerTabUpdated(ViewerLayer layer) implements ComponentEvent {

}
