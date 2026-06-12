---
name: starsector-data-parsing
description: Guidelines for Jackson configuration, JSON pre-processing, CSV serialization rules, and entity ID extraction in Starsector Ship Editor.
---

# Starsector Ship Editor — Data Parsing & Structure

## Skill Directory Structure

This skill is organized as follows:
- **`SKILL.md`**: Main instructions (this file).
- **`resources/`**: Configurations and schemas.
  - [JacksonConfig.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-data-parsing/resources/JacksonConfig.java): Reference showing Jackson configuration flags for parsing Starsector files.
- **`examples/`**: Code references.
  - [CustomCsvSerializer.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-data-parsing/examples/CustomCsvSerializer.java): Reference implementation of the custom map serializer avoiding quotes on `#` comment columns.
- **`scripts/`**: Tooling.
  - [verify_json.py](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-data-parsing/scripts/verify_json.py): Dry-run verification script for processing formats.

## 1. The Core Problem

Starsector's data files are **not valid JSON or CSV** by any standard parser's definition. They contain:
- `#` comments (not `//` or `/* */`)
- Semicolons as separators instead of commas
- Unquoted string keys and values
- Trailing decimal points (`1.` instead of `1.0`)
- Java-style numeric type suffixes (`100f`, `2.5d`)
- Trailing commas in arrays and objects
- Leading `+` signs on numbers
- Leading zeros on numbers (`007`)
- Unescaped control characters in strings

The parsing subsystem exists to survive all of these.

---

## 2. JSON Pre-Processing: `JsonProcessor`

