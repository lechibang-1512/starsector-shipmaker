# Known Issues — Data Loading & Variant Initialization

This document catalogues race conditions, false-positive guards, and popup spam
issues discovered during the data-loading and variant-initialization pipeline.

---

## 1. Loading flags only set by UI panels (race condition)

**Affected files:**
- `LoadHullmodDataAction.java`
- `LoadWingDataAction.java`
- `LoadShipSystemDataAction.java`

**Symptoms:**
- `"Weapon or hullmod data is not loaded to the editor, aborting variant initialization."`
- Variant dropdown refuses to load any variant.
- Built-in hullmods/wings silently skipped during hull initialization.

**Root cause:**
The boolean flags `hullmodDataLoaded`, `wingDataLoaded`, and `shipsystemDataLoaded`
in `GameDataRepository` were **only** set to `true` inside the `setLoadedStatus()`
method of their respective UI tree panels (`HullmodsTreePanel`, `WingsTreePanel`,
`ShipSystemsTreePanel`). These panels call `setLoadedStatus()` from `reload()`,
which is gated by `queueReload()`:

```java
public void queueReload() {
    if (this.isShowing()) {   // <-- panel must be visible!
        this.reload();
    } else {
        this.reloadQueued = true;
    }
}
```

If the user was on a different tab (e.g. Ships) when data finished loading, the
panel was not showing, so `reload()` never ran, and the flag stayed `false`.

Downstream consumers like `ShipPainter.installVariant()` and `ShipHull`
constructor checked these flags and either aborted or skipped initialization.

**Fix:**
Set the loaded flags directly in the background `DataLoadingAction` subclasses,
immediately after populating the data maps — before publishing the EventBus event.
This guarantees the flags are `true` regardless of which UI tab is active.

The UI panels (`WeaponsTreePanel`, `HullmodsTreePanel`, etc.) still redundantly
set the same flags in `setLoadedStatus()`, which is harmless.

---

## 2. Variant initialization hard-abort on missing data flags

**Affected file:** `ShipPainter.java`

**Symptom:**
`"Weapon or hullmod data is not loaded to the editor, aborting variant initialization."`
popup when selecting any variant from the dropdown.

**Root cause:**
`ShipPainter.installVariant()` contained a guard that checked
`isWeaponsDataLoaded()` and `isHullmodDataLoaded()`. Due to Issue #1, these flags
were often `false` even though the actual data maps were fully populated. The
guard completely prevented variant initialization and showed a blocking popup.

**Fix:**
Removed the guard entirely. The variant initializer (`ShipVariant.initialize()`)
already handles missing individual entries gracefully by logging errors and
skipping them.

---

## 3. Popup spam during variant initialization for missing entries

**Affected file:** `ShipVariant.java`

**Symptoms:**
- Multiple blocking `JOptionPane.showMessageDialog` popups when opening a variant
  that references weapons, hullmods, or wings not present in the loaded data.
- Each missing entry produced a separate popup the user had to dismiss manually.

**Root cause:**
The `initialize()` method and `constructModsList()` helper used
`JOptionPane.showMessageDialog()` for every individual missing weapon, hullmod,
and wing entry. For variants with many references to modded content, this could
produce dozens of blocking popups.

**Fix:**
Replaced all three `JOptionPane.showMessageDialog()` calls with `log.error()`
calls. Missing entries are now logged to the console and silently skipped in the
UI, allowing the variant to load with whatever data is available.

---

## 4. Wings tab gated on ship data loading

**Affected file:** `WingsTreePanel.java`

**Symptom:**
Wings tab appears empty / fails to populate its tree if the user navigates to it
before ship data finishes loading.

**Root cause:**
`WingsTreePanel.populateEntries()` contained an early-return guard:

```java
if (!data.isShipDataLoaded()) return;
```

Wing CSV data is completely independent of ship data. This check was a
false-positive dependency that prevented the Wings tab from rendering if ships
hadn't finished loading yet — which is common since ships are a much larger
dataset and load later in the pipeline.

