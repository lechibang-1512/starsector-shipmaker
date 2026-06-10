package shipeditor.communication.events.files;

import shipeditor.communication.events.BusEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.representation.ship.HullStyle;

import java.util.Map;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record HullStylesLoaded(Map<String, HullStyle> hullStyles) implements BusEvent {

}
