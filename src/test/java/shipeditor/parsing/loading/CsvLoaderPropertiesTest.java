package shipeditor.parsing.loading;

import net.jqwik.api.*;
import shipeditor.utility.text.StringConstants;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvLoaderPropertiesTest {

    @Property
    void testNormalValidationPredicate(
            @ForAll String id,
            @ForAll String name) {
        
        Map<String, String> row = new HashMap<>();
        if (id != null) row.put(StringConstants.ID, id);
        if (name != null) row.put("name", name);
        
        boolean expected = (id != null && !id.isEmpty()) && (name == null || !name.startsWith("#"));
        
        assertEquals(expected, CsvLoader.getNormalValidationPredicate().test(row));
    }

    @Property
    void testWingValidationPredicate(
            @ForAll String id) {
        
        Map<String, String> row = new HashMap<>();
        if (id != null) row.put(StringConstants.ID, id);
        
        boolean expected = (id != null && !id.isEmpty() && !id.startsWith("#"));
        
        assertEquals(expected, CsvLoader.getWingValidationPredicate().test(row));
    }
}
