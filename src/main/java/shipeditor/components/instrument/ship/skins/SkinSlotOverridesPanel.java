package shipeditor.components.instrument.ship.skins;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.InstrumentRepaintQueued;
import shipeditor.communication.events.viewer.layers.ActiveLayerUpdated;
import shipeditor.communication.events.viewer.layers.LayerWasSelected;
import shipeditor.components.instrument.EditorInstrument;
import shipeditor.components.viewer.entities.weapon.WeaponSlotOverride;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.components.viewer.painters.points.ship.WeaponSlotPainter;
import shipeditor.representation.weapon.WeaponMount;
import shipeditor.representation.weapon.WeaponSize;
import shipeditor.representation.weapon.WeaponType;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import shipeditor.utility.components.UIConstants;

/** * Panel for viewing and editing weapon slot overrides defined in skin files.
 * When a skin is active, displays all weapon slots and shows which properties
 * are overridden by the skin, allowing users to edit override values.*/
public class SkinSlotOverridesPanel extends JPanel {

    private final JLabel statusLabel;

    private final JTable overridesTable;

    private final SlotOverrideTableModel tableModel;

    private final JPanel editorPanel;

    private ShipPainter cachedPainter;

    public SkinSlotOverridesPanel() {
        this.setLayout(new BorderLayout());

        statusLabel = new JLabel("No skin active");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(new EmptyBorder(8, 8, 8, 8));

        tableModel = new SlotOverrideTableModel();
        overridesTable = new JTable(tableModel);
        overridesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        overridesTable.setRowHeight(24);
        overridesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        overridesTable.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        overridesTable.setDefaultRenderer(Object.class, centerRenderer);

        JScrollPane scrollPane = new JScrollPane(overridesTable);
        scrollPane.setBorder(UIConstants.EMPTY_BORDER);

        editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        editorPanel.add(createOverrideEditorPanel(), BorderLayout.PAGE_START);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(statusLabel, BorderLayout.PAGE_START);
        topContainer.add(editorPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topContainer, scrollPane);
        splitPane.setResizeWeight(0.35);
        splitPane.setDividerSize(4);
        this.add(splitPane, BorderLayout.CENTER);

        overridesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshEditorPanel();
            }
        });

        this.initEventListening();
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private void initEventListening() {
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerWasSelected checked) {
                handleLayerChange(checked.selected());
            } else if (event instanceof ActiveLayerUpdated checked) {
                handleLayerChange(checked.updated());
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof InstrumentRepaintQueued checked) {
                if (checked.editorMode() == EditorInstrument.SKIN_SLOTS) {
                    refreshContent();
                }
            }
        });
    }

    private void handleLayerChange(ViewerLayer layer) {
        if (layer instanceof ShipLayer shipLayer) {
            ShipPainter painter = shipLayer.getPainter();
            if (painter != null && !painter.isUninitialized()) {
                this.cachedPainter = painter;
                refreshContent();
                return;
            }
        }
        this.cachedPainter = null;
        refreshContent();
    }

    private void refreshContent() {
        tableModel.clear();
        editorPanel.setVisible(false);

        if (cachedPainter == null || cachedPainter.isUninitialized()) {
            statusLabel.setText("No ship layer selected");
            return;
        }

        ShipSkin activeSkin = cachedPainter.getActiveSkin();
        if (activeSkin == null || activeSkin.isBase()) {
            statusLabel.setText("No skin active — select a skin in the Skin Data tab");
            return;
        }

        WeaponSlotPainter slotPainter = cachedPainter.getWeaponSlotPainter();
        List<WeaponSlotPoint> slots = slotPainter.getPointsIndex();

        if (slots.isEmpty()) {
            statusLabel.setText("No weapon slots defined on this hull");
            return;
        }

        Map<String, WeaponSlotOverride> overrides = activeSkin.getWeaponSlotChanges();
        int overrideCount = (overrides != null) ? overrides.size() : 0;
        statusLabel.setText("Skin: " + activeSkin + "  —  " + overrideCount + " slot override(s)");

        tableModel.populate(slots, overrides);
        editorPanel.setVisible(true);
        refreshEditorPanel();
    }

    private JPanel createOverrideEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(panel, "Selected Slot Override");
        return panel;
    }

    private void refreshEditorPanel() {
        JPanel innerEditor = (JPanel) editorPanel.getComponent(0);
        innerEditor.removeAll();

        int selectedRow = overridesTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= tableModel.getRowCount()) {
            JLabel hint = new JLabel("Select a slot from the table to view its override");
            hint.setHorizontalAlignment(SwingConstants.CENTER);
            hint.setForeground(UIManager.getColor("Label.disabledForeground"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(8, 4, 8, 4);
            innerEditor.add(hint, gbc);
            innerEditor.revalidate();
            innerEditor.repaint();
            return;
        }

        SlotOverrideRow row = tableModel.getRow(selectedRow);
        populateEditorForRow(innerEditor, row);

        innerEditor.revalidate();
        innerEditor.repaint();
    }

    private void populateEditorForRow(JPanel panel, SlotOverrideRow row) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.anchor = GridBagConstraints.LINE_START;
        labelGbc.insets = new Insets(2, 4, 2, 4);

        GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.fill = GridBagConstraints.HORIZONTAL;
        fieldGbc.weightx = 1;
        fieldGbc.insets = new Insets(2, 4, 2, 4);

        int gridRow = 0;

        // Slot ID (read-only)
        addReadOnlyField(panel, "Slot ID:", row.slotId, labelGbc, fieldGbc, gridRow++);

        // Base values (read-only)
        addReadOnlyField(panel, "Base Type:", String.valueOf(row.baseType), labelGbc, fieldGbc, gridRow++);
        addReadOnlyField(panel, "Base Mount:", String.valueOf(row.baseMount), labelGbc, fieldGbc, gridRow++);
        addReadOnlyField(panel, "Base Size:", String.valueOf(row.baseSize), labelGbc, fieldGbc, gridRow++);

        // Override indicator
        String overrideStatus = row.hasOverride ? "✓ Has Override" : "✗ No Override";
        Color overrideColor = row.hasOverride
                ? new Color(100, 200, 100)
                : UIManager.getColor("Label.disabledForeground");
        addColoredField(panel, "Status:", overrideStatus, overrideColor, labelGbc, fieldGbc, gridRow++);

        if (row.hasOverride && row.override != null) {
            WeaponSlotOverride ov = row.override;
            if (ov.getWeaponType() != null) {
                addReadOnlyField(panel, "Override Type:", ov.getWeaponType().toString(), labelGbc, fieldGbc, gridRow++);
            }
            if (ov.getWeaponMount() != null) {
                addReadOnlyField(panel, "Override Mount:", ov.getWeaponMount().toString(), labelGbc, fieldGbc, gridRow++);
            }
            if (ov.getWeaponSize() != null) {
                addReadOnlyField(panel, "Override Size:", ov.getWeaponSize().toString(), labelGbc, fieldGbc, gridRow++);
            }
            if (ov.getBoxedAngle() != null) {
                addReadOnlyField(panel, "Override Angle:", String.valueOf(ov.getAngle()), labelGbc, fieldGbc, gridRow++);
            }
            if (ov.getBoxedArc() != null) {
                addReadOnlyField(panel, "Override Arc:", String.valueOf(ov.getArc()), labelGbc, fieldGbc, gridRow++);
            }
            if (ov.getRenderOrderModBoxed() != null) {
                addReadOnlyField(panel, "Override Render Order:", String.valueOf(ov.getRenderOrderMod()), labelGbc, fieldGbc, gridRow);
            }
        }
    }

    private static void addReadOnlyField(JPanel panel, String labelText, String value,
                                          GridBagConstraints labelGbc, GridBagConstraints fieldGbc, int row) {
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        panel.add(new JLabel(labelText), labelGbc);

        fieldGbc.gridx = 1;
        fieldGbc.gridy = row;
        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setColumns(12);
        panel.add(field, fieldGbc);
    }

    private static void addColoredField(JPanel panel, String labelText, String value, Color color,
                                          GridBagConstraints labelGbc, GridBagConstraints fieldGbc, int row) {
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        panel.add(new JLabel(labelText), labelGbc);

        fieldGbc.gridx = 1;
        fieldGbc.gridy = row;
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(color);
        panel.add(valueLabel, fieldGbc);
    }

    // ======== Table Model ========

    private static final class SlotOverrideRow {
        String slotId;
        WeaponType baseType;
        WeaponMount baseMount;
        WeaponSize baseSize;
        boolean hasOverride;
        WeaponSlotOverride override;

        SlotOverrideRow(WeaponSlotPoint slot, WeaponSlotOverride slotOverride) {
            this.slotId = slot.getId();
            this.baseType = slot.getBaseType();
            this.baseMount = slot.getBaseMount();
            this.baseSize = slot.getBaseSize();
            this.hasOverride = (slotOverride != null);
            this.override = slotOverride;
        }
    }

    private static final class SlotOverrideTableModel extends AbstractTableModel {

        private static final String[] COLUMN_NAMES = {
                "Slot ID", "Base Type", "Base Mount", "Base Size", "Override?"
        };

        private final List<SlotOverrideRow> rows = new ArrayList<>();

        void clear() {
            int oldSize = rows.size();
            rows.clear();
            if (oldSize > 0) {
                fireTableRowsDeleted(0, oldSize - 1);
            }
        }

        void populate(List<WeaponSlotPoint> slots, Map<String, WeaponSlotOverride> overrides) {
            rows.clear();
            for (WeaponSlotPoint slot : slots) {
                WeaponSlotOverride override = null;
                if (overrides != null) {
                    override = overrides.get(slot.getId());
                }
                rows.add(new SlotOverrideRow(slot, override));
            }
            fireTableDataChanged();
        }

        SlotOverrideRow getRow(int rowIndex) {
            return rows.get(rowIndex);
        }

        @Override
        public int getRowCount() {
            return rows.size();
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
        public Object getValueAt(int rowIndex, int columnIndex) {
            SlotOverrideRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return row.slotId;
                case 1: return row.baseType;
                case 2: return row.baseMount;
                case 3: return row.baseSize;
                case 4: return row.hasOverride ? "✓" : "";
                default: return "";
            }
        }
    }

}
