package shipeditor.communication.events.viewer;

import java.awt.Color;

public record ViewerBackgroundChanged(Color newColor) implements ViewerEvent {

}
