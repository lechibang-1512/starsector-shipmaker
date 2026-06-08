package oth.shipeditor.communication.events.viewer.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.engine.EnginePoint;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record EngineInsertedConfirmed(EnginePoint toInsert, int precedingIndex) implements PointEvent {

}
