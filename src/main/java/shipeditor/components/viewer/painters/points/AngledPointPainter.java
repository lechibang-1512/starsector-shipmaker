package shipeditor.components.viewer.painters.points;

import java.awt.KeyEventDispatcher;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.ViewerRepaintQueued;
import shipeditor.components.viewer.control.ControlPredicates;
import shipeditor.components.viewer.entities.AngledPoint;
import shipeditor.components.viewer.entities.BaseWorldPoint;
import shipeditor.components.viewer.entities.WorldPoint;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.utility.Utility;

import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.KeyboardFocusManager;

public abstract class AngledPointPainter extends MirrorablePointPainter {



    protected AngledPointPainter(LayerPainter parent) {
        super(parent);
        this.initHotkeys();
    }

    @Override
    public void cleanupListeners() {
        super.cleanupListeners();
    }

    protected abstract int getControlHotkey();

    protected abstract int getCreationHotkey();

    protected abstract void setControlHotkeyPressed(boolean pressed);

    protected abstract void setCreationHotkeyPressed(boolean pressed);

    protected void initHotkeys() {
        EventBus.subscribe(this, event -> {
            if (!this.getParentLayer().isLayerActive()) {
                return;
            }
            if (event instanceof shipeditor.communication.events.viewer.control.ControlEvents.ViewerRawKeyPressed pressedEvent) {
                int keyCode = pressedEvent.keyEvent().getKeyCode();
                boolean isControlHotkey = (keyCode == getControlHotkey());
                boolean isCreationHotkey = (keyCode == getCreationHotkey());
                if (isControlHotkey) {
                    setControlHotkeyPressed(true);
                    EventBus.publish(new ViewerRepaintQueued());
                } else if (isCreationHotkey) {
                    setCreationHotkeyPressed(true);
                    EventBus.publish(new ViewerRepaintQueued());
                }
            } else if (event instanceof shipeditor.communication.events.viewer.control.ControlEvents.ViewerRawKeyReleased releasedEvent) {
                int keyCode = releasedEvent.keyEvent().getKeyCode();
                boolean isControlHotkey = (keyCode == getControlHotkey());
                boolean isCreationHotkey = (keyCode == getCreationHotkey());
                if (isControlHotkey) {
                    setControlHotkeyPressed(false);
                    EventBus.publish(new ViewerRepaintQueued());
                } else if (isCreationHotkey) {
                    setCreationHotkeyPressed(false);
                    EventBus.publish(new ViewerRepaintQueued());
                }
            }
        });
    }

    protected void changePointAngleByTarget(Point2D worldTarget) {
        WorldPoint selected = getSelected();
        if (selected == null) return;
        if (selected instanceof AngledPoint checked) {
            double result = AngledPointPainter.getTargetRotation(checked, worldTarget);
            this.changePointAngleWithMirrorCheck(checked, result);
        }
        else {
            throwIllegalPoint();
        }
    }

    protected static double getTargetRotation(WorldPoint selected, Point2D worldTarget) {
        Point2D pointPosition = selected.getPosition();
        double deltaX = worldTarget.getX() - pointPosition.getX();
        double deltaY = worldTarget.getY() - pointPosition.getY();

        double radians = Math.atan2(deltaX, deltaY);

        double rotationDegrees = Math.toDegrees(radians) + 180;
        double result = rotationDegrees;
        if (ControlPredicates.isRotationRoundingEnabled()) {
            result = Math.round(rotationDegrees * 2.0d) / 2.0d;
        }
        if (Utility.areDoublesEqual(result, 360)) {
            result = 0.0d;
        }
        return result;
    }

    public void changePointAngleWithMirrorCheck(AngledPoint slotPoint, double angleDegrees) {
        slotPoint.changeSlotAngle(angleDegrees);
        boolean mirrorMode = ControlPredicates.isMirrorModeEnabled();
        BaseWorldPoint mirroredCounterpart = getMirroredCounterpart(slotPoint);
        if (mirrorMode && mirroredCounterpart instanceof AngledPoint checkedSlot) {
            double angle = Utility.flipAngle(angleDegrees);
            Point2D slotPosition = checkedSlot.getPosition();
            double slotX = slotPosition.getX();
            LayerPainter parentLayer = getParentLayer();
            Point2D entityCenter = parentLayer.getEntityCenter();
            double centerX = entityCenter.getX();
            if ((Math.abs(slotX - centerX) < 0.05d)) {
                angle = angleDegrees;
            }
            checkedSlot.changeSlotAngle(angle);
        }
    }

}
