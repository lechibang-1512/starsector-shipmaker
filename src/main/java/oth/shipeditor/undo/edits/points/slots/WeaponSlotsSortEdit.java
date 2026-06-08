package oth.shipeditor.undo.edits.points.slots;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import oth.shipeditor.components.viewer.painters.points.ship.WeaponSlotPainter;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

import java.util.List;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WeaponSlotsSortEdit extends AbstractEdit {

    private final WeaponSlotPainter pointPainter;

    private final List<WeaponSlotPoint> oldList;

    private final List<WeaponSlotPoint> newList;

    public WeaponSlotsSortEdit(WeaponSlotPainter painter, List<WeaponSlotPoint> old, List<WeaponSlotPoint> changed) {
        this.pointPainter = painter;
        this.oldList = old;
        this.newList = changed;
    }

    @Override
    public void undo() {
        pointPainter.setSlotPoints(oldList);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
    }

    @Override
    public void redo() {
        pointPainter.setSlotPoints(newList);
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        repainter.queueSlotsPanelRepaint();
    }

    @Override
    public String getName() {
        return "Sort Weapon Slots";
    }

}
