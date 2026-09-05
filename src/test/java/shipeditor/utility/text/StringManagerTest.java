package shipeditor.utility.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringManagerTest {

    @Test
    @DisplayName("Verify that StringManager successfully loads strings from JSON resources")
    void testLoadsStrings() {
        String defaultStr = StringManager.getString("DEFAULT");
        assertNotNull(defaultStr);
        assertEquals("Default", defaultStr);
    }

    @Test
    @DisplayName("Verify missing key fallback returns the key itself")
    void testMissingKeyFallback() {
        String nonexistentKey = "THIS_KEY_DOES_NOT_EXIST_XYZ_123";
        assertEquals(nonexistentKey, StringManager.getString(nonexistentKey));
    }

    @Test
    @DisplayName("Verify parameterized message formatting with arguments")
    void testParameterizedFormatting() {
        String formatted = StringManager.getString("DEFAULT", "arg1", "arg2");
        assertEquals("Default", formatted);
    }

    @Test
    @DisplayName("Verify containsKey check")
    void testContainsKey() {
        assertTrue(StringManager.containsKey("DEFAULT"));
    }
}
