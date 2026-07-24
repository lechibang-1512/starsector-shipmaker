package shipeditor.components;

import lombok.Getter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import shipeditor.utility.text.StringValues;

public class ComponentEnums {


    @Getter public static enum CoordsDisplayMode {

    WORLD("World",
            "World: 0,0 at start of coordinate system"),

    SPRITE_CENTER("Sprite",
            "Sprite: 0,0 at selected sprite center"),

    SHIPCENTER_ANCHOR("Entity Center Anchor",
            "Entity Center Anchor: 0,0 at bottom left corner of selected sprite"),

    SHIP_CENTER("Entity Center",
            "Entity Center: 0,0 at designated entity center of selected layer");

    private final String shortName;

    private final String displayedText;

    CoordsDisplayMode(String name, String text) {
        this.shortName = name;
        this.displayedText = text;
    }

}


    @Getter public static enum EditorInstrument {

    LAYER(StringValues.LAYER, FluentUiRegularAL.LAYER_20),
    COLLISION(StringValues.COLLISION, FluentUiRegularMZ.TARGET_20),
    SHIELD(StringValues.SHIELD, FluentUiRegularMZ.SHIELD_20),
    BOUNDS("Bounds", FluentUiRegularAL.DATA_AREA_24),
    WEAPON_SLOTS("Weapon Slots", FluentUiRegularMZ.TARGET_EDIT_20),
    LAUNCH_BAYS("Launch Bays", FluentUiRegularMZ.ROCKET_20),
    ENGINES("Engines", FluentUiRegularMZ.VEHICLE_BICYCLE_20),
    BUILT_IN_MODS("Built-in Mods", FluentUiRegularMZ.WRENCH_20),
    BUILT_IN_WINGS("Built-in Wings", FluentUiRegularAL.AIRPLANE_20),
    SKIN_DATA("Skin: Data", FluentUiRegularAL.CLIPBOARD_TEXT_20),
    SKIN_SLOTS("Skin: Overrides", FluentUiRegularAL.EDIT_20),
    SKIN_ENGINES("Skin: Engines", FluentUiRegularAL.EDIT_20),
    SKIN_REMOVALS("Skin: Removals", FluentUiRegularAL.EDIT_20),
    VARIANT_DATA("Variant: Data", FluentUiRegularAL.DOCUMENT_EDIT_20),
    VARIANT_WEAPONS("Variant: Weapons", FluentUiRegularMZ.TARGET_20),
    VARIANT_MODULES("Variant: Modules", FluentUiRegularAL.BOARD_24),
    WEAPON_DATA("Weapon Data", FluentUiRegularAL.DOCUMENT_EDIT_20),
    WEAPON_VISUALS("Weapon Visuals", FluentUiRegularAL.IMAGE_20),
    WEAPON_OFFSETS("Weapon Offsets", FluentUiRegularMZ.TARGET_20),
    PROJECTILE_DATA("Projectile Data", FluentUiRegularAL.DOCUMENT_EDIT_20);

    private final String title;
    private final Ikon icon;

    EditorInstrument(String name, Ikon ikon) {
        this.title = name;
        this.icon = ikon;
    }

}


    @Getter public static enum VariantDataTab {
    MAIN,
    HULLMODS,
    WINGS
}


    @Getter public static enum SlotCreationMode {
    BY_CLOSEST, BY_DEFAULT
}


    @Getter public static enum OpenDataTarget {
    FILE, CONTAINER, PACKAGE
}

}
