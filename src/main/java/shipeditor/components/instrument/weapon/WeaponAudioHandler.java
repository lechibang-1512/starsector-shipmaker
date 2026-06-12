package shipeditor.components.instrument.weapon;

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
