package shipeditor.components.instrument.ship.skins;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import shipeditor.utility.text.StringManager;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerUpdated;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ActiveShipSpec;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.UIConstants;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public abstract class AbstractSkinOverridesPanel<M extends AbstractTableModel> extends JPanel {

    protected final JComboBox<ShipSkin> skinChooser;
    protected final JLabel statusLabel;
    protected final JTable overridesTable;
    protected final M tableModel;
    protected final JPanel editorPanel;
    protected ShipPainter cachedPainter;
    private final EditorInstrument editorMode;
    private boolean isUpdatingSkinChooser;

    public AbstractSkinOverridesPanel(M tableModel, EditorInstrument editorMode, String editorTitle) {
        this.tableModel = tableModel;
        this.editorMode = editorMode;
        
        this.setLayout(new BorderLayout());

        JPanel chooserPanel = new JPanel(new BorderLayout(6, 0));
        chooserPanel.setBorder(new EmptyBorder(4, 6, 2, 6));
        JLabel skinLabel = new JLabel("Skin:");
        skinChooser = new JComboBox<>();
        skinChooser.addActionListener(e -> {
            if (isUpdatingSkinChooser || cachedPainter == null || cachedPainter.isUninitialized()) {
                return;
            }
            ShipSkin chosen = (ShipSkin) skinChooser.getSelectedItem();
            ActiveShipSpec spec = (chosen != null && !chosen.isBase()) ? ActiveShipSpec.SKIN : ActiveShipSpec.HULL;
            cachedPainter.setActiveSpec(spec, chosen);
        });
        chooserPanel.add(skinLabel, BorderLayout.LINE_START);
        chooserPanel.add(skinChooser, BorderLayout.CENTER);

        statusLabel = new JLabel(StringManager.getString("NO_SKIN_ACTIVE"));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));

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
        
        JPanel titlePanel = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(titlePanel, editorTitle);
        editorPanel.add(titlePanel, BorderLayout.PAGE_START);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(chooserPanel, BorderLayout.PAGE_START);
        topContainer.add(statusLabel, BorderLayout.CENTER);
        topContainer.add(editorPanel, BorderLayout.PAGE_END);

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

    protected void updateSkinChooser() {
        isUpdatingSkinChooser = true;
        skinChooser.removeAllItems();
        if (cachedPainter != null && !cachedPainter.isUninitialized()) {
            ShipLayer parentLayer = cachedPainter.getParentLayer();
            if (parentLayer != null && parentLayer.getSkins() != null) {
                for (ShipSkin skin : parentLayer.getSkins()) {
                    skinChooser.addItem(skin);
                }
                skinChooser.setSelectedItem(cachedPainter.getActiveSkin());
                skinChooser.setEnabled(skinChooser.getItemCount() > 0);
            } else {
                skinChooser.setEnabled(false);
            }
        } else {
            skinChooser.setEnabled(false);
        }
        isUpdatingSkinChooser = false;
    }

    protected abstract void refreshEditorPanel();
    
    protected abstract void refreshContent();

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
                if (checked.editorMode() == this.editorMode) {
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

    protected <K, V> void commitOverride(K key, V override,
                                         java.util.function.Function<ShipSkin, java.util.Map<K, V>> mapGetter,
                                         java.util.function.BiConsumer<ShipSkin, java.util.Map<K, V>> mapSetter) {
        if (cachedPainter == null || cachedPainter.isUninitialized()) return;
        ShipSkin activeSkin = cachedPainter.getActiveSkin();
        if (activeSkin == null || activeSkin.isBase()) return;

        java.util.Map<K, V> oldMap = mapGetter.apply(activeSkin);
        if (oldMap == null) {
            oldMap = new java.util.LinkedHashMap<>();
        } else {
            oldMap = new java.util.LinkedHashMap<>(oldMap);
        }

        java.util.Map<K, V> newMap = new java.util.LinkedHashMap<>(oldMap);
        if (override == null) {
            newMap.remove(key);
        } else {
            newMap.put(key, override);
        }

        if (newMap.isEmpty()) {
            newMap = null;
        }

        var edit = new shipeditor.undo.edits.features.SkinOverrideEdits.SkinMapOverrideEdit<>(
                map -> mapSetter.accept(activeSkin, map),
                mapGetter.apply(activeSkin),
                newMap,
                this.editorMode,
                activeSkin
        );
        shipeditor.undo.UndoOverseer.post(edit);
        edit.redo();
    }

    protected void addField(JPanel panel, String labelText, java.awt.Component comp,
                            java.awt.GridBagConstraints labelGbc, java.awt.GridBagConstraints fieldGbc, int row) {
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        panel.add(new JLabel(labelText), labelGbc);

        fieldGbc.gridx = 1;
        fieldGbc.gridy = row;
        panel.add(comp, fieldGbc);
    }

    protected void addReadOnlyField(JPanel panel, String labelText, String value,
                                    java.awt.GridBagConstraints labelGbc, java.awt.GridBagConstraints fieldGbc, int row) {
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

    protected void addColoredField(JPanel panel, String labelText, String value, java.awt.Color color,
                                  java.awt.GridBagConstraints labelGbc, java.awt.GridBagConstraints fieldGbc, int row) {
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        panel.add(new JLabel(labelText), labelGbc);

        fieldGbc.gridx = 1;
        fieldGbc.gridy = row;
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(color);
        panel.add(valueLabel, fieldGbc);
    }

    protected void showNoSelectionHint(JPanel panel, String hintText) {
        JLabel hint = new JLabel(hintText);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new java.awt.Insets(8, 4, 8, 4);
        panel.add(hint, gbc);
        panel.revalidate();
        panel.repaint();
    }
}
