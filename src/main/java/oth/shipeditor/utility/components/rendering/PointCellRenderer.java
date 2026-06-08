package oth.shipeditor.utility.components.rendering;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import oth.shipeditor.components.viewer.entities.BaseWorldPoint;

import javax.swing.*;
import java.awt.*;

@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class PointCellRenderer extends BoxPanelCellRenderer<BaseWorldPoint> {

    private final JLabel textLabel;

    private final JLabel positionLabel;

    public PointCellRenderer() {
        textLabel = new JLabel();
        positionLabel = new JLabel();

        JPanel leftContainer = getLeftContainer();
        leftContainer.add(textLabel);

        JPanel rightContainer = getRightContainer();
        rightContainer.add(positionLabel);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends BaseWorldPoint> list,
                                                  BaseWorldPoint value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        String text = value.getNameForLabel() + " #" + index + ":";
        String position = value.getPositionText();

        textLabel.setText(text);
        positionLabel.setText(position);

        return this;
    }

}
