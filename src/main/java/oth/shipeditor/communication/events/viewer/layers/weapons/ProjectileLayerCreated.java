package oth.shipeditor.communication.events.viewer.layers.weapons;

import oth.shipeditor.communication.events.viewer.layers.LayerEvent;
import oth.shipeditor.components.viewer.layers.weapon.ProjectileLayer;

public record ProjectileLayerCreated(ProjectileLayer newLayer) implements LayerEvent {
}
