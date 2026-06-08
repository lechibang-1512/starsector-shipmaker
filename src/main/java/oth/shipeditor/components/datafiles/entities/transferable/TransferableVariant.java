package oth.shipeditor.components.datafiles.entities.transferable;

import oth.shipeditor.representation.ship.VariantFile;

import java.awt.datatransfer.DataFlavor;

public class TransferableVariant extends TransferableEntry{

    public TransferableVariant(VariantFile variant, Object source) {
        super(variant, source);
    }

    @Override
    protected DataFlavor getTypeFlavor() {
        return TRANSFERABLE_VARIANT;
    }

    @Override
    public VariantFile getNodeData() {
        return (VariantFile) super.getNodeData();
    }

}
