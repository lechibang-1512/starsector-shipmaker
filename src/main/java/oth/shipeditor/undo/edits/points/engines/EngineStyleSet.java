package oth.shipeditor.undo.edits.points.engines;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.engine.EnginePoint;
import oth.shipeditor.representation.ship.EngineStyle;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class EngineStyleSet extends AbstractEdit {

    private final EnginePoint enginePoint;

    private final EngineStyle oldStyle;

    private final EngineStyle updatedStyle;

    public EngineStyleSet(EnginePoint point, EngineStyle old, EngineStyle updated) {
        this.enginePoint = point;
        this.oldStyle = old;
        this.updatedStyle = updated;
    }

    @Override
    public void undo() {
        enginePoint.setStyle(oldStyle);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueEnginesPanelRepaint();
    }

    @Override
    public void redo() {
        enginePoint.setStyle(updatedStyle);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueEnginesPanelRepaint();
    }

    @Override
    public String getName() {
        return "Change Engine Style";
    }

}
