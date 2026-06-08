package oth.shipeditor.communication.events.components;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.datafiles.entities.WeaponCSVEntry;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record SelectWeaponDataEntry(WeaponCSVEntry entry) implements ComponentEvent {

}
