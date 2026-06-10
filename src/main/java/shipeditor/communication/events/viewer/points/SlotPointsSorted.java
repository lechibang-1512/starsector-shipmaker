package shipeditor.communication.events.viewer.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;

import java.util.List;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record SlotPointsSorted(List<WeaponSlotPoint> rearranged) implements PointEvent {

}
