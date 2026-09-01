package shipeditor.components.instrument.ship.centers;

import shipeditor.utility.text.StringManager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.painters.points.ship.CenterPointPainter;
import shipeditor.utility.components.widgets.PointLocationWidget;
import shipeditor.utility.components.widgets.Spinners;
import shipeditor.utility.components.widgets.TwinSpinnerPanel;
import javax.swing.JButton;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.geom.Point2D;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class ModuleAnchorPanel extends PointLocationWidget {

    @Getter(AccessLevel.PRIVATE) @Setter
    private CenterPointPainter centerPainter;

    @Override
    protected void populateContent() {
        super.populateContent();

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 10, 0, 6);
        constraints.gridwidth = 2;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.LINE_START;

        JButton createDeleteButton = getCreateDeleteButton();
        createDeleteButton.setEnabled(false);

        String clearAnchor = "Clear anchor";
        registerWidgetListeners(createDeleteButton, layer -> {
            createDeleteButton.setText(StringManager.getString("DEFINE_ANCHOR"));
            createDeleteButton.setEnabled(false);
        }, layer -> {
            Supplier<Point2D> getter = retrieveGetter();
            Point2D existing = getter.get();
            if (existing != null) {
                createDeleteButton.setText(clearAnchor);
            }
            else {
                createDeleteButton.setText(StringManager.getString("DEFINE_ANCHOR"));
            }
            createDeleteButton.setEnabled(true);
        });

        constraints.gridy = 3;
        TwinSpinnerPanel twinSpinnerPanel = getTwinSpinnerPanel();
        twinSpinnerPanel.add(createDeleteButton, constraints);
    }

    private JButton getCreateDeleteButton() {
        JButton createDeleteButton = new JButton(StringManager.getString("DEFINE_ANCHOR"));
        createDeleteButton.addActionListener(e -> {
            if (isWidgetsReadyForInput()) {
                Supplier<Point2D> getter = retrieveGetter();
                Point2D existing = getter.get();
                Consumer<Point2D> setter = retrieveSetter();
                if (existing != null) {
                    setter.accept(null);
                }
                else {
                    CenterPointPainter centerPainter = getCenterPainter();
                    if (centerPainter != null && centerPainter.getParentLayer() != null) {
                        ShipPainter shipPainter = centerPainter.getParentLayer();
                        java.util.List<shipeditor.components.viewer.entities.BoundPoint> bounds = shipPainter.getBoundsPainter().getPointsIndex();
                        if (bounds != null && !bounds.isEmpty()) {
                            double sumFileX = 0;
                            double sumFileY = 0;
                            for (shipeditor.components.viewer.entities.BaseWorldPoint bp : bounds) {
                                Point2D fileCoord = shipeditor.utility.Utility.getPointCoordinatesForDisplay(
                                        bp.getPosition(), shipPainter, shipeditor.components.ComponentEnums.CoordsDisplayMode.SHIP_CENTER);
                                sumFileX += fileCoord.getX();
                                sumFileY += fileCoord.getY();
                            }
                            double avgFileX = sumFileX / bounds.size();
                            double avgFileY = sumFileY / bounds.size();
                            setter.accept(new Point2D.Double(Math.round(avgFileY), Math.round(avgFileX)));
                        } else {
                            setter.accept(new Point2D.Double());
                        }
                    } else {
                        setter.accept(new Point2D.Double());
                    }
                }
                processChange();
            }
        });
        return createDeleteButton;
    }

    /**
     * The coordinate name reversal completely intentional - blame Alex and module anchor field for that!
     */
    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected TwinSpinnerPanel createSpinnerPanel(Point2D initialPoint, Consumer<Point2D> pointSetter) {
        TwinSpinnerPanel spinnerPanel = Spinners.createLocationSpinners(initialPoint, retrieveGetter(), pointSetter,
                StringManager.getString("Y_COORDINATE"), StringManager.getString("X_COORDINATE"));
        spinnerPanel.setToolTipText(StringManager.getString("POSITION_OFFSET_FOR_SHIP_CENTER"));
        return spinnerPanel;
    }

    @Override
    protected boolean isLayerPainterEligible(LayerPainter layerPainter) {
        return layerPainter instanceof ShipPainter shipPainter && !shipPainter.isUninitialized();
    }

    @Override
    protected String getPanelTitleText() {
        return StringManager.getString("MODULE_ANCHOR");
    }

    @Override
    protected Supplier<Point2D> retrieveGetter() {
        return () -> {
            CenterPointPainter painter = getCenterPainter();
            if (painter != null) {
                return painter.getModuleAnchorOffset();
            }
            return null;
        };
    }

    @Override
    protected Consumer<Point2D> retrieveSetter() {
        return point -> {
            CenterPointPainter painter = getCenterPainter();
            if (painter != null) {
                painter.changeModuleAnchor(point);
            }
        };
    }

}
