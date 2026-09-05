package shipeditor.utility.components.dialog;

import shipeditor.utility.text.StringManager;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.representation.weapon.WeaponSlot;
import shipeditor.utility.text.StringConstants;

import java.util.ArrayList;
import java.util.List;

public class HullQAReportDialog extends AbstractQAReportDialog {

    public HullQAReportDialog() {
        super(
                "Hull QA Report",
                "Hull ID",
                StringManager.getString("SCANNING_HULLS"),
                StringManager.getString("ALL_HULLS_PASSED_QA"),
                " potential issues."
        );
        runQAAnalysis();
    }

    @Override
    protected List<QAIssue> performAnalysis() {
        List<QAIssue> issues = new ArrayList<>();
        List<IndexedFile> hullFiles = DatabaseQueryService.getFilesByType(StringConstants.SHIP_TYPE);

        for (IndexedFile f : hullFiles) {
            try {
                HullSpecFile spec = FileLoading.loadHullFile(f.getFilePath().toFile());
                if (spec == null) {
                    continue;
                }

                WeaponSlot[] slots = spec.getWeaponSlots();
                int bayCount = 0;
                if (slots != null) {
                    List<String> seenIds = new ArrayList<>();
                    for (WeaponSlot slot : slots) {
                        // Duplicate ID check
                        if (seenIds.contains(slot.getId())) {
                            issues.add(new QAIssue(spec.getHullId(), "Duplicate Slot ID", "Duplicate ID found: " + slot.getId()));
                        } else {
                            seenIds.add(slot.getId());
                        }

                        if (StringConstants.LAUNCH_BAY.equals(slot.getType())) {
                            bayCount++;
                            // Degenerate arcs
                            if (slot.getArc() <= 0) {
                                issues.add(new QAIssue(spec.getHullId(), "Degenerate Arc", "Bay " + slot.getId() + " has arc <= 0."));
                            }
                            // Orphaned bays
                            if (slot.getLocations() == null || slot.getLocations().length == 0) {
                                issues.add(new QAIssue(spec.getHullId(), "Orphaned Bay", "Bay " + slot.getId() + " has no port points."));
                            } else {
                                for (int i = 0; i < slot.getLocations().length; i++) {
                                    java.awt.geom.Point2D.Double loc = slot.getLocations()[i];
                                    if (loc != null && loc.x == 0 && loc.y == 0) {
                                        issues.add(new QAIssue(spec.getHullId(), "Center Port", "Bay " + slot.getId() + " port " + i + " is at (0, 0)."));
                                    }
                                }
                            }
                        }
                    }
                }

                // Check ships with fewer bays than ship_data.csv fighter bays
                ShipCSVEntry csvEntry = GameDataRepository.retrieveShipCSVEntryByID(spec.getHullId());
                if (csvEntry != null) {
                    int csvBays = csvEntry.getBayCount();
                    if (csvBays > bayCount) {
                        issues.add(new QAIssue(spec.getHullId(), "Missing Bays", "Hull has " + bayCount + " bays, but ship_data.csv specifies " + csvBays + "."));
                    }
                }

            } catch (Exception e) {
                issues.add(new QAIssue(f.getEntityId(), "Parse Error", "Failed to parse hull file."));
            }
        }
        return issues;
    }
}
