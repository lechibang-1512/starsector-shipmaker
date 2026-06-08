package oth.shipeditor.menubar;

import lombok.extern.log4j.Log4j2;

import javax.swing.*;

@Log4j2
public final class PrimaryMenuBar extends JMenuBar {

    public PrimaryMenuBar() {
        this.add(PrimaryMenuBar.createFileMenu());
        this.add(PrimaryMenuBar.createEditMenu());
        this.add(PrimaryMenuBar.createViewMenu());
        this.add(PrimaryMenuBar.createLayersMenu());
        this.add(PrimaryMenuBar.createHelpMenu());

        SettingsMenu settings = new SettingsMenu();
        settings.initialize();
        this.add(settings);

        ApplicationMenu application = new ApplicationMenu();
        application.initialize();
        this.add(application);
    }

    private static JMenu createFileMenu() {
        FileMenu fileMenu = new FileMenu();
        fileMenu.initialize();
        return fileMenu;
    }

    private static JMenu createViewMenu() {
        ViewMenu viewMenu = new ViewMenu();
        viewMenu.initialize();
        return viewMenu;
    }

    private static JMenu createEditMenu() {
        EditMenu editMenu = new EditMenu();
        editMenu.initialize();
        return editMenu;
    }

    private static JMenu createLayersMenu() {
        LayersMenu layersMenu = new LayersMenu();
        layersMenu.initialize();
        return layersMenu;
    }

    private static JMenu createHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        
        JMenuItem helpItem = new JMenuItem("Help");
        helpItem.addActionListener(e -> {
            JDialog dialog = new JDialog();
            dialog.setTitle("Help");
            dialog.setModal(false);
            dialog.setSize(600, 400);
            dialog.setLocationRelativeTo(null);
            dialog.add(new oth.shipeditor.components.help.HelpMainPanel());
            dialog.setVisible(true);
        });
        helpMenu.add(helpItem);
        
        JMenuItem logsItem = new JMenuItem("Logs");
        logsItem.addActionListener(e -> {
            JDialog dialog = new JDialog();
            dialog.setTitle("Logs");
            dialog.setModal(false);
            dialog.setSize(800, 300);
            dialog.setLocationRelativeTo(null);
            dialog.add(new oth.shipeditor.components.logging.LogsPanel());
            dialog.setVisible(true);
        });
        helpMenu.add(logsItem);
        
        return helpMenu;
    }

}
