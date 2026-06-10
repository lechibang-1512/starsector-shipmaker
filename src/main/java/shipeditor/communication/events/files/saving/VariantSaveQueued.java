package shipeditor.communication.events.files.saving;

import shipeditor.communication.events.BusEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.components.viewer.layers.ship.data.ShipVariant;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record VariantSaveQueued(ShipVariant variant) implements BusEvent {

}
