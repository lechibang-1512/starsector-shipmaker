package shipeditor.components.datafiles.entities.transferable;

import shipeditor.components.datafiles.entities.WingCSVEntry;

import java.awt.datatransfer.DataFlavor;

public class TransferableWing extends TransferableEntry {

    public TransferableWing(WingCSVEntry data, Object source) {
        super(data, source);
    }

    @Override
    protected DataFlavor getTypeFlavor() {
        return TRANSFERABLE_WING;
    }

    @Override
    public WingCSVEntry getNodeData() {
        return (WingCSVEntry) super.getNodeData();
    }

}
