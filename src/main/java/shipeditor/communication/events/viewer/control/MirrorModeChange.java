package shipeditor.communication.events.viewer.control;

import shipeditor.communication.events.viewer.ViewerEvent;

public record MirrorModeChange(boolean enabled) implements ViewerEvent {

}
