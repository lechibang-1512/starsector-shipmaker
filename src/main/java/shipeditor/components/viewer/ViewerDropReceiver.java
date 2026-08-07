package shipeditor.components.viewer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import shipeditor.components.datafiles.entities.InstallableEntry;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.components.datafiles.entities.transferable.Transferables.TransferableEntry;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.control.ViewerControl;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.FeaturesOverseer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.painters.points.ship.WeaponSlotPainter;
import shipeditor.parsing.FileUtilities;
import shipeditor.representation.ship.VariantFile;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.undo.UndoOverseer;
import shipeditor.utility.Errors;
import shipeditor.utility.overseers.StaticController;

import javax.swing.JOptionPane;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class ViewerDropReceiver extends DropTarget {

    private final PrimaryViewer viewer;

    @Getter
    private static InstallableEntry draggedEntry;

    @Getter
    private static DataFlavor currentFlavor;

    private static final Object STATIC_LOCK = new Object();

    @Getter
    private static boolean dragToViewerInProgress;

    ViewerDropReceiver(PrimaryViewer parent) {
        this.viewer = parent;
    }

    @Override
    public synchronized void dragEnter(DropTargetDragEvent dtde) {
        super.dragEnter(dtde);
        ViewerControl controls = viewer.getViewerControls();
        controls.notifyCursorState(dtde.getLocation());
        viewer.setCursorInViewer(true);
    }

    @Override
    public synchronized void dragOver(DropTargetDragEvent dtde) {
        super.dragOver(dtde);
        ViewerControl controls = viewer.getViewerControls();
        controls.notifyCursorState(dtde.getLocation());
    }

    @Override
    public synchronized void dragExit(DropTargetEvent dte) {
        super.dragExit(dte);
        viewer.setCursorInViewer(false);
    }

    @SuppressWarnings("SynchronizedMethod")
    public static void commenceDragToViewer(InstallableEntry dragged, DataFlavor flavor) {
        synchronized (STATIC_LOCK) {
            dragToViewerInProgress = true;
            draggedEntry = dragged;
            currentFlavor = flavor;
        }

        PrimaryViewer viewer = StaticController.getViewer();
        viewer.setCursorInViewer(false);
    }

    @SuppressWarnings("SynchronizedMethod")
    public static void finishDragToViewer() {
        synchronized (STATIC_LOCK) {
            dragToViewerInProgress = false;
            draggedEntry = null;
            currentFlavor = null;
        }
    }

    @SuppressWarnings({"unchecked", "AccessToStaticFieldLockedOnInstance", "IfStatementWithTooManyBranches"})
    public synchronized void drop(DropTargetDropEvent dtde) {
        try {
            Transferable transferable = dtde.getTransferable();
            DataFlavor[] transferDataFlavors = transferable.getTransferDataFlavors();
            List<DataFlavor> flavorList = Arrays.asList(transferDataFlavors);

            DataFlavor filesFlavor = DataFlavor.javaFileListFlavor;
            DataFlavor shipFlavor = TransferableEntry.TRANSFERABLE_SHIP;
            DataFlavor weaponFlavor = TransferableEntry.TRANSFERABLE_WEAPON;
            DataFlavor variantFlavor = TransferableEntry.TRANSFERABLE_VARIANT;

            if (flavorList.contains(filesFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);

                Iterable<File> droppedFiles = (Iterable<File>) transferable.getTransferData(filesFlavor);
                ViewerDropReceiver.handleExternalFilesDrop(dtde, droppedFiles);
            } else if (flavorList.contains(shipFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);

                ShipCSVEntry shipEntry = (ShipCSVEntry) transferable.getTransferData(shipFlavor);
                ViewerDropReceiver.handleShipEntryDrop(dtde, shipEntry);
            } else if (flavorList.contains(variantFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);

                VariantFile variantFile = (VariantFile) transferable.getTransferData(variantFlavor);
                ViewerDropReceiver.handleVariantFileDrop(dtde, variantFile);
            } else if (flavorList.contains(weaponFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);

                WeaponCSVEntry weaponEntry = (WeaponCSVEntry) transferable.getTransferData(weaponFlavor);
                ViewerDropReceiver.handleWeaponEntryDrop(dtde, weaponEntry);
            }
        } catch (java.awt.datatransfer.UnsupportedFlavorException | java.io.IOException | RuntimeException ex) {
            dtde.dropComplete(false);
            ViewerDropReceiver.finishDragToViewer();
            log.error("Drag-and-drop to viewer failed!");
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    "Failed to conclude drag-and-drop action for viewer, exception thrown at: " + dtde,
                    "Drag-and-drop operation error!",
                    JOptionPane.ERROR_MESSAGE);
            Errors.printToStream(ex);
        }
        ViewerDropReceiver.finishDragToViewer();
    }

    private static void handleExternalFilesDrop(DropTargetDropEvent dtde, Iterable<File> files) {
        File firstEligible = null;
        for (File file : files) {
            boolean correctExtension = file.getName().toLowerCase(Locale.ROOT).endsWith(".png");
            boolean correctLocation = FileUtilities.isFileWithinGamePackages(file);
            if (correctExtension && correctLocation) {
                firstEligible = file;
                break;
            }
        }
        if (firstEligible == null) {
            log.error("Drag-and-drop sprite loading unsuccessful: requires PNG file located in game packages.");
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    "Failed to load file as sprite or initialize layer with it:" +
                            " requires PNG file located in game packages.",
                    "Drag-and-drop layer initialization unsuccessful!",
                    JOptionPane.INFORMATION_MESSAGE);
            dtde.dropComplete(false);
            ViewerDropReceiver.finishDragToViewer();
            return;
        }
        FileUtilities.createShipLayerWithSprite(firstEligible);
        dtde.dropComplete(true);
        ViewerDropReceiver.finishDragToViewer();
    }

    private static boolean hasModuleSlotsInActiveLayer() {
        boolean hasModuleSlots = false;
        if (StaticController.getActiveLayer() instanceof ShipLayer targetLayer) {
            ShipPainter shipPainter = targetLayer.getPainter();
            if (shipPainter == null || shipPainter.isUninitialized()) return false;
            WeaponSlotPainter weaponSlotPainter = shipPainter.getWeaponSlotPainter();
            hasModuleSlots = weaponSlotPainter.hasSlotsOfType(WeaponType.STATION_MODULE);
        }
        return hasModuleSlots;
    }

    private static void handleShipEntryDrop(DropTargetDropEvent dtde, ShipCSVEntry shipEntry) {
        try {
            boolean hasSlots = ViewerDropReceiver.hasModuleSlotsInActiveLayer();
            boolean isModulesMode = StaticController.getEditorMode() == EditorInstrument.VARIANT_MODULES;

            if (isModulesMode && hasSlots) {
                VariantFile forInstall = FeaturesOverseer.getModuleVariantForInstall();
                ViewerDropReceiver.addAsModule(dtde, forInstall);
            } else {
                ShipLayer shipLayer = shipEntry.loadLayerFromEntry();
                ViewerDropReceiver.dropShipLayer(dtde, shipLayer);
            }
        } catch (RuntimeException exception) {
            Errors.printToStream(exception);
            dtde.dropComplete(false);
        }
        ViewerDropReceiver.finishDragToViewer();
    }

    private static void handleVariantFileDrop(DropTargetDropEvent dtde, VariantFile variantFile) {
        try {
            boolean hasSlots = ViewerDropReceiver.hasModuleSlotsInActiveLayer();
            boolean isModuleMode = StaticController.getEditorMode() == EditorInstrument.VARIANT_MODULES;
            if (isModuleMode && hasSlots) {
                ViewerDropReceiver.addAsModule(dtde, variantFile);
            } else {
                ShipLayer shipLayer = shipeditor.components.viewer.layers.LayerFactory.createLayerFromVariant(variantFile);
                ViewerDropReceiver.dropShipLayer(dtde, shipLayer);
            }
        } catch (RuntimeException exception) {
            Errors.printToStream(exception);
            dtde.dropComplete(false);
        }
        ViewerDropReceiver.finishDragToViewer();
    }

    private static void dropShipLayer(DropTargetDropEvent dtde,
                                      ShipLayer shipLayer) {
        ShipPainter shipPainter = shipLayer.getPainter();
        Point2D difference = shipPainter.getSpriteCenterDifferenceToAnchor();

        Point2D currentCursor = StaticController.getCorrectedWithoutRotate();
        Point2D targetForSpriteCenter = new Point2D.Double(currentCursor.getX() - difference.getX(),
                currentCursor.getY() - difference.getY());

        shipPainter.updateAnchorOffset(targetForSpriteCenter);
        UndoOverseer.finishAllEdits();
        StaticController.reselectCurrentLayer();

        dtde.dropComplete(true);
        ViewerDropReceiver.finishDragToViewer();
    }

    private static void addAsModule(DropTargetDropEvent dtde, VariantFile variantFile) {
        try {
            ViewerLayer viewerLayer = StaticController.getActiveLayer();
            if (viewerLayer instanceof ShipLayer shipLayer) {
                FeaturesOverseer featuresOverseer = shipLayer.getFeaturesOverseer();
                if (featuresOverseer != null) {
                    featuresOverseer.addModuleToSelectedSlot(variantFile);
                    dtde.dropComplete(true);
                } else {
                    dtde.dropComplete(false);
                }
            } else {
                dtde.dropComplete(false);
            }
        } catch (RuntimeException exception) {
            Errors.printToStream(exception);
            dtde.dropComplete(false);
        }
        ViewerDropReceiver.finishDragToViewer();
    }

    private static void handleWeaponEntryDrop(DropTargetDropEvent dtde, WeaponCSVEntry weaponEntry) {
        try {
            ViewerLayer viewerLayer = StaticController.getActiveLayer();
            if (viewerLayer instanceof ShipLayer shipLayer) {
                FeaturesOverseer featuresOverseer = shipLayer.getFeaturesOverseer();
                if (featuresOverseer != null) {
                    featuresOverseer.addWeaponToSelectedSlot(weaponEntry);
                    dtde.dropComplete(true);
                } else {
                    dtde.dropComplete(false);
                }
            } else {
                dtde.dropComplete(false);
            }
        } catch (RuntimeException exception) {
            Errors.printToStream(exception);
            dtde.dropComplete(false);
        }
        ViewerDropReceiver.finishDragToViewer();
    }

}
