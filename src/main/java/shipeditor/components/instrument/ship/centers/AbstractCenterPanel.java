package shipeditor.components.instrument.ship.centers;

import shipeditor.communication.EventBus;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.instrument.ship.AbstractShipPropertiesPanel;
import shipeditor.utility.overseers.StaticController;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;

public abstract class AbstractCenterPanel extends AbstractShipPropertiesPanel {

    @Override
    protected void initLayerListeners() {
        super.initLayerListeners();
        EventBus.subscribe(this, event -> {
            if (event instanceof InstrumentRepaintQueued checked) {
                if (checked.editorMode() == getMode()) {
                    this.handleRefreshFromLayer(StaticController.getActiveLayer());
                }
            }
        });
    }

    protected abstract EditorInstrument getMode();

}
