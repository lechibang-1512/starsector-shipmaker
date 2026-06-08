package oth.shipeditor.components.datafiles.entities.transferable;

import oth.shipeditor.components.datafiles.entities.HullmodCSVEntry;

import java.awt.datatransfer.DataFlavor;

public class TransferableHullmod extends TransferableEntry {

    public TransferableHullmod(HullmodCSVEntry data, Object source) {
        super(data, source);
    }

    @Override
    protected DataFlavor getTypeFlavor() {
        return TRANSFERABLE_MOD;
    }

    @Override
    public HullmodCSVEntry getNodeData() {
        return (HullmodCSVEntry) super.getNodeData();
    }

}
