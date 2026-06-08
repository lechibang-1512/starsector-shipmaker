package oth.shipeditor.communication.events.viewer.layers;

import oth.shipeditor.components.viewer.painters.points.AbstractPointPainter;

public record PainterOpacityChangeQueued(Class<? extends AbstractPointPainter> painterClass,
                                         float change) implements LayerEvent {

}
