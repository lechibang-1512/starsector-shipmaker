package oth.shipeditor.communication.events.viewer.status;

import oth.shipeditor.communication.events.BusEvent;
import oth.shipeditor.components.CoordsDisplayMode;

public record CoordsModeChanged(CoordsDisplayMode newMode) implements BusEvent {

}
