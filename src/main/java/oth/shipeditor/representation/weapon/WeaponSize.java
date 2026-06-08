package oth.shipeditor.representation.weapon;

import lombok.Getter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import oth.shipeditor.representation.SizeEnum;
import oth.shipeditor.utility.themes.Themes;

@Getter
public enum WeaponSize implements SizeEnum {

    SMALL("SMALL", "Small", BoxiconsRegular.DICE_1, 1),
    MEDIUM("MEDIUM", "Medium", BoxiconsRegular.DICE_2, 2),
    LARGE("LARGE", "Large", BoxiconsRegular.DICE_3, 3);

    private final String id;
    private final String displayedName;

    private final int numericSize;

    private final Ikon ikon;

    WeaponSize(String serialized, String name, Ikon ikonTemplate, int numeric) {
        this.id = serialized;
        this.displayedName = name;
        this.numericSize = numeric;
        this.ikon = ikonTemplate;
    }

    @Override
    public FontIcon getIcon() {
        return FontIcon.of(this.ikon, 19, Themes.getIconColor());
    }

    static int getSizeDifference(WeaponSize firstSize, WeaponSize secondSize) {
        if (firstSize == null || secondSize == null) {
            throw new IllegalArgumentException("Both sizes must be non-null");
        }

        return firstSize.numericSize - secondSize.numericSize;
    }

    public static WeaponSize value(String textValue) {
        if (textValue == null || textValue.isEmpty()) {
            return null;
        } else return WeaponSize.valueOf(textValue);
    }

}
