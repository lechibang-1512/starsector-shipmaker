package oth.shipeditor.undo.edits.points.slots;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.weapon.SlotData;
import oth.shipeditor.representation.weapon.WeaponMount;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class SlotMountChangeEdit extends AbstractEdit {

    private final SlotData slot;

    private final WeaponMount old;

    private final WeaponMount updated;

    public SlotMountChangeEdit(SlotData point, WeaponMount oldMount, WeaponMount newMount) {
        this.slot = point;
        this.old = oldMount;
        this.updated = newMount;
    }

    @Override
    public void undo() {
        slot.setWeaponMount(old);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
        repainter.queueBaysPanelRepaint();
    }

    @Override
    public void redo() {
        slot.setWeaponMount(updated);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
        repainter.queueBaysPanelRepaint();
    }

    @Override
    public String getName() {
        return "Slot Mount Change";
    }

}
