import shipeditor.persistence.SettingsManager;
import shipeditor.persistence.Settings;
import shipeditor.parsing.FileUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestCore {
    public static void main(String[] args) {
        shipeditor.parsing.loading.FileLoading.initializeDatabaseInProcess();
        shipeditor.persistence.SettingsManager.updateFileFromRuntime();
        Settings settings = SettingsManager.getSettings();
        Path corePath = SettingsManager.getCoreFolderPath();
        System.out.println("Core path: " + corePath);
        Path coreFileName = corePath.getFileName();
        System.out.println("Core file name: " + coreFileName);
        boolean isActive = SettingsManager.isModActive(coreFileName.toString());
        System.out.println("Is active: " + isActive);
        System.out.println("Is core folder: " + SettingsManager.isCoreFolder(corePath));
    }
}
