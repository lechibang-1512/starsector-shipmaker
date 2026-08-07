package shipeditor.communication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shipeditor.communication.events.BusEvent;

import javax.swing.SwingUtilities;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {

    private static class TestEvent implements BusEvent {}
    private static class AnotherEvent implements BusEvent {}

    private static class TestListener implements BusEventListener {
        final AtomicInteger callCount = new AtomicInteger(0);
        final List<BusEvent> receivedEvents = new ArrayList<>();
        
        @Override
        public void handleEvent(BusEvent event) {
            callCount.incrementAndGet();
            receivedEvents.add(event);
        }
    }

    @BeforeEach
    void setUp() {
        // We rely on GC to clear weak references or explicit unsubscribe.
    }

    private void waitForEDT() {
        try {
            SwingUtilities.invokeAndWait(() -> {});
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testFloatingListenerGC() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        // Scope to allow listener to be GC'd
        WeakReference<BusEventListener> weakRef;
        {
            BusEventListener listener = event -> callCount.incrementAndGet();
            EventBus.subscribe(listener);
            weakRef = new WeakReference<>(listener);
            
            EventBus.publish(new TestEvent());
            waitForEDT();
            assertEquals(1, callCount.get());
        }
        
        // Force GC
        for (int i = 0; i < 10; i++) {
            System.gc();
            if (weakRef.get() == null) break;
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        }
        
        assertNull(weakRef.get(), "Listener should have been garbage collected");
        
        EventBus.publish(new TestEvent());
        waitForEDT();
        assertEquals(1, callCount.get(), "Listener should not receive events after GC");
    }

    @Test
    void testLifecycleListenerGC() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        WeakReference<Object> parentRef;
        BusEventListener listener = event -> callCount.incrementAndGet();
        {
            Object parent = new Object();
            parentRef = new WeakReference<>(parent);
            EventBus.subscribe(parent, listener);
            
            EventBus.publish(new TestEvent());
            waitForEDT();
            assertEquals(1, callCount.get());
        }
        
        // Force GC
        for (int i = 0; i < 10; i++) {
            System.gc();
            if (parentRef.get() == null) break;
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        }
        
        assertNull(parentRef.get(), "Parent should have been garbage collected");
        
        // However, for lifecycle listeners, the GC of the parent removes the key from the WeakHashMap.
        // Let's trigger a publish to ensure the listener doesn't fire.
        EventBus.publish(new TestEvent());
        waitForEDT();
        assertEquals(1, callCount.get(), "Listener should not receive events after parent GC");
    }

    @Test
    void testUnsubscribe() {
        TestListener listener = new TestListener();
        EventBus.subscribe(listener);
        
        EventBus.publish(new TestEvent());
        waitForEDT();
        assertEquals(1, listener.callCount.get());
        
        EventBus.unsubscribe(listener);
        EventBus.publish(new TestEvent());
        waitForEDT();
        assertEquals(1, listener.callCount.get());
    }

    @Test
    void testUnsubscribeByParent() {
        Object parent = new Object();
        TestListener listener = new TestListener();
        EventBus.subscribe(parent, listener);
        
        EventBus.publish(new TestEvent());
        waitForEDT();
        assertEquals(1, listener.callCount.get());
        
        EventBus.unsubscribeByParent(parent);
        EventBus.publish(new TestEvent());
        waitForEDT();
        assertEquals(1, listener.callCount.get());
    }

    @Test
    void testRecursiveEventDispatch() {
        AtomicInteger initialEventCalls = new AtomicInteger(0);
        AtomicInteger nestedEventCalls = new AtomicInteger(0);
        
        // A listener that triggers another event when it receives the first one
        BusEventListener recursiveListener = new BusEventListener() {
            @Override
            public void handleEvent(BusEvent event) {
                if (event instanceof TestEvent) {
                    initialEventCalls.incrementAndGet();
                    EventBus.publish(new AnotherEvent());
                } else if (event instanceof AnotherEvent) {
                    nestedEventCalls.incrementAndGet();
                }
            }
        };
        
        EventBus.subscribe(recursiveListener);
        
        // A normal listener to ensure it receives both events
        TestListener observer = new TestListener();
        EventBus.subscribe(observer);
        
        EventBus.publish(new TestEvent());
        waitForEDT();
        
        assertEquals(1, initialEventCalls.get());
        assertEquals(1, nestedEventCalls.get());
        
        assertEquals(2, observer.callCount.get());
        
        // Order could vary depending on HashSet iterator but observer usually receives TestEvent before AnotherEvent if DepthList pushes synchronously.
        // Wait, dispatch context depthLists processes synchronous layers.
        // AnotherEvent is published while TestEvent is still being handled by recursiveListener. 
        // AnotherEvent goes to depth=1, completes its cycle, then returns to TestEvent.
        // Since observer is after recursiveListener in floating subscribers, observer receives TestEvent AFTER AnotherEvent if order is strict.
        // Since floatingSubscribers is a Set, order is not guaranteed. We just check if it contains both.
        assertTrue(observer.receivedEvents.stream().anyMatch(e -> e instanceof TestEvent));
        assertTrue(observer.receivedEvents.stream().anyMatch(e -> e instanceof AnotherEvent));
        
        // Cleanup to prevent side effects in other tests due to the static bus
        EventBus.unsubscribe(recursiveListener);
        EventBus.unsubscribe(observer);
    }

    @Test
    void testExceptionIsolation() {
        TestListener goodListener1 = new TestListener();
        TestListener goodListener2 = new TestListener();
        
        BusEventListener badListener = event -> {
            throw new RuntimeException("Simulated failure");
        };
        
        EventBus.subscribe(goodListener1);
        EventBus.subscribe(badListener);
        EventBus.subscribe(goodListener2);
        
        EventBus.ExceptionHandler originalHandler = EventBus.getExceptionHandler();
        EventBus.setExceptionHandler((t, l, e) -> {
            // Silently ignore during this test
        });
        
        try {
            // Publishing should not throw the exception back to us; it should be caught and ignored
            assertDoesNotThrow(() -> {
                EventBus.publish(new TestEvent());
                waitForEDT();
            });
        } finally {
            EventBus.setExceptionHandler(originalHandler);
        }
        
        // Both good listeners should have received the event despite the bad listener throwing
        assertEquals(1, goodListener1.callCount.get());
        assertEquals(1, goodListener2.callCount.get());
        
        EventBus.unsubscribe(goodListener1);
        EventBus.unsubscribe(badListener);
        EventBus.unsubscribe(goodListener2);
    }
    
    @Test
    void testTraceLogging() {
        EventBus.TRACE_ENABLED = true;
        assertDoesNotThrow(() -> EventBus.publish(new TestEvent()));
        EventBus.TRACE_ENABLED = false;
    }
}
