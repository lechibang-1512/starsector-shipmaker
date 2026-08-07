package shipeditor.utility.themes;

import shipeditor.utility.graphics.ColorUtilities;

import javax.swing.UIManager;
import java.awt.Color;

public final class Themes {

    private Themes() {
    }

    public static Color getIconColor() {
        Color color = UIManager.getColor("Menu.icon.arrowColor");
        return color != null ? color : Color.LIGHT_GRAY;
    }

    public static Color getDisabledIconColor() {
        Color color = UIManager.getColor("Menu.icon.disabledArrowColor");
        return color != null ? color : Color.GRAY;
    }

    public static Color getBorderColor() {
        return UIManager.getColor("Component.borderColor");
    }

    public static Color getTextColor() {
        Color color = UIManager.getColor("Label.foreground");
        return color != null ? color : Color.WHITE;
    }

    public static Color getDisabledTextColor() {
        return UIManager.getColor("Label.disabledForeground");
    }

    public static Color getCorePackageTextColor() {
        return Themes.getReddishFontColor();
    }

    public static Color getPinnedPackageTextColor() {
        return ColorUtilities.getBlendedColor(Themes.getTextColor(), Color.BLUE, 0.75f);
    }

    public static Color getDarkerBackgroundColor() {
        return ColorUtilities.getBlendedColor(Themes.getPanelDarkColor(), Color.BLACK, 0.15f);
    }

    public static Color getTabBackgroundColor() {
        return ColorUtilities.getBlendedColor(Themes.getTabColor(), Color.BLACK, 0.05f);
    }

    public static Color getBrighterSelectionColor() {
        return UIManager.getColor("Button.default.hoverBackground");
    }

    public static Color getReddishFontColor() {
        return ColorUtilities.getBlendedColor(Themes.getTextColor(), Color.RED, 0.75f);
    }

    public static Color getPanelBackgroundColor() {
        Color color = UIManager.getColor("Panel.background");
        return color != null ? color : Color.DARK_GRAY;
    }

    public static Color getPanelHighlightColor() {
        return UIManager.getColor("TextArea.background");
    }

    public static Color getPanelDarkColor() {
        return ColorUtilities.getBlendedColor(Themes.getPanelBackgroundColor(), Color.BLACK, 0.15f);
    }

    public static Color getListBackgroundColor() {
        return UIManager.getColor("List.background");
    }

    public static Color getListDisabledColor() {
        return UIManager.getColor("ComboBox.disabledBackground");
    }

    private static Color getTabColor() {
        return UIManager.getColor("TabbedPane.background");
    }

    public static void setupColors() {
        UIManager.put("SplitPane.background", Themes.getPanelDarkColor());

        String gripColorID = "SplitPaneDivider.gripColor";
        Color gripColor = UIManager.getColor(gripColorID);
        Color darkerGripColor = ColorUtilities.getBlendedColor(gripColor,
                Color.BLACK, 0.5f);

        String dividerDraggingColorID = "SplitPaneDivider.draggingColor";
        Color dividerDraggingColor = UIManager.getColor(dividerDraggingColorID);
        Color darkDividerDraggingColor = ColorUtilities.getBlendedColor(dividerDraggingColor,
                Color.BLACK, 0.25f);

        UIManager.put(gripColorID, darkerGripColor);
        UIManager.put(dividerDraggingColorID, darkDividerDraggingColor);

        Color selectedTabColor = ColorUtilities.getBlendedColor(Themes.getTabColor(),
                Color.WHITE, 0.05f);
        UIManager.put("TabbedPane.selectedBackground", selectedTabColor);
    }

    public static Color getSuccessColor() {
        Color color = UIManager.getColor("Component.success.focusedBorderColor");
        return color != null ? color : new Color(40, 167, 69);
    }
    
    public static Color getWarningColor() {
        Color color = UIManager.getColor("Component.warning.focusedBorderColor");
        return color != null ? color : new Color(255, 193, 7);
    }

    public static Color getErrorColor() {
        Color color = UIManager.getColor("Component.error.focusedBorderColor");
        return color != null ? color : new Color(220, 53, 69);
    }

}
