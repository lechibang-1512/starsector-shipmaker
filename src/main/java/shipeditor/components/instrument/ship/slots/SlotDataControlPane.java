package shipeditor.components.instrument.ship.slots;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.components.instrument.ship.shared.AbstractSlotValuesPanel;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.painters.points.ship.WeaponSlotPainter;
import shipeditor.representation.weapon.WeaponMount;
import shipeditor.representation.weapon.WeaponSize;
import shipeditor.representation.weapon.WeaponType;
import shipeditor.utility.Utility;
import shipeditor.utility.overseers.StaticController;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class SlotDataControlPane extends AbstractSlotValuesPanel {

    private final WeaponSlotList slotList;

    private WeaponSlotPoint cachedSelected;

    SlotDataControlPane(WeaponSlotList weaponSlotList) {
        super(true);
        this.slotList = weaponSlotList;
    }

    public void refreshWithSelectedPoint(LayerPainter painter, WeaponSlotPoint selected) {
        this.cachedSelected = selected;
        this.refresh(painter);
    }

    @Override
    protected String getEntityName() {
        return "Slot";
    }

    @Override
    protected WeaponSlotPoint getSelectedFromLayer(LayerPainter layerPainter) {
        // Such a check is probably unnecessary, but we better err on the side of
        // safety...
        boolean layerContainsPoint = false;
        if (layerPainter instanceof ShipPainter shipPainter) {
            WeaponSlotPainter weaponSlotPainter = shipPainter.getWeaponSlotPainter();
            List<WeaponSlotPoint> pointsIndex = weaponSlotPainter.getPointsIndex();
            layerContainsPoint = pointsIndex.contains(cachedSelected);
        }
        if (cachedSelected != null && layerContainsPoint) {
            return cachedSelected;
        } else
            return Utility.getSelectedFromLayer(layerPainter);
    }

    @Override
    protected String getNextUniqueID() {
        var layer = StaticController.getActiveLayer();
        if (!(layer instanceof ShipLayer shipLayer))
            return null;
        var shipPainter = shipLayer.getPainter();
        if (shipPainter == null || shipPainter.isUninitialized())
            return null;

        var slotPainter = shipPainter.getWeaponSlotPainter();
        return slotPainter.generateUniqueSlotID();
    }

    @Override
    protected Consumer<String> getIDSetter() {
        return slotID -> actOnSelectedValues(
                (weaponSlotPainter, slotPoints) -> weaponSlotPainter.changeSlotsIDWithMirrorCheck(slotID, slotPoints));
    }

    @Override
    protected Consumer<WeaponType> getTypeSetter() {
        return type -> actOnSelectedValues(
                (weaponSlotPainter, slotPoints) -> weaponSlotPainter.changeSlotsTypeWithMirrorCheck(type, slotPoints));
    }

    @Override
    protected Consumer<WeaponMount> getMountSetter() {
        return mount -> this.actOnSelectedValues((weaponSlotPainter, weaponSlotPoints) -> weaponSlotPainter
                .changeSlotsMountWithMirrorCheck(mount, weaponSlotPoints));
    }

    @Override
    protected Consumer<WeaponSize> getSizeSetter() {
        return size -> this.actOnSelectedValues((weaponSlotPainter, weaponSlotPoints) -> weaponSlotPainter
                .changeSlotsSizeWithMirrorCheck(size, weaponSlotPoints));
    }

    @Override
    protected Consumer<Double> getAngleSetter() {
        return angle -> {
            ShipPainter slotParent = getCachedLayerPainter();
            WeaponSlotPainter weaponSlotPainter = slotParent.getWeaponSlotPainter();
            WeaponSlotPoint selectedFromLayer = getSelectedFromLayer(slotParent);
            weaponSlotPainter.changePointAngleWithMirrorCheck(selectedFromLayer, angle);
        };
    }

    @Override
    protected Consumer<Double> getArcSetter() {
        return arc -> {
            ShipPainter slotParent = getCachedLayerPainter();
            WeaponSlotPainter weaponSlotPainter = slotParent.getWeaponSlotPainter();
            WeaponSlotPoint selectedFromLayer = getSelectedFromLayer(slotParent);
            weaponSlotPainter.changeArcWithMirrorCheck(selectedFromLayer, arc);
        };
    }

    @Override
    protected Consumer<Double> getRenderOrderSetter() {
        return renderOrder -> {
            ShipPainter slotParent = getCachedLayerPainter();
            WeaponSlotPainter weaponSlotPainter = slotParent.getWeaponSlotPainter();
            WeaponSlotPoint selectedFromLayer = getSelectedFromLayer(slotParent);
            int renderOrderValue = renderOrder.intValue();
            weaponSlotPainter.changeRenderOrderWithMirrorCheck(selectedFromLayer, renderOrderValue);

        };
    }

    @Override
    protected void populateContent() {
        super.populateContent();

        JPanel builtInPanel = createBuiltInFeaturePanel();
        this.add(builtInPanel, BorderLayout.PAGE_END);
    }

    private JPanel createBuiltInFeaturePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        shipeditor.utility.components.ComponentUtilities.outfitPanelWithTitle(panel, "Built-In Feature");

        JPanel content = new JPanel(new BorderLayout());

        JLabel infoLabel = new JLabel("None");
        JButton actionButton = new JButton("Install...");

        actionButton.addActionListener(e -> {
            if (cachedSelected == null)
                return;
            var shipPainter = cachedSelected.getParent();
            if (shipPainter == null)
                return;

            boolean hasFeature = false;
            var activeSkin = shipPainter.getActiveSkin();
            if (activeSkin != null && !activeSkin.isBase()) {
                hasFeature = activeSkin.getBuiltInWeapons().containsKey(cachedSelected.getId());
            } else {
                hasFeature = shipPainter.getBuiltInWeapons().containsKey(cachedSelected.getId());
            }

            var layer = StaticController.getActiveLayer();
            if (layer instanceof ShipLayer shipLayer) {
                if (hasFeature) {
                    shipLayer.getFeaturesOverseer().uninstallBuiltIn(cachedSelected);
                } else {
                    shipeditor.components.datafiles.entities.WeaponCSVEntry picked = shipeditor.utility.components.dialog.DialogUtilities
                            .showWeaponPickerDialog(cachedSelected);
                    if (picked != null) {
                        shipLayer.getFeaturesOverseer().installBuiltIn(cachedSelected, picked);
                    }
                }
            }
        });

        registerWidgetListeners(actionButton, layerPainter -> {
            actionButton.setEnabled(false);
            infoLabel.setText("None");
        }, layerPainter -> {
            if (cachedSelected != null) {
                actionButton.setEnabled(true);
                var shipPainter = cachedSelected.getParent();
                String featureName = null;
                var activeSkin = shipPainter.getActiveSkin();
                if (activeSkin != null && !activeSkin.isBase()) {
                    var w = activeSkin.getBuiltInWeapons().get(cachedSelected.getId());
                    if (w != null)
                        featureName = w.toString();
                } else {
                    var f = shipPainter.getBuiltInWeapons().get(cachedSelected.getId());
                    if (f != null)
                        featureName = f.getName();
                }

                if (featureName != null) {
                    infoLabel.setText(featureName);
                    actionButton.setText("Remove");
                } else {
                    infoLabel.setText("None");
                    actionButton.setText("Install...");
                }
            } else {
                actionButton.setEnabled(false);
                infoLabel.setText("None");
            }
        });

        content.add(infoLabel, BorderLayout.CENTER);
        content.add(actionButton, BorderLayout.LINE_END);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private void actOnSelectedValues(BiConsumer<WeaponSlotPainter, List<WeaponSlotPoint>> action) {
        WeaponSlotPoint selectedValue = slotList.getSelectedValue();
        if (selectedValue == null)
            return;
        ShipPainter parentLayer = selectedValue.getParent();
        WeaponSlotPainter slotPainter = parentLayer.getWeaponSlotPainter();
        List<WeaponSlotPoint> selectedValuesList = slotList.getSelectedValuesList();
        if (!selectedValuesList.isEmpty()) {
            action.accept(slotPainter, selectedValuesList);
        }
    }

}
