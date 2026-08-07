package shipeditor.menubar;

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
import javax.swing.JOptionPane;
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
        saveButton.setToolTipText("Save Active Layer (Ctrl+S)");
        saveButton.addActionListener(e -> {
            LayerManager layerManager = StaticController.getLayerManager();
            if (layerManager != null && layerManager.getActiveLayer() != null) {
                shipeditor.components.viewer.layers.ViewerLayer activeLayer = layerManager.getActiveLayer();
                if (activeLayer instanceof shipeditor.components.viewer.layers.ship.ShipLayer shipLayer) {
                    EventBus.publish(new shipeditor.communication.events.files.FileEvents.HullSaveQueued(shipLayer));
                    if (shipLayer.getPainter() != null && shipLayer.getPainter().getActiveVariant() != null && !shipLayer.getPainter().getActiveVariant().isEmpty()) {
                        EventBus.publish(new shipeditor.communication.events.files.FileEvents.VariantSaveQueued(shipLayer.getPainter().getActiveVariant()));
                    }
                } else if (activeLayer instanceof shipeditor.components.viewer.layers.weapon.WeaponLayer weaponLayer) {
                    EventBus.publish(new shipeditor.communication.events.files.FileEvents.WeaponSaveQueued(weaponLayer));
                }
            } else {
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(), "No active layer to save.", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        });
        this.add(saveButton);

        this.addSeparator();

        // --- HISTORY ---
        JButton undoButton = new JButton();
        undoButton.setAction(UndoOverseer.getUndoAction());
        undoButton.setHideActionText(true);
        this.styleToolbarButton(undoButton, BoxiconsRegular.UNDO);
        undoButton.setToolTipText("Undo (Ctrl+Z)");
        this.add(undoButton);

        JButton redoButton = new JButton();
        redoButton.setAction(UndoOverseer.getRedoAction());
        redoButton.setHideActionText(true);
        this.styleToolbarButton(redoButton, BoxiconsRegular.REDO);
        redoButton.setToolTipText("Redo (Ctrl+Y)");
        this.add(redoButton);

        this.addSeparator();

        // --- LAYER MANAGEMENT ---
        JButton createShipLayerButton = new JButton();
        this.styleToolbarButton(createShipLayerButton, BoxiconsRegular.ROCKET);
        createShipLayerButton.setToolTipText("Create New Ship Layer (Ctrl+N)");
        createShipLayerButton.addActionListener(event -> EventBus.publish(new ShipLayerCreationQueued()));
        this.add(createShipLayerButton);
        
        JButton createWeaponLayerButton = new JButton();
        this.styleToolbarButton(createWeaponLayerButton, BoxiconsRegular.CROSSHAIR);
        createWeaponLayerButton.setToolTipText("Create New Weapon Layer (Ctrl+Shift+N)");
        createWeaponLayerButton.addActionListener(event -> EventBus.publish(new WeaponLayerCreationQueued()));
        this.add(createWeaponLayerButton);

        JButton removeLayerButton = new JButton();
        this.styleToolbarButton(removeLayerButton, BoxiconsRegular.MINUS);
        removeLayerButton.setToolTipText("Remove Active Layer (Ctrl+Delete)");
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
        snapButton.setToolTipText("Toggle Cursor Snapping");
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
        centerViewButton.setToolTipText("Center View on Layer (Ctrl+0)");
        centerViewButton.addActionListener(event -> EventBus.publish(new ViewerTransformsReset()));
        this.add(centerViewButton);

        this.addSeparator();

        // --- UTILITY ---
        JButton reloadDataButton = new JButton();
        this.styleToolbarButton(reloadDataButton, BoxiconsRegular.REFRESH);
        reloadDataButton.setToolTipText("Reload Game Data (F5)");
        reloadDataButton.addActionListener(event -> FileLoading.loadGameData());
        this.add(reloadDataButton);
        
        this.addSeparator();
        
        JButton combinedFiltersBtn = new JButton();
        this.styleToolbarButton(combinedFiltersBtn, BoxiconsRegular.FILTER);
        combinedFiltersBtn.setToolTipText("Data Filters");
        combinedFiltersBtn.addActionListener(e -> shipeditor.components.datafiles.trees.FilterDialogs.showCombinedFilters(combinedFiltersBtn));
        this.add(combinedFiltersBtn);

        this.addSeparator();

        JButton prefsButton = new JButton();
        this.styleToolbarButton(prefsButton, BoxiconsRegular.COG);
        prefsButton.setToolTipText("Preferences (Ctrl+,)");
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
