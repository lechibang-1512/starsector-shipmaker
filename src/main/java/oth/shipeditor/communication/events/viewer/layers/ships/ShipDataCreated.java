package oth.shipeditor.communication.events.viewer.layers.ships;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.communication.events.viewer.layers.LayerEvent;
import oth.shipeditor.components.viewer.layers.ship.ShipLayer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record ShipDataCreated(ShipLayer layer) implements LayerEvent {

}
