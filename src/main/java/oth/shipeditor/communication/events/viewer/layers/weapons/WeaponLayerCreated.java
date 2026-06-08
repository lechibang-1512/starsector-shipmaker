package oth.shipeditor.communication.events.viewer.layers.weapons;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.communication.events.viewer.layers.LayerEvent;
import oth.shipeditor.components.viewer.layers.weapon.WeaponLayer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record WeaponLayerCreated(WeaponLayer newLayer) implements LayerEvent {

}
