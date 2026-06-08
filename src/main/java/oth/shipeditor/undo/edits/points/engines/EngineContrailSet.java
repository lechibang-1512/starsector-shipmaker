package oth.shipeditor.undo.edits.points.engines;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.engine.EnginePoint;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class EngineContrailSet extends AbstractEdit {

    private final EnginePoint enginePoint;

    private final int oldContrail;

    private final int updatedContrail;

    public EngineContrailSet(EnginePoint point, int old, int updated) {
        this.enginePoint = point;
        this.oldContrail = old;
        this.updatedContrail = updated;
        this.setFinished(false);
    }

    @Override
    public void undo() {
        undoSubEdits();
        enginePoint.setContrailSize(oldContrail);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueEnginesPanelRepaint();
    }

    @Override
    public void redo() {
        enginePoint.setContrailSize(updatedContrail);
        redoSubEdits();
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueEnginesPanelRepaint();
    }

    @Override
    public String getName() {
        return "Change Engine Contrail Size";
    }

}
