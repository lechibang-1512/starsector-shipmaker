package shipeditor;

import shipeditor.persistence.SettingsManager;
import shipeditor.persistence.Initializations;
import shipeditor.representation.GameDataRepository;
import shipeditor.components.datafiles.entities.ShipSystemCSVEntry;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;

public class DebugShipSystems {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Initializations.initializeSettingsFile();
        Initializations.selectGameFolder();
        shipeditor.parsing.loading.FileLoading.loadGameData().join();
        GameDataRepository repo = SettingsManager.getGameData();
        Map<Path, List<ShipSystemCSVEntry>> map = repo.getShipSystemEntriesByPackage();
        if (map == null) {
            System.out.println("DEBUG: map is null");
        } else {
            System.out.println("DEBUG: map size = " + map.size());
            for (Map.Entry<Path, List<ShipSystemCSVEntry>> entry : map.entrySet()) {
                System.out.println("DEBUG: list for " + entry.getKey() + " size = " + entry.getValue().size());
            }
        }
    }
}
