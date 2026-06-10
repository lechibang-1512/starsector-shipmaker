package shipeditor.components.instrument.ship.variant.modules;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.InstrumentRepaintQueued;
import shipeditor.communication.events.viewer.points.PointSelectedConfirmed;
import shipeditor.components.instrument.EditorInstrument;
import shipeditor.components.instrument.ship.AbstractShipPropertiesPanel;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.overseers.StaticController;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class VariantModulesPanel extends AbstractShipPropertiesPanel {

    private ModuleList moduleList;

    private ModuleControlPanel controlPanel;

    private DefaultListModel<InstalledFeature> model;

    @SuppressWarnings({"OverlyComplexBooleanExpression", "ChainedMethodCall"})
    @Override
    public void refreshContent(LayerPainter layerPainter) {
        DefaultListModel<InstalledFeature> newModel = new DefaultListModel<>();

        if (!(layerPainter instanceof ShipPainter shipPainter)
                || shipPainter.isUninitialized()
                || shipPainter.getActiveVariant() == null
                || shipPainter.getActiveVariant().isEmpty()) {
            this.model = newModel;
            this.moduleList.setModel(newModel);

            fireClearingListeners(layerPainter);
            this.controlPanel.refresh(null);

            this.moduleList.setEnabled(false);

            return;
        }

        ShipVariant activeVariant = shipPainter.getActiveVariant();

        newModel.addAll(activeVariant.getFittedModulesList());

        this.model = newModel;
        this.moduleList.setModel(newModel);
        this.moduleList.setEnabled(true);

        fireRefresherListeners(layerPainter);
        refreshModuleControlPane();
    }

    @Override
    protected void populateContent() {
        this.setLayout(new BorderLayout());

        this.model = new DefaultListModel<>();
        this.moduleList = createModuleList();
        this.controlPanel = new ModuleControlPanel(moduleList);

        JPanel northContainer = new JPanel(new BorderLayout());

        JPanel buttonContainer = new JPanel(new BorderLayout());
        buttonContainer.setBorder(new EmptyBorder(4, 4, 0, 4));
        buttonContainer.add(getLoadModulesButton(), BorderLayout.CENTER);

        northContainer.add(buttonContainer, BorderLayout.PAGE_START);

        ComponentUtilities.outfitPanelWithTitle(controlPanel, "Selected module");
        northContainer.add(controlPanel, BorderLayout.CENTER);

        this.refreshModuleControlPane();

        JScrollPane scrollableContainer = new JScrollPane(moduleList);

        this.add(northContainer, BorderLayout.PAGE_START);
        this.add(scrollableContainer, BorderLayout.CENTER);
    }

    private JButton getLoadModulesButton() {
        JButton loadModulesAsLayers = new JButton("Load modules as layers");

        registerWidgetListeners(loadModulesAsLayers, layer ->
                loadModulesAsLayers.setEnabled(false), layer -> {
            ShipVariant currentVariant = getCurrentVariant();
            if (currentVariant != null) {
                List<InstalledFeature> fittedModulesList = currentVariant.getFittedModulesList();
                loadModulesAsLayers.setEnabled(!fittedModulesList.isEmpty());
            } else {
                loadModulesAsLayers.setEnabled(false);
            }
        });

        loadModulesAsLayers.addActionListener(event -> {
            ShipVariant currentVariant = getCurrentVariant();
            if (currentVariant != null) {
                List<InstalledFeature> fittedModulesList = currentVariant.getFittedModulesList();
                if (!fittedModulesList.isEmpty()) {
                    fittedModulesList.forEach(InstalledFeature::loadAsSeparateLayer);
                }
            }
        });

        return loadModulesAsLayers;
    }

    @Override
    public ShipPainter getCachedLayerPainter() {
        LayerPainter cachedLayerPainter = super.getCachedLayerPainter();
        if (cachedLayerPainter instanceof ShipPainter shipPainter && !shipPainter.isUninitialized()) {
            return shipPainter;
        }
        return null;
    }

    private ShipVariant getCurrentVariant() {
        ShipPainter shipPainter = getCachedLayerPainter();
        ShipVariant activeVariant = shipPainter.getActiveVariant();
        if (activeVariant != null && !activeVariant.isEmpty()) {
            return activeVariant;
        }
        return null;
    }

    private void refreshModuleControlPane() {
        InstalledFeature selectedValue = moduleList.getSelectedValue();
        if (selectedValue == null) {
            this.controlPanel.refresh(null);
            return;
        }
        ShipPainter painter = (ShipPainter) selectedValue.getFeaturePainter();
        this.controlPanel.refresh(painter);
    }

    @Override
    protected void initLayerListeners() {
        super.initLayerListeners();
        EventBus.subscribe(this, event -> {
            if (event instanceof PointSelectedConfirmed checked) {
                if (!(checked.point() instanceof WeaponSlotPoint slotPoint)) return;
                if (moduleList != null && StaticController.getEditorMode() == EditorInstrument.VARIANT_MODULES) {
                    moduleList.selectEntryByPoint(slotPoint);
                }
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof InstrumentRepaintQueued checked) {
                if (checked.editorMode() != EditorInstrument.VARIANT_MODULES) {
                    return;
                }
                ShipVariant currentVariant = getCurrentVariant();
                if (currentVariant != null) {
                    java.util.List<InstalledFeature> currentFeatures = currentVariant.getFittedModulesList();
                    boolean modelsEqual = true;
                    if (this.model.getSize() == currentFeatures.size()) {
                        for (int i = 0; i < this.model.getSize(); i++) {
                            if (this.model.getElementAt(i) != currentFeatures.get(i)) {
                                modelsEqual = false;
                                break;
                            }
                        }
                    } else {
                        modelsEqual = false;
                    }

                    if (!modelsEqual) {
                        int[] cachedSelected = this.moduleList.getSelectedIndices();
                        DefaultListModel<InstalledFeature> newModel = new DefaultListModel<>();
                        newModel.addAll(currentFeatures);

                        this.model = newModel;
                        this.moduleList.setModel(newModel);
                        this.moduleList.setSelectedIndices(cachedSelected);
                        if (!this.model.isEmpty() && cachedSelected.length > 0) {
                            this.moduleList.ensureIndexIsVisible(cachedSelected[0]);
                        }
                    } else {
                        this.moduleList.repaint();
                    }
                }

                this.refreshModuleControlPane();
            }
        });
    }

    private ModuleList createModuleList() {
        Consumer<InstalledFeature> removeAction = feature ->
                StaticController.actOnCurrentVariant((shipLayer, variant) -> {
                    Map<String, InstalledFeature> fittedModules = variant.getFittedModules();
                    if (fittedModules == null) {
                        return;
                    }
                    EditDispatch.postFeatureUninstalled(fittedModules,
                            feature.getSlotID(), feature, null);
                });

        Consumer<Map<String, InstalledFeature>> sortAction = rearranged ->
                StaticController.actOnCurrentVariant((shipLayer, variant) ->
                        variant.sortModules(rearranged));

        moduleList = new ModuleList(this::refreshModuleControlPane, model, removeAction, sortAction);
        moduleList.setBorder(new LineBorder(Color.LIGHT_GRAY));
        return moduleList;
    }

}
