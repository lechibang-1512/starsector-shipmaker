package shipeditor.communication.events.files;

import shipeditor.communication.events.BusEvent;

import shipeditor.utility.graphics.Sprite;

public record SpriteOpened(Sprite sprite) implements BusEvent {

}
