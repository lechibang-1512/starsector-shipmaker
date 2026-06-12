package shipeditor.menubar;

import shipeditor.parsing.FileUtilities;
import shipeditor.parsing.JsonProcessor;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.utility.Utility;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

class DataMenu extends JMenu {

    DataMenu() {
        super("Data");
    }

    void initialize() {
        JMenuItem reloadAllGameData = new JMenuItem("Reload all game data");
        reloadAllGameData.addActionListener(event -> FileLoading.loadGameData());
        if (FileLoading.isLoadingInProgress()) {
            reloadAllGameData.setEnabled(false);
        }
        this.add(reloadAllGameData);

        JMenuItem jsonCorrector = DataMenu.getJSONCorrector();
        this.add(jsonCorrector);
    }

    private static JMenuItem getJSONCorrector() {
        JMenuItem jsonCorrector = new JMenuItem("Correct non-conforming JSON");
        jsonCorrector.setToolTipText("Fixes semantically incorrect JSON, then saves it to the same location");
        jsonCorrector.addActionListener(e -> {
            JFileChooser fileChooser = FileUtilities.getFileChooser();

            File directory = FileUtilities.getLastGeneralDirectory();
            if (directory != null) {
                fileChooser.setCurrentDirectory(directory);
            }

            int returnVal = fileChooser.showOpenDialog(shipeditor.PrimaryWindow.getInstance());
            File currentDirectory = fileChooser.getCurrentDirectory();
            FileUtilities.setLastGeneralDirectory(currentDirectory);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                DataMenu.correctJSON(fileChooser, currentDirectory);
            }
        });
        return jsonCorrector;
    }

    private static void correctJSON(JFileChooser fileChooser, File currentDirectory) {
        File file = fileChooser.getSelectedFile();

        String result = JsonProcessor.straightenMalformed(file);

        String fixedFileName = Utility.getFilenameWithoutExtension(file.getName()) + "_corrected.json";

        String path = currentDirectory.getPath();
        String targetFilePath = path + "\\" + fixedFileName;
        try (PrintWriter out = new PrintWriter(targetFilePath, StandardCharsets.UTF_8)) {
            out.println(result);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
