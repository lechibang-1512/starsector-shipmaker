package shipeditor.components.viewer.painters.points.ship;

import shipeditor.components.instrument.ship.slots.SlotCreationPane;
import shipeditor.components.instrument.ship.slots.WeaponSlotClipboard;
import shipeditor.components.viewer.control.ControlPredicates;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.Utility;

import java.awt.geom.Point2D;
import java.util.List;

public final class WeaponSlotCreationController {

    private WeaponSlotCreationController() {
    }

    public static void handleCreation(WeaponSlotPainter painter, Point2D position) {
        ShipPainter parentLayer = painter.getParentLayer();
        boolean mirrorMode = ControlPredicates.isMirrorModeEnabled();

        WeaponSlotPoint created = null;
        WeaponSlotPoint counterpart = null;

        String uniqueID = painter.generateUniqueSlotID();

        switch (SlotCreationPane.getMode()) {
            case BY_CLOSEST -> {
                WeaponSlotPoint closest = (WeaponSlotPoint) painter.findClosestPoint(position);
                if (closest != null) {
                    created = new WeaponSlotPoint(position, parentLayer, closest);
                } else {
                    created = new WeaponSlotPoint(position, parentLayer);
                    created.setWeaponType(SlotCreationPane.getDefaultType());
                    created.setWeaponMount(SlotCreationPane.getDefaultMount());
                    created.setWeaponSize(SlotCreationPane.getDefaultSize());
                    created.setAngle(SlotCreationPane.getDefaultAngle());
                    created.setArc(SlotCreationPane.getDefaultArc());
                    created.setRenderOrderMod(SlotCreationPane.getDefaultRenderOrderMod());
                }
                created.setId(uniqueID);
            }
            case BY_DEFAULT -> {
                created = new WeaponSlotPoint(position, parentLayer);
                created.setId(uniqueID);
                created.setWeaponType(SlotCreationPane.getDefaultType());
                created.setWeaponMount(SlotCreationPane.getDefaultMount());
                created.setWeaponSize(SlotCreationPane.getDefaultSize());
                created.setAngle(SlotCreationPane.getDefaultAngle());
                created.setArc(SlotCreationPane.getDefaultArc());
                created.setRenderOrderMod(SlotCreationPane.getDefaultRenderOrderMod());
            }
        }

        if (mirrorMode) {
            if (painter.getMirroredCounterpart(created) == null) {
                Point2D counterpartPosition = painter.createCounterpartPosition(position);
                counterpart = new WeaponSlotPoint(counterpartPosition, parentLayer, created);
                String incrementedID = parentLayer.incrementUniqueSlotID(uniqueID);
                counterpart.setId(incrementedID);
                double flipAngle = Utility.flipAngle(counterpart.getAngle());
                counterpart.setAngle(flipAngle);
            }
        }

        EditDispatch.postPointAdded(painter, created);
        if (counterpart != null) {
            EditDispatch.postPointAdded(painter, counterpart);
        }
    }

    public static void pasteSlots(WeaponSlotPainter painter, List<WeaponSlotClipboard.CopiedSlotData> copiedSlots, Point2D targetPosition) {
        if (copiedSlots == null || copiedSlots.isEmpty()) return;

        ShipPainter parentLayer = painter.getParentLayer();
        boolean mirrorMode = ControlPredicates.isMirrorModeEnabled();

        double sumX = 0;
        double sumY = 0;
        for (WeaponSlotClipboard.CopiedSlotData data : copiedSlots) {
            sumX += data.x;
            sumY += data.y;
        }
        double avgX = sumX / copiedSlots.size();
        double avgY = sumY / copiedSlots.size();

        for (WeaponSlotClipboard.CopiedSlotData data : copiedSlots) {
            double offsetX = data.x - avgX;
            double offsetY = data.y - avgY;

            Point2D position;
            if (targetPosition != null) {
                position = new Point2D.Double(targetPosition.getX() + offsetX, targetPosition.getY() + offsetY);
            } else {
                position = new Point2D.Double(data.x + 10.0, data.y + 10.0);
            }

            String uniqueID = painter.generateUniqueSlotID();
            WeaponSlotPoint created = new WeaponSlotPoint(position, parentLayer);
            created.setId(uniqueID);
            created.setWeaponType(data.type);
            created.setWeaponMount(data.mount);
            created.setWeaponSize(data.size);
            created.setAngle(data.angle);
            created.setArc(data.arc);
            created.setRenderOrderMod(data.renderOrderMod);

            WeaponSlotPoint counterpart = null;
            if (mirrorMode) {
                if (painter.getMirroredCounterpart(created) == null) {
                    Point2D counterpartPosition = painter.createCounterpartPosition(position);
                    counterpart = new WeaponSlotPoint(counterpartPosition, parentLayer, created);
                    String incrementedID = parentLayer.incrementUniqueSlotID(uniqueID);
                    counterpart.setId(incrementedID);
                    double flipAngle = Utility.flipAngle(counterpart.getAngle());
                    counterpart.setAngle(flipAngle);
                }
            }

            EditDispatch.postPointAdded(painter, created);
            if (counterpart != null) {
                EditDispatch.postPointAdded(painter, counterpart);
            }
        }
    }
}
