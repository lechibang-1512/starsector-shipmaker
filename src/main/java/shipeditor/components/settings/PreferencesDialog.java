package shipeditor.components.settings;

import shipeditor.Main;
import shipeditor.parsing.FileUtilities;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.UtilityEnums.Theme;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.File;

public class PreferencesDialog extends JDialog {

    public PreferencesDialog(Frame owner) {
        super(owner, "Preferences", true);
        this.initUI();
    }

    private void initUI() {
        this.setLayout(new BorderLayout());
        this.setSize(600, 450);
        this.setLocationRelativeTo(this.getOwner());

        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("General", this.createGeneralPanel());
        tabbedPane.addTab("Theme", this.createThemePanel());
        tabbedPane.addTab("About", this.createAboutPanel());

        this.add(tabbedPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> this.dispose());
        bottomPanel.add(closeButton);
        this.add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        Settings settings = SettingsManager.getSettings();

        JCheckBox autoLoadData = new JCheckBox("Auto-load data at start");
        autoLoadData.setSelected(SettingsManager.isDataAutoloadEnabled());
        autoLoadData.addActionListener(event ->
                settings.setLoadDataAtStart(autoLoadData.isSelected())
        );
        panel.add(autoLoadData);

        JCheckBox togglePromptMods = new JCheckBox("Always show Mod Selection at startup");
        togglePromptMods.setSelected(settings.isPromptForModsAtStart());
        togglePromptMods.addActionListener(event ->
                settings.setPromptForModsAtStart(togglePromptMods.isSelected())
        );
        panel.add(togglePromptMods);

        JCheckBox toggleFileErrorPopups = new JCheckBox("Enable file error pop-ups");
        toggleFileErrorPopups.setSelected(SettingsManager.areFileErrorPopupsEnabled());
        toggleFileErrorPopups.addActionListener(event ->
                settings.setShowLoadingErrors(toggleFileErrorPopups.isSelected())
        );
        panel.add(toggleFileErrorPopups);

        JCheckBox toggleDeveloperMode = new JCheckBox("Enable developer messages/logs");
        toggleDeveloperMode.setSelected(SettingsManager.isDeveloperModeEnabled());
        toggleDeveloperMode.addActionListener(event ->
                settings.setDeveloperMode(toggleDeveloperMode.isSelected())
        );
        panel.add(toggleDeveloperMode);

        JButton openSettings = new JButton("Open settings file");
        openSettings.addActionListener(e -> {
            File settingsPath = SettingsManager.getSettingsPath();
            FileUtilities.openPathInDesktop(settingsPath);
        });
        panel.add(openSettings);

        JButton openEditorFolder = new JButton("Open editor folder");
        openEditorFolder.addActionListener(e -> {
            File editorFolder = SettingsManager.getSettingsPath().getParentFile();
            FileUtilities.openPathInDesktop(editorFolder);
        });
        panel.add(openEditorFolder);

        panel.add(javax.swing.Box.createVerticalStrut(10));

        JPanel blacklistPanel = new JPanel(new BorderLayout(5, 5));
        blacklistPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Mod Blacklist"));
        javax.swing.JTextArea blacklistArea = new javax.swing.JTextArea(4, 20);
        blacklistArea.setToolTipText("Comma-separated list of mod folder names or prefixes to ignore.");
        if (settings.getBlacklistedMods() != null) {
            blacklistArea.setText(String.join(", ", settings.getBlacklistedMods()));
        }
        blacklistArea.getDocument().addDocumentListener(new BlacklistDocumentListener(blacklistArea, settings));
        blacklistPanel.add(new javax.swing.JScrollPane(blacklistArea), BorderLayout.CENTER);
        panel.add(blacklistPanel);
        panel.add(javax.swing.Box.createVerticalStrut(10));

        JButton openModSelectionBtn = new JButton("Select Mod Packages...");
        openModSelectionBtn.setToolTipText("Open the mod selection dialog to change indexed mods");
        openModSelectionBtn.addActionListener(e -> {
            shipeditor.components.dialogs.ModSelectionDialog dialog = 
                new shipeditor.components.dialogs.ModSelectionDialog(
                    (Frame) javax.swing.SwingUtilities.getWindowAncestor(this)
                );
            if (dialog.showDialog()) {
                shipeditor.parsing.loading.FileLoading.forceReindexAndLoadGameData();
            }
        });
        panel.add(openModSelectionBtn);

        return panel;
    }

    private JPanel createThemePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        panel.add(new JLabel("Select application theme (will take effect after restart):"));

        Settings settings = SettingsManager.getSettings();
        var themes = Theme.values();
        ButtonGroup buttonGroup = new ButtonGroup();

        for (Theme theme : themes) {
            JRadioButton setTheme = new JRadioButton(theme.getDisplayedName());
            setTheme.addActionListener(e -> settings.setTheme(theme));

            buttonGroup.add(setTheme);

            Theme settingsTheme = settings.getTheme();
            if (settingsTheme == theme) {
                setTheme.setSelected(true);
            }

            panel.add(setTheme);
        }

        return panel;
    }

    private JPanel createAboutPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Authors: thevolkflower"));
        panel.add(new JLabel("Started: May 2026"));
        String projectVersion = Main.VERSION;
        panel.add(new JLabel("Current version: " + projectVersion));

        return panel;
    }
    private static final class BlacklistDocumentListener implements javax.swing.event.DocumentListener {
        private final javax.swing.JTextArea blacklistArea;
        private final Settings settings;

        BlacklistDocumentListener(javax.swing.JTextArea blacklistArea, Settings settings) {
            this.blacklistArea = blacklistArea;
            this.settings = settings;
        }

        private void update() {
            String text = blacklistArea.getText();
            java.util.List<String> list = java.util.Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            settings.setBlacklistedMods(list);
        }

        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    }
}
