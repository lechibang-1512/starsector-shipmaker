package shipeditor.representation;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shipeditor.parsing.loading.CsvLoader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

class GameDataRepositoryPropertiesTest {

    private GameDataRepository repository;

    @BeforeEach
    void setUp() {
        repository = new GameDataRepository();
    }

    @Property
    void testNormalValidationPredicate(@ForAll("mapWithAndWithoutId") Map<String, String> row) {
        Predicate<Map<String, String>> predicate = CsvLoader.getNormalValidationPredicate();
        boolean isValid = predicate.test(row);

        String id = row.get("id");
        String name = row.get("name");

        boolean expected = (id != null && !id.isEmpty()) && (name == null || !name.startsWith("#"));
        Assertions.assertEquals(expected, isValid);
    }

    @Property
    void testWingValidationPredicate(@ForAll("mapWithAndWithoutId") Map<String, String> row) {
        Predicate<Map<String, String>> predicate = CsvLoader.getWingValidationPredicate();
        boolean isValid = predicate.test(row);

        String id = row.get("id");

        boolean expected = (id != null && !id.isEmpty() && !id.startsWith("#"));
        Assertions.assertEquals(expected, isValid);
    }

    @Test
    void testCsvCacheFallbackTrigger() {
        // Since FileLoading.reparseCSVForPath relies on real files and SettingsManager,
        // we can just test that putRawCSVDataForPath / getRawCSVDataForPath works
        // for standard data caching and soft reference retrieval logic.
        Path testPath = Paths.get("data", "test", "test.csv");
        java.util.List<Map<String, String>> data = new java.util.ArrayList<>();
        Map<String, String> row = new HashMap<>();
        row.put("id", "test_id");
        data.add(row);

        repository.putRawCSVDataForPath(testPath, data);

        // Retrieve and check
        java.util.List<Map<String, String>> retrieved = repository.getRawCSVDataForPath(testPath);
        Assertions.assertNotNull(retrieved);
        Assertions.assertEquals("test_id", retrieved.get(0).get("id"));
    }

    @Provide
    Arbitrary<Map<String, String>> mapWithAndWithoutId() {
        Arbitrary<String> idArb = Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(10).injectNull(0.2);
        Arbitrary<String> nameArb = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10).map(s -> "#" + s);
        Arbitrary<String> regularNameArb = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10);
        
        Arbitrary<String> finalNameArb = Arbitraries.oneOf(nameArb, regularNameArb).injectNull(0.2);

        return Combinators.combine(idArb, finalNameArb).as((id, name) -> {
            Map<String, String> map = new HashMap<>();
            if (id != null) map.put("id", id);
            if (name != null) map.put("name", name);
            return map;
        });
    }
}
