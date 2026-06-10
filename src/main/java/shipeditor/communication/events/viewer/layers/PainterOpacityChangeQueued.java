package shipeditor.communication.events.viewer.layers;

import shipeditor.components.viewer.painters.points.AbstractPointPainter;

import shipeditor.communication.events.BusEvent;

public record PainterOpacityChangeQueued(Class<? extends AbstractPointPainter> painterClass,
                                         float change) implements BusEvent {

}
