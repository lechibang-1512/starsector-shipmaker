package shipeditor.components.viewer.painters.points.ship.features;

import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;

import java.io.Serializable;
import java.util.Comparator;

public class InstalledFeatureComparator implements Comparator<InstalledFeature>, Serializable {

    @Override
    public int compare(InstalledFeature first, InstalledFeature second) {
        if (first == second) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }

        int typeComparison = InstalledFeatureComparator.compareByType(first, second);
        if (typeComparison != 0) {
            return typeComparison;
        }

        int sizeComparison = InstalledFeatureComparator.compareBySize(first, second);
        if (sizeComparison != 0) {
            return sizeComparison;
        }

        return InstalledFeatureComparator.compareAlphabetically(first, second);
    }

    private static int compareByType(InstalledFeature first, InstalledFeature second) {
        return InstalledFeatureComparator.getTypeOrder(first) -
                InstalledFeatureComparator.getTypeOrder(second);
    }

    private static int getTypeOrder(InstalledFeature feature) {
        if (feature == null || !(feature.getDataEntry() instanceof WeaponCSVEntry weaponEntry)) {
            return 2;
        }
        WeaponType type = weaponEntry.getType();
        if (type == WeaponType.BUILT_IN) {
            return 0;
        }
        return 1;
    }

    private static int compareBySize(InstalledFeature first, InstalledFeature second) {
        return InstalledFeatureComparator.getSizeOrder(first) -
                InstalledFeatureComparator.getSizeOrder(second);
    }

    private static int getSizeOrder(InstalledFeature feature) {
        if (feature == null || !(feature.getDataEntry() instanceof WeaponCSVEntry weaponEntry)) {
            return 0;
        }
        WeaponSize size = weaponEntry.getSize();
        return size != null ? size.getNumericSize() : 0;
    }

    private static int compareAlphabetically(InstalledFeature first, InstalledFeature second) {
        String firstName = first != null && first.getName() != null ? first.getName() : "";
        String secondName = second != null && second.getName() != null ? second.getName() : "";
        return firstName.compareToIgnoreCase(secondName);
    }

}
