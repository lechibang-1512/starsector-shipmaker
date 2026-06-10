package shipeditor.components.datafiles.trees;

import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.HullTreeReloadQueued;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.ship.HullSize;
import shipeditor.representation.ship.SkinSpecFile;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import shipeditor.utility.components.UIConstants;

public class ShipFilterPanel extends AbstractFilterPanel {

    @Getter @Setter
    private static String currentTextFilter;

    @SuppressWarnings("StaticCollection")
    @Getter
    private static final Map<HullSize, Boolean> SIZE_FILTERS = new EnumMap<>(HullSize.class);

    @SuppressWarnings("StaticCollection")
    @Getter @Setter
    private static Map<Path, Boolean> factionFilters;

    static {
        for (HullSize size : HullSize.values()) {
            SIZE_FILTERS.put(size, true);
        }
    }

    public ShipFilterPanel() {
        super();
        this.initUI();
    }

    @Override
    protected void initFilterPanelContent(JPanel filtersPane) {
        JPanel selectionButtons = this.getSelectionButtonsPanel();
        filtersPane.add(selectionButtons);

        Dimension padding = UIConstants.PADDING_10_4;
        if (factionFilters != null) {
            filtersPane.add(Box.createRigidArea(padding));
            filtersPane.add(this.createFactionFilters());
        }

        filtersPane.add(Box.createRigidArea(padding));
        filtersPane.add(this.createHullSizeFilters());
        filtersPane.add(Box.createRigidArea(padding));
    }

    @Override
    protected void toggleAll(boolean enable) {
        if (factionFilters != null) {
            factionFilters.forEach((key, aBoolean) -> factionFilters.put(key, enable));
        }
        SIZE_FILTERS.forEach((key, aBoolean) -> SIZE_FILTERS.put(key, enable));
        this.updateAllFilterBoxes(enable);
        EventBus.publish(new HullTreeReloadQueued());
    }

    private JPanel createFactionFilters() {
        return this.createFilterSection("Faction / Package",
                factionFilters.keySet(),
                factionFilters,
                path -> {
                    Path fileName = path.getFileName();
                    return fileName != null ? fileName.toString() : "";
                },
                null,
                HullTreeReloadQueued::new);
    }

    private JPanel createHullSizeFilters() {
        Iterable<HullSize> hullSizes = new ArrayList<>(List.of(HullSize.values()));
        return this.createFilterSection("Hull size",
                hullSizes,
                SIZE_FILTERS,
                HullSize::getDisplayedName,
                null,
                HullTreeReloadQueued::new);
    }

    private static boolean shouldDisplayByHandle(ShipCSVEntry entry) {
        if (currentTextFilter == null || currentTextFilter.isEmpty()) return true;
        String currentInput = currentTextFilter.toLowerCase(Locale.ROOT);
        String name = entry.toString();
        if (name.toLowerCase(Locale.ROOT).contains(currentInput)) {
            return true;
        }
        String id = entry.getHullID();
        if (id.toLowerCase(Locale.ROOT).contains(currentInput)) {
            return true;
        }

        Map<String, SkinSpecFile> skins = entry.getSkins();
        if (skins != null) {
            for (SkinSpecFile skin : skins.values()) {
                if (skin != null && !skin.isBase()) {
                    String skinName = skin.getHullName();
                    if (skinName != null && skinName.toLowerCase(Locale.ROOT).contains(currentInput)) {
                        return true;
                    }
                    String skinId = skin.getSkinHullId();
                    if (skinId != null && skinId.toLowerCase(Locale.ROOT).contains(currentInput)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean shouldDisplayBySize(ShipCSVEntry entry) {
        HullSize entrySize = entry.getSize();
        return SIZE_FILTERS.get(entrySize);
    }

    private static boolean shouldDisplayByFaction(ShipCSVEntry entry) {
        if (factionFilters == null) return true;
        Path folderPath = entry.getPackageFolderPath();
        Boolean display = factionFilters.get(folderPath);
        if (display != null && display) return true;

        Map<String, SkinSpecFile> skins = entry.getSkins();
        if (skins != null) {
            for (SkinSpecFile skin : skins.values()) {
                if (skin != null && !skin.isBase()) {
                    Path skinFolderPath = skin.getContainingPackage();
                    Boolean skinDisplay = factionFilters.get(skinFolderPath);
                    if (skinDisplay != null && skinDisplay) return true;
                }
            }
        }
        return false;
    }

    static Map<Path, List<ShipCSVEntry>> getFilteredEntries() {
        GameDataRepository gameData = SettingsManager.getGameData();
        Map<Path, List<ShipCSVEntry>> shipEntriesByPackage = gameData.getShipEntriesByPackage();

        if (shipEntriesByPackage == null) return null;

        Map<Path, List<ShipCSVEntry>> filteredResult = new HashMap<>();
        for (Map.Entry<Path, List<ShipCSVEntry>> entryPackage : shipEntriesByPackage.entrySet()) {
            List<ShipCSVEntry> entryList = entryPackage.getValue();
            List<ShipCSVEntry> filteredList = entryList.stream()
                    .filter(ShipFilterPanel::shouldDisplayByFaction)
                    .filter(ShipFilterPanel::shouldDisplayBySize)
                    .filter(ShipFilterPanel::shouldDisplayByHandle)
                    .toList();
            if (!filteredList.isEmpty()) {
                filteredResult.put(entryPackage.getKey(), filteredList);
            }
        }
        return filteredResult;
    }
}
