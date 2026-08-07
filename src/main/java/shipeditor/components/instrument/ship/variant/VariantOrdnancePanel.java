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
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import shipeditor.utility.themes.Themes;

public class VariantOrdnancePanel extends JPanel {
    private final JLabel shipOPCap;
    private final JLabel usedOPTotal;
    private final JLabel usedOPInHullmods;
    private final JLabel usedOPInWings;
    private final JLabel usedOPInWeapons;
    private final JLabel usedVentsAndMax;
    private final JLabel usedCapsAndMax;
    private final JLabel totalFluxCapacity;
    private final JLabel totalFluxDissipation;
    private final JLabel opStatusLabel;

    public VariantOrdnancePanel() {
        ComponentUtilities.outfitPanelWithTitle(this, "Ordnance & Flux Stats");
        this.setLayout(new GridBagLayout());

        Border emptyBorder = new EmptyBorder(2, 0, 8, 0);

        JLabel shipOPCapLabel = new JLabel(StringValues.TOTAL_OP_CAPACITY);
        shipOPCapLabel.setBorder(emptyBorder);
        shipOPCap = new JLabel();

        JLabel usedOPTotalLabel = new JLabel("Used OP for ship:");
        usedOPTotalLabel.setBorder(emptyBorder);
        usedOPTotal = new JLabel();

        JLabel usedOPInWeaponsLabel = new JLabel("Used OP in weapons:");
        usedOPInWeaponsLabel.setBorder(emptyBorder);
        usedOPInWeapons = new JLabel();

        JLabel usedOPInModsLabel = new JLabel(StringValues.USED_OP_IN_HULLMODS);
        usedOPInModsLabel.setBorder(emptyBorder);
        usedOPInHullmods = new JLabel();

        JLabel usedOPInWingsLabel = new JLabel(StringValues.USED_OP_IN_WINGS);
        usedOPInWingsLabel.setBorder(emptyBorder);
        usedOPInWings = new JLabel();

        JLabel ventsMaxLabel = new JLabel("Flux Vents (Allocated/Max):");
        ventsMaxLabel.setBorder(emptyBorder);
        usedVentsAndMax = new JLabel();

        JLabel capsMaxLabel = new JLabel("Flux Capacitors (Allocated/Max):");
        capsMaxLabel.setBorder(emptyBorder);
        usedCapsAndMax = new JLabel();

        JLabel fluxCapLabel = new JLabel("Total Flux Capacity:");
        fluxCapLabel.setBorder(emptyBorder);
        totalFluxCapacity = new JLabel();

        JLabel fluxDissLabel = new JLabel("Total Flux Dissipation:");
        fluxDissLabel.setBorder(emptyBorder);
        totalFluxDissipation = new JLabel();

        JLabel opStatusTitle = new JLabel("OP Status:");
        opStatusTitle.setBorder(emptyBorder);
        opStatusLabel = new JLabel();

        JPanel opPanel = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(opPanel, "OP Breakdown");
        JPanel fluxPanel = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(fluxPanel, "Flux Stats");

        ComponentUtilities.addLabelAndComponent(opPanel, shipOPCapLabel, shipOPCap, 0);
        ComponentUtilities.addLabelAndComponent(opPanel, usedOPTotalLabel, usedOPTotal, 1);
        ComponentUtilities.addLabelAndComponent(opPanel, usedOPInWeaponsLabel, usedOPInWeapons, 2);
        ComponentUtilities.addLabelAndComponent(opPanel, usedOPInModsLabel, usedOPInHullmods, 3);
        ComponentUtilities.addLabelAndComponent(opPanel, usedOPInWingsLabel, usedOPInWings, 4);
        ComponentUtilities.addLabelAndComponent(opPanel, opStatusTitle, opStatusLabel, 5);
        GridBagConstraints fillOP = new GridBagConstraints();
        fillOP.gridy = 6; fillOP.weighty = 1;
        opPanel.add(new JLabel(), fillOP);

        ComponentUtilities.addLabelAndComponent(fluxPanel, ventsMaxLabel, usedVentsAndMax, 0);
        ComponentUtilities.addLabelAndComponent(fluxPanel, capsMaxLabel, usedCapsAndMax, 1);
        ComponentUtilities.addLabelAndComponent(fluxPanel, fluxCapLabel, totalFluxCapacity, 2);
        ComponentUtilities.addLabelAndComponent(fluxPanel, fluxDissLabel, totalFluxDissipation, 3);
        GridBagConstraints fillFlux = new GridBagConstraints();
        fillFlux.gridy = 4; fillFlux.weighty = 1;
        fluxPanel.add(new JLabel(), fillFlux);

        this.setLayout(new GridLayout(1, 2, 10, 0));
        this.add(opPanel);
        this.add(fluxPanel);
    }

    public void refreshOrdnanceInfo(ViewerLayer selected) {
        String notInitialized = StringValues.NOT_INITIALIZED;

        if (selected instanceof ShipLayer shipLayer) {
            String totalOPStr = Utility.translateIntegerValue(shipLayer::getTotalOP);
            shipOPCap.setText(totalOPStr);

            var activeVariant = shipLayer.getActiveVariant();
            if (activeVariant == null) {
                resetFields(notInitialized);
                return;
            }

            int totalUsedOP = shipLayer.getTotalUsedOP();
            usedOPTotal.setText(String.valueOf(totalUsedOP));
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

                totalFluxCapacity.setText(totalCapVal + " (Base: " + baseCapacity + " + " + (usedCaps * 200) + ")");
                totalFluxDissipation.setText(totalDissVal + " (Base: " + baseDissipation + " + " + (usedVents * 10) + ")");
            } else {
                totalFluxCapacity.setText(String.valueOf(usedCaps * 200));
                totalFluxDissipation.setText(String.valueOf(usedVents * 10));
            }

            int opCap = shipLayer.getTotalOP();
            if (opCap > 0) {
                if (totalUsedOP > opCap) {
                    opStatusLabel.setText("OP EXCEEDED! (Exceeds by " + (totalUsedOP - opCap) + " OP)");
                    opStatusLabel.setForeground(Themes.getReddishFontColor());
                    opStatusLabel.setFont(opStatusLabel.getFont().deriveFont(java.awt.Font.BOLD));
                } else if (totalUsedOP < opCap) {
                    opStatusLabel.setText("WARNING: " + (opCap - totalUsedOP) + " unallocated OP remaining!");
                    opStatusLabel.setForeground(Themes.getWarningColor());
                    opStatusLabel.setFont(opStatusLabel.getFont().deriveFont(java.awt.Font.BOLD));
                } else {
                    opStatusLabel.setText("All OP Allocated");
                    opStatusLabel.setForeground(Themes.getSuccessColor());
                    opStatusLabel.setFont(opStatusLabel.getFont().deriveFont(java.awt.Font.BOLD));
                }
            } else {
                opStatusLabel.setText("N/A");
                opStatusLabel.setForeground(null);
            }
        } else {
            shipOPCap.setText(notInitialized);
            resetFields(notInitialized);
        }
    }

    private void resetFields(String notInitialized) {
        usedOPTotal.setText(notInitialized);
        usedOPInWeapons.setText(notInitialized);
        usedOPInHullmods.setText(notInitialized);
        usedOPInWings.setText(notInitialized);
        usedVentsAndMax.setText(notInitialized);
        usedCapsAndMax.setText(notInitialized);
        totalFluxCapacity.setText(notInitialized);
        totalFluxDissipation.setText(notInitialized);
        opStatusLabel.setText(notInitialized);
        opStatusLabel.setForeground(null);
    }
}
