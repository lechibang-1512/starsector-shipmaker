package oth.shipeditor.undo.edits.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.communication.events.Events;
import oth.shipeditor.components.viewer.entities.BoundPoint;
import oth.shipeditor.components.viewer.painters.points.ship.BoundPointsPainter;
import oth.shipeditor.undo.AbstractEdit;

import java.util.List;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class BoundsSortEdit extends AbstractEdit {

    private final BoundPointsPainter pointPainter;

    private final List<BoundPoint> oldList;

    private final List<BoundPoint> newList;

    public BoundsSortEdit(BoundPointsPainter painter, List<BoundPoint> old, List<BoundPoint> changed) {
        this.pointPainter = painter;
        this.oldList = old;
        this.newList = changed;
    }

    @Override
    public void undo() {
        pointPainter.setBoundPoints(oldList);
        Events.repaintShipView();
    }

    @Override
    public void redo() {
        pointPainter.setBoundPoints(newList);
        Events.repaintShipView();
    }

    @Override
    public String getName() {
        return "Sort Bounds";
    }

}
