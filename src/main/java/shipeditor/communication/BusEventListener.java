package shipeditor.communication;

import shipeditor.communication.events.BusEvent;

@FunctionalInterface
public interface BusEventListener {

    void handleEvent(BusEvent event);

}
