package oth.shipeditor.undo.edits;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.undo.Edit;
import oth.shipeditor.utility.overseers.StaticController;

import java.util.Deque;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class LayerRotationEdit extends AbstractEdit implements LayerEdit {

    @Getter
    private LayerPainter layerPainter;

    private final double oldRotation;

    private final double updatedRotation;

    public LayerRotationEdit(LayerPainter painter, double old, double updated) {
        this.layerPainter = painter;
        this.oldRotation = old;
        this.updatedRotation = updated;
        this.setFinished(false);
    }

    @Override
    public void undo() {
        undoSubEdits();
        layerPainter.setRotationRadians(oldRotation);
        var repainter = StaticController.getScheduler();
        repainter.queueLayerPropertiesRepaint();
        repainter.queueCenterPanelsRepaint();
    }

    @Override
    public void redo() {
        layerPainter.setRotationRadians(updatedRotation);
        redoSubEdits();
        var repainter = StaticController.getScheduler();
        repainter.queueLayerPropertiesRepaint();
        repainter.queueCenterPanelsRepaint();
    }

    @Override
    public String getName() {
        return "Rotate Layer";
    }

    @Override
    public void cleanupReferences() {
        Deque<Edit> subEdits = this.getSubEdits();
        subEdits.forEach(edit -> {
            if (edit instanceof LayerRotationEdit checked) {
                checked.cleanupReferences();
            }
        });
        this.layerPainter = null;
    }

}
