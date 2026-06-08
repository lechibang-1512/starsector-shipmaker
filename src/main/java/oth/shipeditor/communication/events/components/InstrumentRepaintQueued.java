package oth.shipeditor.communication.events.components;

import oth.shipeditor.components.instrument.EditorInstrument;

public record InstrumentRepaintQueued(EditorInstrument editorMode) implements ComponentEvent {

}
