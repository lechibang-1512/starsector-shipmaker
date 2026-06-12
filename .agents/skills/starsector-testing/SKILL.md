---
name: starsector-testing
description: Guidelines for JUnit, jqwik property-based testing, SpotBugs static analysis, and verification rules.
---

# Starsector Ship Editor — Testing, Verification & Validation

## Skill Directory Structure

This skill is organized as follows:
- **`SKILL.md`**: Main instructions (this file).
- **`resources/`**: Configurations and templates.
  - [spotbugs-exclude-template.xml](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-testing/resources/spotbugs-exclude-template.xml): Template for SpotBugs bug suppressions.
- **`examples/`**: Code references.
  - [SampleJqwikTest.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-testing/examples/SampleJqwikTest.java): Reference implementation of jqwik property-based tests.
- **`scripts/`**: Automation.
  - [run_checks.sh](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-testing/scripts/run_checks.sh): Helper script to build, test, and perform static analysis.

## 1. Test Frameworks

| Framework | Version | Scope | Purpose |
|---|---|---|---|
| **JUnit Jupiter** | 5.10.3 | `test` | Standard test runner and assertions |
| **jqwik** | 1.9.0 | `test` | Property-based testing with random input generation |

Both are executed via `maven-surefire-plugin` (3.2.5).

---

## 2. Existing Test Suite

The test suite is focused on **mathematical correctness** of utility functions that underpin coordinate transformations and data display. Two test files exist:

### `UtilityPropertiesTest`
[UtilityPropertiesTest.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/src/test/java/shipeditor/utility/UtilityPropertiesTest.java) — Property-based tests for core mathematical utilities.

| Property | What It Validates |
|---|---|
| `testParseIntegerOrDefault` | `Utility.parseIntegerOrDefault()` returns parsed int for valid strings, default for invalid/empty |
| `testParseDoubleOrDefault` | `Utility.parseDoubleOrDefault()` returns parsed double for valid strings, default for invalid/empty |
| `testClampAngleWithRounding` | Result is always in `[0, 360)` for any input in `[-1e9, 1e9]` |
| `testFlipAngle` | Flipping twice returns the normalized original (within 0.0001 delta) |
| `testRound` | `Utility.round(value, decimalPlaces)` produces a result for all finite values |
| `testTransformAngle` | `Utility.transformAngle()` output is always in `[-90, 270)` |

**Why jqwik here**: These functions are called thousands of times per frame for coordinate display and point placement. A single edge case (e.g., `NaN`, negative zero, very large values) would corrupt the viewport or produce nonsensical coordinates. Property-based testing with random inputs catches edge cases that handwritten examples miss.

### `CoordinatesFormatterPropertiesTest`
[CoordinatesFormatterPropertiesTest.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/src/test/java/shipeditor/utility/text/CoordinatesFormatterPropertiesTest.java) — Property-based tests for coordinate rounding.

| Property | What It Validates |
|---|---|
| `testRoundMaintainsValue` | `CoordinatesFormatter.round()` produces results within 0.005 of input for normal values, handles `NaN` and `Infinity` correctly, and doesn't overflow for very large values |
| `testRoundPointMaintainsValue` | `CoordinatesFormatter.roundPoint()` preserves `NaN` propagation for both X and Y |

**Quirk — Overflow Guard**: The test explicitly handles the case where `Math.abs(value) >= Long.MAX_VALUE / 1000.0`, because the rounding implementation internally converts to `long` for fixed-point math. For values exceeding this threshold, the test relaxes the precision requirement to 1% of the input value.

---

## 3. What Is NOT Tested (and Why)

### No GUI Tests
There are no automated Swing/AWT UI tests. The `PrimaryViewer`, `PaintOrderController`, and all painter classes require:
- A live OpenGL context (GPU required)
- A visible X11/Wayland window
- Swing EDT initialization

These are impractical in CI environments. Visual verification is done manually.

### No Jackson Parsing Tests
The `JsonProcessor.straightenMalformed()` pipeline is not unit-tested. This is a gap — the complex regex and character-by-character state machine are prime candidates for property-based testing with fuzzed Starsector JSON inputs.

### No Database Tests
`DatabaseManager` and `DatabaseQueryService` are not tested in isolation. The schema, PRAGMAs, and query correctness are validated implicitly by the full-application launch test.

---

## 4. Static Analysis: SpotBugs

**Plugin**: `spotbugs-maven-plugin` 4.8.3.1  
**Configuration**: `effort=Max`, `threshold=Low`, `xmlOutput=true`

