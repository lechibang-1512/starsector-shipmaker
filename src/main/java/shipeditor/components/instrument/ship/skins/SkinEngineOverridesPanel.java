package shipeditor.components.instrument.ship.skins;

import shipeditor.utility.text.StringManager;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerUpdated;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.entities.engine.EngineDataOverride;
import shipeditor.components.viewer.entities.engine.EnginePoint;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.components.viewer.painters.points.ship.EngineSlotPainter;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.ship.HullStyle;
import shipeditor.undo.UndoOverseer;
import shipeditor.undo.edits.features.SkinOverrideEdits.SkinMapOverrideEdit;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.UIConstants;
import shipeditor.utility.components.UIFactory;
import shipeditor.utility.themes.Themes;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class SkinEngineOverridesPanel extends JPanel {

    private final JLabel statusLabel;
    private final JTable overridesTable;
    private final EngineOverrideTableModel tableModel;
    private final JPanel editorPanel;
    private ShipPainter cachedPainter;

    public SkinEngineOverridesPanel() {
        this.setLayout(new BorderLayout());

        statusLabel = UIFactory.createLabel("No skin active");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(new EmptyBorder(8, 8, 8, 8));

        tableModel = new EngineOverrideTableModel();
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
                if (checked.editorMode() == EditorInstrument.SKIN_ENGINES) {
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

        EngineSlotPainter slotPainter = cachedPainter.getEnginePainter();
        List<EnginePoint> slots = slotPainter.getPointsIndex();

        if (slots.isEmpty()) {
            statusLabel.setText(StringManager.getString("NO_ENGINE_SLOTS_DEFINED_ON_THIS_HULL"));
            return;
        }

        Map<Integer, EngineDataOverride> overrides = activeSkin.getEngineSlotChanges();
        int overrideCount = (overrides != null) ? overrides.size() : 0;
        statusLabel.setText(StringManager.getString("SKIN") + activeSkin + "  —  " + overrideCount + " engine override(s)");

        tableModel.populate(slots, overrides);
        editorPanel.setVisible(true);
        refreshEditorPanel();
    }

    private JPanel createOverrideEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(panel, "Selected Engine Override");
        return panel;
    }

    private void refreshEditorPanel() {
        JPanel innerEditor = (JPanel) editorPanel.getComponent(0);
        innerEditor.removeAll();

        int selectedRow = overridesTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= tableModel.getRowCount()) {
            JLabel hint = UIFactory.createLabel("Select an engine slot from the table to view/edit its override");
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

        EngineOverrideRow row = tableModel.getRow(selectedRow);
        populateEditorForRow(innerEditor, row);

        innerEditor.revalidate();
        innerEditor.repaint();
    }

    private void populateEditorForRow(JPanel panel, EngineOverrideRow row) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.anchor = GridBagConstraints.LINE_START;
        labelGbc.insets = new Insets(2, 4, 2, 4);

        GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.fill = GridBagConstraints.HORIZONTAL;
        fieldGbc.weightx = 1;
        fieldGbc.insets = new Insets(2, 4, 2, 4);

        int gridRow = 0;

        addReadOnlyField(panel, "Engine Index:", String.valueOf(row.index), labelGbc, fieldGbc, gridRow++);

        String overrideStatus = row.hasOverride ? "✓ Has Override" : "✗ No Override";
        Color overrideColor = row.hasOverride
                ? Themes.getSuccessColor()
                : UIManager.getColor("Label.disabledForeground");
        addColoredField(panel, "Status:", overrideStatus, overrideColor, labelGbc, fieldGbc, gridRow++);

        SpinnerNumberModel angleModel = new SpinnerNumberModel(
                row.hasOverride && row.override.getAngle() != null ? row.override.getAngle().doubleValue() : row.baseAngle,
                -360.0, 360.0, 1.0);
        JSpinner angleSpinner = new JSpinner(angleModel);

        SpinnerNumberModel lengthModel = new SpinnerNumberModel(
                row.hasOverride && row.override.getLength() != null ? row.override.getLength().doubleValue() : row.baseLength,
                0.0, 1000.0, 1.0);
        JSpinner lengthSpinner = new JSpinner(lengthModel);

        SpinnerNumberModel widthModel = new SpinnerNumberModel(
                row.hasOverride && row.override.getWidth() != null ? row.override.getWidth().doubleValue() : row.baseWidth,
                0.0, 1000.0, 1.0);
        JSpinner widthSpinner = new JSpinner(widthModel);

        Map<String, HullStyle> allStyles = SettingsManager.getGameData().getAllHullStyles();
        Vector<String> styleIds = new Vector<>();
        styleIds.add(""); // Empty means no override
        if (allStyles != null) {
            styleIds.addAll(allStyles.keySet());
        }
        JComboBox<String> styleBox = new JComboBox<>(styleIds);
        if (row.hasOverride && row.override.getStyleID() != null) {
            styleBox.setSelectedItem(row.override.getStyleID());
        } else {
            styleBox.setSelectedIndex(0);
        }

        addField(panel, "Override Angle:", angleSpinner, labelGbc, fieldGbc, gridRow++);
        addField(panel, "Override Length:", lengthSpinner, labelGbc, fieldGbc, gridRow++);
        addField(panel, "Override Width:", widthSpinner, labelGbc, fieldGbc, gridRow++);
        addField(panel, "Override Style:", styleBox, labelGbc, fieldGbc, gridRow++);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = UIFactory.createButton("Apply Override");
        applyButton.addActionListener(e -> {
            EngineDataOverride.EngineDataOverrideBuilder builder = EngineDataOverride.builder();
            builder.index(row.index);
            builder.angle(((Number) angleModel.getValue()).doubleValue());
            builder.length(((Number) lengthModel.getValue()).doubleValue());
            builder.width(((Number) widthModel.getValue()).doubleValue());
            String selectedStyle = (String) styleBox.getSelectedItem();
            if (selectedStyle != null && !selectedStyle.isEmpty()) {
                builder.styleID(selectedStyle);
            }

            commitOverride(row.index, builder.build());
        });

        JButton clearButton = UIFactory.createButton("Clear Override");
        clearButton.setEnabled(row.hasOverride);
        clearButton.addActionListener(e -> {
            commitOverride(row.index, null);
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

    private void commitOverride(Integer index, EngineDataOverride override) {
        ShipSkin activeSkin = cachedPainter.getActiveSkin();
        Map<Integer, EngineDataOverride> oldMap = activeSkin.getEngineSlotChanges();
        if (oldMap == null) {
            oldMap = new LinkedHashMap<>();
        } else {
            oldMap = new LinkedHashMap<>(oldMap);
        }

        Map<Integer, EngineDataOverride> newMap = new LinkedHashMap<>(oldMap);
        if (override == null) {
            newMap.remove(index);
        } else {
            newMap.put(index, override);
        }

        if (newMap.isEmpty()) {
            newMap = null; // Clear out if empty
        }

        var edit = new SkinMapOverrideEdit<>(
                activeSkin::setEngineSlotChanges,
                activeSkin.getEngineSlotChanges(),
                newMap,
                EditorInstrument.SKIN_ENGINES,
                activeSkin
        );
        UndoOverseer.post(edit);
        edit.redo();
    }

    private static void addReadOnlyField(JPanel panel, String labelText, String value,
                                          GridBagConstraints labelGbc, GridBagConstraints fieldGbc, int row) {
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        panel.add(UIFactory.createLabel(labelText), labelGbc);

        fieldGbc.gridx = 1;
        fieldGbc.gridy = row;
        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setColumns(12);
        panel.add(field, fieldGbc);
    }

    private static void addField(JPanel panel, String labelText, Component comp,
                                 GridBagConstraints labelGbc, GridBagConstraints fieldGbc, int row) {
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        panel.add(UIFactory.createLabel(labelText), labelGbc);

        fieldGbc.gridx = 1;
        fieldGbc.gridy = row;
        panel.add(comp, fieldGbc);
    }

    private static void addColoredField(JPanel panel, String labelText, String value, Color color,
                                          GridBagConstraints labelGbc, GridBagConstraints fieldGbc, int row) {
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        panel.add(UIFactory.createLabel(labelText), labelGbc);

        fieldGbc.gridx = 1;
        fieldGbc.gridy = row;
        JLabel valueLabel = UIFactory.createLabel(value);
        valueLabel.setForeground(color);
        panel.add(valueLabel, fieldGbc);
    }

    private static final class EngineOverrideRow {
        Integer index;
        double baseAngle;
        double baseLength;
        double baseWidth;
        boolean hasOverride;
        EngineDataOverride override;

        EngineOverrideRow(EnginePoint slot, Integer index, EngineDataOverride override) {
            this.index = index;
            this.baseAngle = slot.getAngle();
            this.baseLength = slot.getLength();
            this.baseWidth = slot.getWidth();
            this.hasOverride = (override != null);
            this.override = override;
        }
    }

    private static final class EngineOverrideTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {
                "Index", "Base Angle", "Base Length", "Base Width", "Override?"
        };

        private final List<EngineOverrideRow> rows = new ArrayList<>();

        void clear() {
            int oldSize = rows.size();
            rows.clear();
            if (oldSize > 0) {
                fireTableRowsDeleted(0, oldSize - 1);
            }
        }

        void populate(List<EnginePoint> slots, Map<Integer, EngineDataOverride> overrides) {
            rows.clear();
            for (int i = 0; i < slots.size(); i++) {
                EnginePoint slot = slots.get(i);
                EngineDataOverride override = null;
                if (overrides != null) {
                    override = overrides.get(i);
                }
                rows.add(new EngineOverrideRow(slot, i, override));
            }
            fireTableDataChanged();
        }

        EngineOverrideRow getRow(int rowIndex) {
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
            EngineOverrideRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return row.index;
                case 1: return row.baseAngle;
                case 2: return row.baseLength;
                case 3: return row.baseWidth;
                case 4: return row.hasOverride ? "✓" : "";
                default: return "";
            }
        }
    }
}
