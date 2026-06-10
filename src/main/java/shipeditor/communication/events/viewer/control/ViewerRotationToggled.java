package shipeditor.communication.events.viewer.control;

import shipeditor.communication.events.viewer.ViewerEvent;

public record ViewerRotationToggled(boolean isSelected, boolean isEnabled) implements ViewerEvent {

}
