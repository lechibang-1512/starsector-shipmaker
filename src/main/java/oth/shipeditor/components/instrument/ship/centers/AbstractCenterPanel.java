package oth.shipeditor.components.instrument.ship.centers;

import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.components.InstrumentRepaintQueued;
import oth.shipeditor.components.instrument.EditorInstrument;
import oth.shipeditor.components.instrument.ship.AbstractShipPropertiesPanel;
import oth.shipeditor.utility.overseers.StaticController;

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
