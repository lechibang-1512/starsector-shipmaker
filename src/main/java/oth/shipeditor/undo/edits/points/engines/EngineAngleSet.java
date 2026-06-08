package oth.shipeditor.undo.edits.points.engines;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.engine.EnginePoint;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class EngineAngleSet extends AbstractEdit {

    private final EnginePoint enginePoint;

    private final double oldAngle;

    private final double updatedAngle;

    public EngineAngleSet(EnginePoint point, double old, double updated) {
        this.enginePoint = point;
        this.oldAngle = old;
        this.updatedAngle = updated;
        this.setFinished(false);
    }

    @Override
    public void undo() {
        undoSubEdits();
        enginePoint.setAngle(oldAngle);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueEnginesPanelRepaint();
    }

    @Override
    public void redo() {
        enginePoint.setAngle(updatedAngle);
        redoSubEdits();
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueEnginesPanelRepaint();
    }

    @Override
    public String getName() {
        return "Change Engine Angle";
    }

}
