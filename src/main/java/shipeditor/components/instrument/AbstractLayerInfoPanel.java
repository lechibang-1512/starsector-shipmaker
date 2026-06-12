package shipeditor.components.instrument;
import shipeditor.components.ComponentEnums.EditorInstrument;


import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.utility.overseers.StaticController;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;

public abstract class AbstractLayerInfoPanel extends JPanel {

    private final LayerCircumstancePanel layerCircumstancePanel;

    protected AbstractLayerInfoPanel() {
        this.setLayout(new BorderLayout());

        layerCircumstancePanel = new LayerCircumstancePanel();
        this.add(layerCircumstancePanel, BorderLayout.PAGE_START);

        this.initListeners();
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private void initListeners() {
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerWasSelected checked) {
                handleLayerSelected(checked.selected());
            } else if (event instanceof InstrumentRepaintQueued checked) {
                var editorMode = checked.editorMode();
                if (editorMode == EditorInstrument.LAYER) {
                    handleLayerSelected(StaticController.getActiveLayer());
                }
            }
        });
    }

    private void handleLayerSelected(ViewerLayer selected) {
        clearData();

        if (selected == null || selected.getPainter() == null) {
            layerCircumstancePanel.refresh(null);
            return;
        }
        LayerPainter layerPainter = selected.getPainter();
        if (!isValidLayer(layerPainter)) {
            layerCircumstancePanel.refresh(null);
            return;
        }

        this.refreshData(selected);
        layerCircumstancePanel.refresh(selected.getPainter());
    }

    protected abstract boolean isValidLayer(LayerPainter layerPainter);

    protected abstract void clearData();

    /**
     * @param selected passed checks and is guaranteed to be valid layer.
     */
    protected abstract void refreshData(ViewerLayer selected);

}
