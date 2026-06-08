package oth.shipeditor.communication.events.viewer.control;

import oth.shipeditor.communication.events.viewer.ViewerEvent;

public record ViewerRotationToggled(boolean isSelected, boolean isEnabled) implements ViewerEvent {

}
