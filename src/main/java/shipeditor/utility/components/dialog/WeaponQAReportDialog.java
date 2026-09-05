package shipeditor.utility.components.dialog;

import shipeditor.utility.text.StringManager;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.text.StringConstants;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class WeaponQAReportDialog extends AbstractQAReportDialog {

    public WeaponQAReportDialog() {
        super(
                "Weapon Offset QA Report",
                "Weapon ID",
                StringManager.getString("SCANNING_WEAPONS"),
                StringManager.getString("ALL_WEAPONS_PASSED_OFFSET_QA"),
                " potential offset issues."
        );
        runQAAnalysis();
    }

    @Override
    protected List<QAIssue> performAnalysis() {
        List<QAIssue> issues = new ArrayList<>();
        List<IndexedFile> weaponFiles = DatabaseQueryService.getFilesByType(StringConstants.WEAPON_TYPE);

        for (IndexedFile f : weaponFiles) {
            try {
                WeaponSpecFile spec = FileLoading.loadWeaponFile(f.getFilePath().toFile());
                if (spec == null) {
                    continue;
                }

                Point2D[] turret = spec.getTurretOffsets();
                Point2D[] hardpoint = spec.getHardpointOffsets();

                int turretBarrels = turret != null ? turret.length : 0;
                int hardpointBarrels = hardpoint != null ? hardpoint.length : 0;

                // 1. Check barrel count mismatch
                if (turretBarrels != hardpointBarrels) {
                    issues.add(new QAIssue(spec.getId(), "Barrel Mismatch",
                            "Turret has " + turretBarrels + " barrels, Hardpoint has " + hardpointBarrels + "."));
                }

                // 2. Check Y-symmetry
                if (turret != null && turretBarrels > 1 && turretBarrels % 2 == 0) {
                    double ySum = 0;
                    for (Point2D pt : turret) {
                        ySum += pt.getY();
                    }
                    if (Math.abs(ySum) > 0.01) {
                        issues.add(new QAIssue(spec.getId(), "Asymmetrical Offsets",
                                "Turret Y-offsets sum to " + ySum + ". They might be asymmetrical."));
                    }
                }

            } catch (Exception e) {
                issues.add(new QAIssue(f.getEntityId(), "Parse Error", "Failed to parse weapon file."));
            }
        }
        return issues;
    }
}
