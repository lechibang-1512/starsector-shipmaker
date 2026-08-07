package shipeditor;

import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.text.StringConstants;
import java.util.List;
import java.util.Map;

public class DebugDataReference {
    public static void main(String[] args) {
        System.out.println("Starting DebugDataReference...");
        try {
            shipeditor.persistence.database.DatabaseManager.initializeDatabase();
            
            System.out.println("--- DB HULLMODS ---");
            Map<String, List<IndexedFile>> hullmods = DatabaseQueryService.getFilesByTypeGroupedByMod(StringConstants.HULLMOD_CSV_TYPE);
            System.out.println("Hullmods mods count: " + hullmods.size());
            for (String mod : hullmods.keySet()) {
                System.out.println(" Mod: " + mod + " files: " + hullmods.get(mod).size());
                for (IndexedFile f : hullmods.get(mod)) {
                     System.out.println("   File: " + f.getFilePath());
                }
            }

            System.out.println("--- DB SHIPSYSTEMS ---");
            Map<String, List<IndexedFile>> systems = DatabaseQueryService.getFilesByTypeGroupedByMod(StringConstants.SHIPSYSTEM_CSV_TYPE);
            System.out.println("Shipsystems mods count: " + systems.size());
            for (String mod : systems.keySet()) {
                System.out.println(" Mod: " + mod + " files: " + systems.get(mod).size());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
