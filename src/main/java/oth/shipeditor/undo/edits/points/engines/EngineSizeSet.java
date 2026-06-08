package oth.shipeditor.undo.edits.points.engines;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.engine.EnginePoint;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.objects.Size2D;
import oth.shipeditor.utility.overseers.StaticController;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class EngineSizeSet extends AbstractEdit {

    private final EnginePoint enginePoint;

    private final Size2D oldSize;

    private final Size2D updatedSize;

    public EngineSizeSet(EnginePoint point, Size2D old, Size2D updated) {
        this.enginePoint = point;
        this.oldSize = old;
        this.updatedSize = updated;
        this.setFinished(false);
    }

    @Override
    public void undo() {
        undoSubEdits();
        enginePoint.setSize(oldSize);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueEnginesPanelRepaint();
    }

    @Override
    public void redo() {
        enginePoint.setSize(updatedSize);
        redoSubEdits();
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueEnginesPanelRepaint();
    }

    @Override
    public String getName() {
        return "Change Engine Size";
    }

}
