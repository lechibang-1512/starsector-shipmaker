package shipeditor.utility.components.widgets;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import shipeditor.components.CoordsDisplayMode;
import shipeditor.components.instrument.LayerPropertiesPanel;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.Utility;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.StringValues;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public abstract class PointLocationWidget extends LayerPropertiesPanel {

    private TwinSpinnerPanel twinSpinnerPanel;

    @Override
    public void refreshContent(LayerPainter layerPainter) {
        boolean uninitialized = layerPainter instanceof ShipPainter shipPainter && shipPainter.isUninitialized();
        if (layerPainter == null || uninitialized) {
            fireClearingListeners(null);
            return;
        }
        if (!isLayerPainterEligible(layerPainter)) {
            fireClearingListeners(layerPainter);
            return;
        }

        fireRefresherListeners(layerPainter);
    }

    protected abstract boolean isLayerPainterEligible(LayerPainter layerPainter);

    protected TwinSpinnerPanel createSpinnerPanel(Point2D initialPoint, Consumer<Point2D> pointSetter) {
        TwinSpinnerPanel spinnerPanel = Spinners.createLocationSpinners(initialPoint,
                retrieveGetter(), pointSetter);
        spinnerPanel.setToolTipText(StringValues.POINT_LOCATION_IN_WORLD_COORDINATES);
        return spinnerPanel;
    }

    @Override
    protected void populateContent() {
        this.setLayout(new BorderLayout());
        Point2D initialPoint = new Point2D.Double();
        Consumer<Point2D> pointSetter = changed -> {
            if (isWidgetsReadyForInput()) {
                Consumer<Point2D> setter = retrieveSetter();
                setter.accept(changed);
                processChange();
            }
        };

        twinSpinnerPanel = createSpinnerPanel(initialPoint, pointSetter);

        registerWidgetListeners(twinSpinnerPanel, layer -> {
            twinSpinnerPanel.clear();
            twinSpinnerPanel.setEnabled(false);
        }, layer -> {
            Supplier<Point2D> getter = retrieveGetter();
            Point2D existing = getter.get();

            if (existing != null) {
                twinSpinnerPanel.setEnabled(true);

                JSpinner firstSpinner = twinSpinnerPanel.getFirstSpinner();
                firstSpinner.setValue(existing.getX());

                JSpinner secondSpinner = twinSpinnerPanel.getSecondSpinner();
                secondSpinner.setValue(existing.getY());
            } else {
                twinSpinnerPanel.clear();
                twinSpinnerPanel.setEnabled(false);
            }
        });

        twinSpinnerPanel.clear();
        twinSpinnerPanel.setEnabled(false);

        addPanelTitle();

        Dimension containerPreferredSize = twinSpinnerPanel.getPreferredSize();
        int width = twinSpinnerPanel.getMaximumSize().width;
        Dimension maximumSize = new Dimension(width, containerPreferredSize.height);
        twinSpinnerPanel.setMaximumSize(maximumSize);

        this.add(twinSpinnerPanel, BorderLayout.CENTER);
    }

    private void addPanelTitle() {
        Insets insets = new Insets(1, 0, 0, 0);
        ComponentUtilities.outfitPanelWithTitle(this, insets, getPanelTitleText());
    }

    protected JPanel createDependentCoordinatesLabel(String name) {
        JLabel coordsNameLabel = new JLabel(name);

        String coordinatesHint = "Point location depends on coordinate system";
        CoordsDisplayMode coordsMode = StaticController.getCoordsMode();
        String currentMode = "Current system: " + coordsMode.getShortName();
        coordsNameLabel.setToolTipText(Utility.getWithLinebreaks(coordinatesHint, currentMode));

        JLabel coordsDisplayLabel = new JLabel(StringValues.NOT_INITIALIZED);

        registerWidgetListeners(coordsDisplayLabel,
                layer -> coordsDisplayLabel.setText(StringValues.NOT_INITIALIZED),
                layer -> {
                    Supplier<Point2D> getter = retrieveGetter();
                    Point2D existing = getter.get();
                    if (existing != null) {
                        Point2D translated = Utility.getPointCoordinatesForDisplay(existing);
                        coordsDisplayLabel.setText(Utility.getPointPositionText(translated));
                    } else {
                        coordsDisplayLabel.setText(StringValues.NOT_INITIALIZED);
                    }
                });

        var container = createWidgetsPanel(Map.of(coordsNameLabel, coordsDisplayLabel));
        container.setBorder(new EmptyBorder(0, 0, 5, 0));
        return container;
    }

    protected abstract String getPanelTitleText();

    /**
     * Should account for changing entities, e.g. different point painter instances.
     */
    protected abstract Supplier<Point2D> retrieveGetter();

    /**
     * Should account for changing entities.
     */
    protected abstract Consumer<Point2D> retrieveSetter();

}
