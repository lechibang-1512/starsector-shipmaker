# Changelog

## [0.0.1c] - 2026-06-08

### ✨ Features
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

### 🐛 Bug Fixes
- **Parsing Robustness**: Improve robustness by handling and ignoring empty or contentless data files during parsing.
- **Null Checks**: Add defensive null checks to repository retrieval methods, file loading, and data processing components to prevent runtime exceptions.
- **UI Container**: Fix skin data panel UI container update after layout changes.

### ⚡ Refactoring & Performance
- **EventBus Optimization**: Optimize EventBus dispatching with thread-local buffers and migrate EventBus subscriptions to include owner objects for improved lifecycle management.
- **Data Loading Perf**: Optimize data loading with parallel streams and implement mod folder caching; refactor JSON parsing to a linear scan to prevent regex engine stalls.
- **Architecture**: Reorganize project package structure, clean up event-driven architecture modules, and implement global `PrimaryWindow` instance.
- **Database Init**: Migrate database initialization to CLI-driven process and remove splash screen dependency.
- **UI Layout**: Remove `QuickButtonsPanel`, perform minor UI layout cleanup, standardize tooltips, and implement sprite-bounded cursor detection for point interaction and selection logic.
- **General Cleanups**: Remove unused imports, extract CSV validation logic, and clean up technical debt.

### 🔧 Chores, Docs & Build
- **Java 21 Support**: Update Java 21 installation, setup, and build instructions in `README.md` (including Microsoft Build of OpenJDK).
- **Maven Configuration**: Update maven compiler configuration, add surefire plugin, and configure Maven Shade plugin for fat JAR distribution.
- **Dependencies**: Upgrade `log4j` and `jackson` dependencies to latest versions.
- **Cleanup**: Remove old comments, grievances, and design notes; update build file to exclude meta-inf manifest, licence, notice, and dependencies.
- **Warnings**: Suppress unstable module warnings in `module-info`.
