package shipeditor.components.viewer.painters.points.ship;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import shipeditor.communication.BusEventListener;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.ViewerRepaintQueued;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.control.ControlPredicates;
import shipeditor.components.viewer.entities.BaseWorldPoint;
import shipeditor.components.viewer.entities.ShieldCenterPoint;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.representation.ship.HullStyle;
import shipeditor.undo.EditDispatch;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.graphics.ColorUtilities;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;
import shipeditor.communication.events.viewer.points.PointEvents.InstrumentModeChanged;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class ShieldPointPainter extends SinglePointPainter {

    private final List<BaseWorldPoint> points = new ArrayList<>();

    @Getter
    private ShieldCenterPoint shieldCenterPoint;

    private static final int dragShieldRadiusHotkey = KeyEvent.VK_CONTROL;

    private boolean shieldRadiusHotkeyPressed;


    public ShieldPointPainter(ShipPainter parent) {
        super(parent);
        this.initModeListening();
        this.initHotkeys();
        this.setInteractionEnabled(StaticController.getEditorMode() == EditorInstrument.SHIELD);
    }

    @Override
    public void cleanupListeners() {
        super.cleanupListeners();
    }

    public void initShieldPoint(Point2D translated, HullSpecFile hullSpecFile) {
        HullStyle style = GameDataRepository.fetchStyleByID(hullSpecFile.getStyle());
        if (style == null) {
            style = new HullStyle();
        }
        double shieldRadius = hullSpecFile.getShieldRadius();

        if (this.shieldCenterPoint != null) {
            this.removePoint(shieldCenterPoint);
        }

        this.shieldCenterPoint = new ShieldCenterPoint(translated,
                (float) shieldRadius, this.getParentLayer(), style, this);
        this.addPoint(shieldCenterPoint);
        Color shieldInnerColor = style.getShieldInnerColor();
        float styleInnerColorOpacity = ColorUtilities.getOpacityFromAlpha(shieldInnerColor.getAlpha());
        this.setPaintOpacity(styleInnerColorOpacity);
    }

    public void setShieldStyle(HullStyle style) {
        Color shieldInnerColor = style.getShieldInnerColor();
        float styleInnerColorOpacity = ColorUtilities.getOpacityFromAlpha(shieldInnerColor.getAlpha());
        this.setPaintOpacity(styleInnerColorOpacity);

        this.shieldCenterPoint.setAssociatedStyle(style);
    }

    private void initModeListening() {
        BusEventListener modeListener = event -> {
            if (event instanceof InstrumentModeChanged checked) {
                EditorInstrument editorInstrument = EditorInstrument.SHIELD;
                this.setInteractionEnabled(checked.newMode() == editorInstrument);
                EventBus.publish(new InstrumentRepaintQueued(editorInstrument));
            }
        };
        EventBus.subscribe(this, modeListener);
        // Subscribe to raw mouse moved and compute radius drag internally.
        BusEventListener rawMouseMovedListener = event -> {
            if (event instanceof shipeditor.communication.events.viewer.control.ControlEvents.ViewerRawMouseMoved checked && isInteractionEnabled()) {
                if (!shieldRadiusHotkeyPressed) return;
                java.awt.geom.AffineTransform screenToWorld = shipeditor.utility.overseers.StaticController.getScreenToWorld();
                java.awt.event.MouseEvent me = checked.mouseEvent();
                Point2D transformed = screenToWorld.transform(me.getPoint(), null);
                if (ControlPredicates.isCursorSnappingEnabled()) {
                    transformed = screenToWorld.transform(shipeditor.utility.overseers.StaticController.getAdjustedCursor(), null);
                }
                Point2D pointPosition = this.shieldCenterPoint.getPosition();
                float radius = (float) pointPosition.distance(transformed);
                float result = radius;
                if (ControlPredicates.isCursorSnappingEnabled()) {
                    result = Math.round(radius * 2) / 2.0f;
                }
                EditDispatch.postShieldRadiusChanged(this.shieldCenterPoint, result);
            }
        };
        EventBus.subscribe(this, rawMouseMovedListener);
    }

    private void initHotkeys() {
        EventBus.subscribe(this, event -> {
            if (!this.getParentLayer().isLayerActive()) {
                return;
            }
            if (event instanceof shipeditor.communication.events.viewer.control.ControlEvents.ViewerRawKeyPressed pressedEvent) {
                int keyCode = pressedEvent.keyEvent().getKeyCode();
                boolean isShieldHotkey = (keyCode == dragShieldRadiusHotkey);
                if (isShieldHotkey) {
                    this.shieldRadiusHotkeyPressed = true;
                    EventBus.publish(new ViewerRepaintQueued());
                }
            } else if (event instanceof shipeditor.communication.events.viewer.control.ControlEvents.ViewerRawKeyReleased releasedEvent) {
                int keyCode = releasedEvent.keyEvent().getKeyCode();
                boolean isShieldHotkey = (keyCode == dragShieldRadiusHotkey);
                if (isShieldHotkey) {
                    this.shieldRadiusHotkeyPressed = false;
                    EventBus.publish(new ViewerRepaintQueued());
                }
            }
        });
    }

    @Override
    public List<BaseWorldPoint> getPointsIndex() {
        return points;
    }

    @Override
    protected void addPointToIndex(BaseWorldPoint point) {
        points.add(point);
    }

    @Override
    protected void removePointFromIndex(BaseWorldPoint point) {
        points.remove(point);
    }

    @Override
    public int getIndexOfPoint(BaseWorldPoint point) {
        return points.indexOf(point);
    }

    @Override
    protected Class<ShieldCenterPoint> getTypeReference() {
        return ShieldCenterPoint.class;
    }

}
