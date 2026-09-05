package shipeditor.utility.components;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A reusable collapsible section widget. Displays a clickable header bar
 * that toggles the visibility of a content panel.
 */
public class CollapsibleSection extends JPanel {

    private final JPanel contentPanel;
    private final JLabel toggleLabel;
    private boolean collapsed;

    private static final String EXPANDED_ICON = "▼ ";
    private static final String COLLAPSED_ICON = "▶ ";

    public CollapsibleSection(String title, JPanel content) {
        this(title, content, false);
    }

    public CollapsibleSection(String title, JPanel content, boolean startCollapsed) {
        super(new BorderLayout());
        this.contentPanel = content;
        this.collapsed = startCollapsed;

        // Header bar
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color headerBg = UIManager.getColor("TabbedPane.hoverColor");
        if (headerBg == null) {
            headerBg = UIManager.getColor("Panel.background");
            if (headerBg != null) {
                headerBg = headerBg.darker();
            }
        }
        if (headerBg != null) {
            header.setBackground(headerBg);
        }
        header.setOpaque(true);

        toggleLabel = new JLabel(getToggleText(title));
        Font baseFont = toggleLabel.getFont();
        toggleLabel.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D()));

        header.add(toggleLabel);

        Border bottomLine = BorderFactory.createMatteBorder(0, 0, 1, 0,
                UIManager.getColor("Separator.foreground") != null
                        ? UIManager.getColor("Separator.foreground")
                        : Color.GRAY);
        header.setBorder(bottomLine);

        header.addMouseListener(new MouseAdapter() {
            private java.awt.Point pressPt;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    pressPt = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && pressPt != null) {
                    if (pressPt.distanceSq(e.getPoint()) <= 100) {
                        toggleCollapsed(title);
                    }
                    pressPt = null;
                }
            }
        });

        this.add(header, BorderLayout.NORTH);
        this.add(contentPanel, BorderLayout.CENTER);

        contentPanel.setVisible(!collapsed);
    }

    private void toggleCollapsed(String title) {
        collapsed = !collapsed;
        contentPanel.setVisible(!collapsed);
        toggleLabel.setText(getToggleText(title));
        revalidate();
        repaint();
    }

    private String getToggleText(String title) {
        return (collapsed ? COLLAPSED_ICON : EXPANDED_ICON) + title;
    }

    /**
     * Returns the content panel so callers can add components to it.
     */
    public JPanel getContentPanel() {
        return contentPanel;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    @Override
    public Dimension getMaximumSize() {
        // Prevent BoxLayout from stretching this vertically when collapsed.
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
    }
}
