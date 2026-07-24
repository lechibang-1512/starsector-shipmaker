package shipeditor.components.dialogs;

import lombok.extern.log4j.Log4j2;
import shipeditor.PrimaryWindow;
import shipeditor.persistence.Initializations;
import shipeditor.persistence.Settings;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Log4j2
public class FirstTimeSetupDialog extends JDialog {

    private String confirmedPath = null;

    private FirstTimeSetupDialog(String detectedPath, Settings settings) {
        super(PrimaryWindow.getInstance(), "First-Time Setup", true);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.add(new JLabel("Welcome to Starsector Ship Editor!"));
        infoPanel.add(new JLabel("Please select your Starsector installation folder (contains 'starsector-core' and 'mods')."));
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        JTextField pathField = new JTextField(detectedPath != null ? detectedPath : "");
        inputPanel.add(pathField, BorderLayout.CENTER);

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setDialogTitle("Choose folder containing installed game");
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (detectedPath != null && !detectedPath.isEmpty()) {
                folderChooser.setCurrentDirectory(new File(detectedPath));
            }
            int returnVal = folderChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                pathField.setText(folderChooser.getSelectedFile().getAbsolutePath());
            }
        });
        inputPanel.add(browseButton, BorderLayout.EAST);
        mainPanel.add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton("Confirm");
        confirmButton.addActionListener(e -> {
            String currentPath = pathField.getText();
            if (currentPath == null || currentPath.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Path cannot be empty.", "Invalid Path", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Path path = Paths.get(currentPath.trim());
            if (Initializations.checkGameFolderEligibility(path, settings)) {
                confirmedPath = path.toAbsolutePath().toString();
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Selected folder does not contain core and mod data folders.",
                        "Invalid folder",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> this.dispose());

        buttonPanel.add(exitButton);
        buttonPanel.add(confirmButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.add(mainPanel);

        this.pack();
        this.setMinimumSize(new Dimension(500, 200));
        this.setSize(Math.max(500, this.getWidth()), Math.max(200, this.getHeight()));
        this.setLocationRelativeTo(null);
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmedPath = null;
            }
        });
    }

    public static String promptForGameFolder(String detectedPath, Settings settings) {
        FirstTimeSetupDialog dialog = new FirstTimeSetupDialog(detectedPath, settings);
        dialog.setVisible(true);
        if (dialog.confirmedPath == null) {
            log.info("Game folder selection cancelled by user, exiting.");
            System.exit(0);
            return null;
        }
        return dialog.confirmedPath;
    }
}
