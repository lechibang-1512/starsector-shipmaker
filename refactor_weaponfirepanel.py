import os

base_dir = "src/main/java/shipeditor/components/instrument/weapon"
os.makedirs(base_dir, exist_ok=True)

utils_code = """package shipeditor.components.instrument.weapon;

import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.MouseoverLabelListener;
import shipeditor.utility.graphics.ColorUtilities;
import shipeditor.utility.text.StringValues;
import shipeditor.utility.themes.Themes;
import com.formdev.flatlaf.ui.FlatLineBorder;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Insets;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WeaponFirePanelUtilities {

    public static JTextField createTextField(Supplier<Boolean> readinessChecker, Consumer<String> setter, Runnable onChange) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readinessChecker.get()) {
                setter.accept(textField.getText());
                onChange.run();
            }
        });
        return textField;
    }

    public static JTextField createDoubleField(Supplier<Boolean> readinessChecker, Consumer<Double> setter, Runnable onChange) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readinessChecker.get()) {
                try {
                    setter.accept(Double.parseDouble(textField.getText()));
                    onChange.run();
                } catch (NumberFormatException ex) {
                    // Ignore invalid input
                }
            }
        });
        return textField;
    }

    public static JTextField createIntField(Supplier<Boolean> readinessChecker, Consumer<Integer> setter, Runnable onChange) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readinessChecker.get()) {
                try {
                    setter.accept(Integer.parseInt(textField.getText()));
                    onChange.run();
                } catch (NumberFormatException ex) {
                    // Ignore invalid input
                }
            }
        });
        return textField;
    }

    public static JCheckBox createCheckBox(String text, Supplier<Boolean> readinessChecker, Consumer<Boolean> setter, Runnable onChange) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.addActionListener(e -> {
            if (readinessChecker.get()) {
                setter.accept(checkBox.isSelected());
                onChange.run();
            }
        });
        return checkBox;
    }

    public static JLabel createColorLabel(String labelText, JLabel valueLabel, Supplier<Color> getter, Consumer<Color> setter) {
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

    public static void updateColorLabel(JLabel valueLabel, Color color) {
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
}
"""

projectile_code = """package shipeditor.components.instrument.weapon;

import shipeditor.representation.weapon.WeaponEnums.AnimationType;
import shipeditor.representation.weapon.WeaponEnums.BarrelMode;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.function.Supplier;

public class WeaponProjectileHandler {
    private final JTextField projectileSpecIdEditor;
    private final JComboBox<BarrelMode> barrelModeSelector;
    private final JComboBox<AnimationType> animationTypeSelector;

    public WeaponProjectileHandler(Supplier<Boolean> readinessChecker, Runnable onChange, Supplier<WeaponSpecFile> specSupplier) {
        projectileSpecIdEditor = WeaponFirePanelUtilities.createTextField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setProjectileSpecId(value);
        }, onChange);

        barrelModeSelector = new JComboBox<>(BarrelMode.values());
        barrelModeSelector.addActionListener(e -> {
            if (readinessChecker.get()) {
                WeaponSpecFile spec = specSupplier.get();
                if (spec != null) {
                    BarrelMode selected = (BarrelMode) barrelModeSelector.getSelectedItem();
                    if (spec.getBarrelMode() != selected) {
                        spec.setBarrelMode(selected);
                        onChange.run();
                    }
                }
            }
        });

        animationTypeSelector = new JComboBox<>(AnimationType.values());
        animationTypeSelector.addActionListener(e -> {
            if (readinessChecker.get()) {
                WeaponSpecFile spec = specSupplier.get();
                if (spec != null) {
                    AnimationType selected = (AnimationType) animationTypeSelector.getSelectedItem();
                    if (spec.getAnimationType() != selected) {
                        spec.setAnimationType(selected);
                        onChange.run();
                    }
                }
            }
        });
    }

    public int populate(JPanel panel, int startRow) {
        int row = startRow;
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Projectile Spec ID:"), projectileSpecIdEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Barrel Mode:"), barrelModeSelector, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Animation Type:"), animationTypeSelector, row++);
        return row;
    }

    public void refresh(WeaponSpecFile spec) {
        projectileSpecIdEditor.setText(spec.getProjectileSpecId() != null ? spec.getProjectileSpecId() : "");
        barrelModeSelector.setSelectedItem(spec.getBarrelMode());
        animationTypeSelector.setSelectedItem(spec.getAnimationType());
    }

    public void clear() {
        projectileSpecIdEditor.setText("");
        barrelModeSelector.setSelectedItem(null);
        animationTypeSelector.setSelectedItem(null);
    }
}
"""

