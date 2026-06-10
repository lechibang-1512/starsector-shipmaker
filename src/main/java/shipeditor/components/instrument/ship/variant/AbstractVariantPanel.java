package shipeditor.components.instrument.ship.variant;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.ActiveLayerUpdated;
import shipeditor.communication.events.viewer.layers.LayerWasSelected;
import shipeditor.components.viewer.layers.ViewerLayer;

import javax.swing.JPanel;

public abstract class AbstractVariantPanel extends JPanel {

    AbstractVariantPanel() {
        initLayerListeners();
    }

    public abstract void refreshPanel(ViewerLayer selected);

    @SuppressWarnings("ChainOfInstanceofChecks")
    protected void initLayerListeners() {
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerWasSelected checked) {
                this.refreshPanel(checked.selected());
            } else if (event instanceof ActiveLayerUpdated checked) {
                this.refreshPanel(checked.updated());
            }
        });
    }

}
