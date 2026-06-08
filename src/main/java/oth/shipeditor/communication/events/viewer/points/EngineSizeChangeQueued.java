package oth.shipeditor.communication.events.viewer.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.awt.geom.Point2D;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record EngineSizeChangeQueued(Point2D worldTarget) implements PointEvent {

}
