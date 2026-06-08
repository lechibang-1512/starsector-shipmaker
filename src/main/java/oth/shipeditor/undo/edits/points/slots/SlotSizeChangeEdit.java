package oth.shipeditor.undo.edits.points.slots;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.weapon.SlotData;
import oth.shipeditor.representation.weapon.WeaponSize;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class SlotSizeChangeEdit extends AbstractEdit {

    private final SlotData slot;

    private final WeaponSize old;

    private final WeaponSize updated;

    public SlotSizeChangeEdit(SlotData point, WeaponSize oldSize, WeaponSize newSize) {
        this.slot = point;
        this.old = oldSize;
        this.updated = newSize;
    }

    @Override
    public void undo() {
        slot.setWeaponSize(old);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
        repainter.queueBaysPanelRepaint();
        repainter.queueBuiltInsRepaint();
        repainter.queueBaysPanelRepaint();
    }

    @Override
    public void redo() {
        slot.setWeaponSize(updated);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
        repainter.queueBaysPanelRepaint();
        repainter.queueBuiltInsRepaint();
        repainter.queueBaysPanelRepaint();
    }

    @Override
    public String getName() {
        return "Slot Size Change";
    }

}
