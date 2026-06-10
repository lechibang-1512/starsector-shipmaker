package shipeditor.communication.events.viewer.layers.weapons;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import shipeditor.communication.events.BusEvent;

import shipeditor.components.viewer.layers.weapon.ProjectileLayer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public record ProjectileLayerCreated(ProjectileLayer newLayer) implements BusEvent {
}