[JsonProcessor.java](file:///run/media/lechibang/cb09d199-3769-4ec8-9af5-954929515428/projects/starsector-shipmaker/src/main/java/shipeditor/parsing/JsonProcessor.java) is the first line of defense. Before Jackson ever sees the JSON, the raw text goes through `straightenMalformed()`:

### Phase 1: `correctCommentsUnquotedValuesAndSeparators()` — Single-Pass O(N) Scanner
This is the most complex and performance-critical method. It performs three transformations in a single linear sweep:

1. **`#` Comment Stripping**: When `#` is encountered outside quotes, all characters until the next newline are consumed silently.
2. **Semicolon → Comma**: `;` outside quotes becomes `,`.
3. **Unquoted Identifier Quoting**: Any bare word (`[a-zA-Z_][a-zA-Z0-9_]*`) that is not `true`, `false`, or `null`, and is not already adjacent to a quote, is wrapped in double quotes.

**Quirk — Catastrophic Backtracking Avoidance**: An earlier implementation used regex for this, which caused the regex engine to stall on certain mod files with deeply nested structures. The current implementation is a hand-written character-by-character state machine with explicit `inQuotes` and `escape` tracking.

**Quirk — Dot-Preceded Words**: The scanner explicitly checks `input.charAt(i - 1) != '.'` before quoting an identifier. This prevents `style.MIDLINE` from becoming `style."MIDLINE"` — Starsector uses dot-notation for style references.

### Phase 2: `correctNumberLetterSignums()` — Regex
```java
private static final Pattern LETTERS_AFTER_DIGIT = 
    Pattern.compile("(\\b\\d+(?:\\.\\d*)?|\\.\\d+)[fFdD](?![a-zA-Z_])");
```
Strips Java-style type suffixes from numbers: `100f` → `100`, `2.5d` → `2.5`. The negative lookahead `(?![a-zA-Z_])` prevents stripping from hex-like values.

### Phase 3: `correctTrailingPeriods()` — Regex
Two patterns handle the two contexts where trailing periods appear:
- `1.` before a comma → `1`
- `1.` before a closing brace/bracket/newline/EOF → `1,` (Quirk: this adds a comma, not removes the period, because Starsector sometimes uses trailing periods as implicit comma separators)

---

## 3. Jackson Configuration: `FileUtilities`

[FileUtilities.java](file:///run/media/lechibang/cb09d199-3769-4ec8-9af5-954929515428/projects/starsector-shipmaker/src/main/java/shipeditor/parsing/FileUtilities.java) holds a singleton `ObjectMapper` with an extensive set of non-default configurations:

```java
mapper.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
mapper.configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true);
mapper.configure(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS.mappedFeature(), true);
mapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
mapper.configure(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS.mappedFeature(), true);
mapper.configure(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature(), true);

mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
mapper.coercionConfigFor(LogicalType.Collection)
      .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
```

### Why Each Feature Matters
| Feature | Why Needed |
|---|---|
| `ALLOW_YAML_COMMENTS` | Starsector JSON files contain `//` and `/* */` comments (after `#` is stripped by `JsonProcessor`) |
| `ALLOW_TRAILING_COMMA` | Nearly every Starsector array/object ends with a trailing comma |
| `ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS` | Values like `.5` instead of `0.5` appear in weapon data |
| `ALLOW_UNESCAPED_CONTROL_CHARS` | Some mod descriptions contain raw tab/newline characters |
| `ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS` | `+10` appears in modifier values |
| `ALLOW_LEADING_ZEROS_FOR_NUMBERS` | `007` appears as ID prefixes |
| `ACCEPT_SINGLE_VALUE_AS_ARRAY` | Some fields that expect arrays are sometimes written as single values |
| `ACCEPT_EMPTY_STRING_AS_NULL_OBJECT` | Empty string `""` for complex object fields = null, not an error |

### Pretty Printer
The mapper uses a custom `BasicPrettyPrinter` for serialization output, ensuring saved files are human-readable with consistent indentation.

---

## 4. CSV Editing & Serialization Rules

**CRITICAL**: Starsector's CSV parser does not use a standard RFC 4180 implementation. Key behaviors:
- Comment columns (like `#`) must NOT be quoted.
- Header order is significant.
- Re-quoting columns that were originally unquoted will break the game engine.

### The Rule
When modifying Starsector CSV files via Jackson:
1. **Loading**: Cache the raw parsed data maps **and** the original `CsvSchema` in `GameDataRepository`.
2. **Saving**: Use a custom Jackson module with `JsonSerializer<Map<?, ?>>` that bypasses default Jackson CSV serialization. Reconstruct the exact raw structure, rebuilding the schema with `setUseHeader(true)`.

### Why Standard Jackson CSV Fails
Jackson's default `CsvMapper` quotes any field containing special characters. Starsector's comment columns (starting with `#`) would get quoted as `"#This is a comment"`, which the game engine interprets as a literal string value instead of a comment delimiter.

---

## 5. Entity ID Extraction: `IndexScannerTask`

[IndexScannerTask.java](file:///run/media/lechibang/cb09d199-3769-4ec8-9af5-954929515428/projects/starsector-shipmaker/src/main/java/shipeditor/parsing/loading/IndexScannerTask.java) uses a **streaming parser** to extract entity IDs without deserializing the entire file:

```java
try (JsonParser parser = mapper.getFactory().createParser(in)) {
    while (parser.nextToken() != null) {
        if (parser.currentToken() == JsonToken.FIELD_NAME) {
            if (keyToFind.equals(parser.currentName())) {
                parser.nextToken();
                return parser.getText();
            }
        }
    }
}
```

### Entity ID Key Mapping
| Entity Type | JSON Key |
|---|---|
| `SHIP` | `hullId` |
| `SKIN` | `skinHullId` |
| `VARIANT` | `variantId` |
| All others | `id` |

### Fallback (Quirk)
If streaming parsing fails (e.g., malformed JSON that even the pre-processor couldn't fix), the entity ID falls back to the filename with its extension stripped: `paragon.ship` → `paragon`. This ensures the index is always populated, even for broken files.

---

## 6. File Resolution Across Packages

`FileUtilities.getFileFromPackages()` resolves a relative path across the core game and all mod folders. It returns a `LinkedHashMap<Path, File>` preserving insertion order:
1. Core game folder (always first)
2. Mod folders in registration order

This ordering is important for Starsector's override cascade: mods override core files, and later mods override earlier mods.

### `isFileWithinGamePackages()`
A safety check that verifies a file path falls within the core folder or any registered mod folder. Used to prevent file operations on arbitrary filesystem locations.

---

## 7. Data Model Layer: `shipeditor.representation`

All Jackson-annotated data classes live under `shipeditor.representation.ship` and `shipeditor.representation.weapon`. These use:
- **Lombok `@Getter`/`@Setter`/`@Builder`** — Boilerplate reduction.
- **Jackson `@JsonProperty`** — Maps Starsector's field names to Java fields.
- **Event Bus** — Modifications to representations publish events via `EventBus.publish()`, not direct method calls between UI components.

### `GameDataRepository`
[GameDataRepository.java](file:///run/media/lechibang/cb09d199-3769-4ec8-9af5-954929515428/projects/starsector-shipmaker/src/main/java/shipeditor/representation/GameDataRepository.java) is the central in-memory cache. It:
1. Queries `DatabaseQueryService` for file paths
2. Loads files using the pre-processed Jackson pipeline
3. Holds parsed representations in memory for instant UI access
4. Manages raw CSV data maps and schemas atomically using the `CachedCSVData` composite class to prevent cache inconsistencies and GC-induced out-of-sync states.
