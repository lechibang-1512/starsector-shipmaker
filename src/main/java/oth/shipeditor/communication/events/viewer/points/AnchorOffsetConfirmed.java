package oth.shipeditor.communication.events.viewer.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.WorldPoint;

import java.awt.geom.Point2D;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record AnchorOffsetConfirmed(WorldPoint point, Point2D difference) implements PointEvent{

}
