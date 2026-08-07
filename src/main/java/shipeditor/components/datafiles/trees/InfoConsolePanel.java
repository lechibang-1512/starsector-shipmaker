package shipeditor.components.datafiles.trees;

import javax.swing.*;
import java.awt.*;

public class InfoConsolePanel extends JPanel {
    private static InfoConsolePanel instance;
    private final JPanel contentPanel;

    private InfoConsolePanel() {
        this.setLayout(new BorderLayout());
        this.setMinimumSize(new Dimension(100, 150));
        this.setPreferredSize(new Dimension(100, 200));

        // Create a header for the console
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIManager.getColor("Panel.background").darker());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        JLabel titleLabel = new JLabel("Entry Information");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        this.add(headerPanel, BorderLayout.NORTH);

        // Content panel where the info actually goes
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JLabel(""), BorderLayout.CENTER);
        
        this.add(contentPanel, BorderLayout.CENTER);
    }

    public static InfoConsolePanel getInstance() {
        if (instance == null) {
            instance = new InfoConsolePanel();
        }
        return instance;
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
