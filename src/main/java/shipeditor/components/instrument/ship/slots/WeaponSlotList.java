package shipeditor.components.instrument.ship.slots;

import javax.swing.ListModel;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.points.PointEvents.SlotPointsSorted;
import shipeditor.components.datafiles.trees.WeaponFilterPanel;

import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.utility.components.containers.PointList;
import shipeditor.utility.components.rendering.WeaponSlotCellRenderer;

import shipeditor.components.viewer.painters.points.ship.WeaponSlotPainter;
import shipeditor.utility.text.StringValues;
import shipeditor.utility.components.dialog.DialogUtilities;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.List;
import java.util.function.Consumer;

public class WeaponSlotList extends PointList<WeaponSlotPoint> {

    private static final DataFlavor slotFlavor = new DataFlavor(WeaponSlotPoint.class, "Slot");

    private final Consumer<WeaponSlotPoint> selectAction;

    WeaponSlotList(ListModel<WeaponSlotPoint> dataModel, Consumer<WeaponSlotPoint> pointSelectAction) {
        super(dataModel);
        this.setCellRenderer(new WeaponSlotCellRenderer());
        this.selectAction = pointSelectAction;
        initCopyPasteKeyBindings();
    }

    private void initCopyPasteKeyBindings() {
        javax.swing.InputMap inputMap = this.getInputMap(javax.swing.JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap actionMap = this.getActionMap();

        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK), "copySlots");
        actionMap.put("copySlots", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                java.util.List<WeaponSlotPoint> selected = getSelectedValuesList();
                WeaponSlotClipboard.copy(selected);
            }
        });

        inputMap.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK), "pasteSlots");
        actionMap.put("pasteSlots", new PasteAction());
    }

    @Override
    protected java.awt.event.MouseListener createContextMenuListener() {
        return new java.awt.event.MouseAdapter() {
            private javax.swing.JPopupMenu getContextMenu() {
                javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();

                javax.swing.JMenuItem removePoint = new javax.swing.JMenuItem("Remove point");
                removePoint.addActionListener(event -> {
                    for (WeaponSlotPoint point : getSelectedValuesList()) {
                        EventBus.publish(new shipeditor.communication.events.viewer.points.PointEvents.PointRemoveQueued(point, true));
                    }
                });
                menu.add(removePoint);
                menu.addSeparator();

                javax.swing.JMenuItem adjustPosition = new javax.swing.JMenuItem(StringValues.ADJUST_POSITION);
                adjustPosition.addActionListener(event -> {
                    WeaponSlotPoint firstSelected = getSelectedValue();
                    if (firstSelected != null) {
                        DialogUtilities.showAdjustPointDialog(firstSelected);
                    }
                });
                menu.add(adjustPosition);
                menu.addSeparator();

                javax.swing.JMenuItem copyItem = new javax.swing.JMenuItem("Copy selected");
                copyItem.addActionListener(event -> {
                    java.util.List<WeaponSlotPoint> selected = getSelectedValuesList();
                    WeaponSlotClipboard.copy(selected);
                });
                menu.add(copyItem);

                javax.swing.JMenuItem pasteItem = new javax.swing.JMenuItem("Paste");
                pasteItem.setEnabled(WeaponSlotClipboard.hasData());
                pasteItem.addActionListener(event -> {
                    WeaponSlotPainter painter = shipeditor.utility.overseers.StaticController.getSelectedSlotPainter();
                    if (painter != null) {
                        painter.pasteSlots(WeaponSlotClipboard.getClipboard(), null);
                    }
                });
                menu.add(pasteItem);

                return menu;
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                    int index = locationToIndex(e.getPoint());
                    if (index != -1) {
                        if (!isSelectedIndex(index)) {
                            setSelectedIndex(index);
                        }
                    }
                    javax.swing.JPopupMenu menu = getContextMenu();
                    menu.show(WeaponSlotList.this, e.getPoint().x, e.getPoint().y);
                }
            }
        };
    }

    @Override
    protected void handlePointSelection(WeaponSlotPoint point) {
        WeaponFilterPanel.setLastSelectedSlot(point);
        this.selectAction.accept(point);
    }

    @Override
    protected void publishPointsSorted(List<WeaponSlotPoint> rearrangedPoints) {
        EventBus.publish(new SlotPointsSorted(rearrangedPoints));
    }

    @Override
    protected Transferable createTransferableFromEntry(WeaponSlotPoint entry) {
        return new Transferable() {

            private final WeaponSlotPoint slot = entry;

            private final DataFlavor sourceFlavor = new DataFlavor(WeaponSlotList.this.getClass(),
                    String.valueOf(WeaponSlotList.this.hashCode()));

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] {slotFlavor, sourceFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return flavor.equals(slotFlavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) {
                return slot;
            }
        };
    }

    @Override
    protected boolean isSupported(Transferable transferable) {
        return transferable.isDataFlavorSupported(slotFlavor);
    }

    private static class PasteAction extends javax.swing.AbstractAction {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            WeaponSlotPainter painter = shipeditor.utility.overseers.StaticController.getSelectedSlotPainter();
            if (painter != null) {
                painter.pasteSlots(WeaponSlotClipboard.getClipboard(), null);
            }
        }
    }
}
