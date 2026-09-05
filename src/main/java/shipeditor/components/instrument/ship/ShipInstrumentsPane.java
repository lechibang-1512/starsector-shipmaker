package shipeditor.components.instrument.ship;

import com.formdev.flatlaf.FlatLaf;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.components.ComponentEnums.VariantDataTab;
import shipeditor.communication.events.viewer.ViewerRepaintQueued;
import shipeditor.components.instrument.AbstractInstrumentsPane;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.instrument.ship.bays.LaunchBaysPanel;
import shipeditor.components.instrument.ship.bounds.BoundsPanel;
import shipeditor.components.instrument.ship.builtins.hullmods.BuiltInHullmodsPanel;
import shipeditor.components.instrument.ship.builtins.wings.BuiltInWingsPanel;
import shipeditor.components.instrument.ship.centers.CollisionPanel;
import shipeditor.components.instrument.ship.centers.ShieldPanel;
import shipeditor.components.instrument.ship.engines.EnginesPanel;
import shipeditor.components.instrument.ship.hull.ShipLayerInfoPanel;
import shipeditor.components.instrument.ship.skins.SkinDataPanel;
import shipeditor.components.instrument.ship.skins.SkinSlotOverridesPanel;
import shipeditor.components.instrument.ship.skins.SkinEngineOverridesPanel;
import shipeditor.components.instrument.ship.skins.SkinRemovalsPanel;
import shipeditor.components.instrument.ship.slots.WeaponSlotsPanel;
import shipeditor.components.instrument.ship.variant.VariantMainPanel;
import shipeditor.components.instrument.ship.variant.VariantWingsPanel;
import shipeditor.components.instrument.ship.variant.hullmods.VariantHullmodsPanel;
import shipeditor.components.instrument.ship.variant.VariantWeaponsPanel;
import shipeditor.components.instrument.ship.variant.modules.VariantModulesPanel;
import shipeditor.utility.Utility;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.Locale;
import shipeditor.communication.events.components.ComponentEvents.VariantDataTabSelected;
import shipeditor.communication.events.viewer.points.PointEvents.InstrumentModeChanged;

@SuppressWarnings("OverlyCoupledClass")
@Log4j2
public final class ShipInstrumentsPane extends AbstractInstrumentsPane {

    @Getter @Setter
    private static EditorInstrument currentMode;

    public ShipInstrumentsPane() {
        this.createTabs();
        // Initial dispatch — first tab is a JTabbedPane with sub-tabs (Hull Data, Collision, Shield)
        Component selected = getSelectedComponent();
        if (selected instanceof JPanel panel) {
            this.dispatchModeChange(panel);
        } else if (selected instanceof javax.swing.JScrollPane scrollPane) {
            java.awt.Component view = scrollPane.getViewport().getView();
            if (view instanceof JPanel panel) {
                this.dispatchModeChange(panel);
            }
        } else if (selected instanceof JTabbedPane subPane) {
            Component subSelected = subPane.getSelectedComponent();
            if (subSelected instanceof JPanel panel) {
                this.dispatchModeChange(panel);
            }
        }
    }

    private void styleSubTabbedPane(JTabbedPane subPane) {
        subPane.putClientProperty("JTabbedPane.tabType", "underline");
        subPane.putClientProperty("JTabbedPane.tabHeight", 26);
        subPane.putClientProperty("JTabbedPane.showTabSeparators", false);
        subPane.putClientProperty("JTabbedPane.hasFullBorder", false);
        subPane.putClientProperty("JTabbedPane.tabWidthMode", "compact");
    }

