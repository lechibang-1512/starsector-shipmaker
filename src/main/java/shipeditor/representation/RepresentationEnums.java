package shipeditor.representation;

import lombok.Getter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.utility.text.StringValues;
import shipeditor.utility.themes.Themes;

public class RepresentationEnums {


    @Getter public static enum ShipTypeHints {
    FREIGHTER,
    TANKER,
    LINER,
    TRANSPORT,
    CIVILIAN,
    CARRIER,
    COMBAT,
    NO_AUTO_ESCORT,
    UNBOARDABLE,
    STATION,
    SHIP_WITH_MODULES,
    MODULE,
    HIDE_IN_CODEX,
    UNDER_PARENT,
    INDEPENDENT_ROTATION,
    ALWAYS_PANIC,
    WEAPONS_FRONT_TO_BACK,
    WEAPONS_BACK_TO_FRONT,
    DO_NOT_SHOW_MODULES_IN_FLEET_LIST,
    RENDER_ENGINES_BELOW_HULL,
    NEVER_DODGE_MISSILES,
    MISSILE_HARDPOINTS_ROTATE,
    NO_NEURAL_LINK,
    PHASE,
    PLAY_FIGHTER_OVERLOAD_SOUNDS,
}


    @Getter public static enum HullSize implements SizeEnum {

    DEFAULT(BoxiconsRegular.DICE_1, 0, StringValues.DEFAULT),
    FIGHTER(BoxiconsRegular.DICE_1, 0, "Fighter"),
    FRIGATE(BoxiconsRegular.DICE_2, 10, "Frigate"),
    DESTROYER(BoxiconsRegular.DICE_3, 20, "Destroyer"),
    CRUISER(BoxiconsRegular.DICE_4, 30, "Cruiser"),
    CAPITAL_SHIP(BoxiconsRegular.DICE_5, 50, "Capital");

    private final Ikon ikonTemplate;

    private final FontIcon icon;

    private final int maxFluxRegulators;

    private final String displayedName;

    public FontIcon getResizedIcon(int size) {
        return FontIcon.of(ikonTemplate, size, Themes.getIconColor());
    }

    HullSize(Ikon ikon, int fluxCap, String name) {
        this.ikonTemplate = ikon;
        this.icon = FontIcon.of(ikonTemplate, 16, Themes.getIconColor());
        this.maxFluxRegulators = fluxCap;
        this.displayedName = name;
    }

}


    public static interface SizeEnum {

    FontIcon getIcon();

    String getDisplayedName();

}

}
