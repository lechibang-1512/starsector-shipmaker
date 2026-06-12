package shipeditor.menubar;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.ViewerBackgroundChanged;
import shipeditor.communication.events.viewer.control.ControlEvents.ViewerGuidesToggled;
import shipeditor.components.viewer.PaintOrderController;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.graphics.ColorUtilities;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.Color;
import java.awt.event.ActionListener;
import shipeditor.communication.events.viewer.control.ControlEvents.ViewerRotationToggled;

class ViewMenu extends JMenu {

    private JMenuItem toggleRotate;

    private JMenuItem toggleCursorGuides;
    private JMenuItem toggleBorders;
    private JMenuItem toggleSpriteCenter;
    private JMenuItem toggleAxes;

    ViewMenu() {
        super("View");
    }

    void initialize() {
        JMenuItem changeBackground = ViewMenu.createMenuOption("Change background color",
                event -> {
                    Color chosen = ColorUtilities.showColorChooser();
                    Settings settings = SettingsManager.getSettings();
                    settings.setBackgroundColor(chosen);
                    EventBus.publish(new ViewerBackgroundChanged(chosen));
                });
        this.add(changeBackground);

        JMenuItem displayBackgroundImage = new JCheckBoxMenuItem("Display background image");
        displayBackgroundImage.setSelected(true);
        displayBackgroundImage.addActionListener(e ->
                PaintOrderController.setShowBackgroundImage(displayBackgroundImage.isSelected()));
        this.add(displayBackgroundImage);

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

        JMenu guidesSubmenu = this.createGuidesSubmenu();
        this.add(guidesSubmenu);
    }

    private void notifyGuidesToggled() {
        EventBus.publish(new ViewerGuidesToggled(toggleCursorGuides.isSelected(),
                toggleBorders.isSelected(), toggleSpriteCenter.isSelected(),
                toggleAxes.isSelected()));
    }

    private JMenu createGuidesSubmenu() {
        JMenu guidesSubmenu = new JMenu("Toggle guides");

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

        toggleAxes = new JCheckBoxMenuItem("Enable axis lines");
        toggleAxes.setSelected(true);
        toggleAxes.addActionListener(e -> this.notifyGuidesToggled());
        guidesSubmenu.add(toggleAxes);

        return guidesSubmenu;
    }

    static JMenuItem createMenuOption(String text, ActionListener action) {
        JMenuItem newOption = new JMenuItem(text);
        newOption.addActionListener(action);
        return newOption;
    }

}
