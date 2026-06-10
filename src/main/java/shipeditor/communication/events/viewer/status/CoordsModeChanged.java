package shipeditor.communication.events.viewer.status;

import shipeditor.communication.events.BusEvent;
import shipeditor.components.CoordsDisplayMode;

public record CoordsModeChanged(CoordsDisplayMode newMode) implements BusEvent {

}
