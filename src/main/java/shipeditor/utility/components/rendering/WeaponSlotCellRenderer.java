package shipeditor.utility.components.rendering;

import shipeditor.utility.text.StringManager;

import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.representation.weapon.WeaponEnums.WeaponMount;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.utility.Utility;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;

public class WeaponSlotCellRenderer extends BoxPanelCellRenderer<WeaponSlotPoint> {

    private final JLabel sizeIcon;
    private final JLabel mountIcon;
    private final JLabel colorIcon;
    private final JLabel slotIDText;
    private final JLabel builtInText;
    private final JLabel positionText;

    public WeaponSlotCellRenderer() {
        sizeIcon = new JLabel();
        mountIcon = new JLabel();
        colorIcon = new JLabel();
        slotIDText = new JLabel();
        slotIDText.setBorder(new EmptyBorder(0, 3, 0, 0));
        builtInText = new JLabel();
        builtInText.setBorder(new EmptyBorder(0, 4, 0, 0));

        positionText = new JLabel();

        JPanel leftContainer = getLeftContainer();
        leftContainer.add(sizeIcon);
        leftContainer.add(mountIcon);
        leftContainer.add(colorIcon);
        leftContainer.add(slotIDText);
        leftContainer.add(builtInText);

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
        colorIcon.setText(StringManager.getString("EMPTY_STRING_2") + weaponType.getDisplayedName() + "]");
        colorIcon.setForeground(weaponType.getColor());

        WeaponSize size = value.getWeaponSize();
        sizeIcon.setText(StringManager.getString("EMPTY_STRING_2") + size.getDisplayedName() + "]");

        WeaponMount mount = value.getWeaponMount();
        mountIcon.setText(StringManager.getString("EMPTY_STRING_2") + mount.getDisplayName() + "]");

        Color foreground = list.getForeground();
        if (isSelected) {
            foreground = list.getSelectionForeground();
        }

        sizeIcon.setForeground(foreground);
        mountIcon.setForeground(new Color(170, 170, 170));
        slotIDText.setForeground(foreground);
        positionText.setForeground(foreground);

        slotIDText.setText(value.getId());

        String builtInName = value.getBuiltInWeaponName();
        if (builtInName != null) {
            builtInText.setText(StringManager.getString("EMPTY_STRING_4") + builtInName);
            builtInText.setForeground(isSelected ? foreground : new Color(90, 200, 255));
            builtInText.setVisible(true);
        } else {
            builtInText.setText("");
            builtInText.setVisible(false);
        }

        String slotType = "Type: " + value.getWeaponType();
        String slotMount = "Mount: " + value.getWeaponMount();
        String slotSize = "Size: " + value.getWeaponSize();
        String slotAngle = "Angle: " + value.getAngle() + "°";
        String slotArc = "Arc: " + value.getArc() + "°";
        String builtInInfo = builtInName != null ? "Built-In Weapon: " + builtInName : null;
        String tooltipText = builtInInfo != null
                ? Utility.getWithLinebreaks(value.getId(), slotType, slotMount, slotSize, slotAngle, slotArc, builtInInfo)
                : Utility.getWithLinebreaks(value.getId(), slotType, slotMount, slotSize, slotAngle, slotArc);
        this.setToolTipText(tooltipText);

        positionText.setText(value.getPositionText());

        return this;
    }

}
