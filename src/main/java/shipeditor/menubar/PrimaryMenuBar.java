package shipeditor.menubar;

import lombok.extern.log4j.Log4j2;
import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.utility.themes.Themes;

import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

@Log4j2
public final class PrimaryMenuBar extends JMenuBar {

    public PrimaryMenuBar() {
        JMenu fileMenu = PrimaryMenuBar.createFileMenu();
        fileMenu.setIcon(FontIcon.of(BoxiconsRegular.FILE, 16, Themes.getIconColor()));
        this.add(fileMenu);

        JMenu editMenu = PrimaryMenuBar.createEditMenu();
        editMenu.setIcon(FontIcon.of(BoxiconsRegular.EDIT, 16, Themes.getIconColor()));
        this.add(editMenu);

        JMenu viewMenu = PrimaryMenuBar.createViewMenu();
        viewMenu.setIcon(FontIcon.of(BoxiconsRegular.SHOW, 16, Themes.getIconColor()));
        this.add(viewMenu);

        JMenu dataMenu = PrimaryMenuBar.createDataMenu();
        dataMenu.setIcon(FontIcon.of(BoxiconsRegular.DATA, 16, Themes.getIconColor()));
        this.add(dataMenu);

        JMenu windowMenu = PrimaryMenuBar.createWindowMenu();
        windowMenu.setIcon(FontIcon.of(BoxiconsRegular.WINDOWS, 16, Themes.getIconColor()));
        this.add(windowMenu);

        JMenu helpMenu = PrimaryMenuBar.createHelpMenu();
        helpMenu.setIcon(FontIcon.of(BoxiconsRegular.HELP_CIRCLE, 16, Themes.getIconColor()));
        this.add(helpMenu);
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

    private static JMenu createDataMenu() {
        DataMenu dataMenu = new DataMenu();
        dataMenu.initialize();
        return dataMenu;
    }

    private static JMenu createWindowMenu() {
        WindowMenu windowMenu = new WindowMenu();
        windowMenu.initialize();
        return windowMenu;
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
            dialog.add(new shipeditor.components.help.HelpMainPanel());
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
            dialog.add(new shipeditor.components.logging.LogsPanel());
            dialog.setVisible(true);
        });
        helpMenu.add(logsItem);
        
        return helpMenu;
    }

}
