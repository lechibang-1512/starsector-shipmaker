package shipeditor.components.instrument.ship.centers;

import shipeditor.utility.text.StringManager;

import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.entities.ShipCenterPoint;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.ViewerEnums.PainterVisibility;
import shipeditor.components.viewer.painters.points.AbstractPointPainter;
import shipeditor.components.viewer.painters.points.ship.CenterPointPainter;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.UtilityEnums.IncrementType;
import shipeditor.utility.components.widgets.PointLocationWidget;
import shipeditor.utility.components.widgets.Spinners;
import shipeditor.utility.objects.Pair;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public class CollisionPanel extends AbstractCenterPanel {

    private PointLocationWidget shipCenterWidget;

    private ModuleAnchorPanel moduleAnchorWidget;

    @Override
    public void refreshContent(LayerPainter layerPainter) {
        if (!(layerPainter instanceof ShipPainter shipPainter) || shipPainter.isUninitialized()) {
            fireClearingListeners(layerPainter);
            shipCenterWidget.refresh(null);
            moduleAnchorWidget.setCenterPainter(null);
            moduleAnchorWidget.refresh(null);
            return;
        }

        fireRefresherListeners(layerPainter);
        shipCenterWidget.refresh(layerPainter);
        moduleAnchorWidget.setCenterPainter(((ShipPainter) layerPainter).getCenterPointPainter());
        moduleAnchorWidget.refresh(layerPainter);
    }

    @Override
    protected EditorInstrument getMode() {
        return EditorInstrument.COLLISION;
    }

    @Override
    protected void populateContent() {
        this.setLayout(new BorderLayout());

        JPanel topContainer = new JPanel(new BorderLayout());

        Map<JLabel, JComponent> topWidgets = new LinkedHashMap<>();

        var collisionOpacityWidget = createCollisionOpacityWidget();
        topWidgets.put(collisionOpacityWidget.getFirst(), collisionOpacityWidget.getSecond());

        var collisionVisibilityWidget = createCollisionVisibilityWidget();
        topWidgets.put(collisionVisibilityWidget.getFirst(), collisionVisibilityWidget.getSecond());

        Border bottomPadding = new EmptyBorder(0, 0, 4, 0);

        JPanel topWidgetsPanel = createWidgetsPanel(topWidgets);
        topWidgetsPanel.setBorder(bottomPadding);
        topContainer.add(topWidgetsPanel, BorderLayout.PAGE_START);
        shipCenterWidget = createShipCenterLocationWidget();
        topContainer.add(shipCenterWidget, BorderLayout.CENTER);
        this.add(topContainer, BorderLayout.PAGE_START);

        JPanel centerContainer = new JPanel(new BorderLayout());

        var collisionRadiusWidget = createCollisionRadiusSpinner();
        Map<JLabel, JComponent> centerWidgets = Map.of(
                collisionRadiusWidget.getFirst(), collisionRadiusWidget.getSecond()
        );

        JPanel centerWidgetsPanel = createWidgetsPanel(centerWidgets);
        centerWidgetsPanel.setBorder(bottomPadding);
        centerContainer.add(centerWidgetsPanel, BorderLayout.PAGE_START);

        moduleAnchorWidget = CollisionPanel.createModuleAnchorLocationWidget();

        JPanel moduleAnchorWrapper = new JPanel(new BorderLayout());
        moduleAnchorWrapper.add(moduleAnchorWidget, BorderLayout.PAGE_START);
        centerContainer.add(moduleAnchorWrapper, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new java.awt.GridLayout(0, 2, 4, 4));
        buttonsPanel.setBorder(new EmptyBorder(4, 0, 4, 0));
        javax.swing.JButton autoCalcBtn = new javax.swing.JButton("<html><center>Auto-Calculate<br>Radius</center></html>");
        autoCalcBtn.addActionListener(e -> autoCalculateCollisionRadius());
        
        javax.swing.JButton spriteCenterBtn = new javax.swing.JButton("<html><center>Set to<br>Sprite Center</center></html>");
        spriteCenterBtn.addActionListener(e -> setSpriteCenter());
        
        buttonsPanel.add(autoCalcBtn);
        buttonsPanel.add(spriteCenterBtn);
        centerContainer.add(buttonsPanel, BorderLayout.PAGE_END);

        this.add(centerContainer, BorderLayout.CENTER);
    }

    private void autoCalculateCollisionRadius() {
        LayerPainter layerPainter = getCachedLayerPainter();
        if (!(layerPainter instanceof ShipPainter shipPainter)) return;
        
        CenterPointPainter centerPointPainter = shipPainter.getCenterPointPainter();
        shipeditor.components.viewer.entities.ShipCenterPoint shipCenterPoint = centerPointPainter.getCenterPoint();
        java.awt.geom.Point2D center = shipCenterPoint.getPosition();
        
        java.util.List<shipeditor.components.viewer.entities.BoundPoint> bounds = shipPainter.getBoundsPainter().getPointsIndex();
        float maxDistSq = 0;
        if (!bounds.isEmpty()) {
            for (shipeditor.components.viewer.entities.BoundPoint bp : bounds) {
                float distSq = (float) center.distanceSq(bp.getPosition());
                if (distSq > maxDistSq) maxDistSq = distSq;
            }
            float radius = (float) Math.ceil(Math.sqrt(maxDistSq));
            EditDispatch.postCollisionRadiusChanged(shipCenterPoint, radius);
            processChange();
        } else if (shipPainter.getSprite() != null && shipPainter.getSprite().getImage() != null) {
            float w = shipPainter.getSprite().getImage().getWidth();
            float h = shipPainter.getSprite().getImage().getHeight();
            float radius = (float) Math.ceil(Math.hypot(w, h) / 2.0);
            EditDispatch.postCollisionRadiusChanged(shipCenterPoint, radius);
            processChange();
        }
    }

    private void setSpriteCenter() {
        LayerPainter layerPainter = getCachedLayerPainter();
        if (!(layerPainter instanceof ShipPainter shipPainter)) return;
        if (shipPainter.getSprite() == null || shipPainter.getSprite().getImage() == null) return;
        
        float w = shipPainter.getSprite().getImage().getWidth();
        float h = shipPainter.getSprite().getImage().getHeight();
        java.awt.geom.Point2D anchor = shipPainter.getAnchor();
        
        java.awt.geom.Point2D newCenter = new java.awt.geom.Point2D.Double(anchor.getX() + w / 2.0, anchor.getY() + h / 2.0);
        
        CenterPointPainter centerPointPainter = shipPainter.getCenterPointPainter();
        shipeditor.components.viewer.entities.ShipCenterPoint shipCenterPoint = centerPointPainter.getCenterPoint();
        EditDispatch.postPointDragged(shipCenterPoint, newCenter);
        processChange();
    }

    private Pair<JLabel, JSlider> createCollisionOpacityWidget() {
        BooleanSupplier readinessChecker = this::isWidgetsReadyForInput;
        Consumer<Float> opacitySetter = changedValue -> {
            LayerPainter cachedLayerPainter = getCachedLayerPainter();
            if (cachedLayerPainter != null) {
                CenterPointPainter centerPointPainter = ((ShipPainter) cachedLayerPainter).getCenterPointPainter();
                centerPointPainter.setPaintOpacity(changedValue);
                processChange();
            }
        };

        BiConsumer<JComponent, Consumer<LayerPainter>> clearerListener = this::registerWidgetClearer;
        BiConsumer<JComponent, Consumer<LayerPainter>> refresherListener = this::registerWidgetRefresher;

        Function<LayerPainter, Float> opacityGetter = layerPainter -> {
            CenterPointPainter centerPointPainter = ((ShipPainter) layerPainter).getCenterPointPainter();
            return centerPointPainter.getPaintOpacity();
        };

        Pair<JLabel, JSlider> opacityWidget = ComponentUtilities.createOpacityWidget(readinessChecker,
                opacityGetter, opacitySetter, clearerListener, refresherListener);

        JLabel opacityLabel = opacityWidget.getFirst();
        opacityLabel.setText(StringManager.getString("COLLISION_OPACITY"));

        return opacityWidget;
    }

    private Pair<JLabel, JComboBox<PainterVisibility>> createCollisionVisibilityWidget() {
        Function<LayerPainter, AbstractPointPainter> painterGetter = layerPainter -> {
            if (layerPainter instanceof ShipPainter shipPainter) {
                return shipPainter.getCenterPointPainter();
            }
            return null;
        };

        var opacityWidget = createVisibilityWidget(painterGetter);

        JLabel opacityLabel = opacityWidget.getFirst();
        opacityLabel.setText(StringManager.getString("COLLISION_VIEW"));

        return opacityWidget;
    }

    private Pair<JLabel, JSpinner> createCollisionRadiusSpinner() {
        double minimum = 0.0d;
        double maximum = Double.MAX_VALUE;
        double initial = 0.0d;
        SpinnerNumberModel numberModel = new SpinnerNumberModel(initial, minimum, maximum, 1.0d);

        JSpinner radiusSpinner = Spinners.createWheelable(numberModel, IncrementType.CHUNK);
        radiusSpinner.setEnabled(false);
        JLabel radiusLabel = new JLabel(StringManager.getString("COLLISION_RADIUS"));

        radiusSpinner.addChangeListener(e -> {
            if (!isWidgetsReadyForInput()) return;
            Number modelNumber = numberModel.getNumber();
            double newRadius = modelNumber.doubleValue();

            LayerPainter layerPainter = getCachedLayerPainter();
            ShipPainter shipPainter = (ShipPainter) layerPainter;
            CenterPointPainter centerPointPainter = shipPainter.getCenterPointPainter();
            ShipCenterPoint shipCenterPoint = centerPointPainter.getCenterPoint();
            EditDispatch.postCollisionRadiusChanged(shipCenterPoint, (float) newRadius);
            processChange();
        });

        registerWidgetListeners(radiusSpinner, layer -> {
            numberModel.setValue(0.0d);
            radiusSpinner.setEnabled(false);
        }, layerPainter -> {
            ShipPainter shipPainter = (ShipPainter) layerPainter;
            CenterPointPainter centerPointPainter = shipPainter.getCenterPointPainter();
            ShipCenterPoint shipCenterPoint = centerPointPainter.getCenterPoint();
            double currentRadius = shipCenterPoint.getCollisionRadius();

            numberModel.setValue(currentRadius);
            radiusSpinner.setEnabled(true);
        });

        return new Pair<>(radiusLabel, radiusSpinner);
    }

    private PointLocationWidget createShipCenterLocationWidget() {
        return new ShipCenterLocationWidget(this);
    }

    private static ModuleAnchorPanel createModuleAnchorLocationWidget() {
        return new ModuleAnchorPanel();
    }

}
