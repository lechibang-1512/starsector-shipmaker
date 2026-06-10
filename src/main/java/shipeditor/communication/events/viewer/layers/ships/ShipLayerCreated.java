package shipeditor.communication.events.viewer.layers.ships;

import shipeditor.communication.events.BusEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.components.viewer.layers.ship.ShipLayer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record ShipLayerCreated(ShipLayer newLayer) implements BusEvent {

}
