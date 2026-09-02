package shipeditor.menubar;

import shipeditor.utility.text.StringManager;

import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.communication.events.viewer.layers.LayerEvents.ViewerLayerRemovalConfirmed;
import shipeditor.communication.events.viewer.layers.LayerEvents.ShipLayerCreationQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.WeaponLayerCreationQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerRemovalQueued;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.themes.Themes;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

class LayerMenu extends JMenu {

    private JMenuItem removeLayer;

    LayerMenu() {
        super(StringManager.getString("MENU_LAYER"));
        this.setMnemonic(KeyEvent.VK_L);
    }

    void initialize() {
        JMenuItem newShipLayer = new JMenuItem(StringManager.getString("NEW_SHIP_LAYER"));
        newShipLayer.setIcon(FontIcon.of(BoxiconsRegular.ROCKET, 16, Themes.getIconColor()));
        newShipLayer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newShipLayer.addActionListener(e -> EventBus.publish(new ShipLayerCreationQueued()));
        this.add(newShipLayer);

        JMenuItem newWeaponLayer = new JMenuItem(StringManager.getString("NEW_WEAPON_LAYER"));
        newWeaponLayer.setIcon(FontIcon.of(BoxiconsRegular.CROSSHAIR, 16, Themes.getIconColor()));
        newWeaponLayer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        newWeaponLayer.addActionListener(e -> EventBus.publish(new WeaponLayerCreationQueued()));
        this.add(newWeaponLayer);

        this.addSeparator();

        removeLayer = new JMenuItem(StringManager.getString("REMOVE_ACTIVE_LAYER"));
        removeLayer.setIcon(FontIcon.of(BoxiconsRegular.MINUS, 16, Themes.getIconColor()));
        removeLayer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK));
        removeLayer.addActionListener(e -> EventBus.publish(new ActiveLayerRemovalQueued()));
        removeLayer.setEnabled(false);
        this.add(removeLayer);

        EventBus.subscribe(this, event -> {
            LayerManager layerManager = StaticController.getLayerManager();
            if (layerManager != null) {
                if (event instanceof ViewerLayerRemovalConfirmed && layerManager.isEmpty()) {
                    removeLayer.setEnabled(false);
                } else if (event instanceof LayerWasSelected && !layerManager.isEmpty()) {
                    removeLayer.setEnabled(true);
                }
            }
        });
    }
}
