package oth.shipeditor.undo.edits;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.undo.Edit;
import oth.shipeditor.utility.overseers.StaticController;

import java.awt.geom.Point2D;
import java.util.Deque;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class AnchorOffsetEdit extends AbstractEdit implements LayerEdit {

    @Getter
    private LayerPainter layerPainter;

    private final Point2D oldOffset;

    private final Point2D updatedOffset;

    public AnchorOffsetEdit(LayerPainter painter, Point2D old, Point2D updated) {
        this.layerPainter = painter;
        this.oldOffset = old;
        this.updatedOffset = updated;
        this.setFinished(false);
    }

    @Override
    public void undo() {
        undoSubEdits();
        layerPainter.setAnchor(oldOffset);
        var repainter = StaticController.getScheduler();
        repainter.queueLayerPropertiesRepaint();
        repainter.queueCenterPanelsRepaint();
    }

    @Override
    public void redo() {
        layerPainter.setAnchor(updatedOffset);
        redoSubEdits();
        var repainter = StaticController.getScheduler();
        repainter.queueLayerPropertiesRepaint();
        repainter.queueCenterPanelsRepaint();
    }

    @Override
    public String getName() {
        return "Anchor Offset";
    }

    @Override
    public void cleanupReferences() {
        Deque<Edit> subEdits = this.getSubEdits();
        subEdits.forEach(edit -> {
            if (edit instanceof AnchorOffsetEdit checked) {
                checked.cleanupReferences();
            }
        });
        this.layerPainter = null;
    }

}
