package shipeditor.communication.events.files;

import shipeditor.communication.events.BusEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.components.datafiles.entities.ShipSystemCSVEntry;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record ShipSystemsLoaded(Map<Path, List<ShipSystemCSVEntry>> systemsByPackage) implements BusEvent {

}
