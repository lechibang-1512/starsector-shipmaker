package shipeditor.components.datafiles.trees;

import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.components.datafiles.entities.transferable.TransferableEntry;
import shipeditor.components.datafiles.entities.transferable.TransferableVariant;
import shipeditor.components.viewer.ViewerDragListener;
import shipeditor.components.viewer.ViewerDropReceiver;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.ship.VariantFile;

import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;

class LabelDragListener implements DragGestureListener {

    private final VariantFile variant;

    private final Object parentSource;

    LabelDragListener(VariantFile variantFile, Object source) {
        this.variant = variantFile;
        this.parentSource = source;
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        Transferable transferable = new TransferableVariant(variant, parentSource);

        String baseHullID = GameDataRepository.getBaseHullID(variant.getShipHullId());
        ShipCSVEntry shipEntry = GameDataRepository.retrieveShipCSVEntryByID(baseHullID);

        ViewerDropReceiver.commenceDragToViewer(shipEntry, TransferableEntry.TRANSFERABLE_VARIANT);
        dge.startDrag(DragSource.DefaultMoveDrop, transferable, new ViewerDragListener());
    }

}
