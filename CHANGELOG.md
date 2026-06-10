# Changelog
## [0.0.1d] - 2026-06-10

### Features
- **First-Time Setup & Reset**: Added `FirstTimeSetupDialog` for game directory selection with `/run/media` auto-detection, and provided an option to clear app data via the Tools menu.
- **Weapon Configuration & Animation**: Added hidden mount support, introduced weapon fire, visual, and beam configuration panels. Added playback and stepping controls for weapon animations.
- **Visual Recoil**: Implemented weapon visual recoil animation and synchronized projectile offsets to weapon spec file during save.
- **IDE-style Upper Menu Redesign**: Consolidated settings and application menus into a new Preferences Dialog. 
- **Global Toolbar**: Introduced a unified main toolbar for quick access to Undo, Redo, Add/Remove Layers, Filters, and Game Data reloading.
- **Improved Hover Effects**: Reworked toolbar button styling with full hover support (highlight colors, hand cursor, robust FlatLaf integration).
- **OpenGL Rendering Migration**: Migrate the rendering system from Java2D/AWT to a custom high-performance OpenGL implementation using `SpriteRenderer`, `ShapeRenderer`, and `TextRenderer`.
- **Weapon and Projectile Specification Saving**: Implement saving functionality for weapon and projectile specification files with dedicated UI actions and coordinators.
- **Weapon and Projectile Data Panels**: Implement modular weapon and projectile data panels.
- **Menu and Filter Additions**: Add filter and data menus, implement ship filtering, and simplify panel layouts.
- **Ship Filter Enhancements**: Updated ship filtering logic to comprehensively search through skin names and file paths, improving asset discoverability.

### Bug Fixes
- **EventBus**: Implemented recursive depth support for EventBus dispatching to handle nested event calls safely.
- **Null Safety**: Improved null safety and optimized object cloning across multiple control models.
- **ShapeRenderer Mismatch**: Fix `ShapeRenderer` begin/end mismatch in `GuidesPainters`.
- **Instrument Panel Sync**: Improve instrument panel synchronization by adding null checks, active layer updates, and event bus lifecycle management.
- **Rendering Stabilization**: Improve rendering synchronization, stabilize slot scaling, and resolve UI pop-up issues via Java2D property adjustments.
- **UI Stability**: Improve UI stability by staggering load tasks, pre-initializing tooltips, and adding fallback logic to global error handling.

### Refactoring & Performance
- **Package Migration**: Massively migrated the project package namespace from `oth.shipeditor` to `shipeditor`.
- **Dependency Management**: Centralized dependency versions in `pom.xml`.
- **Layer & Utility Optimization**: Decoupled layer and module creation into a new `LayerFactory`, pre-allocated rendering vectors for performance, and improved utility bounds logic.
- **Undo Edit Consolidation**: Consolidated related undo edit classes into categorized managers and grouped files, and removed redundant package-info files.
- **Dialog & Repaint Optimization**: Centralized dialog management, optimized repaint scheduling, and cleaned up unused events and logic.
- **Weapon Animation Rework**: Moved weapon animation controls from the ship variant panel to the weapon visuals panel, supported multi-part weapon sprite animation frames, and later removed deprecated weapon animation support.
- **Cleanup**: Removed obsolete launcher scripts and purged emojis from CHANGELOG headers.
- **OpenGL Adoption**: Wide-spread migration of painter components to use GPU-accelerated drawing primitives.
- **EventBus Management**: Optimize EventBus subscription management.
- **UI Optimizations**: Implement deferred UI reloads for data tree panels using a queueing mechanism, standardize UI element scaling, update UI layouts with menu icons, and improve layer initialization logic.
- **Rendering Tweaks**: Implement manual zoom-based alpha scaling for painter text, reduce line widths and outline alpha. Optimize circle rendering and refine zoom speed and collision visualization. Introduce `FramebufferUtilities` for layer image printing. Improve CSV ID validation logic.
- **Database Initialization**: Replace external CLI database initialization with in-process indexing and add robust database validation checks.
- **Menu Architecture Overhaul**: Deleted numerous obsolete menu classes (`DataMenu`, `FilterMenu`, `LayersMenu`, `ApplicationMenu`, `SettingsMenu`) to massively streamline the top-level UI architecture and reduce codebase clutter.
- **Action Consolidation**: Centralized and relocated straggling utility actions (`JSON Corrector`, `Reset Transform`) into more cohesive namespaces (`ToolsMenu`, `WindowMenu`).
- **Context Menu Decoupling**: Updated `ViewerLayersPanel` to dynamically source right-click context menu options from `WindowMenu` instead of hardcoded legacy menu references, reducing inter-package dependencies.
- **Robust Toolbar UI Styling**: Implemented a reusable `styleToolbarButton` utility that correctly applies FlatLaf `toolBarButton` client properties, `HAND_CURSOR` overrides, and proper `setHideActionText(true)` encapsulation to prevent Swing layout disruption for action-backed buttons.
- **Camera Smoothness**: Reduced zoom interpolation speed in `LayerViewerControls` to provide significantly smoother camera transitions during canvas zoom operations.

