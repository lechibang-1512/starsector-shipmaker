package oth.shipeditor.communication.events.files.saving;

import oth.shipeditor.communication.events.BusEvent;
import oth.shipeditor.components.datafiles.entities.CSVEntry;

/**
 * @author Ontarget (or Antigravity)
 */
public record CSVSaveQueued(CSVEntry entry) implements BusEvent {
}
