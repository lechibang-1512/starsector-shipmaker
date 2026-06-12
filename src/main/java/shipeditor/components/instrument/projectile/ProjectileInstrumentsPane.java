package shipeditor.components.instrument.projectile;

import shipeditor.components.instrument.AbstractInstrumentsPane;
import shipeditor.components.ComponentEnums.EditorInstrument;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Component;

public class ProjectileInstrumentsPane extends AbstractInstrumentsPane {

    public ProjectileInstrumentsPane() {
        createTabs();
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

    private void createTabs() {
        JPanel layerPanel = new JPanel();
        layerPanel.setLayout(new BorderLayout());

        JPanel layerWidgetsPanel = new ProjectileLayerInfoPanel();
        layerPanel.add(layerWidgetsPanel, BorderLayout.CENTER);

        panelMode.put(layerPanel, EditorInstrument.LAYER);
        this.addTab(EditorInstrument.LAYER.getTitle(), null, layerPanel, EditorInstrument.LAYER.getTitle());

        ProjectileDataPanel dataPanel = new ProjectileDataPanel();
        panelMode.put(dataPanel, EditorInstrument.PROJECTILE_DATA);
        this.addTab(EditorInstrument.PROJECTILE_DATA.getTitle(), null, dataPanel, EditorInstrument.PROJECTILE_DATA.getTitle());
    }

}