### Chores, Docs & Build
- **OpenGL Architecture Guide**: Establish OpenGL rendering architecture, performance best practices, and robust state recovery patterns in documentation.


## [0.0.1c] - 2026-06-08

### Features
- **UI rework and Projectile Support** (v0.0.1c)
- **Data Loading & Validation**: Implement data loading progress, dirty state tracking, repository cache updates, and CSV validation.
- **Weapon UI**: Add weapon installation UI and context menu support; implement weapon offset and module UI updates; add pick weapon dialog.
- **Background File Indexing**: Implement SQLite-based background file indexing for Starsector assets with differential scanning and batch processing.
- **Mod Management**: Implement automatic purging of obsolete mods from database index and add validation for mod folder path resolution.
- **CSV Data Editing**: Implement synchronized CSV ID editing, automated re-indexing, and comprehensive CSV dataset save infrastructure for game data exports.
- **Hullmods & Modules**: Add suppressed hullmods management, module installation controls, and weapon offset editing instrument.
- **Skin Slot Overrides**: Implement skin slot overrides editor panel in `ShipInstrumentsPane`.
- **Tooltips**: Implement multi-line hover tooltips for editor points with custom detail formatting.
- **Startup**: Add splash screen for game data loading and implement library mod filtering to exclude non-data dependencies.
- **UI Enhancements**: Update UI tabs to use icons, adjust tab placement, and replace graphical icons with text labels.
- **Cross-Platform Scripts**: Add Windows launch script and improve cross-platform game folder detection.

### Bug Fixes
- **Parsing Robustness**: Improve robustness by handling and ignoring empty or contentless data files during parsing.
- **Null Checks**: Add defensive null checks to repository retrieval methods, file loading, and data processing components to prevent runtime exceptions.
- **UI Container**: Fix skin data panel UI container update after layout changes.

### Refactoring & Performance
- **EventBus Optimization**: Optimize EventBus dispatching with thread-local buffers and migrate EventBus subscriptions to include owner objects for improved lifecycle management.
- **Data Loading Perf**: Optimize data loading with parallel streams and implement mod folder caching; refactor JSON parsing to a linear scan to prevent regex engine stalls.
- **Architecture**: Reorganize project package structure, clean up event-driven architecture modules, and implement global `PrimaryWindow` instance.
- **Database Init**: Migrate database initialization to CLI-driven process and remove splash screen dependency.
- **UI Layout**: Remove `QuickButtonsPanel`, perform minor UI layout cleanup, standardize tooltips, and implement sprite-bounded cursor detection for point interaction and selection logic.
- **General Cleanups**: Remove unused imports, extract CSV validation logic, and clean up technical debt.

