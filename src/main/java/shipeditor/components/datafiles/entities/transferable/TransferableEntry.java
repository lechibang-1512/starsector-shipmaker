package shipeditor.components.datafiles.entities.transferable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import shipeditor.components.datafiles.entities.*;
import shipeditor.representation.ship.VariantFile;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public abstract class TransferableEntry implements Transferable {

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
