package shipeditor.menubar;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.HullSaveQueued;
import shipeditor.communication.events.files.FileEvents.VariantSaveQueued;
import shipeditor.communication.events.files.FileEvents.WeaponSaveQueued;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.menubar.actions.PrintSpriteAction;
import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.persistence.SettingsManager;
import shipeditor.persistence.database.DatabaseManager;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.themes.Themes;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;

@Log4j2
class FileMenu extends JMenu {

    FileMenu() {
        super("File");
        this.setMnemonic(KeyEvent.VK_F);
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

        JMenuItem saveActiveLayer = new JMenuItem("Save Active Layer");
        saveActiveLayer.setIcon(FontIcon.of(BoxiconsRegular.SAVE, 16, Themes.getIconColor()));
        saveActiveLayer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveActiveLayer.addActionListener(e -> {
            LayerManager layerManager = StaticController.getLayerManager();
            if (layerManager != null && layerManager.getActiveLayer() != null) {
                ViewerLayer activeLayer = layerManager.getActiveLayer();
                if (activeLayer instanceof ShipLayer shipLayer) {
                    EventBus.publish(new HullSaveQueued(shipLayer));
                    if (shipLayer.getPainter() != null && shipLayer.getPainter().getActiveVariant() != null && !shipLayer.getPainter().getActiveVariant().isEmpty()) {
                        EventBus.publish(new VariantSaveQueued(shipLayer.getPainter().getActiveVariant()));
                    }
                } else if (activeLayer instanceof WeaponLayer weaponLayer) {
                    EventBus.publish(new WeaponSaveQueued(weaponLayer));
                } else if (activeLayer instanceof shipeditor.components.viewer.layers.weapon.ProjectileLayer projLayer) {
                    EventBus.publish(new shipeditor.communication.events.files.FileEvents.ProjectileSaveQueued(projLayer));
                }
            } else {
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(), "No active layer to save.", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        });
        this.add(saveActiveLayer);

        JMenuItem saveAll = new JMenuItem("Save All Layers");
        saveAll.setIcon(FontIcon.of(BoxiconsRegular.SAVE, 16, Themes.getIconColor()));
        saveAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        saveAll.addActionListener(e -> {
            LayerManager layerManager = StaticController.getLayerManager();
            if (layerManager != null) {
                for (ViewerLayer layer : layerManager.getLayers()) {
                    if (layer instanceof ShipLayer shipLayer) {
                        EventBus.publish(new HullSaveQueued(shipLayer));
                        if (shipLayer.getPainter() != null && shipLayer.getPainter().getActiveVariant() != null && !shipLayer.getPainter().getActiveVariant().isEmpty()) {
                            EventBus.publish(new VariantSaveQueued(shipLayer.getPainter().getActiveVariant()));
                        }
                    } else if (layer instanceof WeaponLayer weaponLayer) {
                        EventBus.publish(new WeaponSaveQueued(weaponLayer));
                    } else if (layer instanceof shipeditor.components.viewer.layers.weapon.ProjectileLayer projLayer) {
                        EventBus.publish(new shipeditor.communication.events.files.FileEvents.ProjectileSaveQueued(projLayer));
                    }
                }
            }
        });
        this.add(saveAll);

        this.addSeparator();

        JMenu exportSubmenu = new JMenu("Export...");
        exportSubmenu.setIcon(FontIcon.of(BoxiconsRegular.EXPORT, 16, Themes.getIconColor()));

        JMenuItem exportPNG = new JMenuItem("Export Canvas to PNG");
        exportPNG.setIcon(FontIcon.of(BoxiconsRegular.IMAGE, 16, Themes.getIconColor()));
        exportPNG.addActionListener(e -> {
            shipeditor.components.viewer.PrimaryViewer viewer = shipeditor.utility.overseers.StaticController.getViewer();
            if (viewer != null) {
                shipeditor.components.viewer.ImageExporter.exportCurrentLayerToPNG(viewer);
            }
        });
        exportSubmenu.add(exportPNG);

        JMenuItem printViewer = new JMenuItem("High-Res Sprite Print...");
        printViewer.setIcon(FontIcon.of(BoxiconsRegular.PRINTER, 16, Themes.getIconColor()));
        printViewer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        printViewer.addActionListener(e -> PrintSpriteAction.showPrintDialog());
        exportSubmenu.add(printViewer);

        this.add(exportSubmenu);

        this.addSeparator();

        JMenuItem preferences = new JMenuItem("Preferences");
        preferences.setIcon(FontIcon.of(BoxiconsRegular.COG, 16, Themes.getIconColor()));
        preferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, InputEvent.CTRL_DOWN_MASK));
        preferences.addActionListener(e -> {
            shipeditor.components.settings.PreferencesDialog dialog = new shipeditor.components.settings.PreferencesDialog(shipeditor.PrimaryWindow.getInstance());
            dialog.setVisible(true);
        });
        this.add(preferences);

        this.addSeparator();

        JMenuItem clearData = new JMenuItem("Clear Data & Reinitialize");
        clearData.setIcon(FontIcon.of(BoxiconsRegular.DATA, 16, Themes.getIconColor()));
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

        this.addSeparator();

        JMenuItem exitAction = new JMenuItem("Exit");
        exitAction.setIcon(FontIcon.of(BoxiconsRegular.EXIT, 16, Themes.getIconColor()));
        exitAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
        exitAction.addActionListener(e -> shipeditor.PrimaryWindow.getInstance().dispatchEvent(new java.awt.event.WindowEvent(shipeditor.PrimaryWindow.getInstance(), java.awt.event.WindowEvent.WINDOW_CLOSING)));
        this.add(exitAction);
    }

    private static JMenu createOpenSubmenu() {
        JMenu newSubmenu = new JMenu("Open");
        newSubmenu.setIcon(FontIcon.of(BoxiconsRegular.FOLDER_OPEN, 16, Themes.getIconColor()));

        JMenuItem openSprite = new JMenuItem(FileLoading.getOpenSprite());
        openSprite.setText("Open sprite to layer");
        openSprite.setIcon(FontIcon.of(BoxiconsRegular.IMAGE_ADD, 16, Themes.getIconColor()));
        newSubmenu.add(openSprite);

        JMenuItem openShipData = new JMenuItem(FileLoading.getOpenShip());
        openShipData.setText("Open ship file to layer");
        openShipData.setIcon(FontIcon.of(BoxiconsRegular.FILE_BLANK, 16, Themes.getIconColor()));
        newSubmenu.add(openShipData);

        return newSubmenu;
    }
}
