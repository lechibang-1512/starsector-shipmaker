package shipeditor.components.instrument;

import shipeditor.utility.text.StringManager;

import shipeditor.components.viewer.control.ControlPredicates;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.utility.Utility;
import shipeditor.utility.UtilityEnums.IncrementType;
import shipeditor.utility.components.widgets.PointLocationWidget;
import shipeditor.utility.components.widgets.Spinners;
import shipeditor.utility.components.widgets.TwinSpinnerPanel;
import shipeditor.utility.objects.Pair;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LayerCircumstancePanel extends LayerPropertiesPanel {

    private PointLocationWidget locationWidget;

    @Override
    public void refreshContent(LayerPainter layerPainter) {
        if (layerPainter == null) {
            fireClearingListeners(null);
            locationWidget.refresh(null);
            return;
        }

        fireRefresherListeners(layerPainter);
        locationWidget.refresh(layerPainter);
    }

    @Override
    protected void populateContent() {
        this.setLayout(new BorderLayout());
        Map<JLabel, JComponent> widgets = new LinkedHashMap<>();

        var layerOpacityWidget = createLayerOpacitySlider();
        widgets.put(layerOpacityWidget.getFirst(), layerOpacityWidget.getSecond());

        var layerRotationWidget = createLayerRotationSpinner();
        widgets.put(layerRotationWidget.getFirst(), layerRotationWidget.getSecond());

        JPanel widgetsPanel = createWidgetsPanel(widgets);
        this.add(widgetsPanel, BorderLayout.PAGE_START);

        locationWidget = LayerCircumstancePanel.createAnchorLocationWidget();
        this.add(locationWidget, BorderLayout.CENTER);
    }

    private Pair<JLabel, JSlider> createLayerOpacitySlider() {
        Consumer<Float> opacitySetter = changedValue -> {
            LayerPainter layerPainter = getCachedLayerPainter();
            if (layerPainter != null) {
                layerPainter.setSpriteOpacity(changedValue);
            }
            processChange();
        };

        Function<LayerPainter, Float> opacityGetter = a -> a.getSpriteOpacity();

        return super.createOpacityWidget(opacityGetter, opacitySetter);
    }

    private Pair<JLabel, JSpinner> createLayerRotationSpinner() {
        double minimum = 0.0d;
        double maximum = 360.0d;
        double initial = 0.0d;
        SpinnerNumberModel rotationModel = new SpinnerNumberModel(initial, minimum, maximum, 1.0d);

        JSpinner rotationSpinner = Spinners.createWheelable(rotationModel, IncrementType.CHUNK);
        rotationSpinner.setEnabled(false);
        JLabel rotationLabel = new JLabel(StringManager.getString("LAYER_ROTATION"));

        rotationSpinner.addChangeListener(e -> {
            if (isWidgetsReadyForInput()) {
                Number modelNumber = rotationModel.getNumber();
                double newRotation = modelNumber.doubleValue();
                double reversed = (360 - newRotation) % 360;

                LayerPainter layerPainter = getCachedLayerPainter();
                layerPainter.rotateLayer(reversed);
                processChange();
            }
        });

        registerWidgetListeners(rotationSpinner, layer -> {
            rotationModel.setValue(0);
            rotationSpinner.setEnabled(false);
        }, layerPainter -> {
            if (ControlPredicates.isRotationRoundingEnabled()) {
                rotationModel.setStepSize(1.0d);
            } else {
                rotationModel.setStepSize(0.005d);
            }
            double currentRotation = layerPainter.getRotationRadians();

            double currentClamped = Utility.clampAngleWithRounding(currentRotation);
            rotationModel.setValue(currentClamped);
            rotationSpinner.setEnabled(true);
        });

        return new Pair<>(rotationLabel, rotationSpinner);
    }

    private static PointLocationWidget createAnchorLocationWidget() {
        return new LayerAnchorLocationWidget();
    }

    private static class LayerAnchorLocationWidget extends PointLocationWidget {

        @Override
        protected TwinSpinnerPanel createSpinnerPanel(Point2D initialPoint, Consumer<Point2D> pointSetter) {
            TwinSpinnerPanel spinnerPanel = Spinners.createLocationSpinners(initialPoint, retrieveGetter(), pointSetter,
                    StringManager.getString("X_COORDINATE"), StringManager.getString("Y_COORDINATE"), 1.0d);
            spinnerPanel.setToolTipText(StringManager.getString("POINT_LOCATION_IN_WORLD_COORDINATES"));
            return spinnerPanel;
        }

        @Override
        protected boolean isLayerPainterEligible(LayerPainter layerPainter) {
            return layerPainter != null;
        }

        @Override
        protected String getPanelTitleText() {
            return "Anchor position";
        }

        @Override
        protected Supplier<Point2D> retrieveGetter() {
            return () -> {
                LayerPainter cachedLayerPainter = getCachedLayerPainter();
                if (cachedLayerPainter != null) {
                    return cachedLayerPainter.getAnchor();
                }
                return null;
            };
        }

        @Override
        protected Consumer<Point2D> retrieveSetter() {
            return point -> {
                LayerPainter cachedLayerPainter = getCachedLayerPainter();
                if (cachedLayerPainter != null) {
                    cachedLayerPainter.updateAnchorOffset(point);
                }
            };
        }

    }

}
