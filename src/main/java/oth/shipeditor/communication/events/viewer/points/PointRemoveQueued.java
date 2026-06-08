package oth.shipeditor.communication.events.viewer.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.BaseWorldPoint;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record PointRemoveQueued(BaseWorldPoint point, boolean fromList) implements PointEvent {
}
