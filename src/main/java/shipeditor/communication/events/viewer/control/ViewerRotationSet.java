package shipeditor.communication.events.viewer.control;

import shipeditor.communication.events.viewer.ViewerEvent;

public record ViewerRotationSet(double degrees) implements ViewerEvent {

}
