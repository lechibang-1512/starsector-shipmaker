package oth.shipeditor.communication.events.files.saving;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.communication.events.files.FileEvent;
import oth.shipeditor.components.viewer.layers.ship.ShipLayer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record HullSaveQueued(ShipLayer shipLayer) implements FileEvent {

}
