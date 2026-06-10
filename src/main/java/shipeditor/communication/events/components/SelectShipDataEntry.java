package shipeditor.communication.events.components;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.components.datafiles.entities.ShipCSVEntry;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record SelectShipDataEntry(ShipCSVEntry entry) implements ComponentEvent{

}
