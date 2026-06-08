package oth.shipeditor.menubar;

import oth.shipeditor.parsing.FileUtilities;
import oth.shipeditor.persistence.Settings;
import oth.shipeditor.persistence.SettingsManager;

import javax.swing.*;
import java.io.File;

public class SettingsMenu extends JMenu {

    SettingsMenu() {
        super("Settings");
    }

    public void initialize() {
        Settings settings = SettingsManager.getSettings();

        JMenuItem autoLoadData = new JCheckBoxMenuItem("Auto-load data at start");
        autoLoadData.setSelected(SettingsManager.isDataAutoloadEnabled());
        autoLoadData.addActionListener(event ->
                settings.setLoadDataAtStart(autoLoadData.isSelected())
        );
        this.add(autoLoadData);

        JMenuItem toggleFileErrorPopups = new JCheckBoxMenuItem("Enable file error pop-ups");
        toggleFileErrorPopups.setSelected(SettingsManager.areFileErrorPopupsEnabled());
        toggleFileErrorPopups.addActionListener(event ->
                settings.setShowLoadingErrors(toggleFileErrorPopups.isSelected())
        );
        this.add(toggleFileErrorPopups);

        this.addSeparator();

        JMenuItem openSettings = new JMenuItem("Open settings file");
        openSettings.addActionListener(e -> {
            File settingsPath = SettingsManager.getSettingsPath();
            FileUtilities.openPathInDesktop(settingsPath);
        });
        this.add(openSettings);

        JMenuItem openEditorFolder = new JMenuItem("Open editor folder");
        openEditorFolder.addActionListener(e -> {
            File editorFolder = SettingsManager.getSettingsPath().getParentFile();
            FileUtilities.openPathInDesktop(editorFolder);
        });
        this.add(openEditorFolder);
    }

}
