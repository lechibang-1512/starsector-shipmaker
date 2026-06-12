package shipeditor.components.instrument.ship.variant;

import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.ViewerEnums.FireMode;
import shipeditor.components.viewer.painters.points.ship.features.FittedWeaponGroup;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.utility.Utility;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.containers.trees.SortableTree;
import shipeditor.utility.components.rendering.CustomTreeNode;
import shipeditor.utility.components.rendering.SortableTreeCellRenderer;
import shipeditor.utility.text.StringValues;
import shipeditor.utility.themes.Themes;
import shipeditor.utility.components.UIConstants;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;

public class WeaponsTreeCellRenderer extends SortableTreeCellRenderer {
    private final JLabel slotTypeIcon;
    private final JLabel builtInIcon;
    private final JLabel upperRightLabel;
    private final JLabel lowerLeftLabel;
    private final JLabel lowerRightLabel;
    private final VariantWeaponsTree parentTree;

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public WeaponsTreeCellRenderer(SortableTree tree, VariantWeaponsTree parentTree) {
        super(tree);
        this.parentTree = parentTree;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        removeAll();

        setFillBackground(true);

        JPanel upperContainer = new JPanel();
        upperContainer.setOpaque(false);
        upperContainer.setLayout(new BoxLayout(upperContainer, BoxLayout.LINE_AXIS));

        slotTypeIcon = new JLabel();
        builtInIcon = new JLabel();

        JLabel textLabel = getTextLabel();
        textLabel.setBorder(new EmptyBorder(0, 4, 0, 0));

        upperRightLabel = new JLabel();

        JPanel leftContainer = getLeftContainer();
        leftContainer.removeAll();
        leftContainer.add(getIconLabel());
        leftContainer.add(slotTypeIcon);
        leftContainer.add(builtInIcon);
        leftContainer.add(textLabel);

        JPanel rightContainer = getRightContainer();
        rightContainer.add(upperRightLabel);

        ComponentUtilities.layoutAsOpposites(upperContainer, leftContainer, rightContainer, 4);

        this.add(upperContainer);

        JPanel lowerContainer = new JPanel();
        lowerContainer.setOpaque(false);
        lowerContainer.setLayout(new BoxLayout(lowerContainer, BoxLayout.LINE_AXIS));

        lowerLeftLabel = new JLabel();
        lowerRightLabel = new JLabel();

        ComponentUtilities.layoutAsOpposites(lowerContainer, lowerLeftLabel, lowerRightLabel, 4);

        this.add(lowerContainer);
    }

