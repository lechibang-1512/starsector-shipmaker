package shipeditor.menubar;

import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.parsing.FileUtilities;
import shipeditor.parsing.JsonProcessor;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.utility.Utility;
import shipeditor.utility.themes.Themes;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

class DataMenu extends JMenu {

    DataMenu() {
        super("Data");
        this.setMnemonic(KeyEvent.VK_D);
    }

    void initialize() {
        JMenuItem reloadAllGameData = new JMenuItem("Reload all game data");
        reloadAllGameData.setIcon(FontIcon.of(BoxiconsRegular.REFRESH, 16, Themes.getIconColor()));
        reloadAllGameData.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        reloadAllGameData.addActionListener(event -> FileLoading.loadGameData());
        if (FileLoading.isLoadingInProgress()) {
            reloadAllGameData.setEnabled(false);
        }
        this.add(reloadAllGameData);

        this.addSeparator();

        JMenuItem reindexData = new JMenuItem("Re-index Mod Folders");
        reindexData.setIcon(FontIcon.of(BoxiconsRegular.REFRESH, 16, Themes.getIconColor()));
        reindexData.addActionListener(event -> {
            if (FileLoading.isLoadingInProgress()) {
                javax.swing.JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        "Cannot re-index while data is currently loading.",
                        "Loading in Progress", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            FileLoading.forceReindexAndLoadGameData();
        });
        if (FileLoading.isLoadingInProgress()) {
            reindexData.setEnabled(false);
        }
        this.add(reindexData);

        this.addSeparator();

        JMenuItem selectMods = new JMenuItem("Select Mods to Load...");
        selectMods.setIcon(FontIcon.of(BoxiconsRegular.LIST_CHECK, 16, Themes.getIconColor()));
        selectMods.addActionListener(event -> {
            if (FileLoading.isLoadingInProgress()) {
                javax.swing.JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        "Cannot modify mod selection while data is currently loading.",
                        "Loading in Progress", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            shipeditor.components.dialogs.ModSelectionDialog modDialog = new shipeditor.components.dialogs.ModSelectionDialog(shipeditor.PrimaryWindow.getInstance());
            boolean shouldLoad = modDialog.showDialog();
            if (shouldLoad) {
                FileLoading.forceReindexAndLoadGameData();
            }
        });
        this.add(selectMods);

        JMenuItem jsonCorrector = DataMenu.getJSONCorrector();
        this.add(jsonCorrector);
    }

    private static JMenuItem getJSONCorrector() {
        JMenuItem jsonCorrector = new JMenuItem("Correct non-conforming JSON");
        jsonCorrector.setIcon(FontIcon.of(BoxiconsRegular.WRENCH, 16, Themes.getIconColor()));
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
