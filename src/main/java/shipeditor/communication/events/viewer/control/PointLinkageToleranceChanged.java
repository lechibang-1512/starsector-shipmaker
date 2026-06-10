package shipeditor.communication.events.viewer.control;

import shipeditor.communication.events.viewer.ViewerEvent;

public record PointLinkageToleranceChanged(int changed) implements ViewerEvent {

}