logic_code = """package shipeditor.components.instrument.weapon;

import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.function.Supplier;

public class WeaponFiringLogicHandler {
    private final JCheckBox autochargeCheckbox;
    private final JCheckBox interruptibleBurstCheckbox;
    private final JCheckBox requiresFullChargeCheckbox;
    private final JCheckBox unaffectedBySpeedBonusesCheckbox;

    public WeaponFiringLogicHandler(Supplier<Boolean> readinessChecker, Runnable onChange, Supplier<WeaponSpecFile> specSupplier) {
        autochargeCheckbox = WeaponFirePanelUtilities.createCheckBox("Autocharge", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setAutocharge(value);
        }, onChange);

        interruptibleBurstCheckbox = WeaponFirePanelUtilities.createCheckBox("Interruptible Burst", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setInterruptibleBurst(value);
        }, onChange);

        requiresFullChargeCheckbox = WeaponFirePanelUtilities.createCheckBox("Requires Full Charge", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setRequiresFullCharge(value);
        }, onChange);

        unaffectedBySpeedBonusesCheckbox = WeaponFirePanelUtilities.createCheckBox("Unaffected By Projectile Speed Bonuses", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setUnaffectedByProjectileSpeedBonuses(value);
        }, onChange);
    }

    public int populate(JPanel panel, int startRow) {
        int row = startRow;
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), autochargeCheckbox, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), interruptibleBurstCheckbox, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), requiresFullChargeCheckbox, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), unaffectedBySpeedBonusesCheckbox, row++);
        return row;
    }

    public void refresh(WeaponSpecFile spec) {
        autochargeCheckbox.setSelected(spec.isAutocharge());
        interruptibleBurstCheckbox.setSelected(spec.isInterruptibleBurst());
        requiresFullChargeCheckbox.setSelected(spec.isRequiresFullCharge());
        unaffectedBySpeedBonusesCheckbox.setSelected(spec.isUnaffectedByProjectileSpeedBonuses());
    }

    public void clear() {
        autochargeCheckbox.setSelected(false);
        interruptibleBurstCheckbox.setSelected(false);
        requiresFullChargeCheckbox.setSelected(false);
        unaffectedBySpeedBonusesCheckbox.setSelected(false);
    }
}
"""

audio_code = """package shipeditor.components.instrument.weapon;

import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.function.Supplier;

public class WeaponAudioHandler {
    private final JTextField fireSoundOneEditor;
    private final JTextField fireSoundTwoEditor;
    private final JCheckBox noImpactSoundsCheckbox;
    private final JCheckBox noShieldImpactSoundsCheckbox;
    private final JCheckBox noNonShieldImpactSoundsCheckbox;
    private final JCheckBox playFullFireSoundOneCheckbox;
    private final JCheckBox stopPreviousFireSoundCheckbox;

    public WeaponAudioHandler(Supplier<Boolean> readinessChecker, Runnable onChange, Supplier<WeaponSpecFile> specSupplier) {
        fireSoundOneEditor = WeaponFirePanelUtilities.createTextField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setFireSoundOne(value);
        }, onChange);

        fireSoundTwoEditor = WeaponFirePanelUtilities.createTextField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setFireSoundTwo(value);
        }, onChange);

        noImpactSoundsCheckbox = WeaponFirePanelUtilities.createCheckBox("No Impact Sounds", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setNoImpactSounds(value);
        }, onChange);

        noShieldImpactSoundsCheckbox = WeaponFirePanelUtilities.createCheckBox("No Shield Impact Sounds", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setNoShieldImpactSounds(value);
        }, onChange);

        noNonShieldImpactSoundsCheckbox = WeaponFirePanelUtilities.createCheckBox("No Non-Shield Impact Sounds", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setNoNonShieldImpactSounds(value);
        }, onChange);

        playFullFireSoundOneCheckbox = WeaponFirePanelUtilities.createCheckBox("Play Full Fire Sound One", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setPlayFullFireSoundOne(value);
        }, onChange);

        stopPreviousFireSoundCheckbox = WeaponFirePanelUtilities.createCheckBox("Stop Previous Fire Sound", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setStopPreviousFireSound(value);
        }, onChange);
    }

    public int populate(JPanel panel, int startRow) {
        int row = startRow;
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Fire Sound One:"), fireSoundOneEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Fire Sound Two:"), fireSoundTwoEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), noImpactSoundsCheckbox, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), noShieldImpactSoundsCheckbox, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), noNonShieldImpactSoundsCheckbox, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), playFullFireSoundOneCheckbox, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), stopPreviousFireSoundCheckbox, row++);
        return row;
    }

    public void refresh(WeaponSpecFile spec) {
        fireSoundOneEditor.setText(spec.getFireSoundOne() != null ? spec.getFireSoundOne() : "");
        fireSoundTwoEditor.setText(spec.getFireSoundTwo() != null ? spec.getFireSoundTwo() : "");
        noImpactSoundsCheckbox.setSelected(spec.isNoImpactSounds());
        noShieldImpactSoundsCheckbox.setSelected(spec.isNoShieldImpactSounds());
        noNonShieldImpactSoundsCheckbox.setSelected(spec.isNoNonShieldImpactSounds());
        playFullFireSoundOneCheckbox.setSelected(spec.isPlayFullFireSoundOne());
        stopPreviousFireSoundCheckbox.setSelected(spec.isStopPreviousFireSound());
    }

    public void clear() {
        fireSoundOneEditor.setText("");
        fireSoundTwoEditor.setText("");
        noImpactSoundsCheckbox.setSelected(false);
        noShieldImpactSoundsCheckbox.setSelected(false);
        noNonShieldImpactSoundsCheckbox.setSelected(false);
        playFullFireSoundOneCheckbox.setSelected(false);
        stopPreviousFireSoundCheckbox.setSelected(false);
    }
}
"""

