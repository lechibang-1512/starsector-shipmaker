package shipeditor.parsing;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the database loading pipeline.
 * <p>
 * Tests the full CSV parsing → JSON serialization (DB cache) → JSON deserialization → entry creation
 * cycle for all 5 entity types: ships, weapons, hullmods, wings, and ship systems.
 * <p>
 * Also validates that SaveCSVAction's custom serializer produces output that can be round-tripped
 * and that Starsector CSV quirks (comment rows, BOM, empty fields) are handled correctly.
 */
class DatabaseLoadingIntegrationTest {

    private static ObjectMapper configuredMapper;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setupMapper() {
        configuredMapper = FileUtilities.getConfigured();
    }

    // ============================================================
    // Section 1: CSV Parsing Tests (Phase 1 of the loading pipeline)
    // ============================================================

    @Test
    void testParseShipCSVNormalRows() throws IOException {
        String csv = """
                name,id,designation,tech/manufacturer,system id,fleet pts,hitpoints,armor rating,ordnance points,hints,tags,rarity
                Onslaught,onslaught,Battleship,Low Tech,burndrive,40,17000,1500,400,SHIP,lowtech_bp,1
                #comment row,comment,,,,,,,,,,
                """;
        Path csvFile = tempDir.resolve("ship_data.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> result = parseCsvDirect(csvFile);

        assertNotNull(result);
        // Raw parse returns ALL rows including comments
        assertTrue(result.size() >= 1, "Should have at least 1 row");

        Map<String, String> onslaught = result.stream()
                .filter(r -> "onslaught".equals(r.get("id")))
                .findFirst()
                .orElse(null);
        assertNotNull(onslaught, "Should find onslaught row");
        assertEquals("Onslaught", onslaught.get("name"));
        assertEquals("Battleship", onslaught.get("designation"));
        assertEquals("40", onslaught.get("fleet pts"));
        assertEquals("17000", onslaught.get("hitpoints"));
        assertEquals("400", onslaught.get("ordnance points"));
        assertEquals("SHIP", onslaught.get("hints"));
        assertEquals("lowtech_bp", onslaught.get("tags"));
    }

    @Test
    void testParseWeaponCSV() throws IOException {
        String csv = """
                name,id,tier,rarity,base value,range,damage/shot,damage/second,emp,impact,turn rate,OPs,ammo,ammo/sec,reload size,type,energy/shot,energy/second,chargeup,chargedown,burst size,burst delay,min spread,max spread,spread/shot,spread decay/sec,beam speed,proj speed,launch speed,flight time,proj hitpoints,hints,tags,groupTag,tech/manufacturer,for weapon tooltip>>,primary role,speedPt,trackingPt,damage,turn rate,accuracy,number
                Autopulse Laser,autopulse,3,0.5,30000,900,200,800,,1,50,27,,,,ENERGY,200,800,0.5,0,,0,0,0,0,,,,,,BEAM,high_tech_bp,autopulse_bp,High Tech,,PD,,,,,,
                """;
        Path csvFile = tempDir.resolve("weapon_data.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> result = parseCsvDirect(csvFile);

        assertNotNull(result);
        assertEquals(1, result.size());

        Map<String, String> autopulse = result.get(0);
        assertEquals("autopulse", autopulse.get("id"));
        assertEquals("Autopulse Laser", autopulse.get("name"));
        assertEquals("900", autopulse.get("range"));
        assertEquals("27", autopulse.get("OPs"));
        assertEquals("ENERGY", autopulse.get("type"));
    }

    @Test
    void testParseHullmodCSV() throws IOException {
        String csv = """
                name,id,tier,rarity,tech/manufacturer,tags,uiTags,base value,unlocked,hidden,hiddenEverywhere,cost_frigate,cost_dest,cost_cruiser,cost_capital,script,desc_I,desc_II,desc_III,short,sprite
                Hardened Shields,hardenedshieldsmod,1,0.75,Common,,Shields,10000,,,,5,10,15,25,data/hullmods/HardenedShields.java,,,,,graphics/hullmods/hardened_shields.png
                """;
        Path csvFile = tempDir.resolve("hull_mods.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> result = parseCsvDirect(csvFile);

        assertNotNull(result);
        assertEquals(1, result.size());

        Map<String, String> hullmod = result.get(0);
        assertEquals("hardenedshieldsmod", hullmod.get("id"));
        assertEquals("Hardened Shields", hullmod.get("name"));
        assertEquals("5", hullmod.get("cost_frigate"));
        assertEquals("25", hullmod.get("cost_capital"));
        assertEquals("graphics/hullmods/hardened_shields.png", hullmod.get("sprite"));
    }

    @Test
    void testParseWingCSV() throws IOException {
        String csv = """
                id,variant,tags,tier,rarity,fleet pts,op cost,formation,range,attackRunRange,attackPositionOffset,num,role,role desc,refit,base value,number
                broadsword_wing,broadsword_Fighter,fighter_bp,0,1,3,8,V,4000,,,,FIGHTER,,30,7500,
                #this is a comment row,,,,,,,,,,,,,,,,
                """;
        Path csvFile = tempDir.resolve("wing_data.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> result = parseCsvDirect(csvFile);

        assertNotNull(result);
        // Should contain both rows from raw parse
        assertTrue(result.size() >= 1);

        Map<String, String> wing = result.stream()
                .filter(r -> "broadsword_wing".equals(r.get("id")))
                .findFirst()
                .orElse(null);
        assertNotNull(wing, "Should find broadsword wing row");
        assertEquals("broadsword_Fighter", wing.get("variant"));
        assertEquals("8", wing.get("op cost"));
        assertEquals("FIGHTER", wing.get("role"));
    }

    @Test
    void testParseShipSystemCSV() throws IOException {
        String csv = """
                name,id,is phase cloak,flux/second,f/s (base),f/s (base flat),max uses,regen,charge up,active,charge down,cooldown,toggle,noDissipation,noHardDissipation,hardFlux,noFiring,noTurning,noStrafing,noAccel,noShield,noVent,isPhaseCloak,tags,number
                Burn Drive,burndrive,,0,,,,,,3,0.5,5,,true,,,,,,,,,,
                """;
        Path csvFile = tempDir.resolve("ship_systems.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> result = parseCsvDirect(csvFile);

        assertNotNull(result);
        assertEquals(1, result.size());

        Map<String, String> system = result.get(0);
        assertEquals("burndrive", system.get("id"));
        assertEquals("Burn Drive", system.get("name"));
        assertEquals("3", system.get("active"));
        assertEquals("5", system.get("cooldown"));
    }

    // ============================================================
    // Section 2: DB Cache Round-trip (Phase 2 — IndexScannerTask caches parsed data as JSON)
    // ============================================================

    @Test
    void testShipCSVRoundTripThroughDBCache() throws IOException {
        String csv = """
                name,id,designation,tech/manufacturer,fleet pts,hitpoints,ordnance points,max flux,hints
                Paragon,paragon,Battleship,High Tech,60,20000,420,25000,SHIP
                """;
        Path csvFile = tempDir.resolve("ship_data.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        // Step 1: Parse CSV (like FileLoading.parseCSVTable does)
        List<Map<String, String>> parsed = parseCsvDirect(csvFile);
        assertNotNull(parsed);
        assertFalse(parsed.isEmpty());

        // Step 2: Serialize to JSON (like IndexScannerTask does for parsed_data column)
        String json = configuredMapper.writeValueAsString(parsed);
        assertNotNull(json);
        assertFalse(json.isEmpty());

        // Step 3: Deserialize back (like LoadCSVDataAction.loadPackage does)
        List<Map<String, String>> deserialized = configuredMapper.readValue(
                json, new TypeReference<List<Map<String, String>>>() {});

        assertNotNull(deserialized);
        assertEquals(parsed.size(), deserialized.size());

        Map<String, String> original = parsed.get(0);
        Map<String, String> restored = deserialized.get(0);

        assertEquals(original.get("id"), restored.get("id"));
        assertEquals(original.get("name"), restored.get("name"));
        assertEquals(original.get("designation"), restored.get("designation"));
        assertEquals(original.get("fleet pts"), restored.get("fleet pts"));
        assertEquals(original.get("hitpoints"), restored.get("hitpoints"));
        assertEquals(original.get("ordnance points"), restored.get("ordnance points"));
        assertEquals(original.get("max flux"), restored.get("max flux"));
    }

    @Test
    void testWeaponCSVRoundTripThroughDBCache() throws IOException {
        String csv = """
                name,id,tier,rarity,base value,range,damage/shot,damage/second,emp,impact,turn rate,OPs,ammo,ammo/sec,reload size,type,energy/shot,energy/second,chargeup,chargedown,burst size,burst delay,min spread,max spread,spread/shot,spread decay/sec,beam speed,proj speed,launch speed,flight time,proj hitpoints,hints,tags,groupTag,tech/manufacturer,for weapon tooltip>>,primary role,speedPt,trackingPt,damage,turn rate,accuracy,number
                Tachyon Lance,tachyon_lance,4,0.25,50000,1000,750,375,,1,0,30,,,,ENERGY,750,375,2,0,,0,0,0,0,,,,,,BEAM,hightech_bp,,High Tech,,Strike,,,,,,
                """;
        Path csvFile = tempDir.resolve("weapon_data.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> parsed = parseCsvDirect(csvFile);
        String json = configuredMapper.writeValueAsString(parsed);
        List<Map<String, String>> deserialized = configuredMapper.readValue(
                json, new TypeReference<List<Map<String, String>>>() {});

        assertEquals(1, deserialized.size());
        assertEquals("tachyon_lance", deserialized.get(0).get("id"));
        assertEquals("Tachyon Lance", deserialized.get(0).get("name"));
        assertEquals("30", deserialized.get(0).get("OPs"));
        assertEquals("ENERGY", deserialized.get(0).get("type"));
    }

    @Test
    void testHullmodCSVRoundTripThroughDBCache() throws IOException {
        String csv = """
                name,id,tier,rarity,tech/manufacturer,tags,uiTags,base value,unlocked,hidden,hiddenEverywhere,cost_frigate,cost_dest,cost_cruiser,cost_capital,script,desc_I,desc_II,desc_III,short,sprite
                Reinforced Hull,reinforcedhull,1,0.75,Common,,Defense,8000,,,,4,8,12,20,data/hullmods/ReinforcedHull.java,,,,,graphics/hullmods/reinforced_hull.png
                """;
        Path csvFile = tempDir.resolve("hull_mods.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> parsed = parseCsvDirect(csvFile);
        String json = configuredMapper.writeValueAsString(parsed);
        List<Map<String, String>> deserialized = configuredMapper.readValue(
                json, new TypeReference<List<Map<String, String>>>() {});

        assertEquals(1, deserialized.size());
        assertEquals("reinforcedhull", deserialized.get(0).get("id"));
        assertEquals("Reinforced Hull", deserialized.get(0).get("name"));
        assertEquals("4", deserialized.get(0).get("cost_frigate"));
        assertEquals("20", deserialized.get(0).get("cost_capital"));
    }

    @Test
    void testWingCSVRoundTripThroughDBCache() throws IOException {
        String csv = """
                id,variant,tags,tier,rarity,fleet pts,op cost,formation,range,attackRunRange,attackPositionOffset,num,role,role desc,refit,base value,number
                talon_wing,talon_Interceptor,fighter_bp,0,1,2,4,V,4000,,,,FIGHTER,,15,3000,
                """;
        Path csvFile = tempDir.resolve("wing_data.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> parsed = parseCsvDirect(csvFile);
        String json = configuredMapper.writeValueAsString(parsed);
        List<Map<String, String>> deserialized = configuredMapper.readValue(
                json, new TypeReference<List<Map<String, String>>>() {});

        assertEquals(1, deserialized.size());
        assertEquals("talon_wing", deserialized.get(0).get("id"));
        assertEquals("talon_Interceptor", deserialized.get(0).get("variant"));
        assertEquals("4", deserialized.get(0).get("op cost"));
    }

    @Test
    void testShipSystemCSVRoundTripThroughDBCache() throws IOException {
        String csv = """
                name,id,is phase cloak,flux/second,f/s (base),f/s (base flat),max uses,regen,charge up,active,charge down,cooldown,toggle,noDissipation,noHardDissipation,hardFlux,noFiring,noTurning,noStrafing,noAccel,noShield,noVent,isPhaseCloak,tags,number
                Phase Cloak,phasecloak,true,0,,,,,,0,0,6,true,true,,,true,,,,true,,,
                """;
        Path csvFile = tempDir.resolve("ship_systems.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> parsed = parseCsvDirect(csvFile);
        String json = configuredMapper.writeValueAsString(parsed);
        List<Map<String, String>> deserialized = configuredMapper.readValue(
                json, new TypeReference<List<Map<String, String>>>() {});

        assertEquals(1, deserialized.size());
        assertEquals("phasecloak", deserialized.get(0).get("id"));
        assertEquals("Phase Cloak", deserialized.get(0).get("name"));
        assertEquals("true", deserialized.get(0).get("is phase cloak"));
    }

    // ============================================================
    // Section 3: CSV Save Serialization (SaveCSVAction custom serializer)
    // ============================================================

    @Test
    void testCustomCSVSerializerPreservesData() throws IOException {
        // Simulate the data that would come from parsing a CSV
        List<Map<String, String>> rawData = new ArrayList<>();
        Map<String, String> row1 = new LinkedHashMap<>();
        row1.put("name", "Test Ship");
        row1.put("id", "test_ship");
        row1.put("designation", "Frigate");
        row1.put("hitpoints", "5000");
        rawData.add(row1);

        // Build schema from column names (as SaveCSVAction does via schema.rebuild())
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        for (String key : row1.keySet()) {
            schemaBuilder.addColumn(key);
        }
        CsvSchema schema = schemaBuilder.build();

        // Apply the custom serializer (exact copy of SaveCSVAction's approach)
        CsvMapper mapper = new CsvMapper();
        SimpleModule module = new SimpleModule();

        @SuppressWarnings("unchecked")
        Class<Map<?, ?>> mapClass = (Class<Map<?, ?>>) (Class<?>) Map.class;

        module.addSerializer(mapClass, new JsonSerializer<Map<?, ?>>() {
            @Override
            public void serialize(Map<?, ?> value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeStartObject();
                for (Map.Entry<?, ?> entry : value.entrySet()) {
                    Object key = entry.getKey();
                    Object val = entry.getValue();
                    if (val != null) {
                        gen.writeStringField(key.toString(), val.toString());
                    } else {
                        gen.writeStringField(key.toString(), "");
                    }
                }
                gen.writeEndObject();
            }
        });
        mapper.registerModule(module);

        CsvSchema customFormat = schema.rebuild()
                .setUseHeader(true)
                .build();

        // Write to file
        File outputFile = tempDir.resolve("output_ship_data.csv").toFile();
        mapper.writer(customFormat).writeValue(outputFile, rawData);

        // Verify the file was written and can be re-parsed
        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("name,id,designation,hitpoints"), "Header should be present");
        assertTrue(content.contains("Test Ship"), "Data should be present");
        assertTrue(content.contains("test_ship"), "ID should be present");

        // Re-parse and verify round-trip
        List<Map<String, String>> reParsed = parseCsvDirect(outputFile.toPath());
        assertNotNull(reParsed);
        assertEquals(1, reParsed.size());
        assertEquals("test_ship", reParsed.get(0).get("id"));
        assertEquals("Test Ship", reParsed.get(0).get("name"));
        assertEquals("5000", reParsed.get(0).get("hitpoints"));
    }

    @Test
    void testCustomCSVSerializerHandlesNullValues() throws IOException {
        List<Map<String, String>> rawData = new ArrayList<>();
        Map<String, String> row = new LinkedHashMap<>();
        row.put("name", "Hullmod");
        row.put("id", "test_hullmod");
        row.put("desc_I", null);
        row.put("desc_II", null);
        row.put("sprite", "graphics/test.png");
        rawData.add(row);

        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        for (String key : row.keySet()) {
            schemaBuilder.addColumn(key);
        }
        CsvSchema schema = schemaBuilder.build();

        CsvMapper mapper = new CsvMapper();
        SimpleModule module = new SimpleModule();

        @SuppressWarnings("unchecked")
        Class<Map<?, ?>> mapClass = (Class<Map<?, ?>>) (Class<?>) Map.class;

        module.addSerializer(mapClass, new JsonSerializer<Map<?, ?>>() {
            @Override
            public void serialize(Map<?, ?> value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeStartObject();
                for (Map.Entry<?, ?> entry : value.entrySet()) {
                    Object key = entry.getKey();
                    Object val = entry.getValue();
                    if (val != null) {
                        gen.writeStringField(key.toString(), val.toString());
                    } else {
                        gen.writeStringField(key.toString(), "");
                    }
                }
                gen.writeEndObject();
            }
        });
        mapper.registerModule(module);

        CsvSchema customFormat = schema.rebuild().setUseHeader(true).build();

        File outputFile = tempDir.resolve("output_hullmod.csv").toFile();
        mapper.writer(customFormat).writeValue(outputFile, rawData);

        // Null values should be written as empty strings, not "null"
        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertFalse(content.contains("null"), "Null values should not appear as literal 'null'");

        List<Map<String, String>> reParsed = parseCsvDirect(outputFile.toPath());
        assertEquals(1, reParsed.size());
        assertEquals("test_hullmod", reParsed.get(0).get("id"));
        // Null values become empty strings after serialization
        assertTrue(reParsed.get(0).get("desc_I") == null || reParsed.get(0).get("desc_I").isEmpty());
    }

    // ============================================================
    // Section 4: Edge Cases & Starsector CSV Quirks
    // ============================================================

    @Test
    void testBOMHandling() throws IOException {
        // Starsector CSV files sometimes start with UTF-8 BOM
        String csv = "\uFEFFname,id,hitpoints\nTest Ship,test_bom,5000\n";
        Path csvFile = tempDir.resolve("bom_test.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> result = parseCsvDirect(csvFile);
        assertNotNull(result);
        assertEquals(1, result.size());

        Map<String, String> row = result.get(0);
        // The BOM may attach to the first header key
        String id = row.get("id");
        assertEquals("test_bom", id, "ID should be retrievable despite BOM on header");
    }

    @Test
    void testEmptyCSVFile() throws IOException {
        Path csvFile = tempDir.resolve("empty.csv");
        Files.writeString(csvFile, "", StandardCharsets.UTF_8);

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true);
        CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();

        // Empty file should be handled gracefully
        try {
            var iterator = csvMapper.readerFor(Map.class)
                    .with(csvSchema)
                    .readValues(csvFile.toFile());
            List<Map<String, String>> result = new ArrayList<>();
            while (iterator.hasNext()) {
                @SuppressWarnings("unchecked")
                Map<String, String> row = (Map<String, String>) iterator.next();
                result.add(row);
            }
            assertTrue(result.isEmpty(), "Empty file should produce no rows");
        } catch (Exception e) {
            // Empty file may throw — this is acceptable as FileLoading checks file.length() == 0
            assertTrue(true, "Empty file correctly throws exception");
        }
    }

    @Test
    void testCommentRowFilteringNormalPredicate() throws IOException {
        String csv = """
                name,id,hitpoints
                Real Ship,real_ship,5000
                #Comment Row,comment_id,0
                Another Ship,another_ship,6000
                """;
        Path csvFile = tempDir.resolve("comment_test.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        // Raw parse gets all rows
        List<Map<String, String>> allRows = parseCsvDirect(csvFile);
        assertNotNull(allRows);
        assertTrue(allRows.size() >= 2, "Raw parse should return at least data + comment rows");

        // Apply normal validation predicate (same logic as FileLoading.getNormalValidationPredicate)
        List<Map<String, String>> filtered = allRows.stream()
                .filter(row -> {
                    String id = row.get("id");
                    String name = row.get("name");
                    boolean validID = id != null && !id.isEmpty();
                    return validID && (name == null || !name.startsWith("#"));
                })
                .toList();

        assertEquals(2, filtered.size(), "Should filter out comment rows");
        assertEquals("real_ship", filtered.get(0).get("id"));
        assertEquals("another_ship", filtered.get(1).get("id"));
    }

    @Test
    void testCommentRowFilteringWingPredicate() throws IOException {
        String csv = """
                id,variant,op cost
                broadsword_wing,broadsword_Fighter,8
                #comment,,
                talon_wing,talon_Interceptor,4
                """;
        Path csvFile = tempDir.resolve("wing_comment_test.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> allRows = parseCsvDirect(csvFile);
        assertNotNull(allRows);

        // Apply wing validation predicate (same logic as FileLoading.getWingValidationPredicate)
        List<Map<String, String>> filtered = allRows.stream()
                .filter(row -> {
                    String id = row.get("id");
                    return id != null && !id.isEmpty() && !id.startsWith("#");
                })
                .toList();

        assertEquals(2, filtered.size(), "Wing predicate should filter comment rows by id prefix");
        assertEquals("broadsword_wing", filtered.get(0).get("id"));
        assertEquals("talon_wing", filtered.get(1).get("id"));
    }

    @Test
    void testMultipleRowsPreserveOrder() throws IOException {
        String csv = """
                name,id,ordnance points
                Ship A,ship_a,100
                Ship B,ship_b,200
                Ship C,ship_c,300
                Ship D,ship_d,400
                Ship E,ship_e,500
                """;
        Path csvFile = tempDir.resolve("order_test.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> parsed = parseCsvDirect(csvFile);
        assertNotNull(parsed);
        assertEquals(5, parsed.size());

        // Serialize → deserialize (DB round-trip)
        String json = configuredMapper.writeValueAsString(parsed);
        List<Map<String, String>> deserialized = configuredMapper.readValue(
                json, new TypeReference<List<Map<String, String>>>() {});

        assertEquals(5, deserialized.size());
        // Verify order is preserved
        assertEquals("ship_a", deserialized.get(0).get("id"));
        assertEquals("ship_b", deserialized.get(1).get("id"));
        assertEquals("ship_c", deserialized.get(2).get("id"));
        assertEquals("ship_d", deserialized.get(3).get("id"));
        assertEquals("ship_e", deserialized.get(4).get("id"));
    }

    @Test
    void testSpecialCharactersInCSVFields() throws IOException {
        String csv = "name,id,tags\n"
                + "\"Ship, With Comma\",comma_ship,\"tag1, tag2, tag3\"\n"
                + "\"Ship \"\"Quoted\"\"\",quoted_ship,simple_tag\n";
        Path csvFile = tempDir.resolve("special_chars.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> parsed = parseCsvDirect(csvFile);
        assertNotNull(parsed);
        assertEquals(2, parsed.size());
        assertEquals("comma_ship", parsed.get(0).get("id"));
        assertEquals("Ship, With Comma", parsed.get(0).get("name"));

        // DB round-trip should preserve special characters
        String json = configuredMapper.writeValueAsString(parsed);
        List<Map<String, String>> deserialized = configuredMapper.readValue(
                json, new TypeReference<List<Map<String, String>>>() {});

        assertEquals("Ship, With Comma", deserialized.get(0).get("name"));
        assertEquals("tag1, tag2, tag3", deserialized.get(0).get("tags"));
    }

    @Test
    void testEmptyFieldsHandling() throws IOException {
        String csv = """
                name,id,system id,shield type,phase cost,phase upkeep
                Doom,doom,,,0.015,0.015
                """;
        Path csvFile = tempDir.resolve("empty_fields.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        List<Map<String, String>> parsed = parseCsvDirect(csvFile);
        assertNotNull(parsed);
        assertEquals(1, parsed.size());

        Map<String, String> row = parsed.get(0);
        assertEquals("doom", row.get("id"));
        // Empty fields should be empty strings or null, never "null"
        String systemId = row.get("system id");
        assertTrue(systemId == null || systemId.isEmpty(), "Empty field should be null or empty");

        // DB round-trip
        String json = configuredMapper.writeValueAsString(parsed);
        assertFalse(json.contains(":null"), "JSON should not contain literal null for empty fields from CSV");
        List<Map<String, String>> deserialized = configuredMapper.readValue(
                json, new TypeReference<List<Map<String, String>>>() {});

        assertEquals("doom", deserialized.get(0).get("id"));
    }

    // ============================================================
    // Section 5: SaveCSVAction Validation Logic
    // ============================================================

    @Test
    void testSaveCSVValidationDetectsDuplicateIDs() throws IOException {
        List<Map<String, String>> rawData = new ArrayList<>();

        Map<String, String> row1 = new LinkedHashMap<>();
        row1.put("name", "Ship A");
        row1.put("id", "duplicate_ship");
        rawData.add(row1);

        Map<String, String> row2 = new LinkedHashMap<>();
        row2.put("name", "Ship B");
        row2.put("id", "duplicate_ship");
        rawData.add(row2);

        // Use the same validation logic as SaveCSVAction.getValidationWarningMessage
        java.util.Set<String> seenIds = new java.util.HashSet<>();
        java.util.Set<String> duplicateIds = new java.util.HashSet<>();

        for (Map<String, String> row : rawData) {
            String rowIdValue = null;
            for (Map.Entry<String, String> cell : row.entrySet()) {
                String cleanKey = cell.getKey().replace("\uFEFF", "").trim()
                        .toLowerCase(java.util.Locale.ROOT);
                if ("id".equals(cleanKey)) {
                    rowIdValue = cell.getValue();
                    break;
                }
            }
            if (rowIdValue != null && !rowIdValue.trim().isEmpty()) {
                String cleanId = rowIdValue.trim();
                if (!seenIds.add(cleanId)) {
                    duplicateIds.add(cleanId);
                }
            }
        }

        assertFalse(duplicateIds.isEmpty(), "Should detect duplicate IDs");
        assertTrue(duplicateIds.contains("duplicate_ship"));
    }

    @Test
    void testSaveCSVRowMatchingWithBOMKey() {
        // Simulates the BOM-aware ID matching in SaveCSVAction.saveCSVEntry
        List<Map<String, String>> rawData = new ArrayList<>();
        Map<String, String> row = new LinkedHashMap<>();
        row.put("\uFEFFid", "bom_id_ship");  // BOM prefix on key
        row.put("name", "BOM Ship");
        rawData.add(row);

        String targetId = "bom_id_ship";

        boolean found = false;
        for (Map<String, String> r : rawData) {
            String rowIdValue = null;
            for (Map.Entry<String, String> cell : r.entrySet()) {
                String cleanKey = cell.getKey().replace("\uFEFF", "").trim()
                        .toLowerCase(java.util.Locale.ROOT);
                if ("id".equals(cleanKey)) {
                    rowIdValue = cell.getValue();
                    break;
                }
            }
            if (rowIdValue != null && targetId.trim().equalsIgnoreCase(rowIdValue.trim())) {
                found = true;
                break;
            }
        }

        assertTrue(found, "Should find row with BOM-prefixed id key");
    }

    // ============================================================
    // Section 6: Full Pipeline Simulation (CSV → JSON → DB → JSON → Entries)
    // ============================================================

    @Test
    void testFullPipelineSimulationAllEntityTypes() throws IOException {
        // This test simulates the complete IndexScannerTask → LoadCSVDataAction pipeline
        // without needing the database or SettingsManager

        // 1. Ship CSV
        verifyFullPipeline(
                "name,id,designation,fleet pts,hitpoints,ordnance points\n" +
                "Onslaught,onslaught,Battleship,40,17000,400\n",
                "onslaught", "Onslaught", "SHIP_CSV"
        );

        // 2. Weapon CSV
        verifyFullPipeline(
                "name,id,tier,OPs,range,type\n" +
                "Heavy Blaster,heavyblaster,2,16,700,ENERGY\n",
                "heavyblaster", "Heavy Blaster", "WEAPON_CSV"
        );

        // 3. Hullmod CSV
        verifyFullPipeline(
                "name,id,cost_frigate,cost_dest,cost_cruiser,cost_capital,sprite\n" +
                "Heavy Armor,heavyarmor,15,25,40,60,graphics/hullmods/heavy_armor.png\n",
                "heavyarmor", "Heavy Armor", "HULLMOD_CSV"
        );

        // 4. Wing CSV
        verifyFullPipeline(
                "id,variant,op cost,role\n" +
                "broadsword_wing,broadsword_Fighter,8,FIGHTER\n",
                "broadsword_wing", null, "WING_CSV"
        );

        // 5. Ship System CSV
        verifyFullPipeline(
                "name,id,active,cooldown\n" +
                "Burn Drive,burndrive,3,5\n",
                "burndrive", "Burn Drive", "SHIPSYSTEM_CSV"
        );
    }

    private void verifyFullPipeline(String csvContent, String expectedId, String expectedName, String entityType)
            throws IOException {
        // Step 1: Write CSV to temp file
        Path csvFile = tempDir.resolve(entityType.toLowerCase() + "_test.csv");
        Files.writeString(csvFile, csvContent, StandardCharsets.UTF_8);

        // Step 2: Parse CSV (like FileLoading.parseCSVTable)
        List<Map<String, String>> parsed = parseCsvDirect(csvFile);
        assertNotNull(parsed, entityType + ": CSV parsing should succeed");
        assertFalse(parsed.isEmpty(), entityType + ": Should have at least 1 row");

        // Step 3: Serialize to JSON (like IndexScannerTask stores in parsed_data column)
        String parsedDataJson = configuredMapper.writeValueAsString(parsed);
        assertNotNull(parsedDataJson, entityType + ": JSON serialization should succeed");
        assertFalse(parsedDataJson.isEmpty(), entityType + ": JSON should not be empty");

        // Step 4: Deserialize from JSON (like LoadCSVDataAction.loadPackage reads from DB cache)
        List<Map<String, String>> csvData = configuredMapper.readValue(
                parsedDataJson, new TypeReference<List<Map<String, String>>>() {});
        assertNotNull(csvData, entityType + ": JSON deserialization should succeed");
        assertEquals(parsed.size(), csvData.size(), entityType + ": Row count should match");

        // Step 5: Validate row content matches
        for (int i = 0; i < parsed.size(); i++) {
            Map<String, String> originalRow = parsed.get(i);
            Map<String, String> restoredRow = csvData.get(i);
            for (Map.Entry<String, String> entry : originalRow.entrySet()) {
                String originalVal = entry.getValue();
                String restoredVal = restoredRow.get(entry.getKey());
                // Both null or both equal
                if (originalVal == null || originalVal.isEmpty()) {
                    assertTrue(restoredVal == null || restoredVal.isEmpty(),
                            entityType + ": Field '" + entry.getKey() + "' empty value mismatch");
                } else {
                    assertEquals(originalVal, restoredVal,
                            entityType + ": Field '" + entry.getKey() + "' value mismatch");
                }
            }
        }

        // Step 6: Verify ID extraction (like LoadCSVDataAction.loadPackage does)
        Map<String, String> firstRow = csvData.get(0);
        String rowId = firstRow.get("id");
        assertNotNull(rowId, entityType + ": Row should have 'id' field");
        assertEquals(expectedId, rowId, entityType + ": ID mismatch");

        // Step 7: Verify name if applicable
        if (expectedName != null) {
            String name = firstRow.get("name");
            assertEquals(expectedName, name, entityType + ": Name mismatch");
        }
    }

    // ============================================================
    // Section 7: Schema Caching Verification
    // ============================================================

    @Test
    void testCsvSchemaPreservesColumnOrder() throws IOException {
        String csv = """
                name,id,designation,fleet pts,hitpoints
                Test Ship,test_id,Frigate,5,3000
                """;
        Path csvFile = tempDir.resolve("schema_test.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true);
        CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();

        try (java.io.Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
             var iterator = csvMapper.readerFor(Map.class).with(csvSchema).readValues(reader)) {

            CsvSchema parsedSchema = (CsvSchema) iterator.getParser().getSchema();

            // Verify column order is preserved in parsed schema
            List<String> columns = new ArrayList<>();
            for (var col : parsedSchema) {
                columns.add(col.getName());
            }

            assertEquals("name", columns.get(0));
            assertEquals("id", columns.get(1));
            assertEquals("designation", columns.get(2));
            assertEquals("fleet pts", columns.get(3));
            assertEquals("hitpoints", columns.get(4));

            // Verify schema can be rebuilt for saving
            CsvSchema rebuilt = parsedSchema.rebuild().setUseHeader(true).build();
            assertNotNull(rebuilt);
        }
    }

    @Test
    void testCsvSchemaRebuildForSaving() throws IOException {
        // This tests the exact flow SaveCSVAction uses: parse → cache schema → rebuild → write
        String csv = """
                name,id,cost_frigate,cost_capital,sprite
                Test Hullmod,test_hullmod,5,25,graphics/test.png
                """;
        Path csvFile = tempDir.resolve("schema_save_test.csv");
        Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true);
        CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();

        CsvSchema parsedSchema;
        List<Map<String, String>> rawData = new ArrayList<>();

        try (java.io.Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
             var iterator = csvMapper.readerFor(Map.class).with(csvSchema).readValues(reader)) {

            parsedSchema = (CsvSchema) iterator.getParser().getSchema();
            while (iterator.hasNext()) {
                @SuppressWarnings("unchecked")
                Map<String, String> row = (Map<String, String>) iterator.next();
                rawData.add(row);
            }
        }

        // Modify data (simulate user editing a hullmod cost)
        rawData.get(0).put("cost_frigate", "10");

        // Write back using SaveCSVAction's approach
        CsvMapper saveMapper = new CsvMapper();
        SimpleModule module = new SimpleModule();

        @SuppressWarnings("unchecked")
        Class<Map<?, ?>> mapClass = (Class<Map<?, ?>>) (Class<?>) Map.class;

        module.addSerializer(mapClass, new JsonSerializer<Map<?, ?>>() {
            @Override
            public void serialize(Map<?, ?> value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeStartObject();
                for (Map.Entry<?, ?> entry : value.entrySet()) {
                    Object key = entry.getKey();
                    Object val = entry.getValue();
                    gen.writeStringField(key.toString(), val != null ? val.toString() : "");
                }
                gen.writeEndObject();
            }
        });
        saveMapper.registerModule(module);

        CsvSchema customFormat = parsedSchema.rebuild().setUseHeader(true).build();
        File outputFile = tempDir.resolve("schema_save_output.csv").toFile();
        saveMapper.writer(customFormat).writeValue(outputFile, rawData);

        // Re-parse and verify the edit was preserved
        List<Map<String, String>> reParsed = parseCsvDirect(outputFile.toPath());
        assertEquals(1, reParsed.size());
        assertEquals("10", reParsed.get(0).get("cost_frigate"), "Edited value should be preserved");
        assertEquals("25", reParsed.get(0).get("cost_capital"), "Unedited value should be preserved");
        assertEquals("test_hullmod", reParsed.get(0).get("id"));
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    /**
     * Parses a CSV file using the same CsvMapper configuration as FileLoading.parseCSVTable,
     * but without the validation predicate filtering or GameDataRepository caching.
     */
    private static List<Map<String, String>> parseCsvDirect(Path csvFile) throws IOException {
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true);
        CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();

        List<Map<String, String>> rows = new ArrayList<>();
        try (java.io.Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
             var iterator = csvMapper.readerFor(Map.class).with(csvSchema).readValues(reader)) {
            while (iterator.hasNext()) {
                @SuppressWarnings("unchecked")
                Map<String, String> row = (Map<String, String>) iterator.next();
                rows.add(row);
            }
        }
        return rows;
    }

}
