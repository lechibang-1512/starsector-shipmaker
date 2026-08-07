# Starsector Ship Editor - Indexing & Data Loading Pipeline

## Overview

The application requires scanning thousands of JSON and CSV files across the Starsector core directory and any active mod directories. The loading pipeline is split into a background SQLite indexer and in-memory caches, but several bottlenecks cause UI freezing and slow startup times.

## Architecture Flow

1. **`FileLoading.loadGameData()`** (Entry Point)
   - Orchestrates the data loading sequence.
   - Triggers `IndexScannerTask.scanAndIndexAll()` synchronously/asynchronously.
   - Clears existing caches and sets `*DataLoaded` flags upon completion.
   - Fires `LoadingTaskCompleted` events.

2. **`IndexScannerTask.scanAndIndexAll()`** (SQLite DB Indexer)
   - Walks all active mod folders looking for `.ship`, `.wpn`, `.csv` etc.
   - Uses Jackson streaming JSON parser to extract entity IDs and sprites (`extractEntityMetadata`).
   - Upserts file paths, modification timestamps, and metadata into a SQLite database.

3. **`CoreIndexManager.loadCoreData()`** (In-Memory Core Indexer)
   - `starsector-core` is too large/static for SQLite upserts on every launch.
   - Instead, it is loaded purely into a `ConcurrentHashMap` via a full directory walk and JSON parse on startup.

4. **`DatabaseQueryService`** (Query Layer)
   - UI elements call methods like `getFilesByType()` which merges `CoreIndexManager` results with `SQLite` query results for the active mods.

5. **`GameDataRepository`** (CSV & Runtime Data Cache)
   - Provides methods like `getHullmodEntriesByPackage()`, `getShipEntriesByPackage()`.
   - Lazy-loads CSV files via `loadCsvEntriesByPackage()`, which triggers another directory walk to find `hull_mods.csv`, `ship_data.csv`, etc.

6. **UI Layer (`GameDataPanel` & `DataTreePanel`)**
   - The left-side panel contains a `JTabbedPane` with tabs for Hulls, Weapons, Hullmods, etc.
   - When a tab becomes visible for the first time, a `HierarchyListener` triggers `getLoadDataAction()`.
   - For CSV tabs, this invokes `GameDataRepository` to parse CSVs on the Event Dispatch Thread (EDT).

## Identified Bottlenecks

1. **Sequential I/O in Indexing**
   - `IndexScannerTask` and `CoreIndexManager` both iterate files sequentially and open a Jackson parser for every `.ship`, `.wpn`, `.variant`, etc.
2. **EDT Blocking on Tab Switch**
   - `DataTreePanel`'s `HierarchyListener` triggers CSV loading synchronously. Since CSV parsing reads from disk, switching to the "Hullmods" or "Shipsystems" tab for the first time completely freezes the UI.
3. **Repeated Directory Walks**
   - `GameDataRepository.loadCsvEntriesByPackage()` walks all mod directories looking for a specific CSV file. Since it is called lazily per CSV type (hulls, weapons, hullmods, systems, wings), it repeats the filesystem walk 5+ times.
4. **Database Query Overhead**
   - `ShipFilterPanel` triggers a tree reload on every keystroke in the search box (debounced to 300ms). This calls `DatabaseQueryService.getFilesByType()`, which executes a fresh SQL query with a dynamically constructed `IN (...)` clause every time.

## Recommended Fixes

- **Parallel Processing:** Use `parallelStream()` or an `ExecutorService` when parsing JSON files in `CoreIndexManager` and `IndexScannerTask`.
- **Background Loading:** Change `DataTreePanel` load actions to use `CompletableFuture` or `SwingWorker`, showing a "Loading..." placeholder in the tree until the data is ready.
- **Batched CSV Discovery:** Scan mod directories once for all known CSV files rather than re-walking the filesystem for each type.
- **Cache Query Results:** Cache `getFilesByType()` results in memory. The database is read-only after the initial scan, so hitting SQLite repeatedly for the same list of ships is unnecessary.
