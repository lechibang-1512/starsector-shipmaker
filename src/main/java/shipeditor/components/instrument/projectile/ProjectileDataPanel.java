package shipeditor.components.instrument.projectile;

import shipeditor.communication.EventBus;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.weapon.ProjectileLayer;
import shipeditor.representation.weapon.ProjectileSpecFile;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ProjectileDataPanel extends AbstractProjectilePropertiesPanel {

    private ProjectileLayer cachedLayer;
    private JTextField idEditor;
    private JTextField specClassEditor;
    private JTextField missileTypeEditor;

    private boolean readyForInput;

    public ProjectileDataPanel() {
        super();
    }

    @Override
    protected void populateContent() {
        this.setLayout(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(this, new Insets(1, 0, 0, 0), "Projectile data");

        addIDPanel();
        addSpecClassPanel();
        addMissileTypePanel();

        clearData();
    }

    private void addIDPanel() {
        JLabel label = new JLabel("ID:");
        idEditor = new JTextField();
        idEditor.setColumns(10);
        idEditor.setEditable(false);
        idEditor.setToolTipText("ID is read-only from the spec file");

        ComponentUtilities.addLabelAndComponent(this, label, idEditor, 0);
    }

    private void addSpecClassPanel() {
        JLabel label = new JLabel("Spec Class:");
        specClassEditor = new JTextField();
        specClassEditor.setColumns(10);
        specClassEditor.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                ProjectileSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    String text = specClassEditor.getText();
                    if (!java.util.Objects.equals(spec.getSpecClass(), text)) {
                        spec.setSpecClass(text);
                        EventBus.publish(new shipeditor.communication.events.components.LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });

        ComponentUtilities.addLabelAndComponent(this, label, specClassEditor, 1);
    }

    private void addMissileTypePanel() {
        JLabel label = new JLabel("Missile Type:");
        missileTypeEditor = new JTextField();
        missileTypeEditor.setColumns(10);
        missileTypeEditor.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                ProjectileSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    String text = missileTypeEditor.getText();
                    if (!java.util.Objects.equals(spec.getMissileType(), text)) {
                        spec.setMissileType(text);
                        EventBus.publish(new shipeditor.communication.events.components.LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });

        ComponentUtilities.addLabelAndComponent(this, label, missileTypeEditor, 2);
    }

    @Override
    public void refreshContent(LayerPainter layerPainter) {
        if (layerPainter == null || !(layerPainter.getParentLayer() instanceof ProjectileLayer projectileLayer)) {
            clearData();
            return;
        }
        cachedLayer = projectileLayer;
        ProjectileSpecFile spec = cachedLayer.getSpecFile();
        if (spec == null) {
            clearData();
            return;
        }

        readyForInput = false;

        idEditor.setText(spec.getId() != null ? spec.getId() : "");
        specClassEditor.setText(spec.getSpecClass() != null ? spec.getSpecClass() : "");
        missileTypeEditor.setText(spec.getMissileType() != null ? spec.getMissileType() : "");

        readyForInput = true;
    }

    private void clearData() {
        readyForInput = false;

        idEditor.setText("");
        specClassEditor.setText("");
        missileTypeEditor.setText("");

        cachedLayer = null;
    }
}