mf_code = """package shipeditor.components.instrument.weapon;

import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.representation.weapon.animation.MuzzleFlashSpec;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.function.Supplier;

public class WeaponMuzzleFlashHandler {
    private final JTextField mfLengthEditor;
    private final JTextField mfSpreadEditor;
    private final JTextField mfParticleSizeMinEditor;
    private final JTextField mfParticleSizeRangeEditor;
    private final JTextField mfParticleDurationEditor;
    private final JTextField mfParticleCountEditor;
    private final JLabel mfParticleColorValue;
    private final JLabel colorLabel;

    private MuzzleFlashSpec getOrCreate(WeaponSpecFile spec) {
        if (spec.getMuzzleFlashSpec() == null) {
            spec.setMuzzleFlashSpec(new MuzzleFlashSpec());
        }
        return spec.getMuzzleFlashSpec();
    }

    public WeaponMuzzleFlashHandler(Supplier<Boolean> readinessChecker, Runnable onChange, Supplier<WeaponSpecFile> specSupplier) {
        mfLengthEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setLength(value);
        }, onChange);

        mfSpreadEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setSpread(value);
        }, onChange);

        mfParticleSizeMinEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setParticleSizeMin(value);
        }, onChange);

        mfParticleSizeRangeEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setParticleSizeRange(value);
        }, onChange);

        mfParticleDurationEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setParticleDuration(value);
        }, onChange);

        mfParticleCountEditor = WeaponFirePanelUtilities.createIntField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setParticleCount(value);
        }, onChange);

        mfParticleColorValue = new JLabel();
        colorLabel = WeaponFirePanelUtilities.createColorLabel("Particle Color:", mfParticleColorValue,
                () -> {
                    WeaponSpecFile spec = specSupplier.get();
                    return (spec != null && spec.getMuzzleFlashSpec() != null) ? spec.getMuzzleFlashSpec().getParticleColor() : null;
                },
                color -> {
                    WeaponSpecFile spec = specSupplier.get();
                    if (spec != null) {
                        getOrCreate(spec).setParticleColor(color);
                        onChange.run();
                    }
                });
    }

    public int populate(JPanel panel, int startRow) {
        int row = startRow;
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("<html><b>Muzzle Flash</b></html>"), new JLabel(""), row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Length:"), mfLengthEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Spread:"), mfSpreadEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Particle Size Min:"), mfParticleSizeMinEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Particle Size Range:"), mfParticleSizeRangeEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Particle Duration:"), mfParticleDurationEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Particle Count:"), mfParticleCountEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, colorLabel, mfParticleColorValue, row++);
        return row;
    }

    public void refresh(WeaponSpecFile spec) {
        MuzzleFlashSpec mfSpec = spec.getMuzzleFlashSpec();
        if (mfSpec != null) {
            mfLengthEditor.setText(String.valueOf(mfSpec.getLength()));
            mfSpreadEditor.setText(String.valueOf(mfSpec.getSpread()));
            mfParticleSizeMinEditor.setText(String.valueOf(mfSpec.getParticleSizeMin()));
            mfParticleSizeRangeEditor.setText(String.valueOf(mfSpec.getParticleSizeRange()));
            mfParticleDurationEditor.setText(String.valueOf(mfSpec.getParticleDuration()));
            mfParticleCountEditor.setText(String.valueOf(mfSpec.getParticleCount()));
            WeaponFirePanelUtilities.updateColorLabel(mfParticleColorValue, mfSpec.getParticleColor());
        } else {
            clear();
        }
    }

    public void clear() {
        mfLengthEditor.setText("");
        mfSpreadEditor.setText("");
        mfParticleSizeMinEditor.setText("");
        mfParticleSizeRangeEditor.setText("");
        mfParticleDurationEditor.setText("");
        mfParticleCountEditor.setText("");
        WeaponFirePanelUtilities.updateColorLabel(mfParticleColorValue, null);
    }
}
"""