### Chores, Docs & Build
- **Java 21 Support**: Update Java 21 installation, setup, and build instructions in `README.md` (including Microsoft Build of OpenJDK).
- **Maven Configuration**: Update maven compiler configuration, add surefire plugin, and configure Maven Shade plugin for fat JAR distribution.
- **Dependencies**: Upgrade `log4j` and `jackson` dependencies to latest versions.
- **Cleanup**: Remove old comments, grievances, and design notes; update build file to exclude meta-inf manifest, licence, notice, and dependencies.
- **Warnings**: Suppress unstable module warnings in `module-info`.

## [0.0.1b]

### New Features & Editor Enhancements:
- **Synchronized CSV ID Editing and Automated Re-indexing**: Implemented automated synchronized CSV ID modifications for all five core entity types (ships, weapons, hullmods, fighter wings, and ship systems). When a user changes an asset's ID—either via the text field in the Hull Data control panel or by editing the "id" column directly in any CSV right-panel spreadsheet table—the application catches the change, updates the model, re-indexes the repository maps in `GameDataRepository` under the new ID, and modifies the raw cached CSV data row.
- **Save Prompts for CSV Updates**: Integrated a modal prompt (`JOptionPane.YES_NO_OPTION`) when a CSV ID is updated. The user is asked whether they want to save the modified CSV file to disk immediately, matching document-editor state behaviors where files are preserved until explicit user confirmation.
- **Detailed Multi-Line Tooltips**: Added rich, multi-line tooltips for editor canvas points. These tooltips format detail parameters, presenting precise coordinates and custom metadata on hover over active interactive points.
- **UI Tab Placements & Adjustments**: Restructured and optimized the main editor workspace tabs by reorganizing the tab components, adjusting layouts, and cleansing redundant UI build artifacts.

### Performance & Optimizations:
- **Workspace Layout Decluttering**: Removed the legacy `QuickButtonsPanel` from the UI layout, maximizing the screen real estate available for the main canvas, and optimized list handling during repository entry updates.

### Architecture, Bug Fixes & Refactoring:
- **Ikonli Icon Framework Purge**: Completely removed the heavy external Ikonli graphical icon dependency from the codebase, refactoring the Swing components, buttons, and tab headers to use lightweight, high-visibility native text-based labels. This speeds up compilation times, reduces final binary size, and removes UI rendering overhead.

### Dependencies:
- **Upgraded Log4j & Jackson Frameworks**: Upgraded the project's logging engine (`log4j`) and JSON/CSV processing libraries (`jackson`) to their latest stable releases, ensuring up-to-date security, enhanced serialization performance, and better resilience during robust data parsing.

## [0.0.1a]

### New Features & Editor Enhancements:
- **Weapon Offset Point Editing ("Offsets" Tab)**: Restored the placeholder offsets tab under `WeaponInstrumentsPane` with a new fully-featured `WeaponOffsetsPanel`. It features an interactive coordinates and angles spreadsheet table with direct addition and deletion of offset points, linked with the custom `WeaponOffsetPainter`.
- **Built-in Modules Installation UI**: Extended `ModuleControlPanel` to feature an intuitive dropdown/variant picker utilizing the available variants in `GameDataRepository`. Allows easy "Install" and "Clear" operations on module slots, fully hooked up to the Undo/Redo system (`EditDispatch`).
- **Suppressed Hullmods Management**: Added a new list editor panel (`SuppressedModsPanel`) inside the `VariantHullmodsPanel` for managing suppressed hullmod string IDs on active ship variants. Supports complete round-trip JSON serialization and deserialization via `SaveVariantAction` and `VariantFileSerializer`.
- **Skin Slot Overrides**: Designed and implemented the dedicated `SkinSlotOverridesPanel` within `ShipInstrumentsPane` to provide seamless editing of skin-specific weapon slot attribute overrides.
- **Project Authorship & Metadata Handover**: Updated authorship metadata across the codebase (including `Readme.txt` and `module-info.java`) to transition the primary maintainer status from `Ontheheavens` to `thevolkflower`. The application's `ApplicationMenu` Swing UI was also updated to accurately reflect the authors (`thevolkflower` & `Xenoargh`).
- **Development Milestone Completion**: Formally closed out the remaining development pipeline checklist items in `checklist.md`. All Tier 3 milestones (interactive Weapon Offsets point editing, Variant Wings UI, and Built-in Module slot handling) and Tier 4 UI cleanups are officially marked as completed and stable.
- **Swing Tooltip and Layout Standardizations**: Standardized blank or empty tooltips across panels to `null` to align with Swing guidelines and resolve rendering glitches, with minor UI cleanups in `AbstractSlotValuesPanel`, `VariantWingsPanel`, and cell renderers.

