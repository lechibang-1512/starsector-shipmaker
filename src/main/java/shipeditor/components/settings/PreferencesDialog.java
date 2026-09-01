package shipeditor.components.settings;

import shipeditor.utility.text.StringManager;

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
        JButton closeButton = new JButton(StringManager.getString("CLOSE"));
        closeButton.addActionListener(e -> this.dispose());
        bottomPanel.add(closeButton);
        this.add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        Settings settings = SettingsManager.getSettings();

        JCheckBox autoLoadData = new JCheckBox(StringManager.getString("AUTO_LOAD_DATA_AT_START"));
        autoLoadData.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        autoLoadData.setToolTipText(StringManager.getString("AUTOMATICALLY_LOAD_INDEXED_MOD_DATA_WHEN_THE_EDITOR_STARTS"));
        autoLoadData.setSelected(SettingsManager.isDataAutoloadEnabled());
        autoLoadData.addActionListener(event ->
                settings.setLoadDataAtStart(autoLoadData.isSelected())
        );
        panel.add(autoLoadData);

        JCheckBox togglePromptMods = new JCheckBox(StringManager.getString("ALWAYS_SHOW_MOD_SELECTION_AT_STARTUP"));
        togglePromptMods.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        togglePromptMods.setToolTipText(StringManager.getString("FORCE_THE_MOD_SELECTION_DIALOG_TO_APPEAR_EVERY_TIME_YOU_OPEN_THE_EDITOR"));
        togglePromptMods.setSelected(settings.isPromptForModsAtStart());
        togglePromptMods.addActionListener(event ->
                settings.setPromptForModsAtStart(togglePromptMods.isSelected())
        );
        panel.add(togglePromptMods);

        JCheckBox toggleFileErrorPopups = new JCheckBox(StringManager.getString("ENABLE_FILE_ERROR_POP_UPS"));
        toggleFileErrorPopups.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        toggleFileErrorPopups.setToolTipText(StringManager.getString("SHOW_ERROR_DIALOGS_WHEN_CORRUPTED_FILES_ARE_ENCOUNTERED_DURING_LOADING"));
        toggleFileErrorPopups.setSelected(SettingsManager.areFileErrorPopupsEnabled());
        toggleFileErrorPopups.addActionListener(event ->
                settings.setShowLoadingErrors(toggleFileErrorPopups.isSelected())
        );
        panel.add(toggleFileErrorPopups);

        JCheckBox toggleDeveloperMode = new JCheckBox(StringManager.getString("ENABLE_DEVELOPER_MESSAGES_LOGS"));
        toggleDeveloperMode.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        toggleDeveloperMode.setToolTipText(StringManager.getString("ENABLE_EXTENSIVE_DIAGNOSTIC_LOGGING_AND_DEVELOPER_ONLY_WARNINGS"));
        toggleDeveloperMode.setSelected(SettingsManager.isDeveloperModeEnabled());
        toggleDeveloperMode.addActionListener(event ->
                settings.setDeveloperMode(toggleDeveloperMode.isSelected())
        );
        panel.add(toggleDeveloperMode);

        JButton openSettings = new JButton(StringManager.getString("OPEN_SETTINGS_FILE"));
        openSettings.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        openSettings.addActionListener(e -> {
            File settingsPath = SettingsManager.getSettingsPath();
            FileUtilities.openPathInDesktop(settingsPath);
        });
        panel.add(openSettings);

        JButton openEditorFolder = new JButton(StringManager.getString("OPEN_EDITOR_FOLDER"));
        openEditorFolder.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        openEditorFolder.addActionListener(e -> {
            File settingsPath = SettingsManager.getSettingsPath();
            File editorFolder = settingsPath != null ? settingsPath.getParentFile() : null;
            if (editorFolder != null) {
                FileUtilities.openPathInDesktop(editorFolder);
            }
        });
        panel.add(openEditorFolder);

        panel.add(javax.swing.Box.createVerticalStrut(10));

        JPanel blacklistPanel = new JPanel(new BorderLayout(5, 5));
        blacklistPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        blacklistPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(StringManager.getString("MOD_BLACKLIST")));
        javax.swing.JTextArea blacklistArea = new javax.swing.JTextArea(4, 20);
        blacklistArea.setToolTipText(StringManager.getString("COMMA_SEPARATED_LIST_OF_MOD_FOLDER_NAMES_OR_PREFIXES_TO_IGNORE"));
        if (settings.getBlacklistedMods() != null) {
            blacklistArea.setText(String.join(", ", settings.getBlacklistedMods()));
        }
        blacklistArea.getDocument().addDocumentListener(new BlacklistDocumentListener(blacklistArea, settings));
        blacklistPanel.add(new javax.swing.JScrollPane(blacklistArea), BorderLayout.CENTER);
        panel.add(blacklistPanel);
        panel.add(javax.swing.Box.createVerticalStrut(10));

        JButton openModSelectionBtn = new JButton(StringManager.getString("SELECT_MOD_PACKAGES"));
        openModSelectionBtn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        openModSelectionBtn.setToolTipText(StringManager.getString("OPEN_THE_MOD_SELECTION_DIALOG_TO_CHANGE_INDEXED_MODS"));
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
        
        JLabel themeLabel = new JLabel(StringManager.getString("SELECT_APPLICATION_THEME_WILL_TAKE_EFFECT_AFTER_RESTART"));
        themeLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        panel.add(themeLabel);
        panel.add(javax.swing.Box.createVerticalStrut(10));

        Settings settings = SettingsManager.getSettings();
        var themes = Theme.values();
        ButtonGroup buttonGroup = new ButtonGroup();

        for (Theme theme : themes) {
            JRadioButton setTheme = new JRadioButton(theme.getDisplayedName());
            setTheme.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
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

        JLabel authorsLabel = new JLabel(StringManager.getString("AUTHORS_THEVOLKFLOWER"));
        authorsLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        panel.add(authorsLabel);
        
        JLabel startedLabel = new JLabel(StringManager.getString("STARTED_MAY_2026"));
        startedLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        panel.add(startedLabel);
        
        String projectVersion = Main.VERSION;
        JLabel versionLabel = new JLabel(StringManager.getString("CURRENT_VERSION") + projectVersion);
        versionLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        panel.add(versionLabel);

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
                    .map(s -> s.trim())
                    .filter(s -> !s.isEmpty())
                    .toList();
            settings.setBlacklistedMods(list);
        }

        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    }
}