smoke_code = """package shipeditor.components.instrument.weapon;

import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.representation.weapon.animation.SmokeSpec;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.function.Supplier;

public class WeaponSmokeHandler {
    private final JTextField smokeParticleSizeMinEditor;
    private final JTextField smokeParticleSizeRangeEditor;
    private final JTextField smokeCloudParticleCountEditor;
    private final JTextField smokeCloudDurationEditor;
    private final JTextField smokeCloudRadiusEditor;
    private final JTextField smokeBlowbackParticleCountEditor;
    private final JTextField smokeBlowbackDurationEditor;
    private final JTextField smokeBlowbackLengthEditor;
    private final JTextField smokeBlowbackSpreadEditor;
    private final JLabel smokeParticleColorValue;
    private final JLabel colorLabel;

    private SmokeSpec getOrCreate(WeaponSpecFile spec) {
        if (spec.getSmokeSpec() == null) {
            spec.setSmokeSpec(new SmokeSpec());
        }
        return spec.getSmokeSpec();
    }

    public WeaponSmokeHandler(Supplier<Boolean> readinessChecker, Runnable onChange, Supplier<WeaponSpecFile> specSupplier) {
        smokeParticleSizeMinEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setParticleSizeMin(value);
        }, onChange);

        smokeParticleSizeRangeEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setParticleSizeRange(value);
        }, onChange);

        smokeCloudParticleCountEditor = WeaponFirePanelUtilities.createIntField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setCloudParticleCount(value);
        }, onChange);

        smokeCloudDurationEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setCloudDuration(value);
        }, onChange);

        smokeCloudRadiusEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setCloudRadius(value);
        }, onChange);

        smokeBlowbackParticleCountEditor = WeaponFirePanelUtilities.createIntField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setBlowbackParticleCount(value);
        }, onChange);

        smokeBlowbackDurationEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setBlowbackDuration(value);
        }, onChange);

        smokeBlowbackLengthEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setBlowbackLength(value);
        }, onChange);

        smokeBlowbackSpreadEditor = WeaponFirePanelUtilities.createDoubleField(readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setBlowbackSpread(value);
        }, onChange);

        smokeParticleColorValue = new JLabel();
        colorLabel = WeaponFirePanelUtilities.createColorLabel("Particle Color:", smokeParticleColorValue,
                () -> {
                    WeaponSpecFile spec = specSupplier.get();
                    return (spec != null && spec.getSmokeSpec() != null) ? spec.getSmokeSpec().getParticleColor() : null;
                },
                color -> {
                    WeaponSpecFile spec = specSupplier.get();
                    if (spec != null) {
                        getOrCreate(spec).setParticleColor(color);
                        onChange.run();
                    }
                });
    }

    public int populate(JPanel panel, int startRow) {
        int row = startRow;
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("<html><b>Smoke</b></html>"), new JLabel(""), row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Particle Size Min:"), smokeParticleSizeMinEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Particle Size Range:"), smokeParticleSizeRangeEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Cloud Particle Count:"), smokeCloudParticleCountEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Cloud Duration:"), smokeCloudDurationEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Cloud Radius:"), smokeCloudRadiusEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Blowback Particle Count:"), smokeBlowbackParticleCountEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Blowback Duration:"), smokeBlowbackDurationEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Blowback Length:"), smokeBlowbackLengthEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Blowback Spread:"), smokeBlowbackSpreadEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, colorLabel, smokeParticleColorValue, row++);
        return row;
    }

    public void refresh(WeaponSpecFile spec) {
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
            WeaponFirePanelUtilities.updateColorLabel(smokeParticleColorValue, smokeSpec.getParticleColor());
        } else {
            clear();
        }
    }

    public void clear() {
        smokeParticleSizeMinEditor.setText("");
        smokeParticleSizeRangeEditor.setText("");
        smokeCloudParticleCountEditor.setText("");
        smokeCloudDurationEditor.setText("");
        smokeCloudRadiusEditor.setText("");
        smokeBlowbackParticleCountEditor.setText("");
        smokeBlowbackDurationEditor.setText("");
        smokeBlowbackLengthEditor.setText("");
        smokeBlowbackSpreadEditor.setText("");
        WeaponFirePanelUtilities.updateColorLabel(smokeParticleColorValue, null);
    }
}
"""

