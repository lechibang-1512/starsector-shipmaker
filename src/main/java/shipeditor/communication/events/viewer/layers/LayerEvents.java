package shipeditor.communication.events.viewer.layers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.geom.Point2D;
import shipeditor.communication.events.BusEvent;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.weapon.ProjectileLayer;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.components.viewer.painters.points.AbstractPointPainter;
import shipeditor.utility.graphics.Sprite;

public class LayerEvents {
    public static record ShipLayerCreationQueued() implements BusEvent {

    }

    public static record WeaponLayerCreationQueued() implements BusEvent {}

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    public static record ProjectileLayerCreated(ProjectileLayer newLayer) implements BusEvent {
    }

    public static record LastLayerSelectQueued() implements BusEvent {

    }

    /** * This one does not have a target layer argument; it assumes that target is always an active layer.*/
    public static record LayerOpacityChangeQueued(float changedValue) implements BusEvent {

    }

    public static record PainterOpacityChangeQueued(Class<? extends AbstractPointPainter> painterClass,
                                             float change) implements BusEvent {

    }

    public static record ActiveLayerRemovalQueued() implements BusEvent {

    }

    public static record LayerShipDataInitialized(ShipPainter source) implements BusEvent {

}


    public static record ShipLayerCreated(ShipLayer newLayer) implements BusEvent {

}


    public static record WeaponLayerCreated(WeaponLayer newLayer) implements BusEvent {

}


    public static record LayerRemovalQueued(ViewerLayer layer) implements BusEvent {

}


    public static record LayerSpriteLoadConfirmed(ViewerLayer updated, Sprite sprite) implements BusEvent {

}


    public static record ActiveLayerUpdated(ViewerLayer updated) implements BusEvent {

}


    public static record LayerSpriteLoadQueued(ViewerLayer updated, Sprite sprite) implements BusEvent {

}


    public static record LayerRotationQueued(LayerPainter layer, Point2D worldTarget) implements BusEvent {

}


    public static record LayerWasSelected(ViewerLayer old, ViewerLayer selected) implements BusEvent {
}


    public static record ViewerLayerRemovalConfirmed(ViewerLayer removed) implements BusEvent {

}

}