### Performance & Optimizations:
- **UI List Rebuild Prevention**: Instrument panels (Engines, Weapon Slots, Variant Modules) now manually compare their existing `ListModel` content against incoming data arrays before updating. If the data hasn't changed, the panels merely repaint instead of wastefully discarding and rebuilding the UI components. This dramatically reduces EDT overhead and prevents selection loss during frequent state updates.
- **Targeted Repaints via EventBus**: Replaced indiscriminate layer reselection during undo/redo actions (like sorting weapon groups or launch ports) with specific queued repaints. This eliminates broad, performance-heavy UI invalidation.
- **Repaint Timer Adjusted**: Lowered the global event scheduler's repaint frequency from ~125Hz (8ms) to ~60Hz (16ms) to save CPU cycles without sacrificing visual smoothness.

### Architecture, Bug Fixes & Refactoring:
- **UI Data Refresh Rendering Fixes**: Addressed a critical Swing rendering issue where dynamic panels would fail to visually update after state or layout changes. Added appropriate `.revalidate()` and `.repaint()` calls during data refreshes in multiple critical containers, including `EngineStylesPanel`, `HullStylesPanel`, `EngineDataPanel`, `ShipLayerInfoPanel`, `SkinDataPanel`, `VariantMainPanel`, and `VariantWeaponsPanel`.
- **EventBus Memory Leak Fix**: Migrated the core `EventBus` to a `WeakHashMap`-backed lifecycle architecture. Over 80 lambda expressions were refactored to bind strictly to component lifecycles, completely eliminating the historically documented listener pile-up.
- **Viewer Input Decoupling**: Dismantled the opinionated 1-to-1 event routing inside `LayerViewerControls`. It now dispatches generic raw mouse events (`ViewerRawMouseDragged`, etc.), allowing individual rendering painters to evaluate `ControlPredicates` internally and significantly improving the codebase's scalability for new UI interactions.
- **Unstable Module Warning Suppressions**: Added `@SuppressWarnings("module")` in `module-info.java` to suppress Java compiler warnings regarding unstable/filename-based automodules (such as `viewer-core`, `geom`, and `filters`), ensuring a clean Maven build process.
- **Technical Debt & Warning Sweep**: Conducted a thorough codebase sweep to eliminate redundant imports, remove obsolete `@SuppressWarnings("unused")` annotations, and resolve compiler/linter warnings across painters, models, and Swing panels.
- **Thread-safe Image Caching**: Upgraded the `ImageCache` backing map to a `ConcurrentHashMap`, preventing `ConcurrentModificationException` during multi-threaded file walking and sprite loading.
- **Robust CSV & Locale Handling**: Enforced a global US locale at startup to prevent comma/period decimal separator mismatches during parsing. Added utility methods for safe integer/double string parsing and improved CSV charset decoding robustness.
- **Instance-based Refactors**: Transitioned `WeaponFilterPanel` to an instance-based implementation, avoiding problematic static state.
- **Maven Shade Packaging & Executable Build**: Configured the `maven-shade-plugin` in `pom.xml` with `Log4j2PluginCacheFileTransformer` and services resources packaging. STANDALONE fat jar deployment is fully streamlined.
