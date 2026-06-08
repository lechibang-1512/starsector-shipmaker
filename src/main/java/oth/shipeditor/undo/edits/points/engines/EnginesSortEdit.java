package oth.shipeditor.undo.edits.points.engines;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.engine.EnginePoint;
import oth.shipeditor.components.viewer.painters.points.ship.EngineSlotPainter;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

import java.util.List;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class EnginesSortEdit extends AbstractEdit {

    private final EngineSlotPainter pointPainter;

    private final List<EnginePoint> oldList;

    private final List<EnginePoint> newList;

    public EnginesSortEdit(EngineSlotPainter painter, List<EnginePoint> old, List<EnginePoint> changed) {
        this.pointPainter = painter;
        this.oldList = old;
        this.newList = changed;
    }

    @Override
    public void undo() {
        pointPainter.setEnginePoints(oldList);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueEnginesPanelRepaint();
    }

    @Override
    public void redo() {
        pointPainter.setEnginePoints(newList);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueEnginesPanelRepaint();
    }

    @Override
    public String getName() {
        return "Sort Engines";
    }

}
