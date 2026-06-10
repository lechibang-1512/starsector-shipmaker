package shipeditor.components.viewer.control;

import java.awt.event.InputEvent;
import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.control.*;

import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import java.util.function.Predicate;

@SuppressWarnings("ClassWithTooManyFields")
public final class ControlPredicates {

    public static final double MAXIMUM_ZOOM = 1000.0;
    public static final double MINIMUM_ZOOM = 0.1;

    public static final double ZOOMING_SPEED = 1.25;
    @SuppressWarnings("WeakerAccess")
    public static final double ROTATION_SPEED = 6.0;
    @Getter
    private static PointSelectionMode selectionMode = PointSelectionMode.CLOSEST;

    @Getter
    private static boolean mirrorModeEnabled = true;

    @Getter
    private static boolean cursorSnappingEnabled = true;

    @Getter
    private static boolean rotationRoundingEnabled = true;

    @Getter @Setter
    private static boolean selectionHoldingEnabled = true;

    @Getter
    private static int mirrorPointLinkageTolerance;

    public static void initSelectionModeListening() {
        EventBus.subscribe(ControlPredicates.class, event -> {
            if (event instanceof PointSelectionModeChange checked) {
                selectionMode = checked.newMode();
            } else if (event instanceof MirrorModeChange checked) {
                mirrorModeEnabled = checked.enabled();
            } else if (event instanceof CursorSnappingToggled checked) {
                cursorSnappingEnabled = checked.toggled();
            } else if (event instanceof RotationRoundingToggled checked) {
                rotationRoundingEnabled = checked.toggled();
            } else if (event instanceof PointLinkageToleranceChanged checked) {
                mirrorPointLinkageTolerance = checked.changed();
            }
        });
    }

    static final Predicate<MouseEvent> translatePredicate = e ->
            SwingUtilities.isMiddleMouseButton(e) && !e.isControlDown() && !e.isShiftDown() && !e.isAltDown();

    static final Predicate<MouseEvent> layerMovePredicate = e ->
            SwingUtilities.isMiddleMouseButton(e) && e.isShiftDown();

    static final Predicate<MouseEvent> layerSelectPredicate = e ->
            e.isControlDown() && e.isAltDown();

    static final Predicate<MouseEvent> layerRotatePredicate = e ->
            SwingUtilities.isMiddleMouseButton(e) && e.isAltDown();

    static final Predicate<MouseEvent> removePointPredicate = e ->
            SwingUtilities.isRightMouseButton(e) && e.isControlDown();

    static final Predicate<MouseEvent> selectPointPredicate = e ->
            SwingUtilities.isLeftMouseButton(e) && !e.isControlDown() && !e.isShiftDown() && !e.isAltDown();

    public static final Predicate<MouseEvent> changeAnglePredicate = e ->
            SwingUtilities.isLeftMouseButton(e) && e.isAltDown();

    public static final Predicate<MouseEvent> changeArcOrSizePredicate = e ->
            SwingUtilities.isRightMouseButton(e) && e.isAltDown();

    static final Predicate<MouseEvent> rotatePredicate = e -> e.isControlDown();

    private ControlPredicates() {
    }

}
