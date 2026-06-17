package shipeditor.components.viewer.painters.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.utility.graphics.opengl.OpenGLPainter;
import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import shipeditor.communication.BusEventListener;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.BusEvent;
import shipeditor.communication.events.viewer.ViewerRepaintQueued;
import shipeditor.communication.events.viewer.points.PointEvents.PointSelectedConfirmed;
import shipeditor.communication.events.viewer.points.PointEvents.PointDragQueued;
import shipeditor.components.viewer.control.ControlPredicates;
import shipeditor.components.viewer.ViewerEnums.PointSelectionMode;
import shipeditor.components.viewer.entities.BaseWorldPoint;
import shipeditor.components.viewer.entities.WorldPoint;
import shipeditor.components.viewer.ViewerEnums.PainterVisibility;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.Utility;
import shipeditor.utility.overseers.StaticController;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import shipeditor.communication.events.viewer.points.PointEvents.PointAddConfirmed;
import shipeditor.communication.events.viewer.points.PointEvents.PointSelectQueued;
import shipeditor.communication.events.viewer.points.PointEvents.PointRemovedConfirmed;
import shipeditor.communication.events.viewer.points.PointEvents.PointRemoveQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.PainterOpacityChangeQueued;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass"})
@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public abstract class AbstractPointPainter implements OpenGLPainter {

    private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();

    @Getter @Setter
    private WorldPoint selected;

    @Getter @Setter
    private PainterVisibility visibilityMode;

    @Setter
    private boolean interactionEnabled;

    /**
     * The world-to-screen transform that will be passed to the delegates
     */
    @Getter
    private final AffineTransform delegateWorldToScreen;

    @Getter
    private float paintOpacity = 1.0f;

    protected AbstractPointPainter() {
        this.delegateWorldToScreen = new AffineTransform();
        this.initPointListeners();
        this.visibilityMode = PainterVisibility.SHOWN_WHEN_EDITED;
        this.setPaintOpacity(1.0f);
    }

    public boolean isInteractionEnabled() {
        return interactionEnabled && isParentLayerActive();
    }

    public void setPaintOpacity(float opacity) {
        if (opacity < 0.0f) {
            this.paintOpacity = 0.0f;
        } else this.paintOpacity = Math.min(opacity, 1.0f);
    }

    protected abstract boolean isMirrorable();

    protected void cleanupListeners() {
        EventBus.unsubscribeByParent(this);
    }

    public void cleanupPointPainter() {
        this.clearPoints();
        this.cleanupListeners();
    }

    public void clearPoints() {
        Iterable<BaseWorldPoint> points = new ArrayList<>(this.getPointsIndex());
        for (BaseWorldPoint point : points) {
            point.cleanupForRemoval();
            this.removePoint(point);
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected BusEventListener createSelectionListener() {
        return new SimplePointSelectionListener();
    }

    private void initPointListeners() {
        BusEventListener pointRemovalListener = event -> {
            if (event instanceof PointRemoveQueued checked && this.isInteractionEnabled()) {
                this.handlePointRemovalEvent(checked.point(), checked.fromList());
            }
        };
        EventBus.subscribe(this, pointRemovalListener);

        BusEventListener pointSelectionListener = createSelectionListener();
        EventBus.subscribe(this, pointSelectionListener);

        BusEventListener pointDragListener = event -> {
            if (event instanceof PointDragQueued checked) {
                if (!this.isInteractionEnabled()) return;
                if (getSelected() == null) return;
                AffineTransform screenToWorld = checked.screenToWorld();
                Point2D target = checked.target();
                Point2D changedPosition = Utility.correctAdjustedCursor(target, screenToWorld);
                this.dragPointWithMirrorCheck(changedPosition);
            }
        };
        EventBus.subscribe(this, pointDragListener);
        BusEventListener painterOpacityListener = event -> {
            if (event instanceof PainterOpacityChangeQueued checked) {
                Class<? extends AbstractPointPainter> painterClass = checked.painterClass();
                if (!painterClass.isInstance(this)) return;
                if (!isParentLayerActive()) return;
                this.setPaintOpacity(checked.change());
                EventBus.publish(new ViewerRepaintQueued());
            }
        };
        EventBus.subscribe(this, painterOpacityListener);
    }

    public void dragPointWithMirrorCheck(Point2D changedPosition) {
        WorldPoint counterpart = null;
        Point2D counterpartNewPosition = null;
        boolean mirroringEnabled = ControlPredicates.isMirrorModeEnabled();
        if (isMirrorable() && mirroringEnabled) {
            counterpart = getMirroredCounterpart(getSelected());
            if (counterpart != null) {
                counterpartNewPosition = createCounterpartPosition(changedPosition);
            }
        }
        EditDispatch.postPointDragged(getSelected(), changedPosition);
        if (counterpartNewPosition != null) {
            EditDispatch.postPointDragged(counterpart, counterpartNewPosition);
        }
    }

    protected void handlePointRemovalEvent(BaseWorldPoint point, boolean removalViaListPanel) {
        Class<? extends BaseWorldPoint> typeReference = getTypeReference();
        if (typeReference.isInstance(point) && removalViaListPanel) {
            this.commencePointRemoval(point);
        } else {
            WorldPoint currentSelected = this.getSelected();
            if (currentSelected != null && !removalViaListPanel) {
                this.commencePointRemoval((BaseWorldPoint) currentSelected);
            }
        }
    }

    private void commencePointRemoval(BaseWorldPoint point) {
        List<? extends BaseWorldPoint> pointsIndex = getPointsIndex();
        if (!pointsIndex.contains(point)) {
            throw new IllegalArgumentException("Point passed for removal is not present in the point painter!");
        }
        boolean mirroringEnabled = ControlPredicates.isMirrorModeEnabled();
        WorldPoint counterpart = null;
        if (isMirrorable() && mirroringEnabled) {
            counterpart = getMirroredCounterpart(point);
        }
        EditDispatch.postPointRemoved(this, point);
        if (counterpart != null) {
            EditDispatch.postPointRemoved(this, (BaseWorldPoint) counterpart);
        }
    }

    protected abstract boolean isParentLayerActive();

    Point2D createCounterpartPosition(Point2D toMirror) {
        throw new UnsupportedOperationException("Point mirroring supported only for specific point painters!");
    }

    @SuppressWarnings("WeakerAccess")
    protected void handlePointSelectionEvent(BaseWorldPoint point) {
        if (point != null) {
            List<? extends BaseWorldPoint> pointsIndex = getPointsIndex();
            if (!pointsIndex.contains(point)) return;
            WorldPoint currentSelected = this.getSelected();
            if (currentSelected == point) return;

            if (currentSelected != null) {
                currentSelected.setPointSelected(false);
            }
            this.setSelected(point);
            currentSelected = this.getSelected();
            if (currentSelected != null) {
                currentSelected.setPointSelected(true);
            }
            EventBus.publish(new PointSelectedConfirmed(currentSelected));
            EventBus.publish(new ViewerRepaintQueued());
        } else {
            selectPointConditionally();
        }
    }

    protected void selectPointConditionally() {
        PointSelectionMode current = ControlPredicates.getSelectionMode();
        if (current == PointSelectionMode.STRICT) {
            this.selectPointStrictly();
            return;
        }
        this.selectPointClosest();
    }

    @SuppressWarnings("WeakerAccess")
    public BaseWorldPoint findClosestPoint(Point2D target) {
        BaseWorldPoint closestPoint = null;
        double closestDistance = Double.MAX_VALUE;

        for (BaseWorldPoint point : this.getEligibleForSelection()) {
            Point2D position = point.getPosition();
            double distance = target.distance(position);

            if (distance < closestDistance) {
                closestPoint = point;
                closestDistance = distance;
            }
        }

        return closestPoint;
    }

    @SuppressWarnings("WeakerAccess")
    protected List<? extends BaseWorldPoint> getEligibleForSelection() {
        return this.getPointsIndex();
    }

    protected void selectPointClosest() {
        Point2D cursor = StaticController.getCorrectedCursor();
        BaseWorldPoint toSelect = findClosestPoint(cursor);

        WorldPoint selectedPoint = this.getSelected();
        if (selectedPoint == toSelect) return;

        if (selectedPoint != null) {
            selectedPoint.setPointSelected(false);
        }
        this.setSelected(toSelect);
        if (toSelect != null) {
            toSelect.setPointSelected(true);
        }
        EventBus.publish(new PointSelectedConfirmed(toSelect));
        EventBus.publish(new ViewerRepaintQueued());
    }

    private void selectPointStrictly() {
        if (!this.isMousedOverPoint()) return;
        BaseWorldPoint mousedOver = this.getMousedOver();
        WorldPoint currentSelected = this.getSelected();
        if (currentSelected == mousedOver) return;

        if (currentSelected != null) {
            currentSelected.setPointSelected(false);
        }
        this.setSelected(mousedOver);
        WorldPoint point = this.getSelected();
        if (point != null) {
            point.setPointSelected(true);
        }
        EventBus.publish(new PointSelectedConfirmed(point));
        EventBus.publish(new ViewerRepaintQueued());
    }

    public abstract List<? extends BaseWorldPoint> getPointsIndex();

    protected abstract void addPointToIndex(BaseWorldPoint point);

    protected abstract void removePointFromIndex(BaseWorldPoint point);

    public abstract int getIndexOfPoint(BaseWorldPoint point);

    protected abstract WorldPoint getMirroredCounterpart(WorldPoint inputPoint);

    protected abstract Class<? extends BaseWorldPoint> getTypeReference();

    @SuppressWarnings("WeakerAccess")
    protected boolean isPointEligible(WorldPoint point) {
        if (point != null) {
            Class<? extends BaseWorldPoint> typeReferenceClass = getTypeReference();
            return typeReferenceClass.isAssignableFrom(point.getClass());
        }
        return true;
    }

    private boolean isMousedOverPoint() {
        return this.getMousedOver() != null;
    }

    private BaseWorldPoint getMousedOver() {
        BaseWorldPoint mousedOver = null;
        for (BaseWorldPoint point : this.getPointsIndex()) {
            if (point.isCursorInBounds()) {
                mousedOver = point;
            }
        }
        return mousedOver;
    }

    public void addPoint(BaseWorldPoint point) {
        this.addPointToIndex(point);
        EventBus.publish(new PointAddConfirmed(point));
    }

    public void removePoint(BaseWorldPoint point) {
        this.removePointFromIndex(point);
        WorldPoint currentSelected = this.getSelected();
        if (currentSelected == point) {
            this.setSelected(null);
        }
        point.setPointSelected(false);
        EventBus.publish(new PointRemovedConfirmed(point));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean hasPointAtCoords(Point2D point2D) {
        boolean pointDoesExist = false;
        for (WorldPoint point : this.getPointsIndex()) {
            Point2D coords = point.getPosition();
            if (point2D.equals(coords)) {
                pointDoesExist = true;
                break;
            }
        }
        return pointDoesExist;
    }

    protected void paintDelegates(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        List<? extends BaseWorldPoint> pointsIndex = this.getPointsIndex();
        pointsIndex.forEach(painter -> paintDelegate(spriteRenderer, shapeRenderer, projection, view, painter));
    }

    @SuppressWarnings("WeakerAccess")
    protected void paintDelegate(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view, OpenGLPainter painter) {
        if (painter != null) {
            painter.paint(spriteRenderer, shapeRenderer, projection, view);
        }
    }

    @Override
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        if (!checkVisibility()) return;

        shapeRenderer.begin(projection, IDENTITY_MATRIX);
        paintDelegates(spriteRenderer, shapeRenderer, projection, view);
        shapeRenderer.end();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean checkVisibility() {
        PainterVisibility visibility = getVisibilityMode();
        if (visibility == PainterVisibility.ALWAYS_HIDDEN) return false;
        if (visibility == PainterVisibility.SHOWN_WHEN_EDITED && this.isInteractionEnabled()) return true;
        if (visibility == PainterVisibility.SHOWN_WHEN_SELECTED && this.isParentLayerActive()) return true;
        return visibility == PainterVisibility.ALWAYS_SHOWN;
    }

    @Override
    public String toString() {
        Class<? extends AbstractPointPainter> identity = this.getClass();
        return identity.getSimpleName() + " @" + this.hashCode();
    }

    /**
     * @throws IllegalArgumentException as a fail-fast precaution when illegal point type is detected.
     */
    @SuppressWarnings("WeakerAccess")
    protected void throwIllegalPoint() {
        Class<? extends AbstractPointPainter> identity = this.getClass();
        throw new IllegalArgumentException("Illegal point type in " + identity.getSimpleName());
    }

    private class SimplePointSelectionListener implements BusEventListener {
        @Override
        public void handleEvent(BusEvent event) {
            if (event instanceof PointSelectQueued checked && AbstractPointPainter.this.isPointEligible(checked.point())) {
                if (!AbstractPointPainter.this.isInteractionEnabled()) return;
                AbstractPointPainter.this.handlePointSelectionEvent((BaseWorldPoint) checked.point());
            }
        }
    }

}