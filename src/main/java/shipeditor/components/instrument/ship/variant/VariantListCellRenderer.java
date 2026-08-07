package shipeditor.components.instrument.ship.variant;

import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.layers.ship.data.Variant;
import shipeditor.representation.ship.VariantFile;
import shipeditor.utility.themes.Themes;
import org.kordamp.ikonli.swing.FontIcon;
import org.kordamp.ikonli.boxicons.BoxiconsSolid;
import org.kordamp.ikonli.boxicons.BoxiconsRegular;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

public class VariantListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        if (isSelected) {
            panel.setBackground(list.getSelectionBackground());
            panel.setForeground(list.getSelectionForeground());
        } else {
            panel.setBackground(list.getBackground());
            panel.setForeground(list.getForeground());
        }

        if (value instanceof Variant variant) {
            boolean isEmpty = false;
            String variantId = variant.getVariantId();
            String displayName = "";
            boolean isGoal = false;
            boolean isCustom = false;

            if (variant instanceof ShipVariant shipVariant) {
                isEmpty = shipVariant.isEmpty();
                displayName = shipVariant.getDisplayName();
                isGoal = shipVariant.isGoalVariant();
                isCustom = !shipVariant.isLoadedFromFile();
            } else if (variant instanceof VariantFile variantFile) {
                isEmpty = variantFile.isEmpty();
                displayName = variantFile.getDisplayName();
                isGoal = variantFile.isGoalVariant();
                isCustom = false;
            }

            if (isEmpty) {
                JLabel emptyLabel = new JLabel("— No Variant —");
                emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.ITALIC));
                if (isSelected) {
                    emptyLabel.setForeground(list.getSelectionForeground());
                } else {
                    emptyLabel.setForeground(Themes.getDisabledTextColor());
                }
                panel.add(emptyLabel, BorderLayout.CENTER);
            } else {
                JLabel idLabel = new JLabel(variantId);
                idLabel.setFont(idLabel.getFont().deriveFont(Font.BOLD));
                
                JLabel nameLabel = new JLabel(" (" + displayName + ")");
                
                if (isSelected) {
                    idLabel.setForeground(list.getSelectionForeground());
                    nameLabel.setForeground(list.getSelectionForeground());
                } else {
                    idLabel.setForeground(list.getForeground());
                    nameLabel.setForeground(Themes.getDisabledTextColor());
                }

                JPanel centerPanel = new JPanel(new BorderLayout());
                centerPanel.setOpaque(false);
                centerPanel.add(idLabel, BorderLayout.WEST);
                centerPanel.add(nameLabel, BorderLayout.CENTER);
                panel.add(centerPanel, BorderLayout.CENTER);

                JPanel iconPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 2, 0));
                iconPanel.setOpaque(false);

                if (isCustom) {
                    JLabel customIcon = new JLabel(FontIcon.of(BoxiconsSolid.EDIT, 14, Themes.getWarningColor()));
                    customIcon.setToolTipText("Custom variant (not saved to file)");
                    iconPanel.add(customIcon);
                }
                if (isGoal) {
                    JLabel goalIcon = new JLabel(FontIcon.of(BoxiconsSolid.STAR, 14, Themes.getSuccessColor()));
                    goalIcon.setToolTipText("Goal Variant");
                    iconPanel.add(goalIcon);
                }

                if (iconPanel.getComponentCount() > 0) {
                    panel.add(iconPanel, BorderLayout.EAST);
                }
            }
        } else {
            JLabel defaultLabel = new JLabel(value != null ? value.toString() : "");
            panel.add(defaultLabel, BorderLayout.CENTER);
        }

        return panel;
    }
}
