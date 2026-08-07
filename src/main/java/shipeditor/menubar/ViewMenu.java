package shipeditor.menubar;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.ViewerBackgroundChanged;
import shipeditor.communication.events.viewer.control.ControlEvents.ViewerGuidesToggled;
import shipeditor.components.viewer.PaintOrderController;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.graphics.ColorUtilities;
import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.utility.themes.Themes;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import shipeditor.communication.events.viewer.control.ControlEvents.ViewerRotationToggled;
import shipeditor.communication.events.viewer.control.ControlEvents.ViewerTransformsReset;

class ViewMenu extends JMenu {

    private JMenuItem toggleRotate;

    private JMenuItem toggleCursorGuides;
    private JMenuItem toggleBorders;
    private JMenuItem toggleSpriteCenter;

    ViewMenu() {
        super("View");
        this.setMnemonic(KeyEvent.VK_V);
    }

    void initialize() {
        JMenuItem centerView = new JMenuItem("Reset View / Center");
        centerView.setIcon(FontIcon.of(BoxiconsRegular.TARGET_LOCK, 16, Themes.getIconColor()));
        centerView.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        centerView.addActionListener(event -> EventBus.publish(new ViewerTransformsReset()));
        this.add(centerView);

        this.addSeparator();
        
        toggleRotate = new JCheckBoxMenuItem("Toggle view rotation");
        toggleRotate.setSelected(true);
        toggleRotate.addActionListener(event ->
                EventBus.publish(new ViewerRotationToggled(toggleRotate.isSelected(), true))
        );
        EventBus.subscribe(this, event -> {
            if (event instanceof ViewerRotationToggled checked) {
                toggleRotate.setSelected(checked.isSelected());
                toggleRotate.setEnabled(checked.isEnabled());
            }
        });
        this.add(toggleRotate);

        this.addSeparator();

        JMenu bgSubmenu = new JMenu("Background Settings");
        bgSubmenu.setIcon(FontIcon.of(BoxiconsRegular.IMAGE, 16, Themes.getIconColor()));

        JMenuItem changeBackground = ViewMenu.createMenuOption("Change background color",
                event -> {
                    Color chosen = ColorUtilities.showColorChooser();
                    Settings settings = SettingsManager.getSettings();
                    settings.setBackgroundColor(chosen);
                    EventBus.publish(new ViewerBackgroundChanged(chosen));
                });
        bgSubmenu.add(changeBackground);

        JMenuItem displayBackgroundImage = new JCheckBoxMenuItem("Display background image");
        displayBackgroundImage.setSelected(true);
        displayBackgroundImage.addActionListener(e ->
                PaintOrderController.setShowBackgroundImage(displayBackgroundImage.isSelected()));
        bgSubmenu.add(displayBackgroundImage);

        this.add(bgSubmenu);

        JMenu guidesSubmenu = this.createGuidesSubmenu();
        this.add(guidesSubmenu);

        this.addSeparator();

        JMenuItem hideNonBuiltIn = new JCheckBoxMenuItem("Hide non-built-in weapons");
        hideNonBuiltIn.setSelected(false);
        hideNonBuiltIn.addActionListener(e -> {
            PaintOrderController.setHideNonBuiltInWeapons(hideNonBuiltIn.isSelected());
            shipeditor.utility.overseers.StaticController.getViewer().setRepaintQueued();
        });
        this.add(hideNonBuiltIn);
    }

    private void notifyGuidesToggled() {
        EventBus.publish(new ViewerGuidesToggled(toggleCursorGuides.isSelected(),
                toggleBorders.isSelected(), toggleSpriteCenter.isSelected()));
    }

    private JMenu createGuidesSubmenu() {
        JMenu guidesSubmenu = new JMenu("Toggle guides");
        guidesSubmenu.setIcon(FontIcon.of(BoxiconsRegular.GRID_ALT, 16, Themes.getIconColor()));

        toggleCursorGuides = new JCheckBoxMenuItem("Enable cursor guides");
        toggleCursorGuides.setSelected(true);
        toggleCursorGuides.addActionListener(e -> this.notifyGuidesToggled());
        guidesSubmenu.add(toggleCursorGuides);

        toggleBorders = new JCheckBoxMenuItem("Enable sprite borders");
        toggleBorders.setSelected(true);
        toggleBorders.addActionListener(e -> this.notifyGuidesToggled());
        guidesSubmenu.add(toggleBorders);

        toggleSpriteCenter = new JCheckBoxMenuItem("Enable sprite center");
        toggleSpriteCenter.setSelected(true);
        toggleSpriteCenter.addActionListener(e -> this.notifyGuidesToggled());
        guidesSubmenu.add(toggleSpriteCenter);

        return guidesSubmenu;
    }

    static JMenuItem createMenuOption(String text, ActionListener action) {
        JMenuItem newOption = new JMenuItem(text);
        newOption.addActionListener(action);
        return newOption;
    }

}
