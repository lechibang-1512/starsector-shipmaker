package shipeditor.menubar;

import lombok.extern.log4j.Log4j2;
import shipeditor.parsing.loading.FileLoading;

import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.persistence.SettingsManager;
import shipeditor.persistence.database.DatabaseManager;
import shipeditor.utility.themes.Themes;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;

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

        JMenuItem preferences = new JMenuItem("Preferences");
        preferences.setIcon(FontIcon.of(BoxiconsRegular.COG, 16, Themes.getIconColor()));
        preferences.addActionListener(e -> {
            shipeditor.components.settings.PreferencesDialog dialog = new shipeditor.components.settings.PreferencesDialog(shipeditor.PrimaryWindow.getInstance());
            dialog.setVisible(true);
        });
        this.add(preferences);

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

}
