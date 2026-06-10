package shipeditor.components.viewer.entities.weapon;

import java.awt.Color;
import java.awt.geom.Point2D;

public interface SlotPoint extends SlotData {

    Point2D getPosition();

    double getPaintSizeMultiplier();

    Color getCurrentColor();

    void setCursorInBounds(boolean inBounds);

    WeaponSlotOverride getSkinOverride();

}
