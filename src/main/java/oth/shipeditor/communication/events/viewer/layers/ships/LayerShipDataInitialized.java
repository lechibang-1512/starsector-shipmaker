package oth.shipeditor.communication.events.viewer.layers.ships;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.communication.events.viewer.layers.LayerEvent;
import oth.shipeditor.components.viewer.layers.ship.ShipPainter;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record LayerShipDataInitialized(ShipPainter source) implements LayerEvent {

}