panel_code = """package shipeditor.components.instrument.weapon;

import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.components.ComponentUtilities;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Supplier;

public class WeaponFirePanel extends AbstractWeaponPropertiesPanel {

    private WeaponLayer cachedLayer;
    private boolean readyForInput;

    private WeaponProjectileHandler projectileHandler;
    private WeaponFiringLogicHandler firingLogicHandler;
    private WeaponAudioHandler audioHandler;
    private WeaponMuzzleFlashHandler muzzleFlashHandler;
    private WeaponSmokeHandler smokeHandler;

    public WeaponFirePanel() {
        super();
    }

    @Override
    protected void populateContent() {
        this.setLayout(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(this, new Insets(1, 0, 0, 0), "Firing & Audio");

        Supplier<Boolean> readinessChecker = () -> readyForInput;
        Runnable onChange = this::processChange;
        Supplier<WeaponSpecFile> specSupplier = () -> cachedLayer != null ? cachedLayer.getSpecFile() : null;

        projectileHandler = new WeaponProjectileHandler(readinessChecker, onChange, specSupplier);
        firingLogicHandler = new WeaponFiringLogicHandler(readinessChecker, onChange, specSupplier);
        audioHandler = new WeaponAudioHandler(readinessChecker, onChange, specSupplier);
        muzzleFlashHandler = new WeaponMuzzleFlashHandler(readinessChecker, onChange, specSupplier);
        smokeHandler = new WeaponSmokeHandler(readinessChecker, onChange, specSupplier);

        int row = 0;
        row = projectileHandler.populate(this, row);
        row = firingLogicHandler.populate(this, row);
        row = audioHandler.populate(this, row);
        row = muzzleFlashHandler.populate(this, row);
        row = smokeHandler.populate(this, row);

        clearData();
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

        projectileHandler.refresh(spec);
        firingLogicHandler.refresh(spec);
        audioHandler.refresh(spec);
        muzzleFlashHandler.refresh(spec);
        smokeHandler.refresh(spec);

        readyForInput = true;
    }

    private void clearData() {
        readyForInput = false;

        if (projectileHandler != null) projectileHandler.clear();
        if (firingLogicHandler != null) firingLogicHandler.clear();
        if (audioHandler != null) audioHandler.clear();
        if (muzzleFlashHandler != null) muzzleFlashHandler.clear();
        if (smokeHandler != null) smokeHandler.clear();

        cachedLayer = null;
    }
}
"""

with open(f"{base_dir}/WeaponFirePanelUtilities.java", "w") as f: f.write(utils_code)
with open(f"{base_dir}/WeaponProjectileHandler.java", "w") as f: f.write(projectile_code)
with open(f"{base_dir}/WeaponFiringLogicHandler.java", "w") as f: f.write(logic_code)
with open(f"{base_dir}/WeaponAudioHandler.java", "w") as f: f.write(audio_code)
with open(f"{base_dir}/WeaponMuzzleFlashHandler.java", "w") as f: f.write(mf_code)
with open(f"{base_dir}/WeaponSmokeHandler.java", "w") as f: f.write(smoke_code)
with open(f"{base_dir}/WeaponFirePanel.java", "w") as f: f.write(panel_code)

print("WeaponFirePanel extracted successfully!")
