package shipeditor.communication.events.files.saving;

import shipeditor.communication.events.BusEvent;
import shipeditor.components.datafiles.entities.CSVEntry;

/**
 * @author Ontarget (or Antigravity)
 */
public record CSVSaveQueued(CSVEntry entry) implements BusEvent {
}
