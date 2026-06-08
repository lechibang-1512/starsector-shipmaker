package oth.shipeditor.utility.components.rendering;

import oth.shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import oth.shipeditor.representation.weapon.WeaponSize;
import oth.shipeditor.representation.weapon.WeaponType;
import oth.shipeditor.utility.Utility;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class WeaponSlotCellRenderer extends BoxPanelCellRenderer<WeaponSlotPoint> {

    private final JLabel sizeIcon;
    private final JLabel colorIcon;

    private final JLabel slotIDText;

    private final JLabel positionText;

    public WeaponSlotCellRenderer() {
        colorIcon = new JLabel();
        sizeIcon = new JLabel();

        positionText = new JLabel();
        slotIDText = new JLabel();
        slotIDText.setBorder(new EmptyBorder(0, 4, 0, 0));

        JPanel leftContainer = getLeftContainer();
        leftContainer.add(sizeIcon);
        leftContainer.add(colorIcon);
        leftContainer.add(slotIDText);

        JPanel rightContainer = getRightContainer();
        rightContainer.add(positionText);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends WeaponSlotPoint> list,
                                                   WeaponSlotPoint value, int index,
                                                   boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        setToolTipText(null);

        WeaponType weaponType = value.getWeaponType();
        colorIcon.setText("[" + weaponType.getDisplayedName() + "]");
        colorIcon.setForeground(weaponType.getColor());

        WeaponSize size = value.getWeaponSize();
        sizeIcon.setText("[" + size.getDisplayedName() + "]");

        Color foreground = list.getForeground();
        if (isSelected) {
            foreground = list.getSelectionForeground();
        }

        slotIDText.setForeground(foreground);
        positionText.setForeground(foreground);

        String slotType = "Type: " + value.getWeaponType();
        String slotMount = "Mount: " + value.getWeaponMount();
        String slotSize = "Size: " + value.getWeaponSize();
        String slotAngle = "Angle: " + value.getAngle();
        String slotArc = "Arc: " + value.getArc();
        String tooltipText = Utility.getWithLinebreaks(slotType, slotMount, slotSize, slotAngle, slotArc);
        this.setToolTipText(tooltipText);

        slotIDText.setText(value.getId() + ":");
        positionText.setText(value.getPositionText());

        return this;
    }

}
