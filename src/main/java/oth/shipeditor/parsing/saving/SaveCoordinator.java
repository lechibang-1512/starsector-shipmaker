package oth.shipeditor.parsing.saving;

import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.files.saving.HullSaveQueued;
import oth.shipeditor.communication.events.files.saving.VariantSaveQueued;

public final class SaveCoordinator {

    private SaveCoordinator() {
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    public static void init() {
        EventBus.subscribe(SaveCoordinator.class, event -> {
            if (event instanceof VariantSaveQueued checked) {
                SaveVariantAction.saveVariant(checked.variant());
            } else if (event instanceof HullSaveQueued checked) {
                SaveHullAction.saveHullFromLayer(checked.shipLayer());
            } else if (event instanceof oth.shipeditor.communication.events.files.saving.CSVSaveQueued checked) {
                SaveCSVAction.saveCSVEntry(checked.entry());
            }
        });
    }

}
