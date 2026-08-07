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
        EventBus.reset();
    }

    @AfterEach
    void tearDown() {
        EventBus.reset();
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
        
        // Force GC with adaptive retry
        for (int i = 0; i < 30; i++) {
            System.gc();
            if (weakRef.get() == null) break;
            try { Thread.sleep(20); } catch (InterruptedException e) {}
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
        
        // Force GC with adaptive retry
        for (int i = 0; i < 30; i++) {
            System.gc();
            if (parentRef.get() == null) break;
            try { Thread.sleep(20); } catch (InterruptedException e) {}
        }
        
        assertNull(parentRef.get(), "Parent should have been garbage collected");
        
        EventBus.publish(new TestEvent());
        waitForEDT();
        assertEquals(1, callCount.get(), "Listener should not receive events after parent GC");
    }

    @Test
    void testUnsubscribe() {
        TestListener listener = new TestListener();
        EventBus.subscribe(listener);
        try {
            EventBus.publish(new TestEvent());
            waitForEDT();
            assertEquals(1, listener.callCount.get());
            
            EventBus.unsubscribe(listener);
            EventBus.publish(new TestEvent());
            waitForEDT();
            assertEquals(1, listener.callCount.get());
        } finally {
            EventBus.unsubscribe(listener);
        }
    }

    @Test
    void testUnsubscribeByParent() {
        Object parent = new Object();
        TestListener listener = new TestListener();
        EventBus.subscribe(parent, listener);
        try {
            EventBus.publish(new TestEvent());
            waitForEDT();
            assertEquals(1, listener.callCount.get());
            
            EventBus.unsubscribeByParent(parent);
            EventBus.publish(new TestEvent());
            waitForEDT();
            assertEquals(1, listener.callCount.get());
        } finally {
            EventBus.unsubscribeByParent(parent);
        }
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
        
        TestListener observer = new TestListener();
        try {
            EventBus.subscribe(recursiveListener);
            EventBus.subscribe(observer);
            
            EventBus.publish(new TestEvent());
            waitForEDT();
            
            assertEquals(1, initialEventCalls.get());
            assertEquals(1, nestedEventCalls.get());
            assertEquals(2, observer.callCount.get());
            assertTrue(observer.receivedEvents.stream().anyMatch(e -> e instanceof TestEvent));
            assertTrue(observer.receivedEvents.stream().anyMatch(e -> e instanceof AnotherEvent));
        } finally {
            EventBus.unsubscribe(recursiveListener);
            EventBus.unsubscribe(observer);
        }
    }

    @Test
    void testExceptionIsolation() {
        TestListener goodListener1 = new TestListener();
        TestListener goodListener2 = new TestListener();
        
        BusEventListener badListener = event -> {
            throw new RuntimeException("Simulated failure");
        };
        
        EventBus.ExceptionHandler originalHandler = EventBus.getExceptionHandler();
        try {
            EventBus.subscribe(goodListener1);
            EventBus.subscribe(badListener);
            EventBus.subscribe(goodListener2);
            
            EventBus.setExceptionHandler((t, l, e) -> {
                // Silently ignore during this test
            });
            
            assertDoesNotThrow(() -> {
                EventBus.publish(new TestEvent());
                waitForEDT();
            });
            
            assertEquals(1, goodListener1.callCount.get());
            assertEquals(1, goodListener2.callCount.get());
        } finally {
            EventBus.setExceptionHandler(originalHandler);
            EventBus.unsubscribe(goodListener1);
            EventBus.unsubscribe(badListener);
            EventBus.unsubscribe(goodListener2);
        }
    }
    
    @Test
    void testTraceLogging() {
        try {
            EventBus.TRACE_ENABLED = true;
            assertDoesNotThrow(() -> EventBus.publish(new TestEvent()));
        } finally {
            EventBus.TRACE_ENABLED = false;
        }
    }
}
