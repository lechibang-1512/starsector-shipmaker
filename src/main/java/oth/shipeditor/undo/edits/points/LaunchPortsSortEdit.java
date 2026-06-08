package oth.shipeditor.undo.edits.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.RequiredArgsConstructor;
import oth.shipeditor.components.instrument.EditorInstrument;
import oth.shipeditor.components.viewer.entities.bays.LaunchBay;
import oth.shipeditor.components.viewer.entities.bays.LaunchPortPoint;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

import java.util.List;

@RequiredArgsConstructor
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class LaunchPortsSortEdit extends AbstractEdit {

    private final LaunchPortPoint portPoint;

    private final LaunchBay targetBay;

    private final LaunchBay oldParentBay;

    private final int targetIndex;

    private final int oldIndex;

    private int cachedBayIndex;

    @Override
    public void undo() {
        this.transferPort(targetBay, oldParentBay, oldIndex);
    }

    @Override
    public void redo() {
        this.transferPort(oldParentBay, targetBay, targetIndex);
    }

    public void transferPort(LaunchBay supplier, LaunchBay recipient, int index) {
        var oldBayPoints = supplier.getPortPoints();
        oldBayPoints.remove(portPoint);

        if (oldBayPoints.isEmpty()) {
            var painter = supplier.getBayPainter();
            List<LaunchBay> baysList = painter.getBaysList();
            cachedBayIndex = baysList.indexOf(supplier);
            painter.removeBay(supplier);
        }

        portPoint.setParentBay(recipient);
        var portPoints = recipient.getPortPoints();
        if (index != -1) {
            portPoints.add(index, portPoint);

            var recipientPainter = recipient.getBayPainter();
            List<LaunchBay> baysList = recipientPainter.getBaysList();
            if (!baysList.contains(recipient)) {
                recipientPainter.insertBay(recipient, cachedBayIndex);
            }
        }

        if (StaticController.getEditorMode() == EditorInstrument.LAUNCH_BAYS) {
            var repainter = StaticController.getScheduler();
            repainter.queueViewerRepaint();
            repainter.queueBaysPanelRepaint();
        }
    }

    @Override
    public String getName() {
        return "Sort Launch Ports";
    }

}
