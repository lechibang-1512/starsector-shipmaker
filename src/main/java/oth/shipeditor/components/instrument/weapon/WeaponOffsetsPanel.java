package oth.shipeditor.components.instrument.weapon;

import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.viewer.layers.LayerWasSelected;
import oth.shipeditor.components.viewer.entities.weapon.OffsetPoint;
import oth.shipeditor.components.viewer.layers.ViewerLayer;
import oth.shipeditor.components.viewer.layers.weapon.WeaponLayer;
import oth.shipeditor.components.viewer.layers.weapon.WeaponPainter;
import oth.shipeditor.components.viewer.painters.points.weapon.WeaponOffsetPainter;
import oth.shipeditor.undo.EditDispatch;
import oth.shipeditor.utility.components.ComponentUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/** * Panel for editing weapon offset points (turret / hardpoint firing positions and angles).*/
class WeaponOffsetsPanel extends JPanel {

    private OffsetsTableModel tableModel;
    private JTable offsetsTable;
    private WeaponPainter cachedPainter;

    WeaponOffsetsPanel() {
        this.setLayout(new BorderLayout());

        JPanel infoPanel = createInfoPanel();
        this.add(infoPanel, BorderLayout.PAGE_START);

        tableModel = new OffsetsTableModel();
        offsetsTable = new JTable(tableModel);
        offsetsTable.setFillsViewportHeight(true);
        offsetsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(offsetsTable);
        this.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        this.add(buttonPanel, BorderLayout.PAGE_END);

        initLayerListeners();
    }

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(infoPanel, "Weapon offsets");

        JLabel description = new JLabel("Firing positions and angles for current mount mode");
        description.setBorder(new EmptyBorder(4, 4, 4, 4));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.LINE_START;
        infoPanel.add(description, constraints);

        return infoPanel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addButton = new JButton("Add offset");
        addButton.addActionListener(e -> {
            if (cachedPainter == null) return;
            WeaponOffsetPainter offsetPainter = cachedPainter.getOffsetPainter();
            Point2D center = cachedPainter.getEntityCenter();
            OffsetPoint newPoint = new OffsetPoint(
                    new Point2D.Double(center.getX(), center.getY()), cachedPainter);
            EditDispatch.postPointAdded(offsetPainter, newPoint);
            refreshTableModel();
        });

        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> {
            if (cachedPainter == null) return;
            int selectedRow = offsetsTable.getSelectedRow();
            if (selectedRow < 0) return;
            WeaponOffsetPainter offsetPainter = cachedPainter.getOffsetPainter();
            List<OffsetPoint> points = offsetPainter.getOffsetPoints();
            if (selectedRow >= points.size()) return;
            OffsetPoint toRemove = points.get(selectedRow);
            EditDispatch.postPointRemoved(offsetPainter, toRemove);
            refreshTableModel();
        });

        panel.add(addButton);
        panel.add(removeButton);

        return panel;
    }

    private void initLayerListeners() {
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerWasSelected checked) {
                ViewerLayer selected = checked.selected();
                refreshPanel(selected);
            }
        });
    }

    private void refreshPanel(ViewerLayer selected) {
        if (selected instanceof WeaponLayer weaponLayer) {
            cachedPainter = weaponLayer.getPainter();
        } else {
            cachedPainter = null;
        }
        refreshTableModel();
    }

    private void refreshTableModel() {
        if (cachedPainter != null) {
            WeaponOffsetPainter offsetPainter = cachedPainter.getOffsetPainter();
            tableModel.setOffsetPoints(offsetPainter.getOffsetPoints());
            offsetsTable.setEnabled(true);
        } else {
            tableModel.setOffsetPoints(new ArrayList<>());
            offsetsTable.setEnabled(false);
        }
        tableModel.fireTableDataChanged();
    }

    /**
     * Simple table model for displaying/editing offset point positions and angles.
     */
    private static class OffsetsTableModel extends AbstractTableModel {

        private static final String[] COLUMN_NAMES = {"#", "X", "Y", "Angle"};
        private List<OffsetPoint> offsetPoints = new ArrayList<>();

        void setOffsetPoints(List<OffsetPoint> points) {
            this.offsetPoints = points;
        }

        @Override
        public int getRowCount() {
            return offsetPoints.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> Integer.class;
                case 1, 2, 3 -> Double.class;
                default -> Object.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Index column is not editable.
            return columnIndex > 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            OffsetPoint point = offsetPoints.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> rowIndex;
                case 1 -> point.getPosition().getX();
                case 2 -> point.getPosition().getY();
                case 3 -> point.getAngle();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            OffsetPoint point = offsetPoints.get(rowIndex);
            double val = ((Number) aValue).doubleValue();
            switch (columnIndex) {
                case 1 -> point.setPosition(val, point.getPosition().getY());
                case 2 -> point.setPosition(point.getPosition().getX(), val);
                case 3 -> point.setAngle(val);
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }

    }

}
