package oth.shipeditor.communication;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.events.BusEvent;
import oth.shipeditor.utility.Errors;

import java.util.Set;

/** * This is a very simple implementation of EventBus-type Observer pattern, meant to decouple different parts of the app.
 * <p>
 * Initially implemented in a much more elaborate form with generics and selective dispatch by class metadata.
 * Yet, excessive complexity here only leads to more issues down the road; best to KISS here.*/
@Log4j2
public final class EventBus {

    private static final EventBus bus = new EventBus();

    // Floating subscribers: the listener itself is the weak key. It will be GC'd if not strongly referenced elsewhere.
    private final Set<BusEventListener> floatingSubscribers;

    // Lifecycle subscribers: the parent component is the weak key. Listeners live as long as the parent lives.
    private final java.util.Map<Object, java.util.List<BusEventListener>> lifecycleSubscribers;

    private static final ThreadLocal<DispatchContext> threadDispatchContext = ThreadLocal.withInitial(DispatchContext::new);

    private static class DispatchContext {
        final java.util.List<BusEventListener> list = new java.util.ArrayList<>(128);
        boolean inUse = false;
    }

    private EventBus() {
        this.floatingSubscribers = java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>()));
        this.lifecycleSubscribers = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    }

    /**
     * Subscribes a listener that will be garbage collected if no other strong references to it exist.
     * WARNING: Do not pass anonymous lambdas here unless you save the reference!
     */
    @SuppressWarnings("UnusedReturnValue")
    public static BusEventListener subscribe(BusEventListener listener) {
        bus.floatingSubscribers.add(listener);
        return listener;
    }

    /**
     * Subscribes a listener whose lifecycle is bound to the provided parent object.
     * The listener will be kept alive as long as the parent is not garbage collected.
     */
    public static BusEventListener subscribe(Object lifecycleParent, BusEventListener listener) {
        bus.lifecycleSubscribers.computeIfAbsent(lifecycleParent, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(listener);
        return listener;
    }

    public static void unsubscribe(BusEventListener listener) {
        bus.floatingSubscribers.remove(listener);
        synchronized (bus.lifecycleSubscribers) {
            for (java.util.List<BusEventListener> list : bus.lifecycleSubscribers.values()) {
                list.remove(listener);
            }
        }
    }

    public static void publish(BusEvent event) {
        DispatchContext context = threadDispatchContext.get();
        java.util.List<BusEventListener> activeListeners;
        boolean wasInUse = context.inUse;
        
        if (wasInUse) {
            activeListeners = new java.util.ArrayList<>();
        } else {
            context.inUse = true;
            activeListeners = context.list;
            activeListeners.clear();
        }
        
        try {
            synchronized (bus.floatingSubscribers) {
                activeListeners.addAll(bus.floatingSubscribers);
            }
            
            synchronized (bus.lifecycleSubscribers) {
                for (java.util.List<BusEventListener> list : bus.lifecycleSubscribers.values()) {
                    activeListeners.addAll(list);
                }
            }

            for (int i = 0; i < activeListeners.size(); i++) {
                BusEventListener receiver = activeListeners.get(i);
                if (receiver == null) continue;
                try {
                    receiver.handleEvent(event);
                } catch (Throwable throwable) {
                    log.error("Error in listener {} handling event {}", getListenerName(receiver), event.getClass().getSimpleName());
                    Errors.printToStream(throwable);
                }
            }
        } finally {
            if (!wasInUse) {
                activeListeners.clear();
                context.inUse = false;
            }
        }
    }

    private static String getListenerName(BusEventListener listener) {
        Class<? extends BusEventListener> identity = listener.getClass();
        String shortName = identity.getSimpleName();
        String pattern = "/0x.*";
        // Apply the pattern and trim the string.
        return shortName.replaceAll(pattern, "");
    }

}
