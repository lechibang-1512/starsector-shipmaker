package shipeditor.utility.components.rendering;

import com.formdev.flatlaf.ui.FlatLineBorder;
import shipeditor.components.datafiles.entities.OrdnancedCSVEntry;
import shipeditor.representation.RepresentationEnums.HullSize;
import shipeditor.utility.overseers.StaticController;

import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class OrdnancedEntryCellRenderer extends PanelListCellRenderer<OrdnancedCSVEntry>{

    private final JLabel iconLabel;

    protected final JLabel textLabel;

    protected final JLabel ordnanceLabel;

    public OrdnancedEntryCellRenderer() {
        iconLabel = new JLabel();
        iconLabel.setOpaque(true);
        iconLabel.setBorder(new FlatLineBorder(new Insets(2, 2, 2, 2), Color.GRAY));
        iconLabel.setBackground(Color.LIGHT_GRAY);

        textLabel = new JLabel();
        ordnanceLabel = new JLabel();
        this.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(2, 2, 0, 0);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        constraints.fill = GridBagConstraints.VERTICAL;
        this.add(iconLabel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 4, 0, 0);
        constraints.weightx = 1;
        constraints.gridheight = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        this.add(textLabel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 1;
        this.add(ordnanceLabel, constraints);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends OrdnancedCSVEntry> list, OrdnancedCSVEntry value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        Color foreground = list.getForeground();
        if (isSelected) {
            foreground = list.getSelectionForeground();
        }

        textLabel.setForeground(foreground);
        ordnanceLabel.setForeground(foreground);

        JLabel label = value.getIconLabel();
        if (label != null) {
            iconLabel.setIcon(label.getIcon());
            iconLabel.setText(label.getText());
        } else {
            iconLabel.setIcon(null);
            iconLabel.setText("?");
        }

        textLabel.setText(value.getEntryName());

        HullSize size = StaticController.getSizeOfActiveLayer();
        if (size != null) {
            int ordnanceCost = value.getOrdnanceCost(size);
            ordnanceLabel.setText("OP: " + ordnanceCost);
        }

        this.setToolTipText(value.getMultilineTooltip());

        return this;
    }

}
