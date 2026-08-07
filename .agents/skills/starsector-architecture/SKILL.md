---
name: starsector-architecture
description: Core system architecture including EventBus, Undo/Redo, Layer System, Threading Model, and Startup Sequence.
---

# Starsector Ship Editor — System Design

## Skill Directory Structure

This skill is organized as follows:
- **`SKILL.md`**: Main instructions (this file).
- **`resources/`**: Configurations and templates.
  - [event_bus_template.md](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-architecture/resources/event_bus_template.md): Template for creating events and subscribing.
- **`examples/`**: Code references.
  - [EventSubscriptionExample.java.txt](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-architecture/examples/EventSubscriptionExample.java.txt): Reference implementation of a lifecycle-bound subscription.
- **`scripts/`**: Tooling.
  - [find_leaking_subscribers.sh](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-architecture/scripts/find_leaking_subscribers.sh): Helper script to identify anonymous lambda EventBus registrations.

## 1. Event-Driven Architecture: The EventBus

[EventBus.java](file:///run/media/lechibang/cb09d199-3769-4ec8-9af5-954929515428/projects/starsector-shipmaker/src/main/java/shipeditor/communication/EventBus.java) is the backbone of inter-component communication. It is a custom, minimal implementation — **not** Google Guava EventBus or GreenRobot.

### Two Subscription Models

1. **Floating Subscribers**: Stored in a `WeakHashMap`-backed `Set`. The listener itself is the weak key. If no strong reference exists elsewhere, the listener is GC'd and silently unsubscribed.
   - **Warning**: Anonymous lambdas passed to `subscribe(listener)` will be immediately eligible for GC unless the caller retains a reference. The Javadoc explicitly warns about this.

2. **Lifecycle Subscribers**: Stored using `WeakListenerWrapper` instances inside a `WeakHashMap<Object, CopyOnWriteArrayList<BusEventListener>>`. The parent component is the weak key. All listeners bound to a parent are kept alive as long as the parent is alive.
   - For `JComponent` parents, listeners are attached via `putClientProperty` for strong referencing.
   - For non-`JComponent` parents, a `fallbackStrongReferences` map ensures the listener isn't garbage collected prematurely while the parent is alive.
   - `subscribe(lifecycleParent, listener)` — Binds the listener's lifetime to the parent.
   - `unsubscribeByParent(parent)` — Removes all listeners for a parent in O(1).

### Dispatch Mechanism (Quirks)

**Reentrant-Safe Dispatch**: Events can trigger other events during handling (e.g., selecting a layer fires `LayerWasSelected`, which might trigger `ViewerRepaintQueued`). The EventBus handles this via a `ThreadLocal<DispatchContext>`:

```java
private static class DispatchContext {
    final List<List<BusEventListener>> depthLists = new ArrayList<>();
    int currentDepth = 0;
}
```

Each nesting level of `publish()` gets its own snapshot list, preventing `ConcurrentModificationException`. The depth counter tracks reentrant calls.

**Broadcast, Not Selective**: Every event is broadcast to every listener. There is no type-based filtering at the bus level — each listener must `instanceof`-check the event type. This was a deliberate KISS design decision; an earlier implementation with generics and selective dispatch caused "more issues down the road" (per the source comment).

**Error Isolation**: The `EventBus` provides an `ExceptionHandler` interface. By default, if a listener throws, the exception is caught, logged with the listener's class name (cleaned of lambda hex suffixes), and dispatch continues. You can inject custom error handling via `EventBus.setExceptionHandler()`.

---

## 2. Undo/Redo System

[UndoOverseer.java](file:///run/media/lechibang/cb09d199-3769-4ec8-9af5-954929515428/projects/starsector-shipmaker/src/main/java/shipeditor/undo/UndoOverseer.java) implements a strict LIFO undo/redo stack.

### Architecture
- **Singleton**: `private static final UndoOverseer seer = new UndoOverseer();`
- **Stacks**: Two `ArrayDeque<Edit>` instances — `undoStack` and `redoStack`.
- **Capacity**: Hardcoded to `MAX_UNDO_CAPACITY = 200`. When exceeded, the oldest edit is silently dropped from the bottom of the undo stack.

### Edit Flow
1. **Post**: `UndoOverseer.post(edit)` pushes to undo stack, clears redo stack, and marks the relevant layer as dirty. (Note: Posting is ignored if `UndoOverseer.isPaused()` is true).
2. **Undo**: Pops from undo, calls `edit.undo()`, pushes to redo.
3. **Redo**: Pops from redo, calls `edit.redo()`, pushes to undo.

### Layer Cleanup (Quirk)
When a layer is removed, `cleanupRemovedLayer(painter)` iterates both stacks and removes all `LayerEdit` instances referencing that painter. This prevents undo/redo from operating on deleted layers. The edit's `cleanupReferences()` is called to null out dangling references.

### Point Drag Adjustment (Quirk)
`adjustPointEditsOffset(point, offset)` iterates **all** edits (both stacks) and adjusts the stored positions of `PointDragEdit`s. This is needed when a layer's anchor changes — existing undo history must be updated to reflect the new coordinate system.

### Dirty Marking
Every edit categorizes itself as `EditCategory.NONE`, `EditCategory.HULL`, or `EditCategory.VARIANT`. Non-NONE edits mark the target layer as unsaved. If the edit implements `LayerEdit`, the target layer is extracted directly from the edit's painter, allowing dirty marking to correctly affect inactive layers (otherwise, it falls back to the currently active layer).

### Selective Undo Forbidden
The class comment explicitly states: *"exposing edit stack to the user for selective undo is strictly forbidden."* The system relies on strict LIFO ordering; cherry-picking edits would break state consistency.

---

## 3. Layer System

### `ViewerLayer` → `LayerPainter` Relationship
Each `ViewerLayer` (abstract) may have a `LayerPainter` attached. Concrete implementations:
- `ShipLayer` → `ShipPainter`
- `WeaponLayer` → `WeaponPainter`

### Multi-Layer Placement (Quirk)
When loading a new layer while others exist, `PrimaryViewer.loadLayer()` automatically positions the new layer's anchor adjacent to the previous layer:
```java
var layerAnchor = layerPainter.getAnchor();
var prevLayerWidth = layerPainter.getSpriteSize();
Point2D widthPoint = new Point2D.Double(
    layerAnchor.getX() + prevLayerWidth.width, layerAnchor.getY()
);
newPainter.updateAnchorOffset(widthPoint);
UndoOverseer.finishAllEdits(); // Mark all existing edits as non-combinable
```
This places sprites side-by-side for visual comparison. `finishAllEdits()` is called to prevent the anchor update from being combined with previous drag operations in the undo stack.

---

## 4. Threading Model

### EDT-Only UI Rule
All Swing UI modifications must happen on the Event Dispatch Thread. `Main.main()` wraps the entire initialization in `SwingUtilities.invokeLater()`. The `IndexScannerTask` uses `SwingUtilities.invokeAndWait()` to show confirmation dialogs from the background scanner thread.

### GL Thread = Swing Repaint Thread
Because `AWTGLCanvas.paintGL()` is called during Swing's repaint cycle, the GL thread **is** the EDT. This means:
- GL calls are safe within `paintGL()` and the `glRunnables` queue.
- GL calls from background threads (e.g., texture loading during indexing) **must** be enqueued via `queueGLTask()`.
- There is no separate render thread — the EDT owns both UI and GL.

### Background Work & Concurrency Guidelines
- `IndexScannerTask.scanAndIndexAll()` runs on a background thread (launched by the data loading system).
- `CompletableFuture` queries in `DatabaseQueryService` run on `ForkJoinPool.commonPool()`.
- **Local map accumulation**: Background parsing/loading actions must accumulate elements into a local `ConcurrentHashMap` or thread-confined collection, rather than mutating the active global collections in `GameDataRepository` directly.
- **Atomic EDT publishing**: Once the background loader task finishes, it must return a `Runnable` that assigns the fully populated local map to `GameDataRepository` atomically on the EDT.
- **Volatile visibility**: All global collections and state flags in `GameDataRepository` and `FileLoading` (such as `allShipEntries`, `allWeaponEntries`, `allVariants`, `allProjectiles`, and `loadingInProgress`) must be marked `volatile` to guarantee immediate visibility of the references/values across thread boundaries.
- **EventBus Synchronization**: Subscriber maps (`lifecycleSubscribers`) must protect all operations (such as `computeIfAbsent()` in `subscribe` and `remove()` in `unsubscribeByParent`) with explicit synchronization (`synchronized (bus.lifecycleSubscribers)`) to prevent map corruption from concurrent access.
- All results are marshalled back to EDT via `SwingUtilities.invokeLater()` before touching UI components.

---

## 5. Static Controller Pattern

`StaticController` is a static singleton that holds references to the active `PrimaryViewer`, `LayerManager`, and active layer. It acts as a global service locator, and integrates with the `EventScheduler`.

This is a pragmatic trade-off: it avoids threading constructor references through deeply nested component hierarchies, at the cost of testability. The entire application assumes a single-viewer, single-window architecture.

### Permanent Event Subscriptions
When static managers (like `StaticController`) need to observe events for the entire lifetime of the application, they use their own `.class` object as the lifecycle parent (e.g., `EventBus.subscribe(StaticController.class, listener)`). This guarantees the subscription is never garbage collected.

---

## 6. Settings & Persistence

Settings are stored in `ship_editor_settings.json` (co-located with the JAR). The `SettingsManager` manages serialization/deserialization of the `Settings` POJO via Jackson.

Key settings include:
- Core game folder path
- List of mod folder paths
- Active theme
- Whether to load data at startup
- Per-package enable/disable state

### Database Co-location
The SQLite database is placed in the same directory as the settings file, discovered via `SettingsManager.getSettingsPath().toPath().getParent()`.

---

## 7. Startup Sequence

`Main.main()` executes:
1. **`checkAndRelaunch()`** — Self-relaunch with memory cap if needed.
2. **Disable Java2D GPU** — `sun.java2d.opengl=false`, `sun.java2d.d3d=false`.
3. **Set locale** — `Locale.US` to ensure consistent number formatting.
4. **On EDT**:
   a. Redirect stdout/stderr to Log4j2.
   b. Install global exception handler.
   c. Initialize settings file.
   d. Configure FlatLaf look-and-feel.
   e. Create `PrimaryWindow`.
   f. Update state from settings.
   g. If `loadDataAtStart`: show window, then `FileLoading.loadGameData()` (triggers `IndexScannerTask`).
   h. Else: show window immediately.

### Quirk: Initialization Order Matters
The source comment explicitly states: *"These method calls are initialization block; the order of calls is important."* For example, settings must be initialized before configuring the LAF (which reads the theme preference), and the window must exist before loading game data (which may show progress dialogs parented to the window).
