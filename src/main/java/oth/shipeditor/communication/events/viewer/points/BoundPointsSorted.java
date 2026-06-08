package oth.shipeditor.communication.events.viewer.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.BoundPoint;

import java.util.List;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record BoundPointsSorted(List<BoundPoint> rearranged) implements PointEvent {

}
