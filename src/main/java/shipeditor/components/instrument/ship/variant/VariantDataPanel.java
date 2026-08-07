package shipeditor.components.instrument.ship.variant;

import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipHull;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.representation.RepresentationEnums.HullSize;
import shipeditor.utility.Utility;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.text.StringValues;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Map;
import java.util.function.Consumer;
import shipeditor.undo.EditDispatch;

public class VariantDataPanel extends JPanel {
    private final JLabel shipHullIDLabel;
    private final JTextField variantIDEditor;
    private final JTextField variantDisplayNameEditor;
    private final DataSpinnerContainer<Integer> ventsSpinner;
    private final DataSpinnerContainer<Integer> capacitorsSpinner;
    private final DataSpinnerContainer<Double> qualitySpinner;
    private final JCheckBox goalVariantCheckbox;

    private Consumer<String> variantIDSetter;
    private Consumer<String> variantDisplayNameSetter;
    private Consumer<Boolean> goalVariantSetter;

    private ShipLayer selectedLayer;
    private ShipVariant cachedVariant;

    public VariantDataPanel(Runnable onRefreshOrdnance) {
        this.setLayout(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(this, "Variant data");

        JLabel shipHullIDConstLabel = new JLabel("Ship hull ID:");
        shipHullIDConstLabel.setBorder(new EmptyBorder(2, 0, 5, 0));
        shipHullIDLabel = new JLabel(StringValues.NOT_INITIALIZED);
        ComponentUtilities.addLabelAndComponent(this, shipHullIDConstLabel, shipHullIDLabel, 0);

        JLabel variantIDLabel = new JLabel("Variant ID:");
        variantIDEditor = new JTextField();
        String editorIDTooltip = Utility.getWithLinebreaks(StringValues.TYPE_AND_PRESS_ENTER_TO_EDIT_ID,
                "Original variant will be copied with new ID, old entry reloaded");
        variantIDEditor.setToolTipText(editorIDTooltip);

        Runnable applyVariantId = () -> {
            if (selectedLayer == null || cachedVariant == null) return;
            String currentText = variantIDEditor.getText();
            if (currentText.equals(cachedVariant.getVariantId())) return;
            
            Map<String, ShipVariant> loadedVariants = selectedLayer.getLoadedVariants();
            String variantId = cachedVariant.getVariantId();
            ShipVariant renamed = loadedVariants.remove(variantId);

            EditDispatch.postVariantFieldChanged(cachedVariant, selectedLayer, variantId, currentText, variantIDSetter, "ID");

            loadedVariants.put(currentText, renamed);
            renamed.setLoadedFromFile(false);
            ShipPainter shipPainter = selectedLayer.getPainter();
            shipPainter.selectVariant(renamed);
        };
        variantIDEditor.addActionListener(e -> applyVariantId.run());
        variantIDEditor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                applyVariantId.run();
            }
        });
        ComponentUtilities.addLabelAndComponent(this, variantIDLabel, variantIDEditor, 1);

        JLabel variantDisplayNameLabel = new JLabel("Display name:");
        variantDisplayNameEditor = new JTextField();
        String editorNameTooltip = Utility.getWithLinebreaks(StringValues.TYPE_AND_PRESS_ENTER_TO_EDIT_ID);
        variantDisplayNameEditor.setToolTipText(editorNameTooltip);

        Runnable applyDisplayName = () -> {
            if (selectedLayer == null || cachedVariant == null) return;
            String currentText = variantDisplayNameEditor.getText();
            if (currentText.equals(cachedVariant.getDisplayName())) return;

            EditDispatch.postVariantFieldChanged(cachedVariant, selectedLayer, cachedVariant.getDisplayName(), currentText, variantDisplayNameSetter, "Display Name");

            ShipPainter shipPainter = selectedLayer.getPainter();
            shipPainter.selectVariant(cachedVariant);
        };
        variantDisplayNameEditor.addActionListener(e -> applyDisplayName.run());
        variantDisplayNameEditor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                applyDisplayName.run();
            }
        });
        ComponentUtilities.addLabelAndComponent(this, variantDisplayNameLabel, variantDisplayNameEditor, 2);

        SpinnerNumberModel ventsModel = new SpinnerNumberModel(0, 0, 0, 1);
        JSpinner ventsSpinnerComponent = new JSpinner(ventsModel);
        ventsSpinner = new DataSpinnerContainer<>(ventsModel, ventsSpinnerComponent);
        this.addDataSpinner(this, "Flux vents:", false, ventsSpinner, 3, onRefreshOrdnance);

        SpinnerNumberModel capacitorsModel = new SpinnerNumberModel(0, 0, 0, 1);
        JSpinner capacitorSpinnerComponent = new JSpinner(capacitorsModel);
        capacitorsSpinner = new DataSpinnerContainer<>(capacitorsModel, capacitorSpinnerComponent);
        this.addDataSpinner(this, "Flux capacitors:", false, capacitorsSpinner, 4, onRefreshOrdnance);

        SpinnerNumberModel qualityModel = new SpinnerNumberModel(0.0d, -1.0d, 0.0d, 0.05d);
        JSpinner qualitySpinnerComponent = new JSpinner(qualityModel);
        qualitySpinnerComponent.setToolTipText("Values less than 0 indicate the field is omitted from variant file.");
        qualitySpinner = new DataSpinnerContainer<>(qualityModel, qualitySpinnerComponent);
        this.addDataSpinner(this, "Variant quality:", true, qualitySpinner, 5, onRefreshOrdnance);

        goalVariantCheckbox = new JCheckBox("Goal variant");
        goalVariantCheckbox.addItemListener(e -> {
            if (selectedLayer == null || cachedVariant == null) return;
            boolean enableGoal = goalVariantCheckbox.isSelected();
            if (enableGoal != cachedVariant.isGoalVariant() && goalVariantSetter != null) {
                EditDispatch.postVariantFieldChanged(cachedVariant, selectedLayer, cachedVariant.isGoalVariant(), enableGoal, goalVariantSetter, "Goal Variant");
            }
        });

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 3, 0, 3);
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.LINE_START;
        this.add(goalVariantCheckbox, constraints);
    }

    public void disableDataSpinners() {
        ventsSpinner.disableSpinner();
        capacitorsSpinner.disableSpinner();
        qualitySpinner.disableSpinner();
    }

    public void installPlaceholders() {
        disableDataSpinners();
        shipHullIDLabel.setText(StringValues.NOT_INITIALIZED);
        variantIDEditor.setEnabled(false);
        variantIDEditor.setText("");
        variantDisplayNameEditor.setEnabled(false);
        variantDisplayNameEditor.setText("");
        goalVariantCheckbox.setSelected(false);
        goalVariantCheckbox.setEnabled(false);
        variantIDSetter = null;
        variantDisplayNameSetter = null;
        goalVariantSetter = null;
        selectedLayer = null;
        cachedVariant = null;
    }

    public void refresh(ShipLayer layer, ShipVariant variant) {
        installPlaceholders();
        if (layer == null || variant == null || variant.isEmpty()) return;

        selectedLayer = layer;
        cachedVariant = variant;

        ShipHull shipHull = layer.getHull();
        if (shipHull == null) return;

        HullSize hullSize = shipHull.getHullSize();
        int maxFluxRegulators = hullSize.getMaxFluxRegulators();

        ventsSpinner.enableSpinner(layer, variant.getFluxVents(), maxFluxRegulators, val -> {
            if (!val.equals(variant.getFluxVents())) {
                EditDispatch.postVariantFieldChanged(variant, layer, variant.getFluxVents(), val, v -> variant.setFluxVents(v != null ? v : 0), "Flux Vents");
            }
        });
        capacitorsSpinner.enableSpinner(layer, variant.getFluxCapacitors(), maxFluxRegulators, val -> {
            if (!val.equals(variant.getFluxCapacitors())) {
                EditDispatch.postVariantFieldChanged(variant, layer, variant.getFluxCapacitors(), val, v -> variant.setFluxCapacitors(v != null ? v : 0), "Flux Capacitors");
            }
        });
        qualitySpinner.enableSpinner(layer, variant.getQuality(), 1.0d, val -> {
            if (!val.equals(variant.getQuality())) {
                EditDispatch.postVariantFieldChanged(variant, layer, variant.getQuality(), val, v -> variant.setQuality(v != null ? v : 0.0d), "Quality");
            }
        });

        shipHullIDLabel.setText(variant.getShipHullId());

        variantIDEditor.setText(variant.getVariantId());
        variantIDEditor.setEnabled(true);
        variantIDSetter = variant::setVariantId;

        variantDisplayNameEditor.setText(variant.getDisplayName());
        variantDisplayNameEditor.setEnabled(true);
        variantDisplayNameSetter = variant::setDisplayName;

        goalVariantSetter = val -> variant.setGoalVariant(val != null && val);
        goalVariantCheckbox.setSelected(variant.isGoalVariant());
        goalVariantCheckbox.setEnabled(true);
    }

    @SuppressWarnings("unchecked")
    private <T extends Number> void addDataSpinner(JPanel target, String labelText, boolean useFloatingPoint,
                                                   DataSpinnerContainer<T> spinnerContainer, int row, Runnable onRefreshOrdnance) {
        JLabel label = new JLabel(labelText);
        SpinnerNumberModel spinnerNumberModel = spinnerContainer.getModel();
        JSpinner spinner = spinnerContainer.getSpinner();

        spinner.addChangeListener(e -> {
            Number modelNumber = spinnerNumberModel.getNumber();
            T result = (T) modelNumber;
            var currentSetter = spinnerContainer.getCurrentSetter();
            if (currentSetter != null) {
                currentSetter.accept(result);
            }
            if (onRefreshOrdnance != null) onRefreshOrdnance.run();
        });

        MouseWheelListener wheelListener = e -> {
            if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL || spinnerContainer.getCurrentSetter() == null) {
                return;
            }
            Number newValue;
            if (useFloatingPoint) {
                double value = (double) spinner.getValue();
                double newValueInt = value - (e.getUnitsToScroll() * 0.05d);
                double minValue = (double) spinnerContainer.getMinValue();
                double maxValue = (double) spinnerContainer.getMaxValue();
                newValue = Math.min(maxValue, Math.max(minValue, newValueInt));
            } else {
                int value = (int) spinner.getValue();
                int newValueInt = value - e.getUnitsToScroll();
                int minValue = (int) spinnerContainer.getMinValue();
                int maxValue = (int) spinnerContainer.getMaxValue();
                newValue = Math.min(maxValue, Math.max(minValue, newValueInt));
            }
            spinner.setValue(newValue);
        };
        spinner.addMouseWheelListener(wheelListener);

        JPanel container = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0));
        container.add(spinner);
        container.add(spinnerContainer.getMaxLabel());
        ComponentUtilities.addLabelAndComponent(target, label, container, row);
    }
}
