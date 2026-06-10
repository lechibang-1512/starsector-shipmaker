package shipeditor.menubar;

import shipeditor.components.datafiles.trees.ShipFilterPanel;
import shipeditor.components.datafiles.trees.WeaponFilterPanel;
import shipeditor.parsing.FileUtilities;
import shipeditor.parsing.JsonProcessor;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.utility.Utility;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import shipeditor.persistence.SettingsManager;
import shipeditor.persistence.database.DatabaseManager;

class ToolsMenu extends JMenu {

    ToolsMenu() {
        super("Tools");
    }

    void initialize() {
        JMenuItem reloadAllGameData = new JMenuItem("Reload all game data");
        reloadAllGameData.addActionListener(event -> FileLoading.loadGameData());
        if (FileLoading.isLoadingInProgress()) {
            reloadAllGameData.setEnabled(false);
        }
        this.add(reloadAllGameData);

        this.addSeparator();

        JMenu filtersSubmenu = new JMenu("Filters");
        JMenuItem shipFiltersItem = new JMenuItem("Ship Filters");
        shipFiltersItem.addActionListener(e -> {
            JDialog dialog = new JDialog();
            dialog.setTitle("Ship Filters");
            dialog.setModal(false);
            dialog.setSize(400, 600);
            dialog.setLocationRelativeTo(null);
            dialog.add(new ShipFilterPanel());
            dialog.setVisible(true);
        });
        filtersSubmenu.add(shipFiltersItem);

        JMenuItem weaponFiltersItem = new JMenuItem("Weapon Filters");
        weaponFiltersItem.addActionListener(e -> {
            JDialog dialog = new JDialog();
            dialog.setTitle("Weapon Filters");
            dialog.setModal(false);
            dialog.setSize(400, 600);
            dialog.setLocationRelativeTo(null);
            dialog.add(new WeaponFilterPanel());
            dialog.setVisible(true);
        });
        filtersSubmenu.add(weaponFiltersItem);
        
        this.add(filtersSubmenu);

        this.addSeparator();

        JMenuItem jsonCorrector = ToolsMenu.getJSONCorrector();
        this.add(jsonCorrector);

        this.addSeparator();

        JMenuItem clearData = new JMenuItem("Clear Data & Reinitialize");
        clearData.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    shipeditor.PrimaryWindow.getInstance(),
                    "Are you sure you want to delete all application data and settings? The application will close.",
                    "Clear Data & Reinitialize",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                try {
                    Files.deleteIfExists(SettingsManager.getSettingsPath().toPath());
                    Files.deleteIfExists(DatabaseManager.getDatabaseFilePath());
                    System.exit(0);
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to delete settings or database files", ex);
                }
            }
        });
        this.add(clearData);
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

            int returnVal = fileChooser.showOpenDialog(null);
            File currentDirectory = fileChooser.getCurrentDirectory();
            FileUtilities.setLastGeneralDirectory(currentDirectory);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                ToolsMenu.correctJSON(fileChooser, currentDirectory);
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
