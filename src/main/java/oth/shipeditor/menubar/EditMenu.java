package oth.shipeditor.menubar;

import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.viewer.control.CursorSnappingToggled;
import oth.shipeditor.communication.events.viewer.control.PointSelectionModeChange;
import oth.shipeditor.communication.events.viewer.control.RotationRoundingToggled;
import oth.shipeditor.components.viewer.control.ControlPredicates;
import oth.shipeditor.components.viewer.control.PointSelectionMode;
import oth.shipeditor.undo.UndoOverseer;

import javax.swing.*;
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
        KeyStroke keyStrokeToUndo = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK);
        undo.setAccelerator(keyStrokeToUndo);
        this.add(undo);

        JMenuItem redo = new JMenuItem("Redo");
        redo.setAction(UndoOverseer.getRedoAction());
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
