package shipeditor.components.datafiles.trees;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.EventBus;
import shipeditor.components.datafiles.entities.CSVEntry;
import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.utility.Errors;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import shipeditor.communication.events.files.FileEvents.WeaponTreeReloadQueued;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WeaponFilterPanel extends AbstractFilterPanel {

    public enum OPCostBracket {
        LOW("0-4"), MEDIUM("5-9"), HIGH("10-14"), VERY_HIGH("15+");
        private final String display;
        OPCostBracket(String d) { this.display = d; }
        public String getDisplay() { return display; }
        public boolean matches(int cost) {
            if (this == LOW) return cost <= 4;
            if (this == MEDIUM) return cost >= 5 && cost <= 9;
            if (this == HIGH) return cost >= 10 && cost <= 14;
            return cost >= 15;
        }
    }

    @Getter
    @Setter
    private static String currentTextFilter;

    @SuppressWarnings("StaticCollection")
    @Getter
    private static final Map<WeaponType, Boolean> TYPE_FILTERS = new EnumMap<>(WeaponType.class);

    @SuppressWarnings("StaticCollection")
    @Getter
    private static final Map<WeaponSize, Boolean> SIZE_FILTERS = new EnumMap<>(WeaponSize.class);

    @SuppressWarnings("StaticCollection")
    @Getter
    private static final Map<OPCostBracket, Boolean> OP_COST_FILTERS = new EnumMap<>(OPCostBracket.class);

    @SuppressWarnings("StaticCollection")
    @Getter @Setter
    private static Map<Path, Boolean> packageFilters;

    @Getter
    private static WeaponSlotPoint lastSelectedSlot;

    private static boolean filterBySelectedSlot;

    private static boolean isMatchAny = false;

    static {
        for (WeaponType type : WeaponType.values()) {
            TYPE_FILTERS.put(type, true);
        }
        for (WeaponSize size : WeaponSize.values()) {
            SIZE_FILTERS.put(size, true);
        }
        for (OPCostBracket bracket : OPCostBracket.values()) {
            OP_COST_FILTERS.put(bracket, true);
        }
    }

    public WeaponFilterPanel() {
        super();
        this.initUI();
    }

    @Override
    protected boolean isMatchAny() {
        return isMatchAny;
    }

    @Override
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    protected void setMatchAny(boolean matchAny) {
        isMatchAny = matchAny;
    }

    @Override
    protected javax.swing.JComponent createHeaderComponent() {
        JCheckBox filterBySlot = new JCheckBox();
        filterBySlot.setText("Filter by last selected slot");
        filterBySlot.setToolTipText("Applied last, does not override other filters");
        filterBySlot.setSelected(filterBySelectedSlot);
        filterBySlot.setBorder(new EmptyBorder(6, 9, 6, 0));
        filterBySlot.addActionListener(e -> {
            filterBySelectedSlot = filterBySlot.isSelected();
        });
        return filterBySlot;
    }

    @Override
    protected void initTabs(javax.swing.JTabbedPane tabbedPane) {
        if (packageFilters != null) {
            this.addTab(tabbedPane, "Package", this.createPackageFilters());
        }
        this.addTab(tabbedPane, "Weapon Type", this.createWeaponTypeFilters());
        this.addTab(tabbedPane, "Weapon Size", this.createWeaponSizeFilters());
        this.addTab(tabbedPane, "OP Cost", this.createOPCostFilters());
    }

    @Override
    protected void toggleAll(boolean enable) {
        if (packageFilters != null) {
            packageFilters.forEach((key, aBoolean) -> packageFilters.put(key, enable));
        }
        TYPE_FILTERS.forEach((key, aBoolean) -> TYPE_FILTERS.put(key, enable));
        SIZE_FILTERS.forEach((key, aBoolean) -> SIZE_FILTERS.put(key, enable));
        OP_COST_FILTERS.forEach((key, aBoolean) -> OP_COST_FILTERS.put(key, enable));
        this.updateAllFilterBoxes(enable);
    }

    @Override
    protected void invertAll() {
        if (packageFilters != null) {
            packageFilters.forEach((key, aBoolean) -> packageFilters.put(key, !aBoolean));
        }
        TYPE_FILTERS.forEach((key, aBoolean) -> TYPE_FILTERS.put(key, !aBoolean));
        SIZE_FILTERS.forEach((key, aBoolean) -> SIZE_FILTERS.put(key, !aBoolean));
        OP_COST_FILTERS.forEach((key, aBoolean) -> OP_COST_FILTERS.put(key, !aBoolean));
        this.updateAllFilterBoxesInverted();
    }

    @Override
    protected void applyFilters() {
        EventBus.publish(new WeaponTreeReloadQueued());
    }

    public static void setLastSelectedSlot(WeaponSlotPoint slotPoint) {
        WeaponFilterPanel.lastSelectedSlot = slotPoint;
        if (filterBySelectedSlot) {
            EventBus.publish(new WeaponTreeReloadQueued());
        }
    }

    static Map<Path, List<WeaponCSVEntry>> getFilteredEntries() {
        GameDataRepository gameData = SettingsManager.getGameData();
        Map<Path, List<WeaponCSVEntry>> weaponEntriesByPackage = gameData.getWeaponEntriesByPackage();

        if (weaponEntriesByPackage == null) return null;

        Map<Path, List<WeaponCSVEntry>> filteredResult = new HashMap<>();
        for (Map.Entry<Path, List<WeaponCSVEntry>> entryPackage : weaponEntriesByPackage.entrySet()) {
            List<WeaponCSVEntry> entryList = entryPackage.getValue();
            List<WeaponCSVEntry> filteredList = entryList.stream()
                    .filter(entry -> {
                        if (!shouldDisplayByHandle(entry)) return false;
                        if (!shouldDisplayBySlot(entry)) return false; // Slot filter is always AND
                        
                        boolean byPackage = shouldDisplayByPackage(entry);
                        boolean byType = shouldDisplayByType(entry);
                        boolean bySize = shouldDisplayBySize(entry);
                        boolean byCost = shouldDisplayByOPCost(entry);
                        
                        if (isMatchAny) {
                            return byPackage || byType || bySize || byCost;
                        } else {
                            return byPackage && byType && bySize && byCost;
                        }
                    })
                    .toList();
            if (!filteredList.isEmpty()) {
                Path entryPackageKey = entryPackage.getKey();
                filteredResult.put(entryPackageKey, filteredList);
            }
        }
        return filteredResult;
    }

    private static boolean shouldDisplayBySlot(WeaponCSVEntry entry) {
        if (lastSelectedSlot != null && filterBySelectedSlot) {
            if (!WeaponType.isWeaponFitting(lastSelectedSlot, entry)) {
                return false;
            }
            
            shipeditor.components.viewer.layers.ViewerLayer activeLayer = shipeditor.utility.overseers.StaticController.getActiveLayer();
            if (activeLayer instanceof shipeditor.components.viewer.layers.ship.ShipLayer shipLayer) {
                shipeditor.components.viewer.layers.ship.ShipPainter shipPainter = shipLayer.getPainter();
                if (shipPainter != null) {
                    java.nio.file.Path shipPackage = null;
                    shipeditor.components.viewer.layers.ship.data.ShipSkin activeSkin = shipPainter.getActiveSkin();
                    if (activeSkin != null && !activeSkin.isBase()) {
                        shipPackage = activeSkin.getContainingPackage();
                    } else {
                        var shipHull = shipLayer.getHull();
                        if (shipHull != null) {
                            var shipEntry = shipeditor.representation.GameDataRepository.retrieveShipCSVEntryByID(shipHull.getHullID());
                            if (shipEntry != null) {
                                shipPackage = shipEntry.getPackageFolderPath();
                            }
                        }
                    }
                    
                    if (shipPackage != null) {
                        java.nio.file.Path weaponPackage = entry.getPackageFolderPath();
                        if (weaponPackage != null && weaponPackage.equals(shipPackage)) {
                            return true;
                        }
                        if (weaponPackage != null && shipeditor.persistence.SettingsManager.isCoreFolder(weaponPackage)) {
                            return true;
                        }
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean shouldDisplayBySize(WeaponCSVEntry entry) {
        WeaponSize weaponSize = entry.getSize();
        return SIZE_FILTERS.get(weaponSize);
    }

    private static boolean shouldDisplayByType(WeaponCSVEntry entry) {
        WeaponType weaponType = entry.getType();
        if (weaponType == null) {
            NullPointerException exception = new NullPointerException("Weapon type not found!");
            Errors.showFileError("Null weapon type was found in weapon spec: " + entry.getWeaponID(), exception);
            Errors.printToStream(exception);
        }
        return TYPE_FILTERS.getOrDefault(weaponType, true);
    }

    private static boolean shouldDisplayByPackage(CSVEntry entry) {
        Path folderPath = entry.getPackageFolderPath();
        return packageFilters.get(folderPath);
    }

    private static boolean shouldDisplayByOPCost(WeaponCSVEntry entry) {
        int cost = entry.getOPCost();
        for (OPCostBracket bracket : OPCostBracket.values()) {
            if (bracket.matches(cost) && OP_COST_FILTERS.getOrDefault(bracket, true)) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldDisplayByHandle(WeaponCSVEntry entry) {
        if (currentTextFilter == null || currentTextFilter.isEmpty()) return true;
        String currentInput = currentTextFilter.toLowerCase(Locale.ROOT);
        String name = entry.toString();
        if (name.toLowerCase(Locale.ROOT).contains(currentInput)) {
            return true;
        }
        String id = entry.getWeaponID();
        return id.toLowerCase(Locale.ROOT).contains(currentInput);
    }

    private JPanel createPackageFilters() {
        return this.createFilterSection("Package",
                packageFilters.keySet(),
                packageFilters,
                path -> {
                    Path fileName = path.getFileName();
                    return fileName != null ? fileName.toString() : "";
                },
                null,
                true);
    }

    private JPanel createWeaponTypeFilters() {
        Collection<WeaponType> weaponTypes = new ArrayList<>(List.of(
                WeaponType.BALLISTIC,
                WeaponType.ENERGY,
                WeaponType.MISSILE,
                WeaponType.DECORATIVE
        ));

        return this.createFilterSection("Weapon Type",
                weaponTypes,
                TYPE_FILTERS,
                WeaponType::getDisplayedName,
                type -> ComponentUtilities.createColorIconLabel(type.getColor()),
                false);
    }

    private JPanel createWeaponSizeFilters() {
        Iterable<WeaponSize> weaponSizes = new ArrayList<>(List.of(WeaponSize.values()));
        return this.createFilterSection("Weapon Size",
                weaponSizes,
                SIZE_FILTERS,
                WeaponSize::getDisplayedName,
                null,
                false);
    }

    private JPanel createOPCostFilters() {
        Iterable<OPCostBracket> brackets = new ArrayList<>(List.of(OPCostBracket.values()));
        return this.createFilterSection("OP Cost",
                brackets,
                OP_COST_FILTERS,
                OPCostBracket::getDisplay,
                null,
                false);
    }

}
