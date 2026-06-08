package oth.shipeditor.components;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.BusEventListener;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.components.*;
import oth.shipeditor.communication.events.viewer.layers.LayerWasSelected;
import oth.shipeditor.components.datafiles.GameDataPanel;
import oth.shipeditor.components.instrument.AbstractInstrumentsPane;
import oth.shipeditor.components.instrument.ship.ShipInstrumentsPane;
import oth.shipeditor.components.instrument.weapon.WeaponInstrumentsPane;
import oth.shipeditor.components.instrument.projectile.ProjectileInstrumentsPane;
import oth.shipeditor.components.viewer.LayerViewer;
import oth.shipeditor.components.viewer.layers.ViewerLayer;
import oth.shipeditor.components.viewer.layers.weapon.WeaponLayer;
import oth.shipeditor.components.viewer.layers.weapon.ProjectileLayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;


/**
 * Main editor container.
 * Outer JSplitPane splits horizontally: GameDataPanel (left) | secondaryLevel (right).
 * secondaryLevel JSplitPane splits horizontally: Canvas/Viewer (left) | InstrumentsPane (right).
 *
 * @author CyberKitsune
 */
@Log4j2
final class TripleSplitContainer extends JSplitPane {

    private ShipInstrumentsPane shipInstrumentsPane;

    private WeaponInstrumentsPane weaponInstrumentsPane;

    private ProjectileInstrumentsPane projectileInstrumentsPane;

    private JSplitPane secondaryLevel;


    TripleSplitContainer() {
        super(JSplitPane.HORIZONTAL_SPLIT);
        this.setOneTouchExpandable(true);
        this.setContinuousLayout(true);
        this.initEventListeners();
    }

    private void initEventListeners() {
        BusEventListener layerSelectListener = event -> {
            if (event instanceof LayerWasSelected checked) {
                ViewerLayer selected = checked.selected();
                if (selected instanceof WeaponLayer) {
                    if (!(secondaryLevel.getRightComponent() instanceof WeaponInstrumentsPane)) {
                        secondaryLevel.setRightComponent(weaponInstrumentsPane);
                        relocateDivider();
                    }
                } else if (selected instanceof ProjectileLayer) {
                    if (!(secondaryLevel.getRightComponent() instanceof ProjectileInstrumentsPane)) {
                        secondaryLevel.setRightComponent(projectileInstrumentsPane);
                        relocateDivider();
                    }
                } else {
                    if (!(secondaryLevel.getRightComponent() instanceof ShipInstrumentsPane)) {
                        secondaryLevel.setRightComponent(shipInstrumentsPane);
                        relocateDivider();
                    }
                }
            }
        };

        EventBus.subscribe(layerSelectListener);

        BusEventListener entrySelectListener = event -> {
            if (event instanceof SelectWeaponDataEntry || event instanceof SelectShipDataEntry) {
                // Ensure Game Data panel on the left is expanded when a data entry is selected
                int currentWidth = this.getWidth();
                if (currentWidth > 0 && this.getDividerLocation() < (currentWidth * 0.1)) {
                    this.setDividerLocation(0.25);
                }
            }
        };

        EventBus.subscribe(entrySelectListener);

        BusEventListener splitterResizeListener = event -> {
            if (event instanceof InstrumentSplitterResized checked) {
                boolean minimized = checked.minimized();
                AbstractInstrumentsPane instrumentsPane = checked.source();
                if (instrumentsPane != null) {
                    instrumentsPane.setInstrumentPaneMinimized(minimized);
                }
                relocateDivider();
            }
        };

        EventBus.subscribe(splitterResizeListener);
    }



    private void relocateDivider() {
        if (secondaryLevel == null) return;
        int width = secondaryLevel.getWidth();
        if (width <= 0) return;

        Component rightComp = secondaryLevel.getRightComponent();
        if (rightComp instanceof AbstractInstrumentsPane instrumentsPane) {
            boolean minimized = instrumentsPane.isInstrumentPaneMinimized();

            int remainder = 125;
            if (rightComp instanceof WeaponInstrumentsPane || rightComp instanceof ProjectileInstrumentsPane) {
                remainder = 70;
            }
            if (minimized) {
                secondaryLevel.setDividerLocation(width - remainder);
                secondaryLevel.setEnabled(false);
            } else {
                secondaryLevel.setEnabled(true);
                int targetWidth = instrumentsPane.getTargetWidth();
                if (targetWidth >= width) {
                    targetWidth = width / 3;
                }
                secondaryLevel.setDividerLocation(width - targetWidth);
            }
        }
    }

    void loadContentPanes(LayerViewer shipView) {
        this.shipInstrumentsPane = new ShipInstrumentsPane();
        this.weaponInstrumentsPane = new WeaponInstrumentsPane();
        this.projectileInstrumentsPane = new ProjectileInstrumentsPane();

        GameDataPanel gameDataPanel = new GameDataPanel();
        gameDataPanel.setMinimumSize(new Dimension(150, 100));
        gameDataPanel.setPreferredSize(new Dimension(350, 0));

        secondaryLevel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        secondaryLevel.setOneTouchExpandable(true);
        secondaryLevel.setContinuousLayout(true);
        secondaryLevel.setResizeWeight(1.0); // Canvas gets all the resize

        Component viewerComponent = (Component) shipView;
        viewerComponent.setMinimumSize(new Dimension(150, 100));

        secondaryLevel.setLeftComponent(viewerComponent);
        secondaryLevel.setRightComponent(shipInstrumentsPane);

        // Track divider movement to update targetWidth of instruments pane
        secondaryLevel.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            Component rightComp = secondaryLevel.getRightComponent();
            if (rightComp instanceof AbstractInstrumentsPane instrumentsPane) {
                if (!instrumentsPane.isInstrumentPaneMinimized()) {
                    int width = secondaryLevel.getWidth() - secondaryLevel.getDividerLocation();
                    if (width > 150) {
                        instrumentsPane.setTargetWidth(width);
                    }
                }
            }
        });

        // Track resize to ensure correct divider location during initial layouts
        secondaryLevel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relocateDivider();
            }
        });

        this.setLeftComponent(gameDataPanel);
        this.setRightComponent(secondaryLevel);
        this.setResizeWeight(0.0); // Sidebar stays same size, right level gets all resize
        this.setDividerLocation(350);
    }

}
