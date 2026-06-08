package oth.shipeditor.menubar;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.parsing.FileUtilities;
import oth.shipeditor.parsing.JsonProcessor;
import oth.shipeditor.parsing.loading.FileLoading;
import oth.shipeditor.utility.Utility;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Log4j2
class FileMenu extends JMenu {

    FileMenu() {
        super("File");
    }

    void initialize() {
        JMenu openSubmenu = FileMenu.createOpenSubmenu();
        this.add(openSubmenu);

        JMenuItem loadHullAsLayer = new JMenuItem(FileLoading.getLoadHullAsLayer());
        loadHullAsLayer.setText("Load ship file as layer");
        this.add(loadHullAsLayer);

        JMenuItem loadSpriteAsHull = new JMenuItem(FileLoading.getLoadSpriteAsHull());
        loadSpriteAsHull.setText("Load sprite as new hull");
        this.add(loadSpriteAsHull);

        this.addSeparator();

        JMenuItem reloadAllGameData = new JMenuItem("Reload all game data");
        reloadAllGameData.addActionListener(event -> FileLoading.loadGameData());
        if (FileLoading.isLoadingInProgress()) {
            reloadAllGameData.setEnabled(false);
        }
        this.add(reloadAllGameData);

        JMenuItem jsonCorrector = FileMenu.getJSONCorrector();
        this.add(jsonCorrector);
    }

    private static JMenu createOpenSubmenu() {
        JMenu newSubmenu = new JMenu("Open");

        JMenuItem openSprite = new JMenuItem(FileLoading.getOpenSprite());
        openSprite.setText("Open sprite to layer");
        newSubmenu.add(openSprite);

        JMenuItem openShipData = new JMenuItem(FileLoading.getOpenShip());
        openShipData.setText("Open ship file to layer");
        newSubmenu.add(openShipData);

        return newSubmenu;
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
                FileMenu.correctJSON(fileChooser, currentDirectory);
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
