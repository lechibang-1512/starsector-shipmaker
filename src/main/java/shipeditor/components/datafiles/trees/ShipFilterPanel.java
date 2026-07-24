package shipeditor.components.datafiles.trees;

import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.EventBus;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.RepresentationEnums.HullSize;

import shipeditor.representation.ship.SkinSpecFile;

import javax.swing.JPanel;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import shipeditor.communication.events.files.FileEvents.HullTreeReloadQueued;

public class ShipFilterPanel extends AbstractFilterPanel {

    @Getter @Setter
    private static String currentTextFilter;

    @SuppressWarnings({"StaticCollection"})
    @Getter
    private static final Map<HullSize, Boolean> SIZE_FILTERS = new EnumMap<>(HullSize.class);



    @SuppressWarnings("StaticCollection")
    @Getter @Setter
    private static Map<Path, Boolean> factionFilters;

    private static boolean isMatchAny = false;

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
    protected boolean isMatchAny() {
        return isMatchAny;
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    protected void setMatchAny(boolean matchAny) {
        isMatchAny = matchAny;
    }

    @Override
    protected void initTabs(javax.swing.JTabbedPane tabbedPane) {
        if (factionFilters != null) {
            this.addTab(tabbedPane, "Faction / Package", this.createFactionFilters());
        }
        this.addTab(tabbedPane, "Hull size", this.createHullSizeFilters());
    }

    @Override
    protected void toggleAll(boolean enable) {
        if (factionFilters != null) {
            factionFilters.forEach((key, aBoolean) -> factionFilters.put(key, enable));
        }
        SIZE_FILTERS.forEach((key, aBoolean) -> SIZE_FILTERS.put(key, enable));
        this.updateAllFilterBoxes(enable);
    }

    @Override
    protected void invertAll() {
        if (factionFilters != null) {
            factionFilters.forEach((key, aBoolean) -> factionFilters.put(key, !aBoolean));
        }
        SIZE_FILTERS.forEach((key, aBoolean) -> SIZE_FILTERS.put(key, !aBoolean));
        this.updateAllFilterBoxesInverted();
    }

    @Override
    protected void applyFilters() {
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
                true);
    }

    private JPanel createHullSizeFilters() {
        Iterable<HullSize> hullSizes = new ArrayList<>(List.of(HullSize.values()));
        return this.createFilterSection("Hull size",
                hullSizes,
                SIZE_FILTERS,
                a -> a.getDisplayedName(),
                null,
                false);
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
                    .filter(entry -> {
                        if (!shouldDisplayByHandle(entry)) return false;
                        
                        boolean byFaction = shouldDisplayByFaction(entry);
                        boolean bySize = shouldDisplayBySize(entry);
                        
                        if (isMatchAny) {
                            return byFaction || bySize;
                        } else {
                            return byFaction && bySize;
                        }
                    })
                    .toList();
            if (!filteredList.isEmpty()) {
                filteredResult.put(entryPackage.getKey(), filteredList);
            }
        }
        return filteredResult;
    }
}
