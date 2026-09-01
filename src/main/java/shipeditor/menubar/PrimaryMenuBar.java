package shipeditor.menubar;

import shipeditor.utility.text.StringManager;

import lombok.extern.log4j.Log4j2;
import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.utility.themes.Themes;

import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

@Log4j2
public final class PrimaryMenuBar extends JMenuBar {

    public PrimaryMenuBar() {
        this.add(PrimaryMenuBar.createFileMenu());
        this.add(PrimaryMenuBar.createEditMenu());
        this.add(PrimaryMenuBar.createViewMenu());
        this.add(PrimaryMenuBar.createLayerMenu());
        this.add(PrimaryMenuBar.createDataMenu());
        this.add(PrimaryMenuBar.createHelpMenu());
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

    private static JMenu createLayerMenu() {
        LayerMenu layerMenu = new LayerMenu();
        layerMenu.initialize();
        return layerMenu;
    }

    private static JMenu createDataMenu() {
        DataMenu dataMenu = new DataMenu();
        dataMenu.initialize();
        return dataMenu;
    }

    private static JMenu createHelpMenu() {
        JMenu helpMenu = new JMenu(StringManager.getString("HELP"));
        
        JMenuItem helpItem = new JMenuItem(StringManager.getString("HELP"));
        helpItem.setIcon(FontIcon.of(BoxiconsRegular.HELP_CIRCLE, 16, Themes.getIconColor()));
        helpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        helpItem.addActionListener(e -> {
            JDialog dialog = new JDialog();
            dialog.setTitle(StringManager.getString("HELP"));
            dialog.setModal(false);
            dialog.setSize(600, 400);
            dialog.setLocationRelativeTo(null);
            dialog.add(new shipeditor.components.help.HelpMainPanel());
            dialog.setVisible(true);
        });
        helpMenu.add(helpItem);
        
        JMenuItem logsItem = new JMenuItem(StringManager.getString("LOGS"));
        logsItem.setIcon(FontIcon.of(BoxiconsRegular.FILE_BLANK, 16, Themes.getIconColor()));
        logsItem.addActionListener(e -> {
            JDialog dialog = new JDialog();
            dialog.setTitle(StringManager.getString("LOGS"));
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
