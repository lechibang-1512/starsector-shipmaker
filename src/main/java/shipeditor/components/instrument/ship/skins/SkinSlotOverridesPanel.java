package shipeditor.components.instrument.ship.skins;

import shipeditor.utility.text.StringManager;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerUpdated;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.entities.weapon.WeaponSlotOverride;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.components.viewer.painters.points.ship.WeaponSlotPainter;
import shipeditor.representation.weapon.WeaponEnums.WeaponMount;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.undo.UndoOverseer;
import shipeditor.undo.edits.features.SkinOverrideEdits.SkinMapOverrideEdit;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import shipeditor.utility.components.UIConstants;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;

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

        statusLabel = new JLabel(StringManager.getString("NO_SKIN_ACTIVE"));
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
            statusLabel.setText(StringManager.getString("NO_SHIP_LAYER_SELECTED"));
            return;
        }

        ShipSkin activeSkin = cachedPainter.getActiveSkin();
        if (activeSkin == null || activeSkin.isBase()) {
            statusLabel.setText(StringManager.getString("NO_SKIN_ACTIVE_SELECT_A_SKIN_IN_THE_SKIN_DATA_TAB"));
            return;
        }

        WeaponSlotPainter slotPainter = cachedPainter.getWeaponSlotPainter();
        List<WeaponSlotPoint> slots = slotPainter.getPointsIndex();

        if (slots.isEmpty()) {
            statusLabel.setText(StringManager.getString("NO_WEAPON_SLOTS_DEFINED_ON_THIS_HULL"));
            return;
        }

        Map<String, WeaponSlotOverride> overrides = activeSkin.getWeaponSlotChanges();
        int overrideCount = (overrides != null) ? overrides.size() : 0;
        statusLabel.setText(StringManager.getString("SKIN") + activeSkin + "  —  " + overrideCount + " slot override(s)");

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
            JLabel hint = new JLabel(StringManager.getString("SELECT_A_SLOT_FROM_THE_TABLE_TO_VIEW_ITS_OVERRIDE"));
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

        addReadOnlyField(panel, "Slot ID:", row.slotId, labelGbc, fieldGbc, gridRow++);

        addReadOnlyField(panel, "Base Type:", String.valueOf(row.baseType), labelGbc, fieldGbc, gridRow++);
        addReadOnlyField(panel, "Base Mount:", String.valueOf(row.baseMount), labelGbc, fieldGbc, gridRow++);
        addReadOnlyField(panel, "Base Size:", String.valueOf(row.baseSize), labelGbc, fieldGbc, gridRow++);

        String overrideStatus = row.hasOverride ? "✓ Has Override" : "✗ No Override";
        Color overrideColor = row.hasOverride
                ? new Color(100, 200, 100)
                : UIManager.getColor("Label.disabledForeground");
        addColoredField(panel, "Status:", overrideStatus, overrideColor, labelGbc, fieldGbc, gridRow++);

        JComboBox<WeaponType> typeBox = new JComboBox<>(WeaponType.values());
        JComboBox<WeaponMount> mountBox = new JComboBox<>(WeaponMount.values());
        JComboBox<WeaponSize> sizeBox = new JComboBox<>(WeaponSize.values());

        SpinnerNumberModel angleModel = new SpinnerNumberModel(0.0, -360.0, 360.0, 1.0);
        JSpinner angleSpinner = new JSpinner(angleModel);
        SpinnerNumberModel arcModel = new SpinnerNumberModel(0.0, 0.0, 360.0, 1.0);
        JSpinner arcSpinner = new JSpinner(arcModel);
        SpinnerNumberModel renderOrderModel = new SpinnerNumberModel(0, -1000, 1000, 1);
        JSpinner renderOrderSpinner = new JSpinner(renderOrderModel);

        if (row.hasOverride && row.override != null) {
            WeaponSlotOverride ov = row.override;
            typeBox.setSelectedItem(ov.getWeaponType() != null ? ov.getWeaponType() : row.baseType);
            mountBox.setSelectedItem(ov.getWeaponMount() != null ? ov.getWeaponMount() : row.baseMount);
            sizeBox.setSelectedItem(ov.getWeaponSize() != null ? ov.getWeaponSize() : row.baseSize);
            angleModel.setValue(ov.getBoxedAngle() != null ? ov.getAngle() : 0.0);
            arcModel.setValue(ov.getBoxedArc() != null ? ov.getArc() : 0.0);
            renderOrderModel.setValue(ov.getRenderOrderModBoxed() != null ? ov.getRenderOrderMod() : 0);
        } else {
            typeBox.setSelectedItem(row.baseType);
            mountBox.setSelectedItem(row.baseMount);
            sizeBox.setSelectedItem(row.baseSize);
        }

        addField(panel, "Override Type:", typeBox, labelGbc, fieldGbc, gridRow++);
        addField(panel, "Override Mount:", mountBox, labelGbc, fieldGbc, gridRow++);
        addField(panel, "Override Size:", sizeBox, labelGbc, fieldGbc, gridRow++);
        addField(panel, "Override Angle:", angleSpinner, labelGbc, fieldGbc, gridRow++);
        addField(panel, "Override Arc:", arcSpinner, labelGbc, fieldGbc, gridRow++);
        addField(panel, "Render Order Mod:", renderOrderSpinner, labelGbc, fieldGbc, gridRow++);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton(StringManager.getString("APPLY_OVERRIDE"));
        applyButton.addActionListener(e -> {
            WeaponSlotOverride.WeaponSlotOverrideBuilder builder = WeaponSlotOverride.builder();
            builder.slotID(row.slotId);
            WeaponType selType = (WeaponType) typeBox.getSelectedItem();
            if (selType != row.baseType) builder.weaponType(selType);
            WeaponMount selMount = (WeaponMount) mountBox.getSelectedItem();
            if (selMount != row.baseMount) builder.weaponMount(selMount);
            WeaponSize selSize = (WeaponSize) sizeBox.getSelectedItem();
            if (selSize != row.baseSize) builder.weaponSize(selSize);

            double angle = ((Number) angleModel.getValue()).doubleValue();
            if (angle != 0.0) builder.angle(angle);
            double arc = ((Number) arcModel.getValue()).doubleValue();
            if (arc != 0.0) builder.arc(arc);
            int rom = ((Number) renderOrderModel.getValue()).intValue();
            if (rom != 0) builder.renderOrderMod(rom);

            commitOverride(row.slotId, builder.build());
        });

        JButton clearButton = new JButton(StringManager.getString("CLEAR_OVERRIDE"));
        clearButton.setEnabled(row.hasOverride);
        clearButton.addActionListener(e -> {
            commitOverride(row.slotId, null);
        });

        buttonPanel.add(clearButton);
        buttonPanel.add(applyButton);

        GridBagConstraints btnGbc = new GridBagConstraints();
        btnGbc.gridx = 0;
        btnGbc.gridy = gridRow;
        btnGbc.gridwidth = 2;
        btnGbc.fill = GridBagConstraints.HORIZONTAL;
        btnGbc.insets = new Insets(8, 4, 4, 4);
        panel.add(buttonPanel, btnGbc);
    }

    private void commitOverride(String slotId, WeaponSlotOverride override) {
        ShipSkin activeSkin = cachedPainter.getActiveSkin();
        Map<String, WeaponSlotOverride> oldMap = activeSkin.getWeaponSlotChanges();
        if (oldMap == null) {
            oldMap = new LinkedHashMap<>();
        } else {
            oldMap = new LinkedHashMap<>(oldMap);
        }

        Map<String, WeaponSlotOverride> newMap = new LinkedHashMap<>(oldMap);
        if (override == null) {
            newMap.remove(slotId);
        } else {
            newMap.put(slotId, override);
        }

        if (newMap.isEmpty()) {
            newMap = null;
        }

        var edit = new SkinMapOverrideEdit<>(
                activeSkin::setWeaponSlotChanges,
                activeSkin.getWeaponSlotChanges(),
                newMap,
                EditorInstrument.SKIN_SLOTS,
                activeSkin
        );
        UndoOverseer.post(edit);
        edit.redo();
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

    private static void addField(JPanel panel, String labelText, java.awt.Component comp,
                                 GridBagConstraints labelGbc, GridBagConstraints fieldGbc, int row) {
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        panel.add(new JLabel(labelText), labelGbc);

        fieldGbc.gridx = 1;
        fieldGbc.gridy = row;
        panel.add(comp, fieldGbc);
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
