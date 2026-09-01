package shipeditor.components.instrument.ship.hull;

import shipeditor.components.instrument.AbstractLayerInfoPanel;
import shipeditor.components.viewer.entities.weapon.SlotData;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.painters.points.ship.WeaponSlotPainter;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Insets;

import java.util.List;
import java.util.Map;

public final class ShipLayerInfoPanel extends AbstractLayerInfoPanel {

    private final HullDataControlPanel hullDataPanel;

    private final JPanel weaponSlotsSummaryPanel;

    public ShipLayerInfoPanel() {
        JPanel dataContainer = new JPanel();
        dataContainer.setLayout(new BorderLayout());

        JPanel centralContainer = new JPanel(new BorderLayout());

        hullDataPanel = new HullDataControlPanel();
        hullDataPanel.setAlignmentY(0);

        centralContainer.add(hullDataPanel, BorderLayout.PAGE_START);

        dataContainer.add(centralContainer, BorderLayout.PAGE_START);

        weaponSlotsSummaryPanel = new JPanel();
        weaponSlotsSummaryPanel.setLayout(new BoxLayout(weaponSlotsSummaryPanel, BoxLayout.PAGE_AXIS));
        shipeditor.utility.components.CollapsibleSection summarySection = new shipeditor.utility.components.CollapsibleSection("Weapon Slots Summary", weaponSlotsSummaryPanel, true);
        dataContainer.add(summarySection, BorderLayout.CENTER);

        this.add(dataContainer, BorderLayout.CENTER);
    }

    @Override
    protected boolean isValidLayer(LayerPainter layerPainter) {
        return layerPainter instanceof ShipPainter shipPainter && !shipPainter.isUninitialized();
    }

    @Override
    protected void clearData() {
        weaponSlotsSummaryPanel.removeAll();
        hullDataPanel.clearData();
        weaponSlotsSummaryPanel.revalidate();
        weaponSlotsSummaryPanel.repaint();
    }

    @Override
    protected void refreshData(ViewerLayer selected) {
        weaponSlotsSummaryPanel.add(Box.createVerticalStrut(8));
        ShipPainter shipPainter = (ShipPainter) selected.getPainter();
        weaponSlotsSummaryPanel.add(ShipLayerInfoPanel.createSlotsSummaryPanel(shipPainter));

        hullDataPanel.refreshData((ShipLayer) selected);
        weaponSlotsSummaryPanel.revalidate();
        weaponSlotsSummaryPanel.repaint();
    }

    private static JPanel createSlotsSummaryPanel(ShipPainter shipPainter) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
        container.setAlignmentX(0.5f);
        container.setAlignmentY(0);



        WeaponSlotPainter slotPainter = shipPainter.getWeaponSlotPainter();
        List<WeaponSlotPoint> slotPointList = slotPainter.getSlotPoints();

        Map<String, Integer> slotSummary = ShipLayerInfoPanel.generateSlotConfigSummary(slotPointList);

        container.add(Box.createVerticalStrut(4));

        for (Map.Entry<String, Integer> entry : slotSummary.entrySet()) {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
            JLabel slotKind = new JLabel(entry.getValue() + "× " + entry.getKey());
            ComponentUtilities.layoutAsOpposites(row, slotKind, new JLabel(""), 4);
            container.add(row);
            container.add(Box.createVerticalStrut(4));
        }

        return container;
    }

    private static Map<String, Integer> generateSlotConfigSummary(Iterable<WeaponSlotPoint> slots) {
        Map<String, Integer> slotConfigSummary = new java.util.LinkedHashMap<>();
        for (SlotData slot : slots) {
            WeaponSize weaponSize = slot.getWeaponSize();
            WeaponType weaponType = slot.getWeaponType();
            String slotConfig = weaponSize.getDisplayedName() + " " + weaponType.getDisplayedName();
            slotConfigSummary.put(slotConfig, slotConfigSummary.getOrDefault(slotConfig, 0) + 1);
        }
        return slotConfigSummary;
    }

}