This is the strictest possible SpotBugs configuration. It catches:
- Null pointer dereferences
- Unclosed resources (streams, connections)
- Concurrency issues (race conditions, unsynchronized access)
- Mutable object exposure

### Annotation-Guided Analysis
The `spotbugs-annotations` dependency (4.8.3) provides `@NonNull`, `@Nullable`, `@SuppressFBWarnings`, etc.

**Deliberate Suppressions** (Quirks):
- `@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})` is used extensively on `PrimaryViewer`, `PaintOrderController`, `UndoOverseer`, and `DrawUtilities`. These classes intentionally expose mutable internal state for performance. SpotBugs would flag every getter that returns a mutable list or map.

---

## 5. Build Pipeline as Validation Gate

The command `mvn clean package` acts as the primary validation gate. It executes:

1. **Compile** — Enforces Java 17 language level. Annotation processors run (Lombok, Log4j2).
2. **Test** — Surefire executes JUnit Jupiter and jqwik tests.
3. **SpotBugs** — Static analysis runs (if configured in a reporting or verify phase).
4. **Package** — JAR creation.
5. **Shade** — Uber-JAR generation with transformer verification.

### Shade Plugin Validation (Quirk)
If the `Log4j2PluginCacheFileTransformer` is missing or misconfigured, the shaded JAR will start but Log4j2 will silently fall back to `NullAppender`, producing zero log output. This is a **silent failure** — the application appears to work but cannot be debugged. The transformer merges `Log4j2Plugins.dat` cache files from all dependencies.

Similarly, if `ServicesResourceTransformer` is missing, JDBC driver auto-discovery fails and `DatabaseManager.getConnection()` throws `ClassNotFoundException` for `org.sqlite.JDBC` (which is why the explicit `Class.forName()` fallback exists).

---

## 6. Runtime Verification

### Manual Launch Test
The editor is launched via:
```bash
mvn exec:exec
```
or directly:
```bash
java -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication \
     -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 \
     -cp <classpath> shipeditor.Main
```

Verification checklist:
- [ ] Window opens without exceptions
- [ ] OpenGL context initializes (checkerboard background visible)
- [ ] Sprite loading and rendering works
- [ ] Coordinate display updates on mouse movement
- [ ] Undo/redo functions correctly
- [ ] Database indexing completes without errors
- [ ] Uber-JAR launches identically to classpath launch

### GPU-Specific Quirk
On Linux with multiple GPUs (e.g., integrated + discrete), the editor must use the correct GPU. The `DRI_PRIME=1` environment variable or explicit `/dev/dri/card0` mapping may be needed to target the discrete GPU for adequate OpenGL 3.3 support.

---

## 7. `.jqwik-database`

A `.jqwik-database` file exists in the project root. This is jqwik's internal database for tracking which random seeds have been tested and which have found failures. It enables **shrinking** (finding minimal failing inputs) and **replay** (re-running previously failing seeds). The file is currently 4 bytes, indicating minimal or no persisted failure history.

---

## 8. Compiler and IDE Warning Resolutions

When writing and refactoring Java code, adhere to these conventions to keep the build completely warning-free:

### Null pointer warnings on conditional check variables (false-positives)
Avoid saving state to a boolean variable before performing null checks. Static analyzers (like Eclipse/ECJ) fail to link the boolean state to the null state of the original object.
- **Bad**:
  ```java
  boolean present = selected != null && selected.getPainter() != null;
  if (!present) return;
  selected.getPainter().doSomething(); // Compiler warning: potential null pointer access
  ```
- **Good**:
  ```java
  if (selected == null || selected.getPainter() == null) return;
  selected.getPainter().doSomething(); // Safe
  ```

### Autoboxing/Unboxing type safety in generic Consumers
When passing method references to generic containers/spinners that expect wrapper types (e.g. `Consumer<Integer>` or `Consumer<Boolean>`), do not use method references targeting primitives (e.g. `void setFluxVents(int)`). This triggers "Null type safety" warnings under strict ECJ compilation. Use lambdas with explicit null-safe defaults instead.
- **Bad**:
  ```java
  ventsSpinner.enableSpinner(layer, variant.getFluxVents(), maxVents, variant::setFluxVents);
  ```
- **Good**:
  ```java
  ventsSpinner.enableSpinner(layer, variant.getFluxVents(), maxVents, val -> variant.setFluxVents(val != null ? val : 0));
  ```

### Redundant Suppressions
Avoid leaving `@SuppressWarnings("unused")` or similar compiler overrides on classes/methods unless there is a specific, active reason to silence a compiler error. Redundant suppressions cause ECJ warnings about unused annotations.
