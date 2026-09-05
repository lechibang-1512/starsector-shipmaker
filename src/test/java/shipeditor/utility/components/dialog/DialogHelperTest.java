package shipeditor.utility.components.dialog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DialogHelperTest {

    @Test
    @DisplayName("resolvePackageName handles null and normal folders")
    void testResolvePackageName() {
        assertEquals("Unknown", DialogHelper.resolvePackageName(null));
        assertEquals("mods_folder", DialogHelper.resolvePackageName(Path.of("/games/starsector/mods/mods_folder")));
    }

    @Test
    @DisplayName("resolveModIdName handles null, blank, core, and normal mod IDs")
    void testResolveModIdName() {
        assertEquals("Starsector Core", DialogHelper.resolveModIdName(null));
        assertEquals("Starsector Core", DialogHelper.resolveModIdName(""));
        assertEquals("Starsector Core", DialogHelper.resolveModIdName("   "));
        assertEquals("Starsector Core", DialogHelper.resolveModIdName("starsector-core"));
        assertEquals("my_mod_id", DialogHelper.resolveModIdName("my_mod_id"));
    }
}
