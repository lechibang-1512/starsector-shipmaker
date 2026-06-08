package oth.shipeditor.undo.edits.points.slots;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.weapon.SlotData;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class SlotAngleSet extends AbstractEdit {

    private final SlotData slotPoint;

    private final double oldAngle;

    private final double updatedAngle;

    public SlotAngleSet(SlotData point, double old, double updated) {
        this.slotPoint = point;
        this.oldAngle = old;
        this.updatedAngle = updated;
        this.setFinished(false);
    }

    @Override
    public void undo() {
        undoSubEdits();
        slotPoint.setAngle(oldAngle);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
        repainter.queueBaysPanelRepaint();
    }

    @Override
    public void redo() {
        slotPoint.setAngle(updatedAngle);
        redoSubEdits();
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
        repainter.queueBaysPanelRepaint();
    }

    @Override
    public String getName() {
        return "Change Slot Angle";
    }

}
