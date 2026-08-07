package shipeditor.components.instrument.ship.variant;

import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.data.ShipHull;
import shipeditor.utility.Utility;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.text.StringValues;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import shipeditor.utility.themes.Themes;

public class VariantOrdnancePanel extends JPanel {
    private final JLabel usedOPInHullmods;
    private final JLabel usedOPInWings;
    private final JLabel usedOPInWeapons;
    private final JLabel usedVentsAndMax;
    private final JLabel usedCapsAndMax;
    private final JLabel totalFluxCapacity;
    private final JLabel totalFluxDissipation;

    public VariantOrdnancePanel() {
        ComponentUtilities.outfitPanelWithTitle(this, "Ordnance & Flux");
        this.setLayout(new GridBagLayout());

        usedOPInWeapons = new JLabel();
        usedOPInHullmods = new JLabel();
        usedOPInWings = new JLabel();
        usedVentsAndMax = new JLabel();
        usedCapsAndMax = new JLabel();
        totalFluxCapacity = new JLabel();
        totalFluxDissipation = new JLabel();

        int row = 0;
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Weapons OP:"), usedOPInWeapons, row++);
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Hullmods OP:"), usedOPInHullmods, row++);
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Wings OP:"), usedOPInWings, row++);
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Vents:"), usedVentsAndMax, row++);
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Capacitors:"), usedCapsAndMax, row++);
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Flux Cap:"), totalFluxCapacity, row++);
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Flux Diss:"), totalFluxDissipation, row++);

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        this.add(new JLabel(), filler);
    }

    public void refreshOrdnanceInfo(ViewerLayer selected) {
        String notInitialized = StringValues.NOT_INITIALIZED;

        if (selected instanceof ShipLayer shipLayer) {
            var activeVariant = shipLayer.getActiveVariant();
            if (activeVariant == null) {
                resetFields(notInitialized);
                return;
            }

            usedOPInWeapons.setText(String.valueOf(activeVariant.getTotalOPInWeapons()));
            usedOPInHullmods.setText(String.valueOf(activeVariant.getTotalOPInHullmods(shipLayer)));
            usedOPInWings.setText(String.valueOf(shipLayer.getTotalOPInWings()));

            int maxVentsCaps = 0;
            ShipHull shipHull = shipLayer.getHull();
            if (shipHull != null) {
                maxVentsCaps = shipHull.getHullSize().getMaxFluxRegulators();
            }

            int usedVents = activeVariant.getFluxVents();
            int usedCaps = activeVariant.getFluxCapacitors();

            usedVentsAndMax.setText(usedVents + " / " + maxVentsCaps);
            usedCapsAndMax.setText(usedCaps + " / " + maxVentsCaps);

            ShipCSVEntry shipEntry = activeVariant.getEntryFromShipID();
            if (shipEntry != null) {
                int baseCapacity = shipEntry.getBaseFluxCapacity();
                int baseDissipation = shipEntry.getBaseFluxDissipation();

                int totalCapVal = baseCapacity + (usedCaps * 200);
                int totalDissVal = baseDissipation + (usedVents * 10);

                totalFluxCapacity.setText(String.valueOf(totalCapVal));
                totalFluxDissipation.setText(String.valueOf(totalDissVal));
            } else {
                totalFluxCapacity.setText(String.valueOf(usedCaps * 200));
                totalFluxDissipation.setText(String.valueOf(usedVents * 10));
            }
        } else {
            resetFields(notInitialized);
        }
    }

    private void resetFields(String notInitialized) {
        usedOPInWeapons.setText(notInitialized);
        usedOPInHullmods.setText(notInitialized);
        usedOPInWings.setText(notInitialized);
        usedVentsAndMax.setText(notInitialized);
        usedCapsAndMax.setText(notInitialized);
        totalFluxCapacity.setText(notInitialized);
        totalFluxDissipation.setText(notInitialized);
    }
}
