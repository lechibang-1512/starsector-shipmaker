import shipeditor.persistence.SettingsManager;
import shipeditor.persistence.Initializations;
import shipeditor.persistence.Settings;
public class print_core {
    public static void main(String[] args) {
        Initializations.initializeSettingsFile();
        Initializations.selectGameFolder();
        System.out.println("CORE FOLDER NAME: " + SettingsManager.getCoreFolderName());
        System.out.println("IS CORE FOLDER? " + SettingsManager.isCoreFolder("starsector-core"));
    }
}
