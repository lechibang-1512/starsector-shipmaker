package shipeditor.communication.events.viewer.control;

import shipeditor.communication.events.viewer.ViewerEvent;
import shipeditor.components.viewer.control.PointSelectionMode;

public record PointSelectionModeChange(PointSelectionMode newMode) implements ViewerEvent {

}
