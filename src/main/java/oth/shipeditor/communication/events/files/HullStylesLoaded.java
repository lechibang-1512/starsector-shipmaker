package oth.shipeditor.communication.events.files;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.representation.ship.HullStyle;

import java.util.Map;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record HullStylesLoaded(Map<String, HullStyle> hullStyles) implements FileEvent {

}
