package shipeditor.components.instrument.ship.skins;

import shipeditor.utility.text.StringManager;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerUpdated;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.UIConstants;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;

public abstract class AbstractSkinOverridesPanel<M extends AbstractTableModel> extends JPanel {

    protected final JLabel statusLabel;
    protected final JTable overridesTable;
    protected final M tableModel;
    protected final JPanel editorPanel;
    protected ShipPainter cachedPainter;
    private final EditorInstrument editorMode;

    public AbstractSkinOverridesPanel(M tableModel, EditorInstrument editorMode, String editorTitle) {
        this.tableModel = tableModel;
        this.editorMode = editorMode;
        
        this.setLayout(new BorderLayout());

        statusLabel = new JLabel(StringManager.getString("NO_SKIN_ACTIVE"));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(new EmptyBorder(8, 8, 8, 8));

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
}
