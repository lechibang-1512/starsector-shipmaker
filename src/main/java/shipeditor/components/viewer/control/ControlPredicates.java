package shipeditor.components.viewer.control;
import shipeditor.components.viewer.ViewerEnums.PointSelectionMode;


import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.EventBus;

import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import java.util.function.Predicate;
import shipeditor.communication.events.viewer.control.ControlEvents.RotationRoundingToggled;
import shipeditor.communication.events.viewer.control.ControlEvents.PointLinkageToleranceChanged;
import shipeditor.communication.events.viewer.control.ControlEvents.PointSelectionModeChange;
import shipeditor.communication.events.viewer.control.ControlEvents.CursorSnappingToggled;
import shipeditor.communication.events.viewer.control.ControlEvents.MirrorModeChange;

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

    @Getter
    @Setter
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

    static final Predicate<MouseEvent> TRANSLATE_PREDICATE = e -> SwingUtilities.isMiddleMouseButton(e)
            && !e.isControlDown() && !e.isShiftDown() && !e.isAltDown();

    static final Predicate<MouseEvent> LAYER_MOVE_PREDICATE = e -> SwingUtilities.isMiddleMouseButton(e)
            && e.isShiftDown();

    static final Predicate<MouseEvent> LAYER_SELECT_PREDICATE = e -> e.isControlDown() && e.isAltDown();

    static final Predicate<MouseEvent> LAYER_ROTATE_PREDICATE = e -> SwingUtilities.isMiddleMouseButton(e)
            && e.isAltDown();

    static final Predicate<MouseEvent> REMOVE_POINT_PREDICATE = e -> SwingUtilities.isRightMouseButton(e)
            && e.isControlDown();

    static final Predicate<MouseEvent> SELECT_POINT_PREDICATE = e -> SwingUtilities.isLeftMouseButton(e)
            && !e.isControlDown() && !e.isShiftDown() && !e.isAltDown();

    public static final Predicate<MouseEvent> CHANGE_ANGLE_PREDICATE = e -> SwingUtilities.isLeftMouseButton(e)
            && e.isAltDown();

    public static final Predicate<MouseEvent> CHANGE_ARC_OR_SIZE_PREDICATE = e -> SwingUtilities.isRightMouseButton(e)
            && e.isAltDown();

    static final Predicate<MouseEvent> ROTATE_PREDICATE = e -> e.isControlDown();

    private ControlPredicates() {
    }

}
