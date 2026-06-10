package shipeditor.menubar;

import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.control.CursorSnappingToggled;
import shipeditor.communication.events.viewer.control.PointSelectionModeChange;
import shipeditor.communication.events.viewer.control.RotationRoundingToggled;
import shipeditor.components.viewer.control.ControlPredicates;
import shipeditor.components.viewer.control.PointSelectionMode;
import shipeditor.undo.UndoOverseer;
import shipeditor.utility.themes.Themes;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

class EditMenu extends JMenu {

    private JCheckBoxMenuItem toggleCursorSnap;

    private JCheckBoxMenuItem toggleRotationRounding;

    EditMenu() {
        super("Edit");
    }

    void initialize() {
        JMenuItem undo = new JMenuItem("Undo");
        undo.setAction(UndoOverseer.getUndoAction());
        undo.setIcon(FontIcon.of(BoxiconsRegular.UNDO, 16, Themes.getIconColor()));
        KeyStroke keyStrokeToUndo = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK);
        undo.setAccelerator(keyStrokeToUndo);
        this.add(undo);

        JMenuItem redo = new JMenuItem("Redo");
        redo.setAction(UndoOverseer.getRedoAction());
        redo.setIcon(FontIcon.of(BoxiconsRegular.REDO, 16, Themes.getIconColor()));
        KeyStroke keyStrokeToRedo = KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK);
        redo.setAccelerator(keyStrokeToRedo);
        this.add(redo);

        this.addSeparator();

        JMenuItem pointSelectionMode = EditMenu.createPointSelectionModeOptions();
        this.add(pointSelectionMode);

        JCheckBoxMenuItem toggleSelectionHold = new JCheckBoxMenuItem("Toggle selection holding");
        toggleSelectionHold.setSelected(true);
        toggleSelectionHold.setToolTipText("Enables CTRL-hold to prevent mouse motion from changing selection.");
        toggleSelectionHold.addActionListener(event ->
                ControlPredicates.setSelectionHoldingEnabled(toggleSelectionHold.isSelected())
        );
        this.add(toggleSelectionHold);

        toggleCursorSnap = new JCheckBoxMenuItem("Toggle cursor snapping");
        toggleCursorSnap.setSelected(true);
        toggleCursorSnap.addActionListener(event ->
                EventBus.publish(new CursorSnappingToggled(toggleCursorSnap.isSelected()))
        );
        EventBus.subscribe(this, event -> {
            if (event instanceof CursorSnappingToggled checked) {
                toggleCursorSnap.setSelected(checked.toggled());
            }
        });
        this.add(toggleCursorSnap);

        toggleRotationRounding = new JCheckBoxMenuItem("Toggle rotation rounding");
        toggleRotationRounding.setSelected(true);
        toggleRotationRounding.addActionListener(event ->
                EventBus.publish(new RotationRoundingToggled(toggleRotationRounding.isSelected()))
        );
        EventBus.subscribe(this, event -> {
            if (event instanceof RotationRoundingToggled checked) {
                toggleRotationRounding.setSelected(checked.toggled());
            }
        });
        this.add(toggleRotationRounding);

        this.addSeparator();

        JMenuItem preferences = new JMenuItem("Preferences");
        preferences.setIcon(FontIcon.of(BoxiconsRegular.COG, 16, Themes.getIconColor()));
        preferences.addActionListener(e -> {
            shipeditor.components.settings.PreferencesDialog dialog = new shipeditor.components.settings.PreferencesDialog(shipeditor.PrimaryWindow.getInstance());
            dialog.setVisible(true);
        });
        this.add(preferences);
    }

    private static JMenu createPointSelectionModeOptions() {
        JMenu newSubmenu = new JMenu("Point selection mode");

        JMenuItem selectHovered = new JRadioButtonMenuItem("Select clicked");
        selectHovered.addActionListener(e ->
                EventBus.publish(new PointSelectionModeChange(PointSelectionMode.STRICT)));
        newSubmenu.add(selectHovered);

        JMenuItem selectClosest = new JRadioButtonMenuItem("Select closest");
        selectClosest.addActionListener(e ->
                EventBus.publish(new PointSelectionModeChange(PointSelectionMode.CLOSEST)));
        newSubmenu.add(selectClosest);
        selectClosest.setSelected(true);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(selectHovered);
        buttonGroup.add(selectClosest);

        EventBus.subscribe(EditMenu.class, event -> {
            if (event instanceof PointSelectionModeChange checked) {
                if (checked.newMode() == PointSelectionMode.STRICT && !selectHovered.isSelected()) {
                    selectHovered.setSelected(true);
                } else if (checked.newMode() == PointSelectionMode.CLOSEST && !selectClosest.isSelected()) {
                    selectClosest.setSelected(true);
                }
            }
        });

        return newSubmenu;
    }

}
