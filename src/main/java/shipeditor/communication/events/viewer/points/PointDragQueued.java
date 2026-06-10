package shipeditor.communication.events.viewer.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record PointDragQueued(AffineTransform screenToWorld, Point2D target) implements PointEvent {

}
