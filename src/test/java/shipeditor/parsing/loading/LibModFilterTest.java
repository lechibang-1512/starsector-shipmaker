package shipeditor.parsing.loading;

import net.jqwik.api.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import shipeditor.persistence.SettingsManager;

import static org.junit.jupiter.api.Assertions.*;

class LibModFilterTest {

    @BeforeAll
    static void setUp() {
        SettingsManager.setSettings(null);
    }

    @Property
    void testNullAndEmptyStringsAreFalse(@ForAll("nullOrEmpty") String folderName) {
        assertFalse(LibModFilter.isLibMod(folderName));
    }

    @Provide
    Arbitrary<String> nullOrEmpty() {
        return Arbitraries.of(null, "");
    }

    @Property
    void testRandomStringsNotContainingPrefixesAreFalse(@ForAll String folderName) {
        String lower = folderName == null ? "" : folderName.toLowerCase(Locale.ROOT);
        boolean containsLib = false;
        String[] prefixes = {"magiclib", "lazylib", "graphicslib", "lunalib", "console commands", "lw_lazylib", "lw_console", "shaderlib"};
        for (String p : prefixes) {
            if (lower.contains(p)) {
                containsLib = true;
                break;
            }
        }
        Assume.that(!containsLib);
        
        assertFalse(LibModFilter.isLibMod(folderName));
    }

    @Property
    void testStringContainingPrefixReturnsTrue(
            @ForAll("libraryPrefixes") String prefix,
            @ForAll("alphaStrings") String prefixPadding,
            @ForAll("alphaStrings") String suffixPadding) {
        
        String mutatedPrefix = mutateCase(prefix);
        String input = prefixPadding + mutatedPrefix + suffixPadding;
                
        assertTrue(LibModFilter.isLibMod(input), "Expected true for input: " + input);
    }

    @Provide
    Arbitrary<String> libraryPrefixes() {
        return Arbitraries.of(
            "magiclib",
            "lazylib",
            "graphicslib",
            "lunalib",
            "console commands",
            "lw_lazylib",
            "lw_console",
            "shaderlib"
        );
    }

    @Provide
    Arbitrary<String> alphaStrings() {
        return Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(10);
    }

    @Property
    void testPathOverload(@ForAll("alphaStrings") String folderName) {
        Path path = folderName.isEmpty() ? null : Paths.get(folderName);
        boolean expected = path != null && LibModFilter.isLibMod(path.getFileName().toString());
        assertEquals(expected, LibModFilter.isLibMod(path));
    }
    
    private String mutateCase(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Math.random() > 0.5) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
