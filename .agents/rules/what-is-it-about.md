---
trigger: always_on
---

Starsector Ship Editor - Rules & Skills
Technology Stack
Language: Java 19+ (Source target 17, but requires newer features up to 21 based on README.md).
GUI Framework: Swing with FlatLaf (for modern look, custom components & dark mode) and JavaGL (for Viewer/AffineTransform capabilities).
Icons: Ikonli (FontAwesome5, FluentUI, Boxicons).
Build Tool: Maven.
Data Binding: Jackson (for both JSON and CSV files).
Boilerplate Reduction: Lombok.
Logging: Log4j2.
Architecture & Coding Conventions
Event Bus (oth.shipeditor.communication)
The application relies heavily on an Event Bus system for loose coupling between components.
Do not pass hard references between distinct UI components or controllers where possible. Instead, fire events and have listeners subscribe to them.
Always implement proper listener cleanup to avoid memory leaks on layer removal or object destruction.
Layer System (oth.shipeditor.components)
Support for simultaneous viewing and editing of multiple ship entities using Layers.
UI features interacting with the data must account for the currently active layer.
Ensure custom graphics operations utilize AffineTransform correctly to preserve interactions during layer rotation, zooming, or scaling.
Data Model & Parsing (oth.shipeditor.representation & parsing)
All JSON/CSV data wrappers are encapsulated in representations.
Handle unconventional JSON structures specific to Starsector gracefully.
Do not hardcode absolute paths; use the Data walker which supports symlinks and game packages.
CSV Editing & Serialization: When modifying Starsector CSV files via Jackson, never overwrite files using standard serializations as this can corrupt Starsector's formatting (like unnecessarily quoting comments). Always cache the raw parsed data maps alongside the original `CsvSchema` in `GameDataRepository` during loading. Upon saving, apply a custom Jackson module with `JsonSerializer<Map<?, ?>>` that bypasses default Jackson behaviors to reconstruct the exact raw structure, rebuilding the schema with `setUseHeader(true)`.
Undo/Redo (oth.shipeditor.undo)
User actions that modify the project state must be encapsulated as edits to plug into the global Undo/Redo system.
Threading & UI
Since the app is built on Swing, all UI modifications must occur on the Event Dispatch Thread (EDT). Use SwingUtilities.invokeLater() when needed.
Graphic repaints are heavily optimized (using a timed repaint technique). Do not indiscriminately call .repaint() on large panels unnecessarily to prevent performance drops.
Common Operations
Adding new Data Types: Update Jackson parsing models in representation, add corresponding Event types, and update the UI trees/tables.
Graphic Editing: For anything drawn on the map, use the custom point painting and layer hierarchy tools, respecting PaintOrderController.W
Code Cleanliness & Refactoring
Always remove unused imports, especially when refactoring complex Swing components, layout managers, or EventBus listeners.
When removing redundant UI elements or panels, ensure that all helper types, enums (e.g., LeftsideTabType), and obsolete listeners are completely purged and deleted from the codebase. Do not leave dead code blocks.