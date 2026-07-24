package shipeditor.menubar;

import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.communication.events.viewer.layers.LayerEvents.ViewerLayerRemovalConfirmed;

import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.undo.UndoOverseer;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.themes.Themes;
import shipeditor.components.datafiles.GameDataReferenceWindow;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import shipeditor.communication.events.viewer.layers.LayerEvents.ShipLayerCreationQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.WeaponLayerCreationQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerRemovalQueued;

public class MainToolBar extends JToolBar {

    public MainToolBar() {
        super();
        this.setFloatable(false);
        this.initUI();
    }

    private void initUI() {
        JButton undoButton = new JButton();
        undoButton.setAction(UndoOverseer.getUndoAction());
        undoButton.setHideActionText(true);
        this.styleToolbarButton(undoButton, BoxiconsRegular.UNDO);
        undoButton.setToolTipText("Undo");
        this.add(undoButton);

        JButton redoButton = new JButton();
        redoButton.setAction(UndoOverseer.getRedoAction());
        redoButton.setHideActionText(true);
        this.styleToolbarButton(redoButton, BoxiconsRegular.REDO);
        redoButton.setToolTipText("Redo");
        this.add(redoButton);

        this.addSeparator();

        JButton createLayerButton = new JButton();
        this.styleToolbarButton(createLayerButton, BoxiconsRegular.PLUS);
        createLayerButton.setToolTipText("Create new layer");
        createLayerButton.addActionListener(event -> {
            Object[] options = { "Ship Layer", "Weapon Layer" };
            int result = JOptionPane.showOptionDialog(null,
                    "Select new layer type:",
                    "Create New Layer",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (result == 0) {
                EventBus.publish(new ShipLayerCreationQueued());
            } else if (result == 1) {
                EventBus.publish(new WeaponLayerCreationQueued());
            }
        });
        this.add(createLayerButton);

        JButton removeLayerButton = new JButton();
        this.styleToolbarButton(removeLayerButton, BoxiconsRegular.MINUS);
        removeLayerButton.setToolTipText("Remove selected layer");
        removeLayerButton.addActionListener(event -> EventBus.publish(new ActiveLayerRemovalQueued()));
        removeLayerButton.setEnabled(false);

        EventBus.subscribe(this, event -> {
            LayerManager layerManager = StaticController.getLayerManager();
            if (layerManager != null) {
                if (event instanceof ViewerLayerRemovalConfirmed && layerManager.isEmpty()) {
                    removeLayerButton.setEnabled(false);
                } else if (event instanceof LayerWasSelected && !layerManager.isEmpty()) {
                    removeLayerButton.setEnabled(true);
                }
            }
        });
        this.add(removeLayerButton);

        JButton reloadDataButton = new JButton();
        this.styleToolbarButton(reloadDataButton, BoxiconsRegular.REFRESH);
        reloadDataButton.setToolTipText("Reload all game data");
        reloadDataButton.addActionListener(event -> FileLoading.loadGameData());
        this.add(reloadDataButton);

        this.addSeparator();

        JButton dataWindowButton = new JButton();
        this.styleToolbarButton(dataWindowButton, BoxiconsRegular.BOOK);
        dataWindowButton.setToolTipText("Show Reference Data");
        dataWindowButton.addActionListener(event -> GameDataReferenceWindow.toggleWindow());
        this.add(dataWindowButton);
    }

    private void styleToolbarButton(JButton button, org.kordamp.ikonli.Ikon icon) {
        button.setIcon(FontIcon.of(icon, 16, Themes.getIconColor()));
        button.setRolloverIcon(FontIcon.of(icon, 16, Themes.getBrighterSelectionColor()));
        button.setPressedIcon(FontIcon.of(icon, 16, Themes.getDarkerBackgroundColor()));
        button.setDisabledIcon(FontIcon.of(icon, 16, Themes.getDisabledIconColor()));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
    }
}
