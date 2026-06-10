package shipeditor.components.instrument.ship.engines;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.InstrumentRepaintQueued;
import shipeditor.communication.events.viewer.points.EngineInsertedConfirmed;
import shipeditor.communication.events.viewer.points.PointAddConfirmed;
import shipeditor.communication.events.viewer.points.PointRemovedConfirmed;
import shipeditor.components.instrument.EditorInstrument;
import shipeditor.components.instrument.ship.AbstractShipPropertiesPanel;
import shipeditor.components.viewer.entities.engine.EnginePoint;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.painters.PainterVisibility;
import shipeditor.components.viewer.painters.points.ship.EngineSlotPainter;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.objects.Pair;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class EnginesPanel extends AbstractShipPropertiesPanel {

    @Getter
    private EngineList enginesContainer;

    private EngineDataPanel dataPanel;

    private JCheckBox reorderCheckbox;

    private DefaultListModel<EnginePoint> model;

    public EnginesPanel() {
        this.initPointListener();
    }

    @Override
    public void refreshContent(LayerPainter layerPainter) {
        DefaultListModel<EnginePoint> newModel = new DefaultListModel<>();

        if (!(layerPainter instanceof ShipPainter shipPainter) || shipPainter.isUninitialized()) {
            this.model = newModel;
            this.enginesContainer.setModel(newModel);

            fireClearingListeners(layerPainter);
            refreshEngineControlPane(null);

            this.enginesContainer.setEnabled(false);
            this.reorderCheckbox.setEnabled(false);
            return;
        }

        EngineSlotPainter enginePainter = shipPainter.getEnginePainter();
        newModel.addAll(enginePainter.getPointsIndex());

        this.model = newModel;
        this.enginesContainer.setModel(newModel);
        this.enginesContainer.setEnabled(true);
        this.reorderCheckbox.setEnabled(true);

        fireRefresherListeners(layerPainter);
        refreshEngineControlPane(enginePainter.getSelected());
    }

    @Override
    protected void populateContent() {
        this.setLayout(new BorderLayout());

        this.model = new DefaultListModel<>();
        this.enginesContainer = new EngineList(model, this::refreshEngineControlPane);
        this.dataPanel = new EngineDataPanel();

        JPanel northContainer = new JPanel(new BorderLayout());
        var visibilityWidget = createEnginesVisibilityWidget();
        Map<JLabel, JComponent> visibilityWidgetMap = Map.of(visibilityWidget.getFirst(), visibilityWidget.getSecond());
        JPanel visibilityWidgetContainer = this.createWidgetsPanel(visibilityWidgetMap);
        visibilityWidgetContainer.setBorder(new EmptyBorder(4, 0, 3, 0));
        northContainer.add(visibilityWidgetContainer, BorderLayout.PAGE_START);

        ComponentUtilities.outfitPanelWithTitle(dataPanel, "Engine Data");
        northContainer.add(dataPanel, BorderLayout.CENTER);

        this.refreshEngineControlPane(null);

        JScrollPane scrollableContainer = new JScrollPane(enginesContainer);

        Pair<JPanel, JCheckBox> reorderWidget = ComponentUtilities.createReorderCheckboxPanel(enginesContainer);
        reorderCheckbox = reorderWidget.getSecond();
        northContainer.add(reorderWidget.getFirst(), BorderLayout.PAGE_END);

        this.add(northContainer, BorderLayout.PAGE_START);

        this.add(scrollableContainer, BorderLayout.CENTER);
    }

    private void refreshEngineControlPane(EnginePoint engine) {
        ShipPainter painter = (ShipPainter) getCachedLayerPainter();
        if (engine != null) {
            painter = (ShipPainter) engine.getParent();
        }
        this.dataPanel.refresh(painter);
    }

    private EngineSlotPainter getCachedEnginePainter() {
        LayerPainter cachedLayerPainter = getCachedLayerPainter();
        if (cachedLayerPainter instanceof ShipPainter shipPainter && !shipPainter.isUninitialized()) {
            return shipPainter.getEnginePainter();
        }
        return null;
    }

    @Override
    protected void initLayerListeners() {
        super.initLayerListeners();
        EventBus.subscribe(this, event -> {
            if (event instanceof InstrumentRepaintQueued checked) {
                if (checked.editorMode() != EditorInstrument.ENGINES) {
                    return;
                }
                EngineSlotPainter cachedEnginePainter = getCachedEnginePainter();
                if (cachedEnginePainter != null) {
                    java.util.List<EnginePoint> currentPoints = cachedEnginePainter.getPointsIndex();
                    boolean modelsEqual = true;
                    if (this.model.getSize() == currentPoints.size()) {
                        for (int i = 0; i < this.model.getSize(); i++) {
                            if (this.model.getElementAt(i) != currentPoints.get(i)) {
                                modelsEqual = false;
                                break;
                            }
                        }
                    } else {
                        modelsEqual = false;
                    }

                    if (!modelsEqual) {
                        int[] cachedSelected = this.enginesContainer.getSelectedIndices();
                        DefaultListModel<EnginePoint> newModel = new DefaultListModel<>();
                        newModel.addAll(currentPoints);

                        this.model = newModel;
                        this.enginesContainer.setModel(newModel);
                        this.enginesContainer.setSelectedIndices(cachedSelected);
                        if (!this.model.isEmpty() && cachedSelected.length > 0) {
                            this.enginesContainer.ensureIndexIsVisible(cachedSelected[0]);
                        }
                    } else {
                        this.enginesContainer.repaint();
                    }
                }

                this.refreshEngineControlPane(null);
            }
        });
    }

    private void initPointListener() {
        EventBus.subscribe(this, event -> {
            if (event instanceof PointRemovedConfirmed checked && checked.point() instanceof EnginePoint point) {
                model.removeElement(point);
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof EngineInsertedConfirmed checked) {
                model.insertElementAt(checked.toInsert(), checked.precedingIndex());
                enginesContainer.setSelectedIndex(model.indexOf(checked.toInsert()));
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof PointAddConfirmed checked && checked.point() instanceof EnginePoint point) {
                model.addElement(point);
                enginesContainer.setSelectedIndex(model.indexOf(point));
            }
        });
    }

    private Pair<JLabel, JComboBox<PainterVisibility>> createEnginesVisibilityWidget() {
        BooleanSupplier readinessChecker = this::isWidgetsReadyForInput;
        Consumer<PainterVisibility> visibilitySetter = changedValue -> {
            LayerPainter cachedLayerPainter = getCachedLayerPainter();
            if (cachedLayerPainter != null) {
                EngineSlotPainter enginePainter = ((ShipPainter) cachedLayerPainter).getEnginePainter();
                enginePainter.setVisibilityMode(changedValue);
                processChange();
            }
        };

        BiConsumer<JComponent, Consumer<LayerPainter>> clearerListener = this::registerWidgetClearer;
        BiConsumer<JComponent, Consumer<LayerPainter>> refresherListener = this::registerWidgetRefresher;

        Function<LayerPainter, PainterVisibility> visibilityGetter = layerPainter -> {
            EngineSlotPainter enginePainter = ((ShipPainter) layerPainter).getEnginePainter();
            return enginePainter.getVisibilityMode();
        };

        var opacityWidget = PainterVisibility.createVisibilityWidget(
                readinessChecker, visibilityGetter, visibilitySetter,
                clearerListener, refresherListener
        );

        JLabel opacityLabel = opacityWidget.getFirst();
        opacityLabel.setText("Engines view");

        return opacityWidget;
    }

}
