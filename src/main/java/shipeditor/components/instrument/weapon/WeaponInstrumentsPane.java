package shipeditor.components.instrument.weapon;

import shipeditor.components.instrument.AbstractInstrumentsPane;
import shipeditor.components.ComponentEnums.EditorInstrument;

import javax.swing.JScrollPane;

/**
 * Weapon instrument pane with three tabs:
 * 1. Properties (identity, collision, projectile, firing logic, audio, beam)
 * 2. Sprites & Rendering (turret/hardpoint sprites, render flags, colors, animation, muzzle flash, smoke)
 * 3. Offsets (firing positions table + recoil controls)
 */
public class WeaponInstrumentsPane extends AbstractInstrumentsPane {

    public WeaponInstrumentsPane() {
        WeaponPropertiesPanel firstPanel = createTabs();
        this.dispatchModeChange(firstPanel);
    }

    /** Returns the first panel so the constructor can dispatch the initial mode change. */
    private WeaponPropertiesPanel createTabs() {
        // Tab 1: Properties
        WeaponPropertiesPanel propertiesPanel = new WeaponPropertiesPanel();
        JScrollPane propertiesScroll = new JScrollPane(propertiesPanel);
        propertiesScroll.setBorder(null);
        propertiesScroll.getVerticalScrollBar().setUnitIncrement(16);
        panelMode.put(propertiesPanel, EditorInstrument.WEAPON_DATA);
        this.addTab("Properties", null, propertiesScroll, EditorInstrument.WEAPON_DATA.getTitle());

        // Tab 2: Sprites & Rendering
        WeaponVisualsPanel visualsPanel = new WeaponVisualsPanel();
        JScrollPane visualsScroll = new JScrollPane(visualsPanel);
        visualsScroll.setBorder(null);
        visualsScroll.getVerticalScrollBar().setUnitIncrement(16);
        panelMode.put(visualsPanel, EditorInstrument.WEAPON_VISUALS);
        this.addTab("Sprites", null, visualsScroll, EditorInstrument.WEAPON_VISUALS.getTitle());

        // Tab 3: Offsets (unchanged)
        WeaponOffsetsPanel offsetPanel = new WeaponOffsetsPanel();
        panelMode.put(offsetPanel, EditorInstrument.WEAPON_OFFSETS);
        this.addTab("Offsets", null, offsetPanel, EditorInstrument.WEAPON_OFFSETS.getTitle());

        return propertiesPanel;
    }

}
