package shipeditor.components.instrument.ship.variant;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.VariantDataTabSelected;
import shipeditor.components.instrument.ship.variant.hullmods.VariantHullmodsPanel;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

public class VariantDataPanel extends JPanel {

    public VariantDataPanel() {
        this.setLayout(new BorderLayout());

        JTabbedPane tabContainer = new JTabbedPane(SwingConstants.BOTTOM);

        VariantMainPanel variantMainPanel = new VariantMainPanel();
        tabContainer.addTab("Main", variantMainPanel);

        VariantHullmodsPanel variantHullmodsPanel = new VariantHullmodsPanel();
        tabContainer.addTab(StringValues.HULLMODS, variantHullmodsPanel);

        VariantWingsPanel variantWingsPanel = new VariantWingsPanel();
        tabContainer.addTab(StringValues.WINGS, variantWingsPanel);

        tabContainer.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabContainer.putClientProperty(StringConstants.TABBED_PANE_TAB_AREA_ALIGNMENT, "fill");
        this.add(tabContainer, BorderLayout.CENTER);

        tabContainer.addChangeListener(event -> {
            JPanel activePanel = (JPanel) tabContainer.getSelectedComponent();
            boolean hullmodsTabSelected = activePanel instanceof VariantHullmodsPanel;
            boolean wingsTabSelected = activePanel instanceof VariantWingsPanel;

            VariantDataTab selected = VariantDataTab.MAIN;
            if (hullmodsTabSelected) {
                selected = VariantDataTab.HULLMODS;
            } else if (wingsTabSelected) {
                selected = VariantDataTab.WINGS;
            }
            EventBus.publish(new VariantDataTabSelected(selected));
        });
    }

}
