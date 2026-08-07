package shipeditor.communication.events.viewer.control;

import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import shipeditor.communication.events.BusEvent;
import shipeditor.communication.events.viewer.ViewerEvent;
import shipeditor.components.ComponentEnums.CoordsDisplayMode;
import shipeditor.components.viewer.ViewerEnums.PointSelectionMode;
import shipeditor.components.viewer.layers.LayerPainter;

public class ControlEvents {
    public static record RotationRoundingToggled(boolean toggled) implements ViewerEvent {

    }

    public static record ViewerMouseReleased() implements ViewerEvent {

    }

    public static record PointLinkageToleranceChanged(int changed) implements ViewerEvent {

    }

    public static record PointSelectionModeChange(PointSelectionMode newMode) implements ViewerEvent {

    }

    public static record ViewerZoomChanged() implements ViewerEvent {

    }

    public static record ViewerTransformChanged() implements ViewerEvent {

    }

    public static record ViewerRotationToggled(boolean isSelected, boolean isEnabled) implements ViewerEvent {

    }

    public static record CursorSnappingToggled(boolean toggled) implements ViewerEvent {

    }

    public static record ViewerRotationSet(double degrees) implements ViewerEvent {

    }

    public static record MirrorModeChange(boolean enabled) implements ViewerEvent {

    }

    public static record ViewerTransformsReset() implements ViewerEvent {

    }

    public static record ViewerTransformRotated() implements ViewerEvent {

    }

    public static record ViewerGuidesToggled(
        boolean guidesEnabled,
        boolean bordersEnabled,
        boolean centerEnabled
    ) implements ViewerEvent {

}


    public static record ViewerRawMouseDragged(MouseEvent mouseEvent) implements BusEvent {
}


    public static record LayerAnchorDragged(AffineTransform screenToWorld, LayerPainter selected,
                                 Point2D difference) implements ViewerEvent {
}


    public static record ViewerRawMouseMoved(MouseEvent mouseEvent) implements BusEvent {
}


    public static record ViewerCursorMoved(Point2D rawCursor,
                                Point2D adjusted,
                                Point2D adjustedAndCorrected)
        implements ViewerEvent {

}


    public static record FeatureInstallQueued(Point2D worldPosition) implements ViewerEvent {

}


    public static record ViewerRawMousePressed(MouseEvent mouseEvent) implements BusEvent {
}


    public static record ViewerRawKeyPressed(java.awt.event.KeyEvent keyEvent) implements BusEvent {
}


    public static record ViewerRawKeyReleased(java.awt.event.KeyEvent keyEvent) implements BusEvent {
}


    public static record CoordsModeChanged(CoordsDisplayMode newMode) implements BusEvent {

}

}
