package shipeditor.components.instrument.ship.variant;

import shipeditor.utility.text.StringManager;

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
import shipeditor.utility.themes.Themes;
import shipeditor.utility.components.UIConstants;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;

import lombok.extern.log4j.Log4j2;

@Log4j2
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
        iconLabel.setText(StringManager.getString("GROUP"));
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 2));
        textLabel.setText(StringManager.getString("WEAPON_GROUP") + weaponGroup.getIndexToDisplay());

        setBackgroundNonSelectionColor(Themes.getPanelDarkColor());

        if (weaponGroup.isAutofire()) {
            slotTypeIcon.setText(StringManager.getString("AUTOFIRE"));
            slotTypeIcon.setForeground(iconColor);
            slotTypeIcon.setOpaque(false);
            slotTypeIcon.setBorder(new EmptyBorder(0, 4, 0, 0));
            slotTypeIcon.setVisible(true);

            treeNode.setFirstLineTip("Autofire: ON");
        }
        if (weaponGroup.getMode() == FireMode.ALTERNATING) {
            builtInIcon.setText(StringManager.getString("ALTERNATING"));
            treeNode.setSecondLineTip("Firing mode: ALTERNATING");
        } else {
            builtInIcon.setText(StringManager.getString("LINKED"));
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
        treeNode.setFirstLineTip(StringManager.getString("WEAPON_ID") + feature.getFeatureID());

        if (feature.isContainedInBuiltIns()) {
            builtInIcon.setText(StringManager.getString("BUILT_IN"));
            builtInIcon.setForeground(Themes.getIconColor());
            builtInIcon.setVisible(true);

            treeNode.setSecondLineTip("Built-in: locked in variant");
            textLabel.setBorder(new EmptyBorder(0, 1, 0, 0));
        }

        if (slot == null) {
            setForeground(Themes.getReddishFontColor());
            iconLabel.setText(StringManager.getString("SLOT_NOT_FOUND"));
            iconLabel.setForeground(Color.RED);
            iconLabel.setOpaque(false);
            iconLabel.setBorder(new EmptyBorder(1, 0, 0, 0));

            textLabel.setBorder(UIConstants.EMPTY_BORDER);

            treeNode.setSecondLineTip(StringManager.getString("INVALIDATED_SLOT_NOT_FOUND"));
        } else {
            slotTypeIcon.setVisible(true);
            WeaponType weaponType = slot.getWeaponType();
            slotTypeIcon.setText(StringManager.getString("EMPTY_STRING_2") + weaponType.getDisplayedName() + "]");
            slotTypeIcon.setForeground(weaponType.getColor());
            slotTypeIcon.setOpaque(false);
            slotTypeIcon.setBorder(null);

            WeaponSize weaponSize = slot.getWeaponSize();
            iconLabel.setText(StringManager.getString("EMPTY_STRING_2") + weaponSize.getDisplayedName() + "]");
            iconLabel.setForeground(Themes.getIconColor());

            if (!slot.canFit(feature)) {
                setForeground(Themes.getReddishFontColor());
                String weaponUnfitForSlot = StringManager.getString("INVALIDATED_WEAPON_UNFIT_FOR_SLOT");
                if (feature.isContainedInBuiltIns()) {
                    weaponUnfitForSlot = Utility.getWithLinebreaks(weaponUnfitForSlot,
                            "Built-in: will appear in game");
                }
                treeNode.setSecondLineTip(weaponUnfitForSlot);
            }
        }
        textLabel.setText(feature.getSlotID());
        upperRightLabel.setText(StringManager.getString("OP") + feature.getOPCost());
    }

    private void handleEmptySlotAppearance(CustomTreeNode treeNode, WeaponSlotPoint slot) {
        JLabel iconLabel = getIconLabel();
        JLabel textLabel = getTextLabel();

        slotTypeIcon.setVisible(true);
        WeaponType weaponType = slot.getWeaponType();
        slotTypeIcon.setText(StringManager.getString("EMPTY_STRING_2") + weaponType.getDisplayedName() + "]");
        slotTypeIcon.setForeground(weaponType.getColor());
        slotTypeIcon.setOpaque(false);
        slotTypeIcon.setBorder(null);

        WeaponSize weaponSize = slot.getWeaponSize();
        iconLabel.setText(StringManager.getString("EMPTY_STRING_2") + weaponSize.getDisplayedName() + "]");
        iconLabel.setForeground(Themes.getIconColor());

        textLabel.setText(slot.getId());
        upperRightLabel.setText(StringManager.getString("EMPTY_STRING_2") + slot.getWeaponMount().getDisplayName() + "]");
        upperRightLabel.setForeground(Themes.getDisabledTextColor());

        lowerRightLabel.setText(StringManager.getString("EMPTY"));
        lowerRightLabel.setForeground(Themes.getDisabledTextColor());

        treeNode.setFirstLineTip("Slot ID: " + slot.getId());
        treeNode.setSecondLineTip("Double-click or right-click to install weapon");
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus) {
        try {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            
            if (!(value instanceof CustomTreeNode treeNode)) {
                return this;
            }
            
            Object object = treeNode.getUserObject();
            if (object == null) {
                return this;
            }
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
            } else if (object instanceof WeaponSlotPoint emptySlot) {
                this.handleEmptySlotAppearance(treeNode, emptySlot);
            } else {
                textLabel.setText(" " + value);
                if (value.toString().startsWith("Unassigned Slots")) {
                    iconLabel.setText(StringManager.getString("EMPTY_SLOTS"));
                    iconLabel.setForeground(Themes.getIconColor());
                    setBackgroundNonSelectionColor(Themes.getPanelDarkColor());
                }
            }

            return this;
        } catch (Exception e) {
            log.error("Silent Swing exception caught during cell render for row {}", row, e);
            getTextLabel().setText(StringManager.getString("RENDER_ERROR"));
            getTextLabel().setForeground(Color.RED);
            return this;
        }
    }
}
