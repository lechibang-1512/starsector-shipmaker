package shipeditor.components.datafiles.entities.transferable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import lombok.Getter;
import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.components.datafiles.entities.WingCSVEntry;
import shipeditor.representation.ship.VariantFile;

public class Transferables {


    @Getter public static class TransferableWing extends TransferableEntry {

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


    @Getter public static class TransferableShip extends TransferableEntry {

    public TransferableShip(ShipCSVEntry data, Object source) {
        super(data, source);
    }

    @Override
    protected DataFlavor getTypeFlavor() {
        return TRANSFERABLE_SHIP;
    }

    @Override
    public ShipCSVEntry getNodeData() {
        return (ShipCSVEntry) super.getNodeData();
    }

}


    @Getter public static abstract class TransferableEntry implements Transferable {

    private final Object nodeData;

    private final DataFlavor sourceFlavor;

    public static final DataFlavor TRANSFERABLE_SHIP = new DataFlavor(ShipCSVEntry.class,
            "Ship Entry");

    public static final DataFlavor TRANSFERABLE_VARIANT = new DataFlavor(VariantFile.class,
            "Variant File");

    public static final DataFlavor TRANSFERABLE_WEAPON = new DataFlavor(WeaponCSVEntry.class,
            "Weapon Entry");

    public static final DataFlavor TRANSFERABLE_MOD = new DataFlavor(HullmodCSVEntry.class,
            "Hullmod Entry");

    public static final DataFlavor TRANSFERABLE_WING = new DataFlavor(WingCSVEntry.class,
            "Wing Entry");

    TransferableEntry(Object data, Object source) {
        this.nodeData = data;
        this.sourceFlavor = new DataFlavor(source.getClass(),
                String.valueOf(source.hashCode()));
    }

    protected abstract DataFlavor getTypeFlavor();

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{getTypeFlavor(), sourceFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(getTypeFlavor());
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (isDataFlavorSupported(flavor)) {
                return nodeData;
        }
        throw new UnsupportedFlavorException(flavor);
    }

}


    @Getter public static class TransferableVariant extends TransferableEntry{

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


    @Getter public static class TransferableWeapon extends TransferableEntry {

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


    @Getter public static class TransferableHullmod extends TransferableEntry {

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

}