    @SuppressWarnings("OverlyCoupledMethod")
    private void createTabs() {
        FlatLaf.showMnemonics(this);

        // Tab 1: Hull — inner tabs for Hull Info, Collision, Shield
        JTabbedPane hullTabs = new JTabbedPane(SwingConstants.TOP);
        hullTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.styleSubTabbedPane(hullTabs);
        this.createInnerTab(hullTabs, new ShipLayerInfoPanel(), "Hull Data", EditorInstrument.LAYER, KeyEvent.VK_H);
        this.createInnerTab(hullTabs, new CollisionPanel(), EditorInstrument.COLLISION, KeyEvent.VK_C);
        this.createInnerTab(hullTabs, new ShieldPanel(), EditorInstrument.SHIELD, KeyEvent.VK_S);
        this.createInnerTab(hullTabs, new BoundsPanel(), EditorInstrument.BOUNDS, KeyEvent.VK_B);
        this.createInnerTab(hullTabs, new EnginesPanel(), EditorInstrument.ENGINES, KeyEvent.VK_E);
        this.addTab("Hull", null, hullTabs, "Hull Properties & Centers");
        this.addInnerTabChangeListener(hullTabs);

        // Tab 2: Fittings — inner tabs for table-based panels
        JTabbedPane fittingsTabs = new JTabbedPane(SwingConstants.TOP);
        fittingsTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.styleSubTabbedPane(fittingsTabs);
        this.createInnerTab(fittingsTabs, new WeaponSlotsPanel(), EditorInstrument.WEAPON_SLOTS, KeyEvent.VK_W);
        this.createInnerTab(fittingsTabs, new LaunchBaysPanel(), EditorInstrument.LAUNCH_BAYS, KeyEvent.VK_L);
        this.createInnerTab(fittingsTabs, new BuiltInHullmodsPanel(), EditorInstrument.BUILT_IN_MODS, KeyEvent.VK_H);
        this.createInnerTab(fittingsTabs, new BuiltInWingsPanel(), EditorInstrument.BUILT_IN_WINGS, KeyEvent.VK_N);
        this.addTab("Fittings", null, fittingsTabs, "Ship Fittings & Modifications");
        this.addInnerTabChangeListener(fittingsTabs);

        // Tab 3: Skins
        JTabbedPane skinsTabs = new JTabbedPane(SwingConstants.TOP);
        skinsTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.styleSubTabbedPane(skinsTabs);
        this.createInnerTab(skinsTabs, new SkinDataPanel(), EditorInstrument.SKIN_DATA, KeyEvent.VK_K);
        this.createInnerTab(skinsTabs, new SkinSlotOverridesPanel(), EditorInstrument.SKIN_SLOTS, KeyEvent.VK_O);
        this.createInnerTab(skinsTabs, new SkinEngineOverridesPanel(), EditorInstrument.SKIN_ENGINES, KeyEvent.VK_E);
        this.createInnerTab(skinsTabs, new SkinRemovalsPanel(), EditorInstrument.SKIN_REMOVALS, KeyEvent.VK_R);
        this.addTab("Skins", null, skinsTabs, "Ship Skins");
        this.addInnerTabChangeListener(skinsTabs);

        // Tab 4: Variants
        JTabbedPane variantsTabs = new JTabbedPane(SwingConstants.TOP);
        variantsTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.styleSubTabbedPane(variantsTabs);
        this.createInnerTab(variantsTabs, new VariantMainPanel(), EditorInstrument.VARIANT_DATA, KeyEvent.VK_V);
        this.createInnerTab(variantsTabs, new VariantWeaponsPanel(), EditorInstrument.VARIANT_WEAPONS, KeyEvent.VK_T);
        this.createInnerTab(variantsTabs, new VariantModulesPanel(), EditorInstrument.VARIANT_MODULES, KeyEvent.VK_M);

        VariantHullmodsPanel hullmodsPanel = new VariantHullmodsPanel();
        panelMode.put(hullmodsPanel, EditorInstrument.VARIANT_DATA);
        variantsTabs.addTab("Hullmods", null, hullmodsPanel, "Variant Hullmods");

        VariantWingsPanel wingsPanel = new VariantWingsPanel();
        panelMode.put(wingsPanel, EditorInstrument.VARIANT_DATA);
        variantsTabs.addTab("Wings", null, wingsPanel, "Variant Wings");

        variantsTabs.addChangeListener(event -> {
            Component activePanel = variantsTabs.getSelectedComponent();
            VariantDataTab selected = VariantDataTab.MAIN;
            if (activePanel instanceof VariantHullmodsPanel) {
                selected = VariantDataTab.HULLMODS;
            } else if (activePanel instanceof VariantWingsPanel) {
                selected = VariantDataTab.WINGS;
            }
            EventBus.publish(new VariantDataTabSelected(selected));
        });

        this.addTab("Variants", null, variantsTabs, "Ship Variants");
        this.addInnerTabChangeListener(variantsTabs);

        updateTooltipText();
    }

    private void createInnerTab(JTabbedPane parent, JPanel panel, EditorInstrument mode, int mnemonic) {
        this.createInnerTab(parent, panel, mode.getTitle(), mode, mnemonic);
    }

    private void createInnerTab(JTabbedPane parent, JPanel panel, String title, EditorInstrument mode, int mnemonic) {
        panelMode.put(panel, mode);
        parent.addTab(title, null, panel, title);
        if (mnemonic != 0) {
            parent.setMnemonicAt(parent.getTabCount() - 1, mnemonic);
        }
    }

    @Override
    protected void dispatchModeChange(JPanel active) {
        EditorInstrument selected = panelMode.get(active);
        ShipInstrumentsPane.setCurrentMode(selected);
        EventBus.publish(new InstrumentModeChanged(selected));
        EventBus.publish(new ViewerRepaintQueued());
    }

    @Override
    protected void updateTooltipText() {
        String minimizePrompt = getMinimizePrompt();
        int size = this.getTabCount();
        for (int i = 0; i < size; i++) {
            String outerTitle = this.getTitleAt(i);
            this.setToolTipTextAt(i, Utility.getWithLinebreaks(outerTitle, minimizePrompt));
            java.awt.Component comp = this.getComponentAt(i);
            if (comp instanceof JTabbedPane subPane) {
                int subSize = subPane.getTabCount();
                for (int j = 0; j < subSize; j++) {
                    String mnemonic = KeyEvent.getKeyText(subPane.getMnemonicAt(j)).toUpperCase(Locale.ROOT);
                    EditorInstrument mode = panelMode.get((JPanel) subPane.getComponentAt(j));
                    String title = mode != null ? mode.getTitle() : "";
                    String tooltip = Utility.getWithLinebreaks(title, "Hotkey: ALT + " + mnemonic);
                    subPane.setToolTipTextAt(j, tooltip);
                }
            }
        }
    }

}
