package oth.shipeditor.undo.edits.points.slots;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.weapon.SlotData;
import oth.shipeditor.undo.AbstractEdit;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class SlotIDChangeEdit extends AbstractEdit {

    private final SlotData slot;

    private final String old;

    private final String updated;

    public SlotIDChangeEdit(SlotData point, String newID, String oldID) {
        this.slot = point;
        this.old = oldID;
        this.updated = newID;
    }

    @Override
    public void undo() {
        slot.changeSlotID(old);

    }

    @Override
    public void redo() {
        slot.changeSlotID(updated);
    }

    @Override
    public String getName() {
        return "Slot ID Change";
    }

}
