package shipeditor.components.instrument.weapon;

import shipeditor.communication.EventBus;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;
import shipeditor.communication.events.components.ComponentEvents.LayerTabUpdated;

public class WeaponDataPanel extends AbstractWeaponPropertiesPanel {

    private WeaponLayer cachedLayer;
    private JTextField idEditor;
    private JComboBox<String> specClassSelector;
    private JComboBox<WeaponType> typeSelector;
    private JComboBox<WeaponSize> sizeSelector;
    private JComboBox<WeaponType> mountTypeOverrideSelector;
    private JComboBox<String> collisionClassSelector;
    private JComboBox<String> collisionClassByFighterSelector;

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
        addSpecClassSelector();
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

    private static final String[] SPEC_CLASS_SUGGESTIONS = {"projectile", "beam"};

    private void addSpecClassSelector() {
        JLabel label = new JLabel("Spec Class:");
        specClassSelector = new JComboBox<>(SPEC_CLASS_SUGGESTIONS);
        specClassSelector.setEditable(true);
        specClassSelector.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                WeaponSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    String selected = (String) specClassSelector.getSelectedItem();
                    if (!Objects.equals(spec.getSpecClass(), selected)) {
                        spec.setSpecClass(selected);
                        EventBus.publish(new LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });

        ComponentUtilities.addLabelAndComponent(this, label, specClassSelector, 1);
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

        ComponentUtilities.addLabelAndComponent(this, label, typeSelector, 2);
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

        ComponentUtilities.addLabelAndComponent(this, label, sizeSelector, 3);
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

        ComponentUtilities.addLabelAndComponent(this, label, mountTypeOverrideSelector, 4);
    }

    private static final String[] COLLISION_CLASS_SUGGESTIONS = {
        "NONE", "RAY", "RAY_FIGHTER", "FIGHTER", "SHIP",
        "PROJECTILE_NO_FF", "PROJECTILE_FF",
        "MISSILE_NO_FF", "MISSILE_FF",
        "HITS_SHIPS_AND_ASTEROIDS",
        "HITS_SHIPS_ONLY_FF", "HITS_SHIPS_ONLY_NO_FF",
        "PROJECTILE_FIGHTER", "ASTEROID"
    };

    private void addCollisionClassPanel() {
        JLabel label = new JLabel("Collision Class:");
        collisionClassSelector = new JComboBox<>(COLLISION_CLASS_SUGGESTIONS);
        collisionClassSelector.setEditable(true);
        collisionClassSelector.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                WeaponSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    String text = (String) collisionClassSelector.getSelectedItem();
                    if (!Objects.equals(spec.getCollisionClass(), text)) {
                        spec.setCollisionClass(text);
                        EventBus.publish(new LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });

        ComponentUtilities.addLabelAndComponent(this, label, collisionClassSelector, 5);
    }

    private void addCollisionClassByFighterPanel() {
        JLabel label = new JLabel("Collision Class By Fighter:");
        collisionClassByFighterSelector = new JComboBox<>(COLLISION_CLASS_SUGGESTIONS);
        collisionClassByFighterSelector.setEditable(true);
        collisionClassByFighterSelector.addActionListener(e -> {
            if (readyForInput && cachedLayer != null) {
                WeaponSpecFile spec = cachedLayer.getSpecFile();
                if (spec != null) {
                    String text = (String) collisionClassByFighterSelector.getSelectedItem();
                    if (!Objects.equals(spec.getCollisionClassByFighter(), text)) {
                        spec.setCollisionClassByFighter(text);
                        EventBus.publish(new LayerTabUpdated(cachedLayer));
                        processChange();
                    }
                }
            }
        });

        ComponentUtilities.addLabelAndComponent(this, label, collisionClassByFighterSelector, 6);
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
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), showDamageWhenDecorativeCheckbox, 7);

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
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), passThroughMissilesCheckbox, 8);
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
        specClassSelector.setSelectedItem(spec.getSpecClass() != null ? spec.getSpecClass() : "");
        typeSelector.setSelectedItem(spec.getType());
        sizeSelector.setSelectedItem(spec.getSize());
        mountTypeOverrideSelector.setSelectedItem(spec.getMountTypeOverride());
        collisionClassSelector.setSelectedItem(spec.getCollisionClass() != null ? spec.getCollisionClass() : "");
        collisionClassByFighterSelector.setSelectedItem(spec.getCollisionClassByFighter() != null ? spec.getCollisionClassByFighter() : "");
        showDamageWhenDecorativeCheckbox.setSelected(spec.isShowDamageWhenDecorative());
        passThroughMissilesCheckbox.setSelected(spec.isPassThroughMissiles());

        readyForInput = true;
    }

    private void clearData() {
        readyForInput = false;

        idEditor.setText("");
        specClassSelector.setSelectedItem(null);
        typeSelector.setSelectedItem(null);
        sizeSelector.setSelectedItem(null);
        mountTypeOverrideSelector.setSelectedItem(null);
        collisionClassSelector.setSelectedItem(null);
        collisionClassByFighterSelector.setSelectedItem(null);
        showDamageWhenDecorativeCheckbox.setSelected(false);
        passThroughMissilesCheckbox.setSelected(false);

        cachedLayer = null;
    }
}
