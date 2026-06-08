package oth.shipeditor.components.instrument.ship;

import com.formdev.flatlaf.FlatLaf;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.viewer.ViewerRepaintQueued;
import oth.shipeditor.communication.events.viewer.points.InstrumentModeChanged;
import oth.shipeditor.components.instrument.AbstractInstrumentsPane;
import oth.shipeditor.components.instrument.EditorInstrument;
import oth.shipeditor.components.instrument.ship.bays.LaunchBaysPanel;
import oth.shipeditor.components.instrument.ship.bounds.BoundsPanel;
import oth.shipeditor.components.instrument.ship.builtins.hullmods.BuiltInHullmodsPanel;
import oth.shipeditor.components.instrument.ship.builtins.wings.BuiltInWingsPanel;
import oth.shipeditor.components.instrument.ship.centers.CollisionPanel;
import oth.shipeditor.components.instrument.ship.centers.ShieldPanel;
import oth.shipeditor.components.instrument.ship.engines.EnginesPanel;
import oth.shipeditor.components.instrument.ship.hull.ShipLayerInfoPanel;
import oth.shipeditor.components.instrument.ship.skins.SkinDataPanel;
import oth.shipeditor.components.instrument.ship.skins.SkinSlotOverridesPanel;
import oth.shipeditor.components.instrument.ship.slots.WeaponSlotsPanel;
import oth.shipeditor.components.instrument.ship.variant.VariantDataPanel;
import oth.shipeditor.components.instrument.ship.variant.VariantWeaponsPanel;
import oth.shipeditor.components.instrument.ship.variant.modules.VariantModulesPanel;
import oth.shipeditor.utility.Utility;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("OverlyCoupledClass")
@Log4j2
public final class ShipInstrumentsPane extends AbstractInstrumentsPane {

    @Getter
    private static EditorInstrument currentMode;

    private final Map<JPanel, EditorInstrument> panelMode;

    public ShipInstrumentsPane() {
        panelMode = new HashMap<>();
        this.createTabs();
        Component selected = getSelectedComponent();
        if (selected instanceof JPanel panel) {
            this.dispatchModeChange(panel);
        } else if (selected instanceof JTabbedPane subPane) {
            Component subSelected = subPane.getSelectedComponent();
            if (subSelected instanceof JPanel panel) {
                this.dispatchModeChange(panel);
            }
        }
    }

    @SuppressWarnings("OverlyCoupledMethod")
    private void createTabs() {
        FlatLaf.showMnemonics(this);

        JTabbedPane coreTabs = new JTabbedPane(SwingConstants.TOP);
        coreTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.createInnerTab(coreTabs, new ShipLayerInfoPanel(), EditorInstrument.LAYER, KeyEvent.VK_Y);
        this.createInnerTab(coreTabs, new CollisionPanel(), EditorInstrument.COLLISION, KeyEvent.VK_C);
        this.createInnerTab(coreTabs, new ShieldPanel(), EditorInstrument.SHIELD, KeyEvent.VK_S);
        this.createInnerTab(coreTabs, new BoundsPanel(), EditorInstrument.BOUNDS, KeyEvent.VK_B);
        this.addTab("Core", null, coreTabs, "Core Ship Instruments");
        this.addInnerTabChangeListener(coreTabs);

        JTabbedPane fittingsTabs = new JTabbedPane(SwingConstants.TOP);
        fittingsTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.createInnerTab(fittingsTabs, new WeaponSlotsPanel(), EditorInstrument.WEAPON_SLOTS, KeyEvent.VK_W);
        this.createInnerTab(fittingsTabs, new LaunchBaysPanel(), EditorInstrument.LAUNCH_BAYS, KeyEvent.VK_L);
        this.createInnerTab(fittingsTabs, new EnginesPanel(), EditorInstrument.ENGINES, KeyEvent.VK_E);
        this.createInnerTab(fittingsTabs, new BuiltInHullmodsPanel(), EditorInstrument.BUILT_IN_MODS, KeyEvent.VK_H);
        this.createInnerTab(fittingsTabs, new BuiltInWingsPanel(), EditorInstrument.BUILT_IN_WINGS, KeyEvent.VK_N);
        this.addTab("Fittings", null, fittingsTabs, "Ship Fittings & Modifications");
        this.addInnerTabChangeListener(fittingsTabs);

        JTabbedPane variantsTabs = new JTabbedPane(SwingConstants.TOP);
        variantsTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.createInnerTab(variantsTabs, new SkinDataPanel(), EditorInstrument.SKIN_DATA, KeyEvent.VK_K);
        this.createInnerTab(variantsTabs, new SkinSlotOverridesPanel(), EditorInstrument.SKIN_SLOTS, KeyEvent.VK_O);
        this.createInnerTab(variantsTabs, new VariantDataPanel(), EditorInstrument.VARIANT_DATA, KeyEvent.VK_V);
        this.createInnerTab(variantsTabs, new VariantWeaponsPanel(), EditorInstrument.VARIANT_WEAPONS, KeyEvent.VK_T);
        this.createInnerTab(variantsTabs, new VariantModulesPanel(), EditorInstrument.VARIANT_MODULES, KeyEvent.VK_M);
        this.addTab("Variants", null, variantsTabs, "Skins & Variants");
        this.addInnerTabChangeListener(variantsTabs);

        updateTooltipText();
    }

    private void createInnerTab(JTabbedPane parent, JPanel panel, EditorInstrument mode, int mnemonic) {
        panelMode.put(panel, mode);
        parent.addTab(mode.getTitle(), null, panel, mode.getTitle());
        int index = parent.getTabCount() - 1;
        parent.setMnemonicAt(index, mnemonic);
    }

    @Override
    protected void dispatchModeChange(JPanel active) {
        EditorInstrument selected = panelMode.get(active);
        currentMode = selected;
        EventBus.publish(new InstrumentModeChanged(selected));
        EventBus.publish(new ViewerRepaintQueued());
    }

    @Override
    protected void updateTooltipText() {
        String minimizePrompt = getMinimizePrompt();
        int size = this.getTabCount();
        for (int i = 0; i < size; i++) {
            java.awt.Component comp = this.getComponentAt(i);
            if (comp instanceof JTabbedPane subPane) {
                int subSize = subPane.getTabCount();
                for (int j = 0; j < subSize; j++) {
                    String mnemonic = KeyEvent.getKeyText(subPane.getMnemonicAt(j)).toUpperCase(Locale.ROOT);
                    EditorInstrument mode = panelMode.get((JPanel) subPane.getComponentAt(j));
                    String title = mode != null ? mode.getTitle() : "";
                    String tooltip = Utility.getWithLinebreaks(title, minimizePrompt, "Hotkey: ALT + " + mnemonic);
                    subPane.setToolTipTextAt(j, tooltip);
                }
            }
        }
    }

}
