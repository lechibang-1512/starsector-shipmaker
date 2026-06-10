package shipeditor.communication.events.viewer.points;

import shipeditor.components.instrument.EditorInstrument;

public record InstrumentModeChanged(EditorInstrument newMode) implements PointEvent {

}
