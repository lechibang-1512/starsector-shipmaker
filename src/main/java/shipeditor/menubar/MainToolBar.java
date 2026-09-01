package shipeditor.menubar;

import shipeditor.utility.text.StringManager;

import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.communication.events.viewer.layers.LayerEvents.ViewerLayerRemovalConfirmed;
import shipeditor.communication.events.viewer.control.ControlEvents.CursorSnappingToggled;
import shipeditor.communication.events.viewer.control.ControlEvents.ViewerTransformsReset;
import shipeditor.components.settings.PreferencesDialog;

import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.undo.UndoOverseer;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.themes.Themes;

import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.JToggleButton;

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
        // --- FILE ACTIONS ---
        JButton saveButton = new JButton();
        this.styleToolbarButton(saveButton, BoxiconsRegular.SAVE);
        saveButton.setToolTipText(StringManager.getString("SAVE_ACTIVE_LAYER_CTRL_S"));
        saveButton.addActionListener(e -> shipeditor.parsing.saving.SaveCoordinator.saveActiveLayer());
        this.add(saveButton);

        this.addSeparator();

        // --- HISTORY ---
        JButton undoButton = new JButton();
        undoButton.setAction(UndoOverseer.getUndoAction());
        undoButton.setHideActionText(true);
        this.styleToolbarButton(undoButton, BoxiconsRegular.UNDO);
        undoButton.setToolTipText(StringManager.getString("UNDO_CTRL_Z"));
        this.add(undoButton);

        JButton redoButton = new JButton();
        redoButton.setAction(UndoOverseer.getRedoAction());
        redoButton.setHideActionText(true);
        this.styleToolbarButton(redoButton, BoxiconsRegular.REDO);
        redoButton.setToolTipText(StringManager.getString("REDO_CTRL_Y"));
        this.add(redoButton);

        this.addSeparator();

        // --- LAYER MANAGEMENT ---
        JButton createShipLayerButton = new JButton();
        this.styleToolbarButton(createShipLayerButton, BoxiconsRegular.ROCKET);
        createShipLayerButton.setToolTipText(StringManager.getString("CREATE_NEW_SHIP_LAYER_CTRL_N"));
        createShipLayerButton.addActionListener(event -> EventBus.publish(new ShipLayerCreationQueued()));
        this.add(createShipLayerButton);
        
        JButton createWeaponLayerButton = new JButton();
        this.styleToolbarButton(createWeaponLayerButton, BoxiconsRegular.CROSSHAIR);
        createWeaponLayerButton.setToolTipText(StringManager.getString("CREATE_NEW_WEAPON_LAYER_CTRL_SHIFT_N"));
        createWeaponLayerButton.addActionListener(event -> EventBus.publish(new WeaponLayerCreationQueued()));
        this.add(createWeaponLayerButton);

        JButton removeLayerButton = new JButton();
        this.styleToolbarButton(removeLayerButton, BoxiconsRegular.MINUS);
        removeLayerButton.setToolTipText(StringManager.getString("REMOVE_ACTIVE_LAYER_CTRL_DELETE"));
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

        // --- CANVAS MODES ---
        JToggleButton snapButton = new JToggleButton();
        snapButton.setIcon(FontIcon.of(BoxiconsRegular.MAGNET, 16, Themes.getIconColor()));
        snapButton.setSelectedIcon(FontIcon.of(BoxiconsRegular.MAGNET, 16, Themes.getBrighterSelectionColor()));
        snapButton.setToolTipText(StringManager.getString("TOGGLE_CURSOR_SNAPPING"));
        snapButton.setSelected(true);
        snapButton.setFocusPainted(false);
        snapButton.addActionListener(e -> EventBus.publish(new CursorSnappingToggled(snapButton.isSelected())));
        EventBus.subscribe(this, event -> {
            if (event instanceof CursorSnappingToggled checked) {
                snapButton.setSelected(checked.toggled());
            }
        });
        this.add(snapButton);

        JButton centerViewButton = new JButton();
        this.styleToolbarButton(centerViewButton, BoxiconsRegular.TARGET_LOCK);
        centerViewButton.setToolTipText(StringManager.getString("RESET_VIEW_CENTER_CTRL_0"));
        centerViewButton.addActionListener(event -> EventBus.publish(new ViewerTransformsReset()));
        this.add(centerViewButton);

        this.addSeparator();

        // --- UTILITY ---
        JButton reloadDataButton = new JButton();
        this.styleToolbarButton(reloadDataButton, BoxiconsRegular.REFRESH);
        reloadDataButton.setToolTipText(StringManager.getString("RELOAD_GAME_DATA_F5"));
        reloadDataButton.addActionListener(event -> FileLoading.loadGameData());
        this.add(reloadDataButton);
        
        this.addSeparator();
        
        JButton combinedFiltersBtn = new JButton();
        this.styleToolbarButton(combinedFiltersBtn, BoxiconsRegular.FILTER);
        combinedFiltersBtn.setToolTipText(StringManager.getString("DATA_FILTERS"));
        combinedFiltersBtn.addActionListener(e -> shipeditor.components.datafiles.trees.FilterDialogs.showCombinedFilters(combinedFiltersBtn));
        this.add(combinedFiltersBtn);

        this.addSeparator();

        JButton prefsButton = new JButton();
        this.styleToolbarButton(prefsButton, BoxiconsRegular.COG);
        prefsButton.setToolTipText(StringManager.getString("PREFERENCES_CTRL"));
        prefsButton.addActionListener(event -> {
            PreferencesDialog dialog = new PreferencesDialog(shipeditor.PrimaryWindow.getInstance());
            dialog.setVisible(true);
        });
        this.add(prefsButton);
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
