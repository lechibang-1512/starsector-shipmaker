package shipeditor.menubar;

import lombok.extern.log4j.Log4j2;
import shipeditor.parsing.loading.FileLoading;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

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
