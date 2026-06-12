package shipeditor.components.instrument.ship.variant.modules;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.control.ControlEvents.FeatureInstallQueued;
import shipeditor.components.instrument.LayerPropertiesPanel;
import shipeditor.components.instrument.ship.centers.ModuleAnchorPanel;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.FeaturesOverseer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.ViewerEnums.PainterVisibility;
import shipeditor.components.viewer.painters.points.AbstractPointPainter;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.ship.VariantFile;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.Utility;
import shipeditor.utility.objects.Pair;
import shipeditor.utility.text.StringValues;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class ModuleControlPanel extends LayerPropertiesPanel {

    private final ModuleList moduleList;

    private ModuleAnchorPanel moduleAnchorWidget;

    ModuleControlPanel(ModuleList list) {
        this.moduleList = list;
    }

    @Override
    public void refreshContent(LayerPainter layerPainter) {
        if (!(layerPainter instanceof ShipPainter shipPainter) || shipPainter.isUninitialized()) {
            fireClearingListeners(layerPainter);

            moduleAnchorWidget.setCenterPainter(null);
            moduleAnchorWidget.refresh(null);
            return;
        }

        fireRefresherListeners(layerPainter);

        moduleAnchorWidget.setCenterPainter(((ShipPainter) layerPainter).getCenterPointPainter());
        moduleAnchorWidget.refresh(layerPainter);
    }

    @Override
    protected void populateContent() {
        this.setLayout(new BorderLayout());

        JPanel topContainer = new JPanel(new BorderLayout());

        Map<JLabel, JComponent> topWidgets = new LinkedHashMap<>();

        var moduleOpacityWidget = createModuleOpacitySlider();
        topWidgets.put(moduleOpacityWidget.getFirst(), moduleOpacityWidget.getSecond());

        var collisionVisibilityWidget = createCollisionVisibilityWidget();
        topWidgets.put(collisionVisibilityWidget.getFirst(), collisionVisibilityWidget.getSecond());

        var boundsVisibilityWidget = createBoundsVisibilityWidget();
        topWidgets.put(boundsVisibilityWidget.getFirst(), boundsVisibilityWidget.getSecond());

        var slotsVisibilityWidget = createSlotsVisibilityWidget();
        topWidgets.put(slotsVisibilityWidget.getFirst(), slotsVisibilityWidget.getSecond());

        Border bottomPadding = new EmptyBorder(0, 0, 4, 0);

        JPanel topWidgetsPanel = createWidgetsPanel(topWidgets);
        topWidgetsPanel.setBorder(bottomPadding);
        topContainer.add(topWidgetsPanel, BorderLayout.PAGE_START);
        moduleAnchorWidget = new ModuleAnchorPanel();

        String tooltip = Utility.getWithLinebreaks("Mockup editing, changes are not saved to file",
                "Create separate ship layer from module to edit module anchor offset");
        moduleAnchorWidget.setToolTipText(tooltip);

        topContainer.add(moduleAnchorWidget, BorderLayout.CENTER);

        JPanel moduleActionsPanel = createModuleActionsPanel();
        topContainer.add(moduleActionsPanel, BorderLayout.PAGE_END);

        this.add(topContainer, BorderLayout.PAGE_START);
    }

    private Pair<JLabel, JComboBox<PainterVisibility>> createCollisionVisibilityWidget() {
        Function<LayerPainter, AbstractPointPainter> painterGetter = layerPainter -> {
            if (layerPainter instanceof ShipPainter shipPainter) {
                return shipPainter.getCenterPointPainter();
            }
            return null;
        };

        Consumer<PainterVisibility> additionalAction = painterVisibility -> actOnSelectedModules(shipPainter -> {
            var pointPainter = shipPainter.getCenterPointPainter();
            pointPainter.setVisibilityMode(painterVisibility);
        });
        var opacityWidget = createVisibilityWidget(painterGetter, additionalAction);

        JLabel opacityLabel = opacityWidget.getFirst();
        opacityLabel.setText(StringValues.COLLISION_VIEW);

        return opacityWidget;
    }

    private Pair<JLabel, JComboBox<PainterVisibility>> createBoundsVisibilityWidget() {
        Function<LayerPainter, AbstractPointPainter> painterGetter = layerPainter -> {
            if (layerPainter instanceof ShipPainter shipPainter) {
                return shipPainter.getBoundsPainter();
            }
            return null;
        };

        Consumer<PainterVisibility> additionalAction = painterVisibility -> actOnSelectedModules(shipPainter -> {
            var pointPainter = shipPainter.getBoundsPainter();
            pointPainter.setVisibilityMode(painterVisibility);
        });
        var opacityWidget = createVisibilityWidget(painterGetter, additionalAction);

        JLabel opacityLabel = opacityWidget.getFirst();
        opacityLabel.setText(StringValues.BOUNDS_VIEW);

        return opacityWidget;
    }

    private Pair<JLabel, JComboBox<PainterVisibility>> createSlotsVisibilityWidget() {
        Function<LayerPainter, AbstractPointPainter> painterGetter = layerPainter -> {
            if (layerPainter instanceof ShipPainter shipPainter) {
                return shipPainter.getWeaponSlotPainter();
            }
            return null;
        };

        Consumer<PainterVisibility> additionalAction = painterVisibility -> actOnSelectedModules(shipPainter -> {
            var pointPainter = shipPainter.getWeaponSlotPainter();
            pointPainter.setVisibilityMode(painterVisibility);
        });
        var opacityWidget = createVisibilityWidget(painterGetter, additionalAction);

        JLabel opacityLabel = opacityWidget.getFirst();
        opacityLabel.setText(StringValues.SLOTS_VIEW);

        return opacityWidget;
    }

    private Pair<JLabel, JSlider> createModuleOpacitySlider() {
        Consumer<Float> opacitySetter = changedValue -> {
            actOnSelectedModules(shipPainter -> {
                if (shipPainter == null) return;
                shipPainter.setSpriteOpacity(changedValue);
                ShipVariant moduleVariant = shipPainter.getActiveVariant();
                if (moduleVariant == null || moduleVariant.isEmpty()) return;
                moduleVariant.setOpacityForAllFitted(changedValue);
            });
            processChange();
        };

        Function<LayerPainter, Float> opacityGetter = LayerPainter::getSpriteOpacity;

        Pair<JLabel, JSlider> opacityWidget = super.createOpacityWidget(opacityGetter, opacitySetter);

        JLabel opacityLabel = opacityWidget.getFirst();
        opacityLabel.setText("Module opacity");

        return opacityWidget;
    }

    private void actOnSelectedModules(Consumer<ShipPainter> action) {
        if (moduleList == null) return;
        List<InstalledFeature> selectedValuesList = moduleList.getSelectedValuesList();
        if (selectedValuesList != null && !selectedValuesList.isEmpty()) {
            for (InstalledFeature feature : selectedValuesList) {
                action.accept((ShipPainter) feature.getFeaturePainter());
            }
        }
    }

    private JPanel createModuleActionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(4, 0, 4, 0));

        JLabel pickerLabel = new JLabel("Module variant:");
        panel.add(pickerLabel, BorderLayout.PAGE_START);

        JPanel controlsRow = new JPanel(new BorderLayout(4, 0));

        JComboBox<VariantFile> variantPicker = new JComboBox<>();
        variantPicker.setEnabled(false);

        // Populate the picker with available variants from game data.
        GameDataRepository dataRepository = SettingsManager.getGameData();
        if (dataRepository != null) {
            Map<String, VariantFile> allVariants = dataRepository.getAllVariants();
            if (allVariants != null) {
                for (VariantFile variant : allVariants.values()) {
                    variantPicker.addItem(variant);
                    variantPicker.setEnabled(true);
                }
            }
        }

        variantPicker.addActionListener(e -> {
            VariantFile selected = (VariantFile) variantPicker.getSelectedItem();
            if (selected != null) {
                FeaturesOverseer.setModuleForInstall(selected);
            }
        });

        JButton installButton = new JButton("Install");
        installButton.setToolTipText("Install selected variant as module to the selected slot");
        installButton.addActionListener(e -> {
            VariantFile selected = (VariantFile) variantPicker.getSelectedItem();
            if (selected != null) {
                FeaturesOverseer.setModuleForInstall(selected);
                EventBus.publish(new FeatureInstallQueued(null));
            }
        });

        JButton clearButton = new JButton("Clear");
        clearButton.setToolTipText("Remove selected module from variant");
        clearButton.addActionListener(e -> {
            if (moduleList == null) return;
            InstalledFeature selectedFeature = moduleList.getSelectedValue();
            if (selectedFeature == null) return;
            LayerPainter painter = getCachedLayerPainter();
            if (!(painter instanceof ShipPainter shipPainter)) return;
            ShipVariant variant = shipPainter.getActiveVariant();
            if (variant == null || variant.isEmpty()) return;
            var modules = variant.getFittedModules();
            String slotID = selectedFeature.getSlotID();
            InstalledFeature toRemove = modules.get(slotID);
            if (toRemove != null) {
                EditDispatch.postFeatureUninstalled(modules, slotID, toRemove, null);
            }
        });

        controlsRow.add(variantPicker, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        buttonsPanel.add(installButton);
        buttonsPanel.add(clearButton);
        controlsRow.add(buttonsPanel, BorderLayout.LINE_END);

        panel.add(controlsRow, BorderLayout.CENTER);

        return panel;
    }

}
