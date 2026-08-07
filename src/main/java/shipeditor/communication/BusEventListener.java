package shipeditor.communication;

import shipeditor.communication.events.BusEvent;

/**
 * Functional listener interface for subscribing to events on the {@link shipeditor.communication.EventBus}.
 */
@FunctionalInterface
public interface BusEventListener {

    /**
     * Called by the {@link shipeditor.communication.EventBus} when a published event is dispatched.
     * 
     * @param event The event payload instance.
     */
    void handleEvent(BusEvent event);

}
