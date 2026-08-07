package shipeditor.components.dialogs;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ModSelectionDialogPropertiesTest {

    @Property
    void testModInfoQueryMatching(
            @ForAll @AlphaChars @StringLength(min = 1, max = 15) String folder,
            @ForAll @AlphaChars @StringLength(min = 1, max = 15) String search) {

        Path path = Paths.get("/dummy/" + folder);
        ModSelectionDialog.ModInfo info = new ModSelectionDialog.ModInfo(folder, path);

        assertNotNull(info);
        assertTrue(info.matchesQuery(""));
        assertTrue(info.matchesQuery(null));
        
        boolean expectedMatch = folder.toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT));
        assertEquals(expectedMatch, info.matchesQuery(search));
    }
}
