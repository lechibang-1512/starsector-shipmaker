package oth.shipeditor.undo.edits.features;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import oth.shipeditor.components.instrument.EditorInstrument;
import oth.shipeditor.components.viewer.painters.points.ship.features.FittedWeaponGroup;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

import java.util.List;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WeaponGroupRemovalEdit extends AbstractEdit {

    private final List<FittedWeaponGroup> weaponGroups;
    private final int groupIndex;
    private final FittedWeaponGroup toRemove;

    @SuppressWarnings("ParameterHidesMemberVariable")
    public WeaponGroupRemovalEdit(List<FittedWeaponGroup> weaponGroups, int groupIndex, FittedWeaponGroup toRemove) {
        this.weaponGroups = weaponGroups;
        this.groupIndex = groupIndex;
        this.toRemove = toRemove;
    }

    @Override
    public void undo() {
        weaponGroups.add(groupIndex, toRemove);
        if (StaticController.getEditorMode() == EditorInstrument.VARIANT_WEAPONS) {
            var repainter = StaticController.getScheduler();
            repainter.queueViewerRepaint();
            repainter.queueVariantWeaponsRepaint();
        }
    }

    @Override
    public void redo() {
        weaponGroups.remove(toRemove);
        if (StaticController.getEditorMode() == EditorInstrument.VARIANT_WEAPONS) {
            var repainter = StaticController.getScheduler();
            repainter.queueViewerRepaint();
            repainter.queueVariantWeaponsRepaint();
        }
    }

    @Override
    public String getName() {
        return "Remove Weapon Group";
    }

}
