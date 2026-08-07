import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class test_core_load {
    public static void main(String[] args) {
        shipeditor.parsing.loading.FileLoading.initializeDatabaseInProcess();
        shipeditor.persistence.SettingsManager.updateFileFromRuntime();
        GameDataRepository repo = SettingsManager.getGameData();
        Map<Path, List<shipeditor.components.datafiles.entities.HullmodCSVEntry>> hullmods = repo.getHullmodEntriesByPackage();
        System.out.println("Hullmods packages loaded: " + hullmods.size());
        for (Path p : hullmods.keySet()) {
            System.out.println("Package: " + p + " -> count: " + hullmods.get(p).size());
        }
    }
}
