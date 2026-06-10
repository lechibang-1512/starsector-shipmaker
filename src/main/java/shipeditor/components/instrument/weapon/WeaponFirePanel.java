package shipeditor.components.instrument.weapon;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.LayerTabUpdated;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.representation.weapon.animation.AnimationType;
import shipeditor.representation.weapon.animation.BarrelMode;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.overseers.StaticController;

import shipeditor.representation.weapon.animation.MuzzleFlashSpec;
import shipeditor.representation.weapon.animation.SmokeSpec;
import shipeditor.utility.text.StringValues;
import shipeditor.utility.graphics.ColorUtilities;
import shipeditor.utility.components.MouseoverLabelListener;
import shipeditor.utility.themes.Themes;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WeaponFirePanel extends AbstractWeaponPropertiesPanel {

    private WeaponLayer cachedLayer;
    private boolean readyForInput;

    private JTextField projectileSpecIdEditor;
    private JComboBox<BarrelMode> barrelModeSelector;
    private JComboBox<AnimationType> animationTypeSelector;

    private JCheckBox autochargeCheckbox;
    private JCheckBox interruptibleBurstCheckbox;
    private JCheckBox requiresFullChargeCheckbox;
    private JCheckBox unaffectedBySpeedBonusesCheckbox;

    private JTextField fireSoundOneEditor;
    private JTextField fireSoundTwoEditor;
    
    private JCheckBox noImpactSoundsCheckbox;
    private JCheckBox noShieldImpactSoundsCheckbox;
    private JCheckBox noNonShieldImpactSoundsCheckbox;
    private JCheckBox playFullFireSoundOneCheckbox;
    private JCheckBox stopPreviousFireSoundCheckbox;

    // Muzzle Flash
    private JTextField mfLengthEditor;
    private JTextField mfSpreadEditor;
    private JTextField mfParticleSizeMinEditor;
    private JTextField mfParticleSizeRangeEditor;
    private JTextField mfParticleDurationEditor;
    private JTextField mfParticleCountEditor;
    private JLabel mfParticleColorValue;

    // Smoke
    private JTextField smokeParticleSizeMinEditor;
    private JTextField smokeParticleSizeRangeEditor;
    private JTextField smokeCloudParticleCountEditor;
    private JTextField smokeCloudDurationEditor;
    private JTextField smokeCloudRadiusEditor;
    private JTextField smokeBlowbackParticleCountEditor;
    private JTextField smokeBlowbackDurationEditor;
    private JTextField smokeBlowbackLengthEditor;
    private JTextField smokeBlowbackSpreadEditor;
    private JLabel smokeParticleColorValue;

    public WeaponFirePanel() {
        super();
    }

    @Override
    protected void populateContent() {
        this.setLayout(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(this, new Insets(1, 0, 0, 0), "Firing & Audio");

        int row = 0;
        
        row = addProjectileFields(row);
        row = addFiringLogicFields(row);
        row = addAudioFields(row);
        row = addMuzzleFlashFields(row);
        row = addSmokeFields(row);

        clearData();
    }

    private int addProjectileFields(int row) {
        projectileSpecIdEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setProjectileSpecId(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Projectile Spec ID:"), projectileSpecIdEditor, row++);

        barrelModeSelector = new JComboBox<>(BarrelMode.values());
        barrelModeSelector.addActionListener(e -> {
            if (readyForInput && cachedLayer != null && cachedLayer.getSpecFile() != null) {
                BarrelMode selected = (BarrelMode) barrelModeSelector.getSelectedItem();
                if (cachedLayer.getSpecFile().getBarrelMode() != selected) {
                    cachedLayer.getSpecFile().setBarrelMode(selected);
                    processChange();
                }
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Barrel Mode:"), barrelModeSelector, row++);

        animationTypeSelector = new JComboBox<>(AnimationType.values());
        animationTypeSelector.addActionListener(e -> {
            if (readyForInput && cachedLayer != null && cachedLayer.getSpecFile() != null) {
                AnimationType selected = (AnimationType) animationTypeSelector.getSelectedItem();
                if (cachedLayer.getSpecFile().getAnimationType() != selected) {
                    cachedLayer.getSpecFile().setAnimationType(selected);
                    processChange();
                }
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Animation Type:"), animationTypeSelector, row++);

        return row;
    }

    private int addFiringLogicFields(int row) {
        autochargeCheckbox = createCheckBox("Autocharge", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setAutocharge(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), autochargeCheckbox, row++);

        interruptibleBurstCheckbox = createCheckBox("Interruptible Burst", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setInterruptibleBurst(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), interruptibleBurstCheckbox, row++);

        requiresFullChargeCheckbox = createCheckBox("Requires Full Charge", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setRequiresFullCharge(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), requiresFullChargeCheckbox, row++);

        unaffectedBySpeedBonusesCheckbox = createCheckBox("Unaffected By Projectile Speed Bonuses", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setUnaffectedByProjectileSpeedBonuses(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), unaffectedBySpeedBonusesCheckbox, row++);

        return row;
    }

    private int addAudioFields(int row) {
        fireSoundOneEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setFireSoundOne(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Fire Sound One:"), fireSoundOneEditor, row++);

        fireSoundTwoEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setFireSoundTwo(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Fire Sound Two:"), fireSoundTwoEditor, row++);

        noImpactSoundsCheckbox = createCheckBox("No Impact Sounds", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setNoImpactSounds(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), noImpactSoundsCheckbox, row++);

        noShieldImpactSoundsCheckbox = createCheckBox("No Shield Impact Sounds", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setNoShieldImpactSounds(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), noShieldImpactSoundsCheckbox, row++);

        noNonShieldImpactSoundsCheckbox = createCheckBox("No Non-Shield Impact Sounds", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setNoNonShieldImpactSounds(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), noNonShieldImpactSoundsCheckbox, row++);

        playFullFireSoundOneCheckbox = createCheckBox("Play Full Fire Sound One", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setPlayFullFireSoundOne(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), playFullFireSoundOneCheckbox, row++);

        stopPreviousFireSoundCheckbox = createCheckBox("Stop Previous Fire Sound", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                cachedLayer.getSpecFile().setStopPreviousFireSound(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel(), stopPreviousFireSoundCheckbox, row++);

        return row;
    }

    private MuzzleFlashSpec getOrCreateMuzzleFlashSpec() {
        WeaponSpecFile spec = cachedLayer.getSpecFile();
        if (spec.getMuzzleFlashSpec() == null) {
            spec.setMuzzleFlashSpec(new MuzzleFlashSpec());
        }
        return spec.getMuzzleFlashSpec();
    }

    private SmokeSpec getOrCreateSmokeSpec() {
        WeaponSpecFile spec = cachedLayer.getSpecFile();
        if (spec.getSmokeSpec() == null) {
            spec.setSmokeSpec(new SmokeSpec());
        }
        return spec.getSmokeSpec();
    }

    private int addMuzzleFlashFields(int row) {
        ComponentUtilities.addLabelAndComponent(this, new JLabel("<html><b>Muzzle Flash</b></html>"), new JLabel(""), row++);
        mfLengthEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateMuzzleFlashSpec().setLength(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Length:"), mfLengthEditor, row++);

        mfSpreadEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateMuzzleFlashSpec().setSpread(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Spread:"), mfSpreadEditor, row++);

        mfParticleSizeMinEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateMuzzleFlashSpec().setParticleSizeMin(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Particle Size Min:"), mfParticleSizeMinEditor, row++);

        mfParticleSizeRangeEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateMuzzleFlashSpec().setParticleSizeRange(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Particle Size Range:"), mfParticleSizeRangeEditor, row++);

        mfParticleDurationEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateMuzzleFlashSpec().setParticleDuration(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Particle Duration:"), mfParticleDurationEditor, row++);

        mfParticleCountEditor = createIntField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateMuzzleFlashSpec().setParticleCount(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Particle Count:"), mfParticleCountEditor, row++);

        mfParticleColorValue = new JLabel();
        JLabel colorLabel = createColorLabel("Particle Color:", mfParticleColorValue,
                () -> cachedLayer != null && cachedLayer.getSpecFile().getMuzzleFlashSpec() != null ? cachedLayer.getSpecFile().getMuzzleFlashSpec().getParticleColor() : null,
                color -> {
                    if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                        getOrCreateMuzzleFlashSpec().setParticleColor(color);
                        processChange();
                    }
                });
        ComponentUtilities.addLabelAndComponent(this, colorLabel, mfParticleColorValue, row++);
        return row;
    }

    private int addSmokeFields(int row) {
        ComponentUtilities.addLabelAndComponent(this, new JLabel("<html><b>Smoke</b></html>"), new JLabel(""), row++);
        smokeParticleSizeMinEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateSmokeSpec().setParticleSizeMin(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Particle Size Min:"), smokeParticleSizeMinEditor, row++);

        smokeParticleSizeRangeEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateSmokeSpec().setParticleSizeRange(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Particle Size Range:"), smokeParticleSizeRangeEditor, row++);

        smokeCloudParticleCountEditor = createIntField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateSmokeSpec().setCloudParticleCount(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Cloud Particle Count:"), smokeCloudParticleCountEditor, row++);

        smokeCloudDurationEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateSmokeSpec().setCloudDuration(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Cloud Duration:"), smokeCloudDurationEditor, row++);

        smokeCloudRadiusEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateSmokeSpec().setCloudRadius(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Cloud Radius:"), smokeCloudRadiusEditor, row++);

        smokeBlowbackParticleCountEditor = createIntField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateSmokeSpec().setBlowbackParticleCount(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Blowback Particle Count:"), smokeBlowbackParticleCountEditor, row++);

        smokeBlowbackDurationEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateSmokeSpec().setBlowbackDuration(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Blowback Duration:"), smokeBlowbackDurationEditor, row++);

        smokeBlowbackLengthEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateSmokeSpec().setBlowbackLength(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Blowback Length:"), smokeBlowbackLengthEditor, row++);

        smokeBlowbackSpreadEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                getOrCreateSmokeSpec().setBlowbackSpread(value);
            }
        });
        ComponentUtilities.addLabelAndComponent(this, new JLabel("Blowback Spread:"), smokeBlowbackSpreadEditor, row++);

        smokeParticleColorValue = new JLabel();
        JLabel colorLabel = createColorLabel("Particle Color:", smokeParticleColorValue,
                () -> cachedLayer != null && cachedLayer.getSpecFile().getSmokeSpec() != null ? cachedLayer.getSpecFile().getSmokeSpec().getParticleColor() : null,
                color -> {
                    if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                        getOrCreateSmokeSpec().setParticleColor(color);
                        processChange();
                    }
                });
        ComponentUtilities.addLabelAndComponent(this, colorLabel, smokeParticleColorValue, row++);
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

    private JLabel createColorLabel(String labelText, JLabel valueLabel, Supplier<Color> getter, Consumer<Color> setter) {
        JLabel label = new JLabel(labelText);
        label.setToolTipText(StringValues.RIGHT_CLICK_TO_CHANGE_COLOR);

        JPopupMenu colorChooserMenu = new JPopupMenu();
        JMenuItem adjustColor = new JMenuItem(StringValues.ADJUST_VALUE);
        adjustColor.addActionListener(event -> {
            Color current = getter.get();
            Color chosen = current != null ? ColorUtilities.showColorChooser(current) : ColorUtilities.showColorChooser();
            if (chosen != null) {
                setter.accept(chosen);
            }
        });
        colorChooserMenu.add(adjustColor);

        JMenuItem removeColor = new JMenuItem("Clear value");
        removeColor.addActionListener(event -> setter.accept(null));
        colorChooserMenu.add(removeColor);

        label.addMouseListener(new MouseoverLabelListener(colorChooserMenu, label));
        Insets insets = ComponentUtilities.createLabelInsets();
        insets.top = 1;
        label.setBorder(ComponentUtilities.createLabelSimpleBorder(insets));

        return label;
    }

    private void updateColorLabel(JLabel valueLabel, Color color) {
        if (color != null) {
            valueLabel.setIcon(ComponentUtilities.createIconFromColor(color, 10, 10));
            valueLabel.setOpaque(true);
            valueLabel.setBorder(new FlatLineBorder(new Insets(2, 2, 2, 2), Color.GRAY));
            valueLabel.setBackground(Color.LIGHT_GRAY);
            valueLabel.setToolTipText(ColorUtilities.getColorBreakdown(color));
            valueLabel.setText(null);
        } else {
            valueLabel.setIcon(null);
            valueLabel.setOpaque(false);
            valueLabel.setBorder(new EmptyBorder(0, 2, 0, 2));
            valueLabel.setBackground(null);
            valueLabel.setToolTipText(null);
            valueLabel.setText("Not defined");
        }
        valueLabel.setForeground(Themes.getTextColor());
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

        projectileSpecIdEditor.setText(spec.getProjectileSpecId() != null ? spec.getProjectileSpecId() : "");
        barrelModeSelector.setSelectedItem(spec.getBarrelMode());
        animationTypeSelector.setSelectedItem(spec.getAnimationType());

        autochargeCheckbox.setSelected(spec.isAutocharge());
        interruptibleBurstCheckbox.setSelected(spec.isInterruptibleBurst());
        requiresFullChargeCheckbox.setSelected(spec.isRequiresFullCharge());
        unaffectedBySpeedBonusesCheckbox.setSelected(spec.isUnaffectedByProjectileSpeedBonuses());

        fireSoundOneEditor.setText(spec.getFireSoundOne() != null ? spec.getFireSoundOne() : "");
        fireSoundTwoEditor.setText(spec.getFireSoundTwo() != null ? spec.getFireSoundTwo() : "");

        noImpactSoundsCheckbox.setSelected(spec.isNoImpactSounds());
        noShieldImpactSoundsCheckbox.setSelected(spec.isNoShieldImpactSounds());
        noNonShieldImpactSoundsCheckbox.setSelected(spec.isNoNonShieldImpactSounds());
        playFullFireSoundOneCheckbox.setSelected(spec.isPlayFullFireSoundOne());
        stopPreviousFireSoundCheckbox.setSelected(spec.isStopPreviousFireSound());

        MuzzleFlashSpec mfSpec = spec.getMuzzleFlashSpec();
        if (mfSpec != null) {
            mfLengthEditor.setText(String.valueOf(mfSpec.getLength()));
            mfSpreadEditor.setText(String.valueOf(mfSpec.getSpread()));
            mfParticleSizeMinEditor.setText(String.valueOf(mfSpec.getParticleSizeMin()));
            mfParticleSizeRangeEditor.setText(String.valueOf(mfSpec.getParticleSizeRange()));
            mfParticleDurationEditor.setText(String.valueOf(mfSpec.getParticleDuration()));
            mfParticleCountEditor.setText(String.valueOf(mfSpec.getParticleCount()));
            updateColorLabel(mfParticleColorValue, mfSpec.getParticleColor());
        } else {
            mfLengthEditor.setText("");
            mfSpreadEditor.setText("");
            mfParticleSizeMinEditor.setText("");
            mfParticleSizeRangeEditor.setText("");
            mfParticleDurationEditor.setText("");
            mfParticleCountEditor.setText("");
            updateColorLabel(mfParticleColorValue, null);
        }

        SmokeSpec smokeSpec = spec.getSmokeSpec();
        if (smokeSpec != null) {
            smokeParticleSizeMinEditor.setText(String.valueOf(smokeSpec.getParticleSizeMin()));
            smokeParticleSizeRangeEditor.setText(String.valueOf(smokeSpec.getParticleSizeRange()));
            smokeCloudParticleCountEditor.setText(String.valueOf(smokeSpec.getCloudParticleCount()));
            smokeCloudDurationEditor.setText(String.valueOf(smokeSpec.getCloudDuration()));
            smokeCloudRadiusEditor.setText(String.valueOf(smokeSpec.getCloudRadius()));
            smokeBlowbackParticleCountEditor.setText(String.valueOf(smokeSpec.getBlowbackParticleCount()));
            smokeBlowbackDurationEditor.setText(String.valueOf(smokeSpec.getBlowbackDuration()));
            smokeBlowbackLengthEditor.setText(String.valueOf(smokeSpec.getBlowbackLength()));
            smokeBlowbackSpreadEditor.setText(String.valueOf(smokeSpec.getBlowbackSpread()));
            updateColorLabel(smokeParticleColorValue, smokeSpec.getParticleColor());
        } else {
            smokeParticleSizeMinEditor.setText("");
            smokeParticleSizeRangeEditor.setText("");
            smokeCloudParticleCountEditor.setText("");
            smokeCloudDurationEditor.setText("");
            smokeCloudRadiusEditor.setText("");
            smokeBlowbackParticleCountEditor.setText("");
            smokeBlowbackDurationEditor.setText("");
            smokeBlowbackLengthEditor.setText("");
            smokeBlowbackSpreadEditor.setText("");
            updateColorLabel(smokeParticleColorValue, null);
        }

        readyForInput = true;
    }

    private void clearData() {
        readyForInput = false;

        projectileSpecIdEditor.setText("");
        barrelModeSelector.setSelectedItem(null);
        animationTypeSelector.setSelectedItem(null);

        autochargeCheckbox.setSelected(false);
        interruptibleBurstCheckbox.setSelected(false);
        requiresFullChargeCheckbox.setSelected(false);
        unaffectedBySpeedBonusesCheckbox.setSelected(false);

        fireSoundOneEditor.setText("");
        fireSoundTwoEditor.setText("");

        noImpactSoundsCheckbox.setSelected(false);
        noShieldImpactSoundsCheckbox.setSelected(false);
        noNonShieldImpactSoundsCheckbox.setSelected(false);
        playFullFireSoundOneCheckbox.setSelected(false);
        stopPreviousFireSoundCheckbox.setSelected(false);

        mfLengthEditor.setText("");
        mfSpreadEditor.setText("");
        mfParticleSizeMinEditor.setText("");
        mfParticleSizeRangeEditor.setText("");
        mfParticleDurationEditor.setText("");
        mfParticleCountEditor.setText("");
        updateColorLabel(mfParticleColorValue, null);

        smokeParticleSizeMinEditor.setText("");
        smokeParticleSizeRangeEditor.setText("");
        smokeCloudParticleCountEditor.setText("");
        smokeCloudDurationEditor.setText("");
        smokeCloudRadiusEditor.setText("");
        smokeBlowbackParticleCountEditor.setText("");
        smokeBlowbackDurationEditor.setText("");
        smokeBlowbackLengthEditor.setText("");
        smokeBlowbackSpreadEditor.setText("");
        updateColorLabel(smokeParticleColorValue, null);

        cachedLayer = null;
    }
}
