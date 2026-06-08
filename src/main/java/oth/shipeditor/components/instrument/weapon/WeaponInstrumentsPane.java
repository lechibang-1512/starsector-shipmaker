package oth.shipeditor.components.instrument.weapon;

import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.viewer.ViewerRepaintQueued;
import oth.shipeditor.communication.events.viewer.points.InstrumentModeChanged;
import oth.shipeditor.components.instrument.AbstractInstrumentsPane;
import oth.shipeditor.components.instrument.EditorInstrument;


import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class WeaponInstrumentsPane extends AbstractInstrumentsPane {

    private final Map<JPanel, EditorInstrument> panelMode;

    public WeaponInstrumentsPane() {
        panelMode = new HashMap<>();
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
    }

    @Override
    protected void dispatchModeChange(JPanel active) {
        EditorInstrument selected = panelMode.get(active);
        if (selected != null) {
            EventBus.publish(new InstrumentModeChanged(selected));
            EventBus.publish(new ViewerRepaintQueued());
        }
    }

    @Override
    protected void updateTooltipText() {
        String minimizePrompt = getMinimizePrompt();
        int size = this.getTabCount();
        for (int i = 0; i < size; i++) {
            EditorInstrument mode = panelMode.get((JPanel) this.getComponentAt(i));
            String title = mode != null ? mode.getTitle() : "";
            String tooltip = oth.shipeditor.utility.Utility.getWithLinebreaks(title, minimizePrompt);
            this.setToolTipTextAt(i, tooltip);
        }
    }

}
