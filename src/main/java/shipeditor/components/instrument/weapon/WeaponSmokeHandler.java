package shipeditor.components.instrument.weapon;

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
