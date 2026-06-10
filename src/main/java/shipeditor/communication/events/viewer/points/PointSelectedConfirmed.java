package shipeditor.communication.events.viewer.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.extern.log4j.Log4j2;
import shipeditor.components.viewer.entities.WorldPoint;

@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record PointSelectedConfirmed(WorldPoint point) implements PointEvent {

}
