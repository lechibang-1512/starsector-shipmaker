# EventBus Event Creation & Subscription Template

### 1. Define the Event
Create a simple record class under `shipeditor.communication.events`:

```java
package shipeditor.communication.events.viewer.layers;

import shipeditor.communication.events.BusEvent;
import shipeditor.components.viewer.layers.ViewerLayer;

public record CustomLayerEvent(ViewerLayer layer, String action) implements BusEvent {
}
```

### 2. Subscribe (Standard Weak Parent)
Bind the subscriber to a parent Swing/AWT component lifecycle to prevent garbage collection:

```java
EventBus.subscribe(this, event -> {
    if (event instanceof CustomLayerEvent checked) {
        handleCustomEvent(checked.layer(), checked.action());
    }
});
```

### 3. Subscribe (Permanent Class-Bound)
For static singletons that should observe events permanently:

```java
EventBus.subscribe(MyController.class, event -> {
    if (event instanceof CustomLayerEvent checked) {
        handleCustomEvent(checked.layer(), checked.action());
    }
});
```
