package shipeditor.menubar;

import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.ActiveLayerRemovalQueued;
import shipeditor.communication.events.viewer.layers.LayerWasSelected;
import shipeditor.communication.events.viewer.layers.ViewerLayerRemovalConfirmed;
import shipeditor.communication.events.viewer.layers.ships.ShipLayerCreationQueued;
import shipeditor.communication.events.viewer.layers.weapons.WeaponLayerCreationQueued;
import shipeditor.components.datafiles.trees.ShipFilterPanel;
import shipeditor.components.datafiles.trees.WeaponFilterPanel;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.undo.UndoOverseer;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.themes.Themes;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

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
            Object[] options = {"Ship Layer", "Weapon Layer"};
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

        this.addSeparator();

        JButton shipFiltersButton = new JButton();
        this.styleToolbarButton(shipFiltersButton, BoxiconsRegular.FILTER_ALT);
        shipFiltersButton.setToolTipText("Ship Filters");
        shipFiltersButton.addActionListener(e -> {
            JDialog dialog = new JDialog();
            dialog.setTitle("Ship Filters");
            dialog.setModal(false);
            dialog.setSize(400, 600);
            dialog.setLocationRelativeTo(null);
            dialog.add(new ShipFilterPanel());
            dialog.setVisible(true);
        });
        this.add(shipFiltersButton);

        JButton weaponFiltersButton = new JButton();
        this.styleToolbarButton(weaponFiltersButton, BoxiconsRegular.FILTER);
        weaponFiltersButton.setToolTipText("Weapon Filters");
        weaponFiltersButton.addActionListener(e -> {
            JDialog dialog = new JDialog();
            dialog.setTitle("Weapon Filters");
            dialog.setModal(false);
            dialog.setSize(400, 600);
            dialog.setLocationRelativeTo(null);
            dialog.add(new WeaponFilterPanel());
            dialog.setVisible(true);
        });
        this.add(weaponFiltersButton);

        this.addSeparator();

        JButton reloadDataButton = new JButton();
        this.styleToolbarButton(reloadDataButton, BoxiconsRegular.REFRESH);
        reloadDataButton.setToolTipText("Reload all game data");
        reloadDataButton.addActionListener(event -> FileLoading.loadGameData());
        this.add(reloadDataButton);
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
