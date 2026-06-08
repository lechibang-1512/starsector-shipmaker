package oth.shipeditor.communication.events.viewer.control;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.communication.events.BusEvent;

import java.awt.event.MouseEvent;

/**
 * Raw mouse moved event published by LayerViewerControls.
 * Painters subscribe to this and evaluate their own predicates.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record ViewerRawMouseMoved(MouseEvent mouseEvent) implements BusEvent {
}
