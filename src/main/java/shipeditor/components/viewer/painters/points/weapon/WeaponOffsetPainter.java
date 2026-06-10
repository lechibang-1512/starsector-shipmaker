package shipeditor.components.viewer.painters.points.weapon;

import shipeditor.utility.graphics.opengl.OpenGLPainter;
import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.events.viewer.points.PointCreationQueued;
import shipeditor.components.instrument.EditorInstrument;
import shipeditor.components.viewer.entities.BaseWorldPoint;
import shipeditor.components.viewer.entities.weapon.OffsetPoint;
import shipeditor.components.viewer.layers.weapon.WeaponPainter;
import shipeditor.components.viewer.painters.points.AngledPointPainter;
import shipeditor.representation.weapon.WeaponMount;
import shipeditor.utility.Utility;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WeaponOffsetPainter extends AngledPointPainter {

    @Setter
    private List<OffsetPoint> offsetPoints;

    private final WeaponMount designatedType;

    public WeaponOffsetPainter(WeaponPainter parent, WeaponMount mount) {
        super(parent);
        this.offsetPoints = new ArrayList<>();
        this.designatedType = mount;
    }

    @Override
    public List<OffsetPoint> getPointsIndex() {
        return offsetPoints;
    }

    @Override
    protected void addPointToIndex(BaseWorldPoint point) {
        if (point instanceof OffsetPoint checked) {
            offsetPoints.add(checked);
        } else {
            throwIllegalPoint();
        }
    }

    @Override
    protected void removePointFromIndex(BaseWorldPoint point) {
        if (point instanceof OffsetPoint checked) {
            offsetPoints.remove(checked);
        } else {
            throwIllegalPoint();
        }
    }

    @Override
    public int getIndexOfPoint(BaseWorldPoint point) {
        if (point instanceof OffsetPoint checked) {
            return offsetPoints.indexOf(checked);
        } else {
            throwIllegalPoint();
            return -1;
        }
    }

    @Override
    public WeaponPainter getParentLayer() {
        return (WeaponPainter) super.getParentLayer();
    }

    @Override
    protected Class<OffsetPoint> getTypeReference() {
        return OffsetPoint.class;
    }

    @Override
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        if (!checkVisibility()) return;

        var parentLayer = getParentLayer();
        if (parentLayer.getMount() != designatedType) return;

        shapeRenderer.begin(projection, new Matrix4f());
        this.paintDelegates(spriteRenderer, shapeRenderer, projection, view);
        shapeRenderer.end();
    }

    @lombok.Getter
    private boolean controlHotkeyPressed;

    @lombok.Getter @lombok.Setter
    private boolean creationHotkeyPressed;

    @Override
    public void setControlHotkeyPressed(boolean pressed) {
        this.controlHotkeyPressed = pressed;
    }

    @Override
    protected int getControlHotkey() {
        return java.awt.event.KeyEvent.VK_ALT;
    }

    @Override
    protected int getCreationHotkey() {
        return java.awt.event.KeyEvent.VK_SHIFT;
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
        
        shipeditor.communication.BusEventListener rawMouseListener = event -> {
            if (!isInteractionEnabled() || !isControlHotkeyPressed()) return;
            
            if (event instanceof shipeditor.communication.events.viewer.control.ViewerRawMouseDragged checked) {
                java.awt.event.MouseEvent me = checked.mouseEvent();
                if (shipeditor.components.viewer.control.ControlPredicates.changeAnglePredicate.test(me)) {
                    java.awt.geom.Point2D worldTarget = computeWorldTarget(me);
                    super.changePointAngleByTarget(worldTarget);
                }
            } else if (event instanceof shipeditor.communication.events.viewer.control.ViewerRawMouseMoved checked) {
                java.awt.event.MouseEvent me = checked.mouseEvent();
                if (shipeditor.components.viewer.control.ControlPredicates.changeAnglePredicate.test(me)) {
                    java.awt.geom.Point2D worldTarget = computeWorldTarget(me);
                    super.changePointAngleByTarget(worldTarget);
                }
            } else if (event instanceof shipeditor.communication.events.viewer.control.ViewerRawMousePressed checked) {
                java.awt.event.MouseEvent me = checked.mouseEvent();
                if (shipeditor.components.viewer.control.ControlPredicates.changeAnglePredicate.test(me)) {
                    java.awt.geom.Point2D worldTarget = computeWorldTarget(me);
                    super.changePointAngleByTarget(worldTarget);
                }
            }
        };
        shipeditor.communication.EventBus.subscribe(this, rawMouseListener);
    }

    protected java.awt.geom.Point2D computeWorldTarget(java.awt.event.MouseEvent me) {
        java.awt.geom.AffineTransform rotatedTransform = shipeditor.utility.overseers.StaticController.getScreenToWorld();
        java.awt.geom.Point2D target = me.getPoint();
        if (shipeditor.components.viewer.control.ControlPredicates.isRotationRoundingEnabled()) {
            target = shipeditor.utility.overseers.StaticController.getAdjustedCursor();
        }
        return rotatedTransform.transform(target, null);
    }

    @Override
    protected EditorInstrument getInstrumentType() {
        return EditorInstrument.WEAPON_OFFSETS;
    }

    @Override
    protected void handleCreation(PointCreationQueued event) {
        OffsetPoint newPoint = new OffsetPoint(event.position(), getParentLayer());
        this.addPoint(newPoint);
    }

    @Override
    public void insertPoint(BaseWorldPoint toInsert, int precedingIndex) {
        if (toInsert instanceof OffsetPoint checked) {
            offsetPoints.add(precedingIndex, checked);
        } else {
            throwIllegalPoint();
        }
    }

}
