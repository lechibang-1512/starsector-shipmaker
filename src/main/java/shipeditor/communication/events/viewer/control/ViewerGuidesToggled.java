package shipeditor.communication.events.viewer.control;

import shipeditor.communication.events.viewer.ViewerEvent;

public record ViewerGuidesToggled(
        boolean guidesEnabled,
        boolean bordersEnabled,
        boolean centerEnabled,
        boolean axesEnabled
    ) implements ViewerEvent {

}
