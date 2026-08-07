package shipeditor;
import java.util.Map;
import java.util.List;
import java.nio.file.Path;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.components.datafiles.entities.ShipCSVEntry;

public class TestShipEntries {
    public static void printCounts() {
        GameDataRepository gameData = SettingsManager.getGameData();
        Map<Path, List<ShipCSVEntry>> ships = gameData.getShipEntriesByPackage();
        if (ships == null) {
            System.out.println("SHIPS IS NULL");
        } else {
            System.out.println("SHIPS PACKAGES COUNT: " + ships.size());
            for (Map.Entry<Path, List<ShipCSVEntry>> entry : ships.entrySet()) {
                System.out.println(entry.getKey() + " -> " + entry.getValue().size() + " ships");
            }
        }
    }
}
