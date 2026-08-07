package shipeditor.components;
import shipeditor.components.ComponentEnums.CoordsDisplayMode;


import com.formdev.flatlaf.ui.FlatLineBorder;
import lombok.extern.log4j.Log4j2;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.Events;
import shipeditor.communication.events.viewer.control.ControlEvents.ViewerCursorMoved;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerSpriteLoadConfirmed;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.communication.events.viewer.control.ControlEvents.CoordsModeChanged;
import shipeditor.components.viewer.LayerViewer;
import shipeditor.components.viewer.PrimaryViewer;
import shipeditor.components.viewer.control.ControlPredicates;
import shipeditor.components.viewer.control.ViewerControl;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.utility.Utility;
import shipeditor.utility.components.MouseoverLabelListener;
import shipeditor.utility.UtilityEnums.IncrementType;
import shipeditor.utility.components.widgets.Spinners;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.CoordinatesFormatter;
import shipeditor.utility.text.StringValues;
import shipeditor.utility.themes.Themes;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.KeyboardFocusManager;
import javax.swing.JFormattedTextField;
import shipeditor.communication.events.viewer.control.ControlEvents.PointLinkageToleranceChanged;
import shipeditor.communication.events.viewer.control.ControlEvents.ViewerZoomChanged;
import shipeditor.communication.events.viewer.control.ControlEvents.MirrorModeChange;
import shipeditor.communication.events.viewer.control.ControlEvents.ViewerTransformRotated;

@Log4j2
final class ViewerStatusPanel extends JPanel {

    private final LayerViewer viewer;

    private JLabel dimensions;

    private JLabel cursorCoords;

    private SpinnerNumberModel zoomModel;

    private SpinnerNumberModel rotationModel;

    private JPanel leftsideContainer;

    private boolean widgetsAcceptChange;

    private boolean cursorNeedsUpdate;

