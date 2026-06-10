package shipeditor.components.instrument.weapon;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.LayerTabUpdated;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.representation.weapon.WeaponSize;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.representation.weapon.WeaponType;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

public class WeaponDataPanel extends AbstractWeaponPropertiesPanel {

    private WeaponLayer cachedLayer;
    private JTextField idEditor;
    private JComboBox<WeaponType> typeSelector;
    private JComboBox<WeaponSize> sizeSelector;
    private JComboBox<WeaponType> mountTypeOverrideSelector;
    private JTextField collisionClassEditor;
    private JTextField collisionClassByFighterEditor;

    private JCheckBox showDamageWhenDecorativeCheckbox;
    private JCheckBox passThroughMissilesCheckbox;

    private boolean readyForInput;

    public WeaponDataPanel() {
        super();
    }

    @Override
    protected void populateContent() {
        this.setLayout(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(this, new Insets(1, 0, 0, 0), "Weapon data");

        addIDPanel();
        addTypeSelector();
        addSizeSelector();
        addMountTypeOverrideSelector();
        addCollisionClassPanel();
        addCollisionClassByFighterPanel();
        addMiscCheckboxes();

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

    private void addTypeSelector() {
        JLabel label = new JLabel("Type:");
        typeSelector = new JComboBox<>(WeaponType.values());
        typeSelector.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                WeaponSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    WeaponType selected = (WeaponType) typeSelector.getSelectedItem();
                    if (spec.getType() != selected) {
                        spec.setType(selected);
                        EventBus.publish(new LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });

        ComponentUtilities.addLabelAndComponent(this, label, typeSelector, 1);
    }

    private void addSizeSelector() {
        JLabel label = new JLabel("Size:");
        sizeSelector = new JComboBox<>(WeaponSize.values());
        sizeSelector.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                WeaponSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    WeaponSize selected = (WeaponSize) sizeSelector.getSelectedItem();
                    if (spec.getSize() != selected) {
                        spec.setSize(selected);
                        EventBus.publish(new LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });

        ComponentUtilities.addLabelAndComponent(this, label, sizeSelector, 2);
    }

    private void addMountTypeOverrideSelector() {
        JLabel label = new JLabel("Mount Type Override:");
        mountTypeOverrideSelector = new JComboBox<>(WeaponType.values());
        mountTypeOverrideSelector.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                WeaponSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    WeaponType selected = (WeaponType) mountTypeOverrideSelector.getSelectedItem();
                    if (spec.getMountTypeOverride() != selected) {
                        spec.setMountTypeOverride(selected);
                        EventBus.publish(new LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });

        ComponentUtilities.addLabelAndComponent(this, label, mountTypeOverrideSelector, 3);
    }

    private void addCollisionClassPanel() {
        JLabel label = new JLabel("Collision Class:");
        collisionClassEditor = new JTextField();
        collisionClassEditor.setColumns(10);
        collisionClassEditor.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                WeaponSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    String text = collisionClassEditor.getText();
                    if (!Objects.equals(spec.getCollisionClass(), text)) {
                        spec.setCollisionClass(text);
                        EventBus.publish(new LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });

        ComponentUtilities.addLabelAndComponent(this, label, collisionClassEditor, 4);
    }

    private void addCollisionClassByFighterPanel() {
        JLabel label = new JLabel("Collision Class By Fighter:");
        collisionClassByFighterEditor = new JTextField();
        collisionClassByFighterEditor.setColumns(10);
        collisionClassByFighterEditor.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                WeaponSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    String text = collisionClassByFighterEditor.getText();
                    if (!Objects.equals(spec.getCollisionClassByFighter(), text)) {
                        spec.setCollisionClassByFighter(text);
                        EventBus.publish(new LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });

        ComponentUtilities.addLabelAndComponent(this, label, collisionClassByFighterEditor, 5);
    }

    private void addMiscCheckboxes() {
        showDamageWhenDecorativeCheckbox = new JCheckBox("Show Damage When Decorative");
        showDamageWhenDecorativeCheckbox.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                WeaponSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    if (spec.isShowDamageWhenDecorative() != showDamageWhenDecorativeCheckbox.isSelected()) {
                        spec.setShowDamageWhenDecorative(showDamageWhenDecorativeCheckbox.isSelected());
                        EventBus.publish(new LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), showDamageWhenDecorativeCheckbox, 6);

        passThroughMissilesCheckbox = new JCheckBox("Pass Through Missiles");
        passThroughMissilesCheckbox.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                WeaponSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    if (spec.isPassThroughMissiles() != passThroughMissilesCheckbox.isSelected()) {
                        spec.setPassThroughMissiles(passThroughMissilesCheckbox.isSelected());
                        EventBus.publish(new LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), passThroughMissilesCheckbox, 7);
    }

    @Override
    public void refreshContent(LayerPainter layerPainter) {
        if (layerPainter == null || !(layerPainter.getParentLayer() instanceof WeaponLayer weaponLayer)) {
            clearData();
            return;
        }
        cachedLayer = weaponLayer;
        WeaponSpecFile spec = cachedLayer.getSpecFile();
        if (spec == null) {
            clearData();
            return;
        }

        readyForInput = false;

        idEditor.setText(spec.getId() != null ? spec.getId() : "");
        typeSelector.setSelectedItem(spec.getType());
        sizeSelector.setSelectedItem(spec.getSize());
        mountTypeOverrideSelector.setSelectedItem(spec.getMountTypeOverride());
        collisionClassEditor.setText(spec.getCollisionClass() != null ? spec.getCollisionClass() : "");
        collisionClassByFighterEditor.setText(spec.getCollisionClassByFighter() != null ? spec.getCollisionClassByFighter() : "");
        showDamageWhenDecorativeCheckbox.setSelected(spec.isShowDamageWhenDecorative());
        passThroughMissilesCheckbox.setSelected(spec.isPassThroughMissiles());

        readyForInput = true;
    }

    private void clearData() {
        readyForInput = false;

        idEditor.setText("");
        typeSelector.setSelectedItem(null);
        sizeSelector.setSelectedItem(null);
        mountTypeOverrideSelector.setSelectedItem(null);
        collisionClassEditor.setText("");
        collisionClassByFighterEditor.setText("");
        showDamageWhenDecorativeCheckbox.setSelected(false);
        passThroughMissilesCheckbox.setSelected(false);

        cachedLayer = null;
    }
}
