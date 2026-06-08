package oth.shipeditor.components.viewer.layers.ship.data;

public enum WeaponGroupType {

    LINKED("Linked"),
    ALTERNATING("Alternating");

    private final String displayName;

    WeaponGroupType(String name) {
        this.displayName = name;
    }

    public String getDisplayName() {
        return displayName;
    }

}