**Fix:**
Removed the `isShipDataLoaded()` guard. Wings now populate immediately when their
own data finishes parsing.

---

## 5. HeadlessException in LoadWingDataAction

**Affected file:** `LoadWingDataAction.java`

**Symptom:**
`HeadlessException` crash when running in CLI/headless mode (e.g. for automated
testing).

**Root cause:**
The wing data loader called `JOptionPane.showMessageDialog()` to report a
false-positive error (see Issue #4), which crashes in headless environments.

**Fix:**
Removed the GUI-dependent error dialog along with the false-positive guard.

---

## 6. NullPointerException in Themes during headless initialization

**Affected file:** `Themes.java`

**Symptom:**
`NullPointerException` when `HullSize` enum constants initialize `FontIcon`
objects in headless mode.

**Root cause:**
`Themes.getIconColor()` and related methods call `UIManager.getColor()`, which
returns `null` when no Look-and-Feel is installed (headless mode). The null
propagated into `FontIcon` constructors causing the crash.

**Fix:**
Added null-safe fallbacks (`Color.GRAY`, `Color.DARK_GRAY`) for all theme color
accessor methods so they return sensible defaults in headless mode.

---

## Summary of changes

| File | Change |
|---|---|
| `LoadHullmodDataAction.java` | Set `hullmodDataLoaded = true` in loader |
| `LoadWingDataAction.java` | Set `wingDataLoaded = true` in loader; removed false-positive guard |
| `LoadShipSystemDataAction.java` | Set `shipsystemDataLoaded = true` in loader |
| `ShipPainter.java` | Removed hard-abort guard in `installVariant()` |
| `ShipVariant.java` | Replaced 3× `JOptionPane` popup spam with `log.error()` |
| `WingsTreePanel.java` | Removed false `isShipDataLoaded()` dependency |
| `Themes.java` | Added null-safe color fallbacks for headless mode |
| `CliLoadingTest.java` | New CLI test harness for verifying data pipeline |

---

# Part 2: Concurrency & Thread-Safety Race Conditions

The following concurrency and thread-safety race conditions were identified during a threading audit and have been resolved:

## 7. `allShipEntries` HashMap corruption

**Affected files:**
- `GameDataRepository.java`
- `LoadShipDataAction.java`

**Severity:** CRITICAL (can cause data corruption / crashes / infinite loops)

**Symptoms:**
- Intermittent UI rendering glitches, missing ship entries, or infinite loops/crashes with `ConcurrentModificationException` during data reloads.

**Root cause:**
- `allShipEntries` was a plain `HashMap` updated directly from background threads in `LoadShipDataAction.collectShips()` and `walkHullFolder()` while the EDT read from it concurrently (e.g. via `retrieveShipCSVEntryByID`).

**Fix:**
- Made `allShipEntries` `volatile` in `GameDataRepository`.
- Refactored `LoadShipDataAction.collectShips()` to build the ship entry map locally inside a thread-safe `ConcurrentHashMap` on the background thread.
- Assigned the fully populated local map to `GameDataRepository` atomically inside the EDT-executed Runnable.

---

## 8. `allWeaponEntries` HashMap corruption

**Affected files:**
- `GameDataRepository.java`
- `LoadWeaponsDataAction.java`

**Severity:** CRITICAL

**Symptoms:**
- Similar to Issue #7, but affecting weapon lookups (e.g., `getWeaponByID()`).

**Root cause:**
- `allWeaponEntries` was a plain `HashMap` populated directly on background threads inside `LoadWeaponsDataAction.collectWeapons()`.

**Fix:**
- Made `allWeaponEntries` `volatile` in `GameDataRepository`.
- Refactored `LoadWeaponsDataAction.collectWeapons()` to build `allWeapons` in a local `ConcurrentHashMap` on the background thread and set it atomically on the EDT.

---

## 9. Non-atomic CSV Schema and Data Caching

**Affected files:**
- `GameDataRepository.java`
- `FileLoading.java`

**Severity:** HIGH

**Symptoms:**
- Potential inconsistencies or cached schema/data mismatch when GC cleared soft references, causing game assets to deserialize incorrectly or corrupt Starsector's formatting on saves.

**Root cause:**
- `FileLoading.readCSVWithCharset()` populated the schema cache (`putCsvSchemaForPath`) and the raw data cache (`putRawCSVDataForPath`) in two separate, non-atomic map entries.

**Fix:**
- Introduced a composite class `CachedCSVData` that groups both the raw CSV data list and the parser schema object.
- Replaced the two independent maps in `GameDataRepository` with a single map `csvCacheByPath` storing `SoftReference<CachedCSVData>`.
- Added `putCachedCSVData` to cache both atomically in a single operation.

---

## 10. `EventBus` Subscriber Map Concurrency Issue

**Affected file:** `EventBus.java`

**Severity:** HIGH

**Symptoms:**
- Unpredictable event dispatch failures, memory leaks, or map corruption.

**Root cause:**
- `lifecycleSubscribers` was initialized as a synchronized map: `Collections.synchronizedMap(new WeakHashMap<>())`.
- However, `subscribe` used `computeIfAbsent()` which is a compound map operation, and `unsubscribeByParent` called `remove()` without synchronizing on the map wrapper.

**Fix:**
- Removed the synchronized map wrapper and used a plain `WeakHashMap`.
- Protected all accesses and operations on `lifecycleSubscribers` (including `subscribe`, `unsubscribe`, `unsubscribeByParent`, and `publish`) with explicit `synchronized (bus.lifecycleSubscribers)` blocks to guarantee thread-safe compound actions.

---

## 11. Thread Visibility of Reassigned State

**Affected files:**
- `GameDataRepository.java`
- `FileLoading.java`

**Severity:** HIGH

**Symptoms:**
- Thread visibility issues where background updates to flags or maps were not immediately observed by the EDT.

**Root cause:**
- Global state holders like `FileLoading.loadingInProgress`, `GameDataRepository.allVariants`, `GameDataRepository.allProjectiles`, and the style maps were mutated by background threads/runnables without being marked `volatile`.

**Fix:**
- Marked `loadingInProgress`, `allVariants`, `allProjectiles`, `allHullStyles`, and `allEngineStyles` as `volatile` to guarantee immediate visibility across all threads.

---

## Summary of All Threading and Loading Changes

| File | Change |
|---|---|
| `LoadHullmodDataAction.java` | Set `hullmodDataLoaded = true` in loader |
| `LoadWingDataAction.java` | Set `wingDataLoaded = true` in loader; removed false-positive guard |
| `LoadShipSystemDataAction.java` | Set `shipsystemDataLoaded = true` in loader |
| `ShipPainter.java` | Removed hard-abort guard in `installVariant()` |
| `ShipVariant.java` | Replaced 3× `JOptionPane` popup spam with `log.error()` |
| `WingsTreePanel.java` | Removed false `isShipDataLoaded()` dependency |
| `Themes.java` | Added null-safe color fallbacks for headless mode |
| `CliLoadingTest.java` | New CLI test harness for verifying data pipeline |
| `GameDataRepository.java` | Changed map fields to `volatile`, switched to `ConcurrentHashMap`, grouped CSV cache into `CachedCSVData`, added atomic setters |
| `FileLoading.java` | Marked `loadingInProgress` `volatile`, grouped CSV data and schema cache writes under `putCachedCSVData` |
| `EventBus.java` | Synchronized `lifecycleSubscribers` accesses explicitly to prevent corruption on compound operations |
| `LoadShipDataAction.java` | Refactored `collectShips` and `walkHullFolder` to build ship maps locally and set on EDT |
| `LoadWeaponsDataAction.java` | Refactored `collectWeapons` to build weapons map locally and set on EDT |
