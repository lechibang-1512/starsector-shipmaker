package shipeditor.components.instrument;

import lombok.Getter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import shipeditor.utility.text.StringValues;

@Getter
public enum EditorInstrument {

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
    VARIANT_DATA("Variant: Data", FluentUiRegularAL.DOCUMENT_EDIT_20),
    VARIANT_WEAPONS("Variant: Weapons", FluentUiRegularMZ.TARGET_20),
    VARIANT_MODULES("Variant: Modules", FluentUiRegularAL.BOARD_24),
    WEAPON_DATA("Weapon Data", FluentUiRegularAL.DOCUMENT_EDIT_20),
    WEAPON_VISUALS("Weapon Visuals", FluentUiRegularAL.IMAGE_20),
    WEAPON_FIRE("Weapon Firing", FluentUiRegularMZ.TARGET_20),
    WEAPON_BEAM("Weapon Beams", FluentUiRegularMZ.TARGET_20),
    WEAPON_OFFSETS("Weapon Offsets", FluentUiRegularMZ.TARGET_20),
    PROJECTILE_DATA("Projectile Data", FluentUiRegularAL.DOCUMENT_EDIT_20);

    private final String title;
    private final Ikon icon;

    EditorInstrument(String name, Ikon ikon) {
        this.title = name;
        this.icon = ikon;
    }

}
