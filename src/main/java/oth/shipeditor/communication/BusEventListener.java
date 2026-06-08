package oth.shipeditor.communication;

import oth.shipeditor.communication.events.BusEvent;

@FunctionalInterface
public interface BusEventListener {

    void handleEvent(BusEvent event);

}
