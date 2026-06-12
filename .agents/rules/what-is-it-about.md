---
trigger: always_on
---

```yaml
project: "Starsector Ship Editor"
description: "Rules & Skills"

technology_stack:
  language: "Java 19+ (Source target 17, but requires newer features up to 21 based on README.md)."
  gui_framework: "Swing with FlatLaf (for modern look, custom components & dark mode) and JavaGL (for Viewer/AffineTransform capabilities)."
  icons: "Ikonli (FontAwesome5, FluentUI, Boxicons)."
  build_tool: "Maven."
  data_binding: "Jackson (for both JSON and CSV files)."
  boilerplate_reduction: "Lombok."
  logging: "Log4j2."

architecture_and_coding_conventions:
  event_bus:
    package: "shipeditor.communication"
    rules:
      - "The application relies heavily on an Event Bus system for loose coupling between components."
      - "Do not pass hard references between distinct UI components or controllers where possible. Instead, fire events and have listeners subscribe to them."
      - "Always implement proper listener cleanup to avoid memory leaks on layer removal or object destruction."
  layer_system:
    package: "shipeditor.components"
    rules:
      - "Support for simultaneous viewing and editing of multiple ship entities using Layers."
      - "UI features interacting with the data must account for the currently active layer."
      - "Ensure custom graphics operations utilize AffineTransform correctly to preserve interactions during layer rotation, zooming, or scaling."
  data_model_and_parsing:
    package: "shipeditor.representation & parsing"
    rules:
      - "All JSON/CSV data wrappers are encapsulated in representations."
      - "Handle unconventional JSON structures specific to Starsector gracefully."
      - "Do not hardcode absolute paths; use the Data walker which supports symlinks and game packages."
      - "CSV Editing & Serialization: When modifying Starsector CSV files via Jackson, never overwrite files using standard serializations as this can corrupt Starsector's formatting (like unnecessarily quoting comments). Always cache the raw parsed data maps alongside the original `CsvSchema` in `GameDataRepository` during loading. Upon saving, apply a custom Jackson module with `JsonSerializer<Map<?, ?>>` that bypasses default Jackson behaviors to reconstruct the exact raw structure, rebuilding the schema with `setUseHeader(true)`."
  undo_redo:
    package: "shipeditor.undo"
    rules:
      - "User actions that modify the project state must be encapsulated as edits to plug into the global Undo/Redo system."
  threading_and_ui:
    rules:
      - "Since the app is built on Swing, all UI modifications must occur on the Event Dispatch Thread (EDT). Use SwingUtilities.invokeLater() when needed."
      - "Graphic repaints are heavily optimized (using a timed repaint technique). Do not indiscriminately call .repaint() on large panels unnecessarily to prevent performance drops."

common_operations:
  adding_new_data_types:
    - "Update Jackson parsing models in representation, add corresponding Event types, and update the UI trees/tables."
  graphic_editing:
    - "For anything drawn on the map, use the custom point painting and layer hierarchy tools, respecting PaintOrderController."

code_cleanliness_and_refactoring:
  rules:
    - "Always remove unused imports, especially when refactoring complex Swing components, layout managers, or EventBus listeners."
    - "When removing redundant UI elements or panels, ensure that all helper types, enums (e.g., LeftsideTabType), and obsolete listeners are completely purged and deleted from the codebase. Do not leave dead code blocks."
  lombok_and_inheritance:
    - "When extending classes that use Lombok `@Getter` annotations (e.g., `LayerPainter`), be careful of field shadowing. Always use polymorphic getter methods (like `getSprite()`) in the base class rather than direct field access (`this.sprite`) to ensure subclasses that override the getter function correctly instead of encountering `NullPointerException`s on uninitialized base class fields."

maven_and_environment_quirks:
  rules:
    - "This project requires JDK 17-21 to compile successfully. The configured Lombok version (1.18.36) crashes on JDK 25 with a `TypeTag :: UNKNOWN` ExceptionInInitializerError."
    - "If compiling on a Fedora-based OS where the system default is Java 25, be aware that Fedora's `mvn` wrapper script completely ignores `update-alternatives` and uses `/etc/java/maven.conf` if `JAVA_HOME` is not explicitly set in the environment. To ensure a successful build without modifying the global system, always advise setting `JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk` in a `~/.mavenrc` file so Maven correctly invokes Java 21 for the compiler and Lombok annotation processor."
```