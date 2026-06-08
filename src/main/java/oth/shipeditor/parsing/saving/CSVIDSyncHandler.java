package oth.shipeditor.parsing.saving;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.components.CSVEntryIDChanged;
import oth.shipeditor.communication.events.components.DataTreesReloadQueued;
import oth.shipeditor.communication.events.files.saving.CSVSaveQueued;
import oth.shipeditor.components.datafiles.entities.*;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.representation.GameDataRepository;
import oth.shipeditor.representation.ship.HullSpecFile;
import oth.shipeditor.representation.ship.ShipSpecFile;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Log4j2
public final class CSVIDSyncHandler {

    private CSVIDSyncHandler() {}

    public static void init() {
        EventBus.subscribe(CSVIDSyncHandler.class, event -> {
            if (event instanceof CSVEntryIDChanged changed) {
                handleIDChange(changed);
            }
        });
    }

    private static void handleIDChange(CSVEntryIDChanged changed) {
        String oldID = changed.oldID();
        String newID = changed.newID();
        CSVEntry entry = changed.entry();
        GameDataRepository repo = SettingsManager.getGameData();

        // 1. Patch the raw CSV row's "id" cell
        updateRawCSVRow(entry.getTableFilePath(), oldID, newID);

        // 2. Update mutable ID field + re-index repository
        if (entry instanceof ShipCSVEntry shipEntry) {
            shipEntry.setHullID(newID);
            repo.reindexShipEntry(oldID, newID, shipEntry);
            repo.reindexSpecEntry(oldID, newID);
            // Also update the HullSpecFile's hullId so JSON save is consistent
            ShipSpecFile spec = GameDataRepository.retrieveSpecByID(newID);
            if (spec instanceof HullSpecFile hullSpec) {
                hullSpec.setHullId(newID);
            }
        } else if (entry instanceof WeaponCSVEntry weaponEntry) {
            weaponEntry.setWeaponID(newID);
            repo.reindexWeaponEntry(oldID, newID, weaponEntry);
        } else if (entry instanceof HullmodCSVEntry hullmodEntry) {
            hullmodEntry.setHullmodID(newID);
            repo.reindexHullmodEntry(oldID, newID, hullmodEntry);
        } else if (entry instanceof WingCSVEntry wingEntry) {
            wingEntry.setWingID(newID);
            repo.reindexWingEntry(oldID, newID, wingEntry);
        } else if (entry instanceof ShipSystemCSVEntry systemEntry) {
            systemEntry.setShipSystemID(newID);
            repo.reindexShipSystemEntry(oldID, newID, systemEntry);
        }

        log.info("CSV entry ID re-indexed: '{}' -> '{}'", oldID, newID);

        // 3. Refresh data trees first so the UI reflects the change
        EventBus.publish(new DataTreesReloadQueued());

        // 4. Prompt user to save to disk
        int response = JOptionPane.showConfirmDialog(null,
                "ID changed from '" + oldID + "' to '" + newID + "'.\n"
                        + "Save the updated CSV to disk now?",
                "CSV ID Updated", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            EventBus.publish(new CSVSaveQueued(entry));
        }
    }

    private static void updateRawCSVRow(Path tablePath, String oldID, String newID) {
        GameDataRepository repo = SettingsManager.getGameData();
        List<Map<String, String>> rawData = repo.getRawCSVDataForPath(tablePath);
        if (rawData == null) return;

        for (Map<String, String> row : rawData) {
            for (Map.Entry<String, String> cell : row.entrySet()) {
                String cleanKey = cell.getKey().replace("\uFEFF", "").trim().toLowerCase();
                if ("id".equals(cleanKey)
                        && cell.getValue() != null
                        && oldID.equalsIgnoreCase(cell.getValue().trim())) {
                    row.put(cell.getKey(), newID);
                    return;
                }
            }
        }
        log.warn("Raw CSV row with ID '{}' not found for path: {}", oldID, tablePath);
    }

}
