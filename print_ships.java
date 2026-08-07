import shipeditor.persistence.SettingsManager;
import shipeditor.persistence.Initializations;
import shipeditor.persistence.Settings;
import shipeditor.persistence.database.DatabaseQueryService;
public class print_ships {
    public static void main(String[] args) {
        Initializations.initializeSettingsFile();
        System.out.println("SHIPS: " + DatabaseQueryService.getFilesByType("SHIP").size());
        System.out.println("WEAPONS: " + DatabaseQueryService.getFilesByType("WEAPON").size());
        System.out.println("PROJECTILES: " + DatabaseQueryService.getFilesByType("PROJECTILE").size());
    }
}
