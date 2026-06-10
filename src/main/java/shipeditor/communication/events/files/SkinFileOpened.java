package shipeditor.communication.events.files;

import shipeditor.communication.events.BusEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.representation.ship.SkinSpecFile;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record SkinFileOpened(SkinSpecFile skinSpecFile, boolean setAsActive) implements BusEvent {

}