    ViewerStatusPanel(LayerViewer viewable) {
        this.setLayout(new BorderLayout());

        this.viewer = viewable;
        this.leftsideContainer = createLeftsidePanel();

        this.initListeners();
        this.setDimensionsLabel(null);
        this.setZoomLevel(StaticController.getZoomLevel());
        this.setRotationDegrees(StaticController.getRotationDegrees());
        this.updateCursorCoordsLabel();

        this.add(leftsideContainer, BorderLayout.LINE_START);

        JPanel rightPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.gridx = 0;
        gbcRight.gridy = 0;
        gbcRight.weightx = 1;
        gbcRight.ipadx = 80;
        gbcRight.insets = new Insets(0, 0, 0, 10);
        gbcRight.anchor = GridBagConstraints.LINE_END;

        ProgressBarPanel progressBarContainer = new ProgressBarPanel();
        rightPanel.add(progressBarContainer, gbcRight);

        gbcRight.gridx = 1;
        gbcRight.weightx = 0;
        gbcRight.ipadx = 0;

        rightPanel.add(ViewerStatusPanel.createMirrorModePanel(), gbcRight);

        this.add(rightPanel, BorderLayout.CENTER);
        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Themes.getBorderColor()));

        Timer cursorUpdateTimer = new Timer(16, e -> {
            if (cursorNeedsUpdate) {
                this.updateCursorCoordsLabel();
                cursorNeedsUpdate = false;
            }
        });
        cursorUpdateTimer.setRepeats(true);
        cursorUpdateTimer.start();
    }

    private static JPanel createMirrorModePanel() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.LINE_AXIS));

        int linkageSpinnerMax = 5;
        int linkageSpinnerMin = 0;

        SpinnerNumberModel linkageSpinnerModel = new SpinnerNumberModel(1,
                linkageSpinnerMin, linkageSpinnerMax, 1);
        JSpinner linkageSpinner = Spinners.createUnaryIntegerWheelable(linkageSpinnerModel);

        JLabel toleranceLabel = new JLabel("Distance:");
        toleranceLabel.setToolTipText("Determines maximum distance at which mirrored points link for interaction");

        JCheckBox mirrorModeCheckbox = new JCheckBox("Mirroring");
        mirrorModeCheckbox.addItemListener(e -> {
            boolean mirrorModeOn = mirrorModeCheckbox.isSelected();
            EventBus.publish(new MirrorModeChange(mirrorModeOn));
        });
        mirrorModeCheckbox.setSelected(true);
        EventBus.subscribe(mirrorModeCheckbox, event -> {
            if (event instanceof shipeditor.communication.events.viewer.control.ControlEvents.ViewerRawKeyReleased releasedEvent) {
                int keyCode = releasedEvent.keyEvent().getKeyCode();
                PrimaryViewer viewer = StaticController.getViewer();
                if (viewer != null && viewer.isCursorInViewer() && keyCode == java.awt.event.KeyEvent.VK_SPACE) {
                    mirrorModeCheckbox.setSelected(!mirrorModeCheckbox.isSelected());
                }
            }
        });
        mirrorModeCheckbox.setMnemonic(KeyEvent.VK_SPACE);
        mirrorModeCheckbox.setToolTipText("Spacebar to toggle");

        int margin = 6;

        container.add(mirrorModeCheckbox);
        container.add(Box.createRigidArea(new Dimension(margin,0)));
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(2, 24));
        separator.setForeground(Themes.getBorderColor());
        container.add(separator);
        container.add(Box.createRigidArea(new Dimension(margin,0)));
        container.add(toleranceLabel);
        container.add(Box.createRigidArea(new Dimension(margin,0)));
        container.add(linkageSpinner);
        container.add(Box.createRigidArea(new Dimension(margin,0)));

        linkageSpinner.addChangeListener(e -> {
            int current = (Integer) linkageSpinnerModel.getValue();
            EventBus.publish(new PointLinkageToleranceChanged(current));
        });
        linkageSpinner.setValue(5);

        return container;
    }

    private JPanel createLeftsidePanel() {
        leftsideContainer = new JPanel();

        dimensions = new JLabel("", SwingConstants.TRAILING);
        dimensions.setToolTipText("Width / height of active layer");
        leftsideContainer.add(dimensions);

        this.addSeparator();

        cursorCoords = new JLabel("", SwingConstants.TRAILING);
        Insets coordsInsets = new Insets(2, 6, 2, 7);
        cursorCoords.setBorder(new FlatLineBorder(coordsInsets, Themes.getBorderColor()));
        cursorCoords.setToolTipText("Right-click to change coordinate system");
        JPopupMenu coordsMenu = this.createCoordsMenu();
        cursorCoords.addMouseListener(new MouseoverLabelListener(coordsMenu,
                cursorCoords));
        leftsideContainer.add(cursorCoords);

        this.addSeparator();

        this.addZoomWidget();

        this.addSeparator();

        JLabel rotationLabel = new JLabel("Rotation:", SwingConstants.TRAILING);

        double minimum = 0.0d;
        double maximum = 360.0d;
        double initial = 0.0d;
        rotationModel = new SpinnerNumberModel(initial, minimum, maximum, 1.0d);
        JSpinner rotationSpinner = createRotationSpinner();

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(rotationSpinner,"0°");
        rotationSpinner.setEditor(editor);

        JPopupMenu rotationResetMenu = new JPopupMenu();
        JMenuItem resetRotation = new JMenuItem("Reset rotation degrees");
        resetRotation.addActionListener(e -> rotationSpinner.setValue(initial));
        rotationResetMenu.add(resetRotation);

        JFormattedTextField formattedField = editor.getTextField();
        formattedField.setColumns(2);

        MouseListener labelResetListener = new ResetMenuListener(rotationResetMenu, rotationLabel);
        rotationLabel.addMouseListener(labelResetListener);
        MouseListener spinnerResetListener = new ResetMenuListener(rotationResetMenu, formattedField);
        formattedField.addMouseListener(spinnerResetListener);

        String rotationTooltip = Utility.getWithLinebreaks("CTRL+Mousewheel to rotate viewer",
                StringValues.RIGHT_CLICK_TO_RESET_VALUE);
        rotationLabel.setToolTipText(rotationTooltip);

        leftsideContainer.add(rotationLabel);
        leftsideContainer.add(rotationSpinner);

        return leftsideContainer;
    }

    private JSpinner createRotationSpinner() {
        JSpinner rotationSpinner = Spinners.createWheelable(rotationModel, IncrementType.UNARY);
        rotationSpinner.addChangeListener(e -> {
            if (widgetsAcceptChange) {
                Number modelNumber = rotationModel.getNumber();
                double currentValue = modelNumber.doubleValue();

                PrimaryViewer primaryViewer = StaticController.getViewer();
                ViewerControl viewerControls = primaryViewer.getViewerControls();
                viewerControls.rotateExact(currentValue);
            }
        });
        return rotationSpinner;
    }

    private void addZoomWidget() {
        JLabel zoomLabel = new JLabel("Zoom:", SwingConstants.TRAILING);
        String zoomTooltip = Utility.getWithLinebreaks("Mousewheel to zoom viewer",
                StringValues.RIGHT_CLICK_TO_RESET_VALUE);
        zoomLabel.setToolTipText(zoomTooltip);

        double minimum = ControlPredicates.MINIMUM_ZOOM;
        double maximum = ControlPredicates.MAXIMUM_ZOOM;
        double initial = 1.0d;
        zoomModel = new SpinnerNumberModel(initial, minimum, maximum, 0.1);
        JSpinner zoomSpinner = createZoomSpinner();

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(zoomSpinner,"0%");
        zoomSpinner.setEditor(editor);

        zoomSpinner.addMouseWheelListener(e -> {
            if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                return;
            }
            double value = (Double) zoomSpinner.getValue();
            double newValue = value * Math.pow(1 + (ControlPredicates.ZOOMING_SPEED * 0.1d), -e.getUnitsToScroll());
            newValue = Math.min(maximum, Math.max(minimum, newValue));
            zoomSpinner.setValue(newValue);
        });

        JPopupMenu zoomResetMenu = new JPopupMenu();
        JMenuItem resetZoom = new JMenuItem("Reset zoom level");
        resetZoom.addActionListener(e -> zoomSpinner.setValue(initial));
        zoomResetMenu.add(resetZoom);

        JFormattedTextField formattedField = editor.getTextField();
        formattedField.setColumns(4);
        MouseListener labelResetListener = new ResetMenuListener(zoomResetMenu, zoomLabel);
        zoomLabel.addMouseListener(labelResetListener);
        MouseListener spinnerResetListener = new ResetMenuListener(zoomResetMenu, formattedField);
        formattedField.addMouseListener(spinnerResetListener);

        leftsideContainer.add(zoomLabel);
        leftsideContainer.add(zoomSpinner);
    }

    private JSpinner createZoomSpinner() {
        JSpinner zoomSpinner = new JSpinner(zoomModel);
        zoomSpinner.addChangeListener(e -> {
            if (widgetsAcceptChange) {
                Number modelNumber = zoomModel.getNumber();
                double currentValue = modelNumber.doubleValue();

                PrimaryViewer primaryViewer = StaticController.getViewer();
                ViewerControl viewerControls = primaryViewer.getViewerControls();
                viewerControls.setZoomExact(currentValue);
            }
        });
        return zoomSpinner;
    }

    private void addSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(2, 24));
        separator.setForeground(Themes.getBorderColor());
        leftsideContainer.add(separator);
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private void initListeners() {
        EventBus.subscribe(this, event -> {
            if (event instanceof ViewerZoomChanged) {
                this.setZoomLevel(StaticController.getZoomLevel());
            } else if (event instanceof ViewerTransformRotated) {
                this.setRotationDegrees(StaticController.getRotationDegrees());
            } else if (event instanceof ViewerCursorMoved) {
                this.cursorNeedsUpdate = true;
            } else if (event instanceof LayerSpriteLoadConfirmed checked) {
                Sprite sprite = checked.sprite();
                if (sprite != null) {
                    this.setDimensionsLabel(sprite.getImage());
                } else {
                    this.setDimensionsLabel(null);
                }
            } else if (event instanceof LayerWasSelected checked) {
                ViewerLayer layer = checked.selected();
                if (layer == null) return;
                LayerPainter layerPainter = layer.getPainter();
                if (layerPainter != null) {
                    BufferedImage layerSprite = layerPainter.getSpriteImage();
                    this.setDimensionsLabel(layerSprite);
                } else {
                    this.setDimensionsLabel(null);
                }
            }
        });
    }

    private JRadioButtonMenuItem createCoordsOption(ButtonGroup group,
                                                    CoordsDisplayMode displayMode) {
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(displayMode.getDisplayedText());
        menuItem.addActionListener(e -> {
            menuItem.setSelected(true);
            this.updateCursorCoordsLabel();
            EventBus.publish(new CoordsModeChanged(displayMode));
            Events.repaintShipView();
        });
        group.add(menuItem);
        return menuItem;
    }

    private JPopupMenu createCoordsMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        ButtonGroup group = new ButtonGroup();

        JRadioButtonMenuItem world = createCoordsOption(group, CoordsDisplayMode.WORLD);
        popupMenu.add(world);

        JRadioButtonMenuItem sprite = createCoordsOption(group, CoordsDisplayMode.SPRITE_CENTER);
        popupMenu.add(sprite);

        JRadioButtonMenuItem shipCenterAnchor = createCoordsOption(group, CoordsDisplayMode.SHIPCENTER_ANCHOR);
        popupMenu.add(shipCenterAnchor);

        JRadioButtonMenuItem shipCenter = createCoordsOption(group, CoordsDisplayMode.SHIP_CENTER);
        shipCenter.setSelected(true);
        popupMenu.add(shipCenter);

        return popupMenu;
    }

    private void setDimensionsLabel(BufferedImage sprite) {
        if (sprite != null) {
            dimensions.setText("Size: " + sprite.getWidth() + " × " + sprite.getHeight());
            log.trace("Layer selected: sprite dimensions loaded.");
        } else {
            dimensions.setText("Size: Sprite not loaded.");
        }

    }

    private void updateCursorCoordsLabel() {
        Point2D cursorPoint = StaticController.getCorrectedCursor();
        LayerPainter selectedLayer = this.viewer.getSelectedLayer();
        if (selectedLayer == null || selectedLayer.isUninitialized()) {
            cursorCoords.setText("Coords: " + CoordinatesFormatter.formatDisplay(cursorPoint));
            return;
        }

        CoordsDisplayMode displayMode = StaticController.getCoordsMode();

        if (displayMode == CoordsDisplayMode.SHIP_CENTER || displayMode == CoordsDisplayMode.SHIPCENTER_ANCHOR) {
            if (!(selectedLayer instanceof ShipPainter)) {
                displayMode = CoordsDisplayMode.WORLD;
            }
        }

        Point2D cursor = Utility.getPointCoordinatesForDisplay(cursorPoint, selectedLayer, displayMode);
        
        cursorCoords.setText("Coords: " + CoordinatesFormatter.formatDisplay(cursor));
    }

    private void setZoomLevel(double newZoom) {
        this.widgetsAcceptChange = false;
        zoomModel.setValue(newZoom);
        this.widgetsAcceptChange = true;
    }

    private void setRotationDegrees(double newRotation) {
        this.widgetsAcceptChange = false;
        double value;
        if (ControlPredicates.isRotationRoundingEnabled()) {
            value = Math.round(newRotation);
        } else {
            value = Utility.round(newRotation, 3);
        }
        rotationModel.setValue(value);
        this.widgetsAcceptChange = true;
    }

    private static final class ResetMenuListener extends MouseAdapter {

        private final JPopupMenu resetMenu;
        private final JComponent sourceComponent;

        private ResetMenuListener(JPopupMenu menu, JComponent component) {
            this.resetMenu = menu;
            this.sourceComponent = component;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                resetMenu.show(sourceComponent, e.getX(), e.getY());
            }
        }

    }

}
