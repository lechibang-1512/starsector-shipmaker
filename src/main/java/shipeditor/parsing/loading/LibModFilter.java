package shipeditor.parsing.loading;

import java.nio.file.Path;

/** * Utility filter for identifying known library/utility mods that should be excluded
 * from game data loading. These mods typically contain no ship, weapon, or hullmod data —
 * only code libraries used by other mods.
 **/
public final class LibModFilter {

    /**
     * Known library mod folder names. Add new entries here as needed.
     */
    private static final String[] LIB_MOD_PREFIXES = {
            "magiclib",
            "lazylib",
            "graphicslib",
            "lunalib",
            "console commands",
            "lw_lazylib",
            "lw_console",
            "shaderlib"
    };

    private LibModFilter() {}

    /**
     * @param folderName the mod folder name (not the full path).
     * @return true if the folder name matches a known library mod.
     */
    public static boolean isLibMod(String folderName) {
        if (folderName == null || folderName.isEmpty()) return false;
        String lowerCaseName = folderName.toLowerCase(java.util.Locale.ROOT);
        
        boolean isLib = false;

        for (String prefix : LIB_MOD_PREFIXES) {
            if (lowerCaseName.contains(prefix)) {
                isLib = true;
                break;
            }
        }
        
        shipeditor.persistence.Settings settings = shipeditor.persistence.SettingsManager.getSettings();
        if (!isLib && settings != null && settings.getBlacklistedMods() != null) {
            for (String prefix : settings.getBlacklistedMods()) {
                if (lowerCaseName.contains(prefix.toLowerCase(java.util.Locale.ROOT))) {
                    isLib = true;
                    break;
                }
            }
        }
        
        return isLib;
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
