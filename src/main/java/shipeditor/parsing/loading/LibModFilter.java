package shipeditor.parsing.loading;

import java.nio.file.Path;
import java.util.Set;

/** * Utility filter for identifying known library/utility mods that should be excluded
 * from game data loading. These mods typically contain no ship, weapon, or hullmod data —
 * only code libraries used by other mods.
 **/
public final class LibModFilter {

    /**
     * Known library mod folder names. Add new entries here as needed.
     */
    private static final Set<String> LIB_MOD_FOLDERS = Set.of(
            "MagicLib",
            "LazyLib",
            "GraphicsLib",
            "LunaLib",
            "Console Commands",
            "lw_lazylib",
            "lw_console",
            "shaderLib"
    );

    private LibModFilter() {}

    /**
     * @param folderName the mod folder name (not the full path).
     * @return true if the folder name matches a known library mod.
     */
    public static boolean isLibMod(String folderName) {
        return LIB_MOD_FOLDERS.contains(folderName);
    }

    /**
     * @param folder the mod folder path; the last path component is used for matching.
     * @return true if the folder matches a known library mod.
     */
    public static boolean isLibMod(Path folder) {
        if (folder == null) return false;
        Path fileNamePath = folder.getFileName();
        if (fileNamePath == null) return false;
        String folderName = fileNamePath.toString();
        return isLibMod(folderName);
    }

}
