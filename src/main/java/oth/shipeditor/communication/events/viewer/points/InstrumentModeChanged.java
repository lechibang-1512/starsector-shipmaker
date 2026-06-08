package oth.shipeditor.communication.events.viewer.points;

import oth.shipeditor.components.instrument.EditorInstrument;

public record InstrumentModeChanged(EditorInstrument newMode) implements PointEvent {

}
