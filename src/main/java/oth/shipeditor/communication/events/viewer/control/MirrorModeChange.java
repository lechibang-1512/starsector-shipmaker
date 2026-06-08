package oth.shipeditor.communication.events.viewer.control;

import oth.shipeditor.communication.events.viewer.ViewerEvent;

public record MirrorModeChange(boolean enabled) implements ViewerEvent {

}
