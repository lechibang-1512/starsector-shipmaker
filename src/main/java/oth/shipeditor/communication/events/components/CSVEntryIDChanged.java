package oth.shipeditor.communication.events.components;

import oth.shipeditor.components.datafiles.entities.CSVEntry;

public record CSVEntryIDChanged(String oldID, String newID, CSVEntry entry) implements ComponentEvent {

}
