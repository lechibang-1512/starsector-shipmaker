package shipeditor.communication.events.viewer.layers;

import shipeditor.communication.events.BusEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.components.viewer.layers.LayerPainter;

import java.awt.geom.Point2D;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record LayerRotationQueued(LayerPainter layer, Point2D worldTarget) implements BusEvent {

}
