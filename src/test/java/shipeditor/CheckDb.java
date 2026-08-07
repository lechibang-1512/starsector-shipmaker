package shipeditor;

import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.persistence.database.DatabaseManager;
import shipeditor.persistence.database.IndexedFile;
import java.util.List;

public class CheckDb {
    public static void main(String[] args) {
        try {
            System.out.println("Checking actual database at: " + DatabaseManager.getDatabaseFilePath());
            System.out.println("Is Database Valid: " + DatabaseManager.isDatabaseValid());
            
            String[] types = {"HULLMOD_CSV", "SHIPSYSTEM_CSV", "HULL_STYLE_JSON", "ENGINE_STYLE_JSON"};
            for (String type : types) {
                List<IndexedFile> files = DatabaseQueryService.getFilesByType(type);
                System.out.println(type + " count: " + files.size());
                if (!files.isEmpty()) {
                    System.out.println("  Sample: " + files.get(0).getEntityId() + " -> " + files.get(0).getFilePath());
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
