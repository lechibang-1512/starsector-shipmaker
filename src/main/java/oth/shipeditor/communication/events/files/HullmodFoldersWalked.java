package oth.shipeditor.communication.events.files;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.datafiles.entities.HullmodCSVEntry;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record HullmodFoldersWalked(Map<Path, List<HullmodCSVEntry>> hullmodsByPackage) implements FileEvent {

}
