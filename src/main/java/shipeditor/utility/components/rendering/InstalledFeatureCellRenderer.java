package shipeditor.utility.components.rendering;

import shipeditor.components.datafiles.entities.CSVEntry;
import shipeditor.components.instrument.ship.shared.InstalledFeatureList;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.ViewerEnums.FeatureOverrideState;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import shipeditor.representation.RepresentationEnums.SizeEnum;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.utility.text.StringValues;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;

public class InstalledFeatureCellRenderer extends BoxPanelCellRenderer<InstalledFeature> {

    private final JLabel slotSizeIcon;
    private final JLabel slotTypeIcon;
    private final JLabel slotIDText;

    private final JLabel featureIDText;
    private final JLabel featureTypeIcon;
    private final JLabel featureSizeIcon;

    public InstalledFeatureCellRenderer() {
        slotTypeIcon = new JLabel();
        slotSizeIcon = new JLabel();

        slotIDText = new JLabel();
        slotIDText.setBorder(new EmptyBorder(0, 4, 0, 0));

        JPanel leftContainer = getLeftContainer();
        leftContainer.add(slotSizeIcon);
        leftContainer.add(slotTypeIcon);
        leftContainer.add(slotIDText);

        featureIDText = new JLabel();
        featureTypeIcon = new JLabel();
        featureSizeIcon = new JLabel();

        JPanel rightContainer = getRightContainer();
        rightContainer.add(featureIDText);
        rightContainer.add(featureTypeIcon);
        rightContainer.add(featureSizeIcon);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends InstalledFeature> list,
            InstalledFeature value, int index,
            boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        String slotID = value.getSlotID();

        var slotPainter = InstalledFeatureList.getSlotPainter();
        var slotPoint = slotPainter.getSlotByID(slotID);

        Color foreground = list.getForeground();

        CSVEntry dataEntry = value.getDataEntry();
        this.setToolTipText(dataEntry.getMultilineTooltip());

        if (isSelected) {
            foreground = list.getSelectionForeground();
        }

        if (slotPoint != null) {
            foreground = populateSlotInfo(value, slotPoint, foreground);
        } else {
            Color errorColor = Color.RED;
            this.setWarningText("[Slot Not Found]", errorColor);

            foreground = errorColor;
            setToolTipText(StringValues.INVALIDATED_SLOT_NOT_FOUND);

            slotIDText.setBorder(new EmptyBorder(0, 1, 0, 0));
        }

        slotIDText.setForeground(foreground);
        featureIDText.setForeground(foreground);

        slotIDText.setText(slotID + ":");

        featureIDText.setText(dataEntry.toString());
        featureIDText.setBorder(new EmptyBorder(0, 0, 0, 3));

        WeaponType featureType = value.getWeaponType();
        featureTypeIcon.setText("[" + featureType.getDisplayedName() + "]");
        featureTypeIcon.setForeground(featureType.getColor());
        featureTypeIcon.setVisible(true);

        SizeEnum size = value.getSize();
        String sizeName = size.getDisplayedName();
        featureSizeIcon.setText("[" + sizeName + "]");
        featureSizeIcon.setVisible(true);

        return this;
    }

    private Color populateSlotInfo(InstalledFeature value, WeaponSlotPoint slotPoint,
            Color foreground) {
        Color foregroundColor = foreground;
        WeaponType weaponType = slotPoint.getWeaponType();
        slotTypeIcon.setVisible(true);
        slotTypeIcon.setText("[" + weaponType.getDisplayedName() + "]");
        slotTypeIcon.setForeground(weaponType.getColor());

        slotIDText.setBorder(new EmptyBorder(0, 4, 0, 0));

        WeaponSize size = slotPoint.getWeaponSize();
        slotSizeIcon.setVisible(true);
        slotSizeIcon.setText("[" + size.getDisplayedName() + "]");
        slotSizeIcon.setForeground(foreground);

        if (!slotPoint.canFit(value)) {
            foregroundColor = Color.RED;
            setToolTipText(StringValues.INVALIDATED_WEAPON_UNFIT_FOR_SLOT);
        } else {
            // Read the pre-computed override state from the feature itself.
            FeatureOverrideState state = value.getOverrideState();
            if (state == FeatureOverrideState.REMOVED) {
                Color warnColor = Color.ORANGE;
                this.setWarningText("[Removed]", warnColor);
                setToolTipText("Overridden: slot install removed");
            } else if (state == FeatureOverrideState.OVERRIDDEN) {
                Color warnColor = Color.GREEN;
                this.setWarningText("[Overridden]", warnColor);
                setToolTipText("Overridden: slot install superseded");
            }
        }
        return foregroundColor;
    }

    private void setWarningText(String text, Color color) {
        slotTypeIcon.setText(text);
        slotTypeIcon.setForeground(color);
        slotTypeIcon.setVisible(true);

        slotSizeIcon.setText("");
        slotSizeIcon.setVisible(false);
    }

}
