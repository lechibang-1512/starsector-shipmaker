package shipeditor.components.instrument.ship.slots;

import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.representation.weapon.WeaponEnums.WeaponMount;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;

import java.util.ArrayList;
import java.util.List;

public final class WeaponSlotClipboard {
    private static final List<CopiedSlotData> CLIPBOARD = new ArrayList<>();

    private WeaponSlotClipboard() {
    }

    public static void copy(List<WeaponSlotPoint> slots) {
        CLIPBOARD.clear();
        if (slots == null || slots.isEmpty()) return;
        for (WeaponSlotPoint slot : slots) {
            CLIPBOARD.add(new CopiedSlotData(slot));
        }
    }

    public static boolean hasData() {
        return !CLIPBOARD.isEmpty();
    }

    public static List<CopiedSlotData> getClipboard() {
        return new ArrayList<>(CLIPBOARD);
    }

    public static class CopiedSlotData {
        public final WeaponType type;
        public final WeaponMount mount;
        public final WeaponSize size;
        public final double angle;
        public final double arc;
        public final int renderOrderMod;
        public final double x;
        public final double y;

        public CopiedSlotData(WeaponSlotPoint slot) {
            this.type = slot.getBaseType();
            this.mount = slot.getBaseMount();
            this.size = slot.getBaseSize();
            this.angle = slot.getAngle();
            this.arc = slot.getArc();
            this.renderOrderMod = slot.getRenderOrderMod();
            this.x = slot.getPosition().getX();
            this.y = slot.getPosition().getY();
        }
    }
}
