package oth.shipeditor.menubar;

import oth.shipeditor.Main;
import oth.shipeditor.persistence.Settings;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.utility.themes.Theme;

import javax.swing.*;

public class ApplicationMenu extends JMenu {

    ApplicationMenu() {
        super("Application");
    }

    public void initialize() {
        this.add(ApplicationMenu.createThemeOptions());

        String infoText = "About";
        JMenuItem projectInfo = new JMenuItem(infoText);

        JPanel aboutInfoPanel = new JPanel();
        aboutInfoPanel.setLayout(new BoxLayout(aboutInfoPanel, BoxLayout.PAGE_AXIS));
        aboutInfoPanel.add(new JLabel("Authors: thevolkflower"));
        aboutInfoPanel.add(new JLabel("Started: May 2026"));
        String projectVersion = Main.VERSION;
        aboutInfoPanel.add(new JLabel("Current version: " + projectVersion));

        projectInfo.addActionListener(e -> JOptionPane.showMessageDialog(oth.shipeditor.PrimaryWindow.getInstance(), aboutInfoPanel,
                infoText, JOptionPane.INFORMATION_MESSAGE));
        this.add(projectInfo);
    }

    private static JMenu createThemeOptions() {
        JMenu themeMenu = new JMenu("Theme");

        Settings settings = SettingsManager.getSettings();
        String themeHint = "Will take effect after restart";

        var themes = Theme.values();

        ButtonGroup buttonGroup = new ButtonGroup();

        for (Theme theme : themes) {
            JMenuItem setTheme = new JRadioButtonMenuItem(theme.getDisplayedName());
            setTheme.addActionListener(e -> settings.setTheme(theme));
            setTheme.setToolTipText(themeHint);

            buttonGroup.add(setTheme);

            Theme settingsTheme = settings.getTheme();
            if (settingsTheme == theme) {
                setTheme.setSelected(true);
            }

            themeMenu.add(setTheme);
        }

        return themeMenu;
    }

}
