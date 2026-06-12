package shipeditor.components.instrument.weapon;

import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class WeaponBeamPanel extends AbstractWeaponPropertiesPanel {

    private WeaponLayer cachedLayer;
    private boolean readyForInput;

    private JTextField everyFrameEffectEditor;
    private JTextField beamEffectEditor;
    
    private JCheckBox beamFireOnlyOnFullChargeCheckbox;
    private JCheckBox useGlowColorForHitGlowCheckbox;
    private JCheckBox darkCoreCheckbox;
    private JCheckBox convergeOnPointCheckbox;
    private JCheckBox skipIdleFrameIfZeroBurstDelayCheckbox;

    private JTextField widthEditor;
    private JTextField coreWidthMultEditor;
    private JTextField textureScrollSpeedEditor;
    private JTextField pixelsPerTexelEditor;
    private JTextField fringeScrollSpeedMultEditor;

    private JTextField hitGlowBrightenDurationEditor;
    private JTextField hitGlowRadiusEditor;
    private JTextField specialWeaponGlowWidthEditor;
    private JTextField specialWeaponGlowHeightEditor;

    private JTextField textureTypeEditor;
    private JTextField pierceSetEditor;

    public WeaponBeamPanel() {
        super();
    }

    @Override
    protected void populateContent() {
        this.setLayout(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(this, new Insets(1, 0, 0, 0), "Beam Specifics");

        int row = 0;
        
        row = addEffectFields(row);
        row = addGeometryFields(row);
        row = addHitGlowFields(row);
        row = addMiscFields(row);

        clearData();
    }

    private int addEffectFields(int row) {
        everyFrameEffectEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setEveryFrameEffect(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Every Frame Effect:"), everyFrameEffectEditor, row++);

        beamEffectEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setBeamEffect(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Beam Effect:"), beamEffectEditor, row++);

        beamFireOnlyOnFullChargeCheckbox = createCheckBox("Beam Fire Only On Full Charge", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setBeamFireOnlyOnFullCharge(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), beamFireOnlyOnFullChargeCheckbox, row++);

        return row;
    }

    private int addGeometryFields(int row) {
        widthEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setWidth(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Width:"), widthEditor, row++);

        coreWidthMultEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setCoreWidthMult(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Core Width Mult:"), coreWidthMultEditor, row++);

        textureScrollSpeedEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setTextureScrollSpeed(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Texture Scroll Speed:"), textureScrollSpeedEditor, row++);

        fringeScrollSpeedMultEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setFringeScrollSpeedMult(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Fringe Scroll Speed Mult:"), fringeScrollSpeedMultEditor, row++);

        pixelsPerTexelEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setPixelsPerTexel(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Pixels Per Texel:"), pixelsPerTexelEditor, row++);

        textureTypeEditor = createListField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setTextureType(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Texture Type (comma-separated):"), textureTypeEditor, row++);

        return row;
    }

    private int addHitGlowFields(int row) {
        hitGlowBrightenDurationEditor = createIntField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setHitGlowBrightenDuration(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Hit Glow Brighten Duration:"), hitGlowBrightenDurationEditor, row++);

        hitGlowRadiusEditor = createIntField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setHitGlowRadius(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Hit Glow Radius:"), hitGlowRadiusEditor, row++);

        specialWeaponGlowWidthEditor = createIntField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setSpecialWeaponGlowWidth(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Special Weapon Glow Width:"), specialWeaponGlowWidthEditor, row++);

        specialWeaponGlowHeightEditor = createIntField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setSpecialWeaponGlowHeight(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Special Weapon Glow Height:"), specialWeaponGlowHeightEditor, row++);

        return row;
    }

    private int addMiscFields(int row) {
        pierceSetEditor = createListField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setPierceSet(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Pierce Set (comma-separated):"), pierceSetEditor, row++);

        useGlowColorForHitGlowCheckbox = createCheckBox("Use Glow Color For Hit Glow", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setUseGlowColorForHitGlow(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), useGlowColorForHitGlowCheckbox, row++);

        darkCoreCheckbox = createCheckBox("Dark Core", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setDarkCore(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), darkCoreCheckbox, row++);

        convergeOnPointCheckbox = createCheckBox("Converge On Point", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setConvergeOnPoint(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), convergeOnPointCheckbox, row++);

        skipIdleFrameIfZeroBurstDelayCheckbox = createCheckBox("Skip Idle Frame If Zero Burst Delay", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setSkipIdleFrameIfZeroBurstDelay(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), skipIdleFrameIfZeroBurstDelayCheckbox, row++);

        return row;
    }

    private JTextField createTextField(Consumer<String> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                setter.accept(textField.getText());
                processChange();
            }
        });
        return textField;
    }

    private JTextField createDoubleField(Consumer<Double> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                try {
                    setter.accept(Double.parseDouble(textField.getText()));
                    processChange();
                } catch (NumberFormatException ex) {
                    // Ignore invalid input
                }
            }
        });
        return textField;
    }

    private JTextField createIntField(Consumer<Integer> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                try {
                    setter.accept(Integer.parseInt(textField.getText()));
                    processChange();
                } catch (NumberFormatException ex) {
                    // Ignore invalid input
                }
            }
        });
        return textField;
    }

    private JTextField createListField(Consumer<List<String>> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                String text = textField.getText();
                if (text.isEmpty()) {
                    setter.accept(null);
                } else {
                    setter.accept(Arrays.asList(text.split("\\s*,\\s*")));
                }
                processChange();
            }
        });
        return textField;
    }

    private JCheckBox createCheckBox(String text, Consumer<Boolean> setter) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.addActionListener(e -> {
            if (readyForInput) {
                setter.accept(checkBox.isSelected());
                processChange();
            }
        });
        return checkBox;
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

        everyFrameEffectEditor.setText(spec.getEveryFrameEffect() != null ? spec.getEveryFrameEffect() : "");
        beamEffectEditor.setText(spec.getBeamEffect() != null ? spec.getBeamEffect() : "");
        beamFireOnlyOnFullChargeCheckbox.setSelected(spec.isBeamFireOnlyOnFullCharge());

        widthEditor.setText(String.valueOf(spec.getWidth()));
        coreWidthMultEditor.setText(String.valueOf(spec.getCoreWidthMult()));
        textureScrollSpeedEditor.setText(String.valueOf(spec.getTextureScrollSpeed()));
        fringeScrollSpeedMultEditor.setText(String.valueOf(spec.getFringeScrollSpeedMult()));
        pixelsPerTexelEditor.setText(String.valueOf(spec.getPixelsPerTexel()));
        
        textureTypeEditor.setText(spec.getTextureType() != null ? String.join(", ", spec.getTextureType()) : "");

        hitGlowBrightenDurationEditor.setText(String.valueOf(spec.getHitGlowBrightenDuration()));
        hitGlowRadiusEditor.setText(String.valueOf(spec.getHitGlowRadius()));
        specialWeaponGlowWidthEditor.setText(String.valueOf(spec.getSpecialWeaponGlowWidth()));
        specialWeaponGlowHeightEditor.setText(String.valueOf(spec.getSpecialWeaponGlowHeight()));

        pierceSetEditor.setText(spec.getPierceSet() != null ? String.join(", ", spec.getPierceSet()) : "");
        useGlowColorForHitGlowCheckbox.setSelected(spec.isUseGlowColorForHitGlow());
        darkCoreCheckbox.setSelected(spec.isDarkCore());
        convergeOnPointCheckbox.setSelected(spec.isConvergeOnPoint());
        skipIdleFrameIfZeroBurstDelayCheckbox.setSelected(spec.isSkipIdleFrameIfZeroBurstDelay());

        readyForInput = true;
    }

    private void clearData() {
        readyForInput = false;

        everyFrameEffectEditor.setText("");
        beamEffectEditor.setText("");
        beamFireOnlyOnFullChargeCheckbox.setSelected(false);

        widthEditor.setText("");
        coreWidthMultEditor.setText("");
        textureScrollSpeedEditor.setText("");
        fringeScrollSpeedMultEditor.setText("");
        pixelsPerTexelEditor.setText("");
        textureTypeEditor.setText("");

        hitGlowBrightenDurationEditor.setText("");
        hitGlowRadiusEditor.setText("");
        specialWeaponGlowWidthEditor.setText("");
        specialWeaponGlowHeightEditor.setText("");

        pierceSetEditor.setText("");
        useGlowColorForHitGlowCheckbox.setSelected(false);
        darkCoreCheckbox.setSelected(false);
        convergeOnPointCheckbox.setSelected(false);
        skipIdleFrameIfZeroBurstDelayCheckbox.setSelected(false);

        cachedLayer = null;
    }
}
