package oth.shipeditor.undo.edits.points.slots;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.weapon.SlotData;
import oth.shipeditor.representation.weapon.WeaponType;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class SlotTypeChangeEdit extends AbstractEdit {

    private final SlotData slot;

    private final WeaponType old;

    private final WeaponType updated;

    public SlotTypeChangeEdit(SlotData point, WeaponType oldType, WeaponType newType) {
        this.slot = point;
        this.old = oldType;
        this.updated = newType;
    }

    @Override
    public void undo() {
        slot.setWeaponType(old);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
        repainter.queueBuiltInsRepaint();
    }

    @Override
    public void redo() {
        slot.setWeaponType(updated);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
        repainter.queueBuiltInsRepaint();
    }

    @Override
    public String getName() {
        return "Slot Type Change";
    }

}
