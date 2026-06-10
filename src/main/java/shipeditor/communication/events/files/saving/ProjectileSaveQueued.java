package shipeditor.communication.events.files.saving;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import shipeditor.communication.events.BusEvent;

import shipeditor.components.viewer.layers.weapon.ProjectileLayer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public record ProjectileSaveQueued(ProjectileLayer projectileLayer) implements BusEvent {
}
