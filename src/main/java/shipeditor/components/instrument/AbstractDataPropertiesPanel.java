package shipeditor.components.instrument;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerUpdated;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;

public abstract class AbstractDataPropertiesPanel<T extends LayerPainter> extends LayerPropertiesPanel {

    private final Class<T> painterClass;

    protected AbstractDataPropertiesPanel(Class<T> painterClass) {
        this.painterClass = painterClass;
        this.initLayerListeners();
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    protected void initLayerListeners() {
        EventBus.subscribe(this, event -> {
            if (event instanceof LayerWasSelected checked) {
                var selected = checked.selected();
                this.handleRefreshFromLayer(selected);
            } else if (event instanceof ActiveLayerUpdated checked) {
                this.handleRefreshFromLayer(checked.updated());
            }
        });
    }

    protected void handleRefreshFromLayer(ViewerLayer selected) {
        if (selected == null || selected.getPainter() == null) {
            this.refresh(null);
            return;
        }
        LayerPainter layerPainter = selected.getPainter();
        if (!painterClass.isInstance(layerPainter) || layerPainter.isUninitialized()) {
            this.refresh(null);
            return;
        }

        this.refresh(layerPainter);
    }
}
