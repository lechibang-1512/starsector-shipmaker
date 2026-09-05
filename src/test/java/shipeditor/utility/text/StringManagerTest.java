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

    @Test
    @DisplayName("Check for missing keys in codebase")
    void testAllKeysPresent() throws Exception {
        java.nio.file.Path srcDir = java.nio.file.Paths.get("src/main/java");
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("StringManager\\.getString\\(\\s*\"([^\"]+)\"");
        java.util.Set<String> missingKeys = new java.util.TreeSet<>();
        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(srcDir)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    String content = java.nio.file.Files.readString(path);
                    java.util.regex.Matcher m = pattern.matcher(content);
                    while (m.find()) {
                        String key = m.group(1);
                        if (!StringManager.containsKey(key)) {
                            missingKeys.add(key + " (in " + srcDir.relativize(path) + ")");
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        for (String mk : missingKeys) {
            System.err.println("MISSING_KEY: " + mk);
        }
        assertTrue(missingKeys.isEmpty(), "Found missing keys: " + missingKeys);
    }
}
