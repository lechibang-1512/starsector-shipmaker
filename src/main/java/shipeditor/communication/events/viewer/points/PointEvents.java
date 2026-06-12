package shipeditor.communication.events.viewer.points;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;
import shipeditor.communication.events.viewer.ViewerEvent;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.entities.BaseWorldPoint;
import shipeditor.components.viewer.entities.BoundPoint;
import shipeditor.components.viewer.entities.WorldPoint;
import shipeditor.components.viewer.entities.bays.LaunchBay;
import shipeditor.components.viewer.entities.engine.EnginePoint;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.LayerPainter;

public class PointEvents {
    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record BoundInsertedConfirmed(BoundPoint toInsert, int precedingIndex) implements PointEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record PointCreationQueued(Point2D position) implements PointEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record EngineInsertedConfirmed(EnginePoint toInsert, int precedingIndex) implements PointEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record PointAddConfirmed(WorldPoint point) implements PointEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record LaunchBayRemoveConfirmed(LaunchBay removed) implements PointEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record WeaponSlotInsertedConfirmed(WeaponSlotPoint toInsert, int precedingIndex) implements PointEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record PointSelectQueued(WorldPoint point) implements PointEvent {
    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record PointRemovedConfirmed(WorldPoint point) implements PointEvent {

    }

    public static record InstrumentModeChanged(EditorInstrument newMode) implements PointEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record LaunchBayAddConfirmed(LaunchBay added, int index) implements PointEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
    public static record PointRemoveQueued(BaseWorldPoint point, boolean fromList) implements PointEvent {
    }

    public static interface PointEvent extends ViewerEvent {

    }

    public static record SlotPointsSorted(List<WeaponSlotPoint> rearranged) implements PointEvent {

}


    public static record PointDragQueued(AffineTransform screenToWorld, Point2D target) implements PointEvent {

}


    public static record EnginePointsSorted(List<EnginePoint> rearranged) implements PointEvent {

}


    public static record BoundPointsSorted(List<BoundPoint> rearranged) implements PointEvent {

}


    public static record PointSelectedConfirmed(WorldPoint point) implements PointEvent {

}


    public static record AnchorOffsetQueued(LayerPainter layer, Point2D difference) implements PointEvent {
}

}
