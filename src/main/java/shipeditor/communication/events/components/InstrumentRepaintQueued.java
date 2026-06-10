package shipeditor.communication.events.components;

import shipeditor.components.instrument.EditorInstrument;

public record InstrumentRepaintQueued(EditorInstrument editorMode) implements ComponentEvent {

}
