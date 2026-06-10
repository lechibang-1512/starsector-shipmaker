package shipeditor.components.viewer.painters.points.ship;

import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.BusEventListener;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.control.ViewerRawMouseDragged;
import shipeditor.communication.events.viewer.control.ViewerRawMouseMoved;
import shipeditor.communication.events.viewer.control.ViewerRawMousePressed;
import shipeditor.components.viewer.control.ControlPredicates;
import shipeditor.components.viewer.entities.BaseWorldPoint;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.painters.points.AngledPointPainter;
import shipeditor.utility.overseers.StaticController;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public abstract class AbstractSlotPainter extends AngledPointPainter {

    @Getter
    private boolean controlHotkeyPressed;

    @Getter @Setter
    private boolean creationHotkeyPressed;

    protected AbstractSlotPainter(ShipPainter parent) {
        super(parent);
    }

    @Override
    public void setControlHotkeyPressed(boolean pressed) {
        this.controlHotkeyPressed = pressed;
    }

    @Override
    protected int getControlHotkey() {
        return KeyEvent.VK_ALT;
    }

    @Override
    protected int getCreationHotkey() {
        return KeyEvent.VK_SHIFT;
    }

    @Override
    protected void handlePointSelectionEvent(BaseWorldPoint point) {
        if (this.controlHotkeyPressed) return;
        super.handlePointSelectionEvent(point);
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    @Override
    protected void initInteractionListeners() {
        super.initInteractionListeners();
        
        BusEventListener rawMouseListener = event -> {
            if (!isInteractionEnabled() || !isControlHotkeyPressed()) return;
            
            if (event instanceof ViewerRawMouseDragged checked) {
                MouseEvent me = checked.mouseEvent();
                if (ControlPredicates.changeAnglePredicate.test(me)) {
                    Point2D worldTarget = computeWorldTarget(me);
                    super.changePointAngleByTarget(worldTarget);
                } else if (ControlPredicates.changeArcOrSizePredicate.test(me)) {
                    AffineTransform rotatedTransform = StaticController.getScreenToWorld();
                    Point2D worldTarget = rotatedTransform.transform(me.getPoint(), null);
                    this.handleSizeOrArcChange(worldTarget);
                }
            } else if (event instanceof ViewerRawMouseMoved checked) {
                MouseEvent me = checked.mouseEvent();
                if (ControlPredicates.changeAnglePredicate.test(me)) {
                    Point2D worldTarget = computeWorldTarget(me);
                    super.changePointAngleByTarget(worldTarget);
                }
            } else if (event instanceof ViewerRawMousePressed checked) {
                MouseEvent me = checked.mouseEvent();
                if (ControlPredicates.changeAnglePredicate.test(me)) {
                    Point2D worldTarget = computeWorldTarget(me);
                    super.changePointAngleByTarget(worldTarget);
                }
            }
        };
        EventBus.subscribe(this, rawMouseListener);
        
        this.initSortingListeners();
    }

    protected Point2D computeWorldTarget(MouseEvent me) {
        AffineTransform rotatedTransform = StaticController.getScreenToWorld();
        Point2D target = me.getPoint();
        if (ControlPredicates.isRotationRoundingEnabled()) {
            target = StaticController.getAdjustedCursor();
        }
        return rotatedTransform.transform(target, null);
    }

    protected abstract void handleSizeOrArcChange(Point2D worldTarget);

    protected abstract void initSortingListeners();

}
