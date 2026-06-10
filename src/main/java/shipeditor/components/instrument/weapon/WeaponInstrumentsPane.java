package shipeditor.components.instrument.weapon;

import shipeditor.components.instrument.AbstractInstrumentsPane;
import shipeditor.components.instrument.EditorInstrument;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

public class WeaponInstrumentsPane extends AbstractInstrumentsPane {

    public WeaponInstrumentsPane() {
        createTabs();
        this.dispatchModeChange((JPanel) getSelectedComponent());
    }

    private void createTabs() {
        JPanel layerPanel = new JPanel();
        layerPanel.setLayout(new BorderLayout());

        JPanel layerWidgetsPanel = new WeaponLayerInfoPanel();
        layerPanel.add(layerWidgetsPanel, BorderLayout.CENTER);

        panelMode.put(layerPanel, EditorInstrument.LAYER);
        this.addTab(EditorInstrument.LAYER.getTitle(), null, layerPanel, EditorInstrument.LAYER.getTitle());

        WeaponOffsetsPanel offsetPanel = new WeaponOffsetsPanel();
        panelMode.put(offsetPanel, EditorInstrument.WEAPON_OFFSETS);
        this.addTab(EditorInstrument.WEAPON_OFFSETS.getTitle(), null, offsetPanel, EditorInstrument.WEAPON_OFFSETS.getTitle());

        WeaponDataPanel dataPanel = new WeaponDataPanel();
        JScrollPane dataScroll = new JScrollPane(dataPanel);
        dataScroll.setBorder(null);
        panelMode.put(dataPanel, EditorInstrument.WEAPON_DATA);
        this.addTab(EditorInstrument.WEAPON_DATA.getTitle(), null, dataScroll, EditorInstrument.WEAPON_DATA.getTitle());

        WeaponVisualsPanel visualsPanel = new WeaponVisualsPanel();
        JScrollPane visualsScroll = new JScrollPane(visualsPanel);
        visualsScroll.setBorder(null);
        panelMode.put(visualsPanel, EditorInstrument.WEAPON_VISUALS);
        this.addTab(EditorInstrument.WEAPON_VISUALS.getTitle(), null, visualsScroll, EditorInstrument.WEAPON_VISUALS.getTitle());

        WeaponFirePanel firePanel = new WeaponFirePanel();
        JScrollPane fireScroll = new JScrollPane(firePanel);
        fireScroll.setBorder(null);
        panelMode.put(firePanel, EditorInstrument.WEAPON_FIRE);
        this.addTab(EditorInstrument.WEAPON_FIRE.getTitle(), null, fireScroll, EditorInstrument.WEAPON_FIRE.getTitle());

        WeaponBeamPanel beamPanel = new WeaponBeamPanel();
        JScrollPane beamScroll = new JScrollPane(beamPanel);
        beamScroll.setBorder(null);
        panelMode.put(beamPanel, EditorInstrument.WEAPON_BEAM);
        this.addTab(EditorInstrument.WEAPON_BEAM.getTitle(), null, beamScroll, EditorInstrument.WEAPON_BEAM.getTitle());
    }

}