    private WeaponSlotPoint getSlotPoint(InstalledFeature installed) {
        return parentTree.getSlotPainter() != null ? parentTree.getSlotPainter().getSlotByID(installed.getSlotID()) : null;
    }

    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        if (upperRightLabel != null) {
            upperRightLabel.setForeground(fg);
        }
        if (lowerLeftLabel != null) {
            lowerLeftLabel.setForeground(fg);
        }
        if (lowerRightLabel != null) {
            lowerRightLabel.setForeground(fg);
        }
    }

    private void handleGroupAppearance(CustomTreeNode treeNode, FittedWeaponGroup weaponGroup) {
        JLabel iconLabel = getIconLabel();
        JLabel textLabel = getTextLabel();

        Color iconColor = Themes.getIconColor();
        iconLabel.setText("[Group]");
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 2));
        textLabel.setText("Weapon Group " + weaponGroup.getIndexToDisplay());

        setBackgroundNonSelectionColor(Themes.getPanelDarkColor());

        if (weaponGroup.isAutofire()) {
            slotTypeIcon.setText("[Autofire]");
            slotTypeIcon.setForeground(iconColor);
            slotTypeIcon.setOpaque(false);
            slotTypeIcon.setBorder(new EmptyBorder(0, 4, 0, 0));
            slotTypeIcon.setVisible(true);

            treeNode.setFirstLineTip("Autofire: ON");
        }
        if (weaponGroup.getMode() == FireMode.ALTERNATING) {
            builtInIcon.setText("[Alternating]");
            treeNode.setSecondLineTip("Firing mode: ALTERNATING");
        } else {
            builtInIcon.setText("[Linked]");
            treeNode.setSecondLineTip("Firing mode: LINKED");
        }
        builtInIcon.setForeground(iconColor);
        builtInIcon.setVisible(true);

        textLabel.setBorder(UIConstants.EMPTY_BORDER);
    }

    private void handleFeatureAppearance(CustomTreeNode treeNode, InstalledFeature feature) {
        JLabel iconLabel = getIconLabel();
        JLabel textLabel = getTextLabel();

        var slot = getSlotPoint(feature);

        lowerRightLabel.setText(feature.getName());
        treeNode.setFirstLineTip(StringValues.WEAPON_ID + feature.getFeatureID());

        if (feature.isContainedInBuiltIns()) {
            builtInIcon.setText("[Built-in]");
            builtInIcon.setForeground(Themes.getIconColor());
            builtInIcon.setVisible(true);

            treeNode.setSecondLineTip("Built-in: locked in variant");
            textLabel.setBorder(new EmptyBorder(0, 1, 0, 0));
        }

        if (slot == null) {
            setForeground(Themes.getReddishFontColor());
            iconLabel.setText("[Slot Not Found]");
            iconLabel.setForeground(Color.RED);
            iconLabel.setOpaque(false);
            iconLabel.setBorder(new EmptyBorder(1, 0, 0, 0));

            textLabel.setBorder(UIConstants.EMPTY_BORDER);

            treeNode.setSecondLineTip(StringValues.INVALIDATED_SLOT_NOT_FOUND);
        } else {
            slotTypeIcon.setVisible(true);
            WeaponType weaponType = slot.getWeaponType();
            slotTypeIcon.setText("[" + weaponType.getDisplayedName() + "]");
            slotTypeIcon.setForeground(weaponType.getColor());
            slotTypeIcon.setOpaque(false);
            slotTypeIcon.setBorder(null);

            WeaponSize weaponSize = slot.getWeaponSize();
            iconLabel.setText("[" + weaponSize.getDisplayedName() + "]");
            iconLabel.setForeground(Themes.getIconColor());

            if (!slot.canFit(feature)) {
                setForeground(Themes.getReddishFontColor());
                String weaponUnfitForSlot = StringValues.INVALIDATED_WEAPON_UNFIT_FOR_SLOT;
                if (feature.isContainedInBuiltIns()) {
                    weaponUnfitForSlot = Utility.getWithLinebreaks(weaponUnfitForSlot,
                            "Built-in: will appear in game");
                }
                treeNode.setSecondLineTip(weaponUnfitForSlot);
            }
        }
        textLabel.setText(feature.getSlotID());
        upperRightLabel.setText("OP: " + feature.getOPCost());
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        CustomTreeNode treeNode = (CustomTreeNode) value;
        Object object = treeNode.getUserObject();
        JLabel iconLabel = getIconLabel();
        JLabel textLabel = getTextLabel();

        iconLabel.setBorder(UIConstants.EMPTY_BORDER);
        iconLabel.setIcon(null);
        iconLabel.setText("");

        slotTypeIcon.setOpaque(false);
        slotTypeIcon.setBorder(null);
        slotTypeIcon.setIcon(null);
        slotTypeIcon.setText("");
        slotTypeIcon.setVisible(false);

        builtInIcon.setIcon(null);
        builtInIcon.setText("");
        builtInIcon.setVisible(false);

        upperRightLabel.setText("");
        lowerLeftLabel.setText("");
        lowerRightLabel.setText("");

        setBackgroundNonSelectionColor(Themes.getListBackgroundColor());

        treeNode.setSecondLineTip(null);
        treeNode.setFirstLineTip(null);
        textLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        if (object instanceof FittedWeaponGroup checked) {
            this.handleGroupAppearance(treeNode, checked);
        } else if (object instanceof InstalledFeature checked && leaf) {
            this.handleFeatureAppearance(treeNode, checked);
        } else {
            textLabel.setText(" " + value);
        }

        return this;
    }
}
