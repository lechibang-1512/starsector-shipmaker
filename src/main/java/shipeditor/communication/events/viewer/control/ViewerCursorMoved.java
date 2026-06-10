package shipeditor.communication.events.viewer.control;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.communication.events.viewer.ViewerEvent;

import java.awt.geom.Point2D;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record ViewerCursorMoved(Point2D rawCursor,
                                Point2D adjusted,
                                Point2D adjustedAndCorrected)
        implements ViewerEvent {

}
