package oth.shipeditor.communication.events.viewer;

import java.awt.*;

public record ViewerBackgroundChanged(Color newColor) implements ViewerEvent {

}
