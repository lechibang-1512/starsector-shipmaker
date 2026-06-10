package shipeditor.communication.events.viewer.control;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.communication.events.viewer.ViewerEvent;
import shipeditor.components.viewer.layers.LayerPainter;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record LayerAnchorDragged(AffineTransform screenToWorld, LayerPainter selected,
                                 Point2D difference) implements ViewerEvent {
}
