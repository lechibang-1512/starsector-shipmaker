package oth.shipeditor.components.datafiles.entities.transferable;

import oth.shipeditor.components.datafiles.entities.WeaponCSVEntry;

import java.awt.datatransfer.DataFlavor;

public class TransferableWeapon extends TransferableEntry {

    public TransferableWeapon(WeaponCSVEntry data, Object source) {
        super(data, source);
    }

    @Override
    protected DataFlavor getTypeFlavor() {
        return TRANSFERABLE_WEAPON;
    }

    @Override
    public WeaponCSVEntry getNodeData() {
        return (WeaponCSVEntry) super.getNodeData();
    }

}
