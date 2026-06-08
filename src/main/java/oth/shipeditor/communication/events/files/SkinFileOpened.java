package oth.shipeditor.communication.events.files;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.representation.ship.SkinSpecFile;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record SkinFileOpened(SkinSpecFile skinSpecFile, boolean setAsActive) implements FileEvent {

}
