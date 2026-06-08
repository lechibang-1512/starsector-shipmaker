package oth.shipeditor.communication.events.viewer.control;

import oth.shipeditor.communication.events.viewer.ViewerEvent;
import oth.shipeditor.components.viewer.control.PointSelectionMode;

public record PointSelectionModeChange(PointSelectionMode newMode) implements ViewerEvent {

}
