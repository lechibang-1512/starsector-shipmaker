package oth.shipeditor.communication.events.viewer.control;

import oth.shipeditor.communication.events.viewer.ViewerEvent;

public record CursorSnappingToggled(boolean toggled) implements ViewerEvent {

}
