package oth.shipeditor.undo.edits.points.slots;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.weapon.SlotData;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class RenderOrderChangeEdit extends AbstractEdit {

    private final SlotData slotPoint;

    private final int oldOrder;

    private final int updatedOrder;

    public RenderOrderChangeEdit(SlotData point, int old, int updated) {
        this.slotPoint = point;
        this.oldOrder = old;
        this.updatedOrder = updated;
        this.setFinished(false);
    }

    @Override
    public void undo() {
        undoSubEdits();
        slotPoint.setRenderOrderMod(oldOrder);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
        repainter.queueBaysPanelRepaint();
    }

    @Override
    public void redo() {
        slotPoint.setRenderOrderMod(updatedOrder);
        redoSubEdits();
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
        repainter.queueBaysPanelRepaint();
    }

    @Override
    public String getName() {
        return "Change Render Order";
    }

}
