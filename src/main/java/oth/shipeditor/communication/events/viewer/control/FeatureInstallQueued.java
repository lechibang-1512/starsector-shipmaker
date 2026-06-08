package oth.shipeditor.communication.events.viewer.control;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.communication.events.viewer.ViewerEvent;

import java.awt.geom.Point2D;

/** * Position is also expected to be adjusted and corrected by grid if respective option is enabled.*/
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record FeatureInstallQueued(Point2D worldPosition) implements ViewerEvent {

}
