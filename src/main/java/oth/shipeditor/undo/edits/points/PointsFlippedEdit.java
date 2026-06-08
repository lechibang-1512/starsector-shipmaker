package oth.shipeditor.undo.edits.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.AllArgsConstructor;
import oth.shipeditor.components.viewer.entities.BaseWorldPoint;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.undo.edits.LayerEdit;
import oth.shipeditor.utility.Utility;

import java.util.List;

@AllArgsConstructor
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class PointsFlippedEdit extends AbstractEdit implements LayerEdit {

    private List<BaseWorldPoint> points;

    private BaseWorldPoint anchor;

    @Override
    public void undo() {
        for (BaseWorldPoint point : points) {
            Utility.flipPointHorizontally(point, anchor);
        }
    }

    @Override
    public void redo() {
        for (BaseWorldPoint point : points) {
            Utility.flipPointHorizontally(point, anchor);
        }
    }

    @Override
    public String getName() {
        return "Flip Ship Points";
    }

    @Override
    public LayerPainter getLayerPainter() {
        return anchor.getParent();
    }

    @Override
    public void cleanupReferences() {
        points = null;
        anchor = null;
    }

}
