package oth.shipeditor.utility.overseers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public final class MiscCaching {

    private static final AffineTransform LAYER_ROTATION_TRANSFORM_CACHED = new AffineTransform();

    private static final Point2D CALCULATION_POINT_CACHED = new Point2D.Double();

    private MiscCaching() {
    }

    public static AffineTransform getLayerRotationTransform() {
        LAYER_ROTATION_TRANSFORM_CACHED.setToIdentity();
        return LAYER_ROTATION_TRANSFORM_CACHED;
    }

    public static Point2D getNewPoint() {
        CALCULATION_POINT_CACHED.setLocation(0, 0);
        return CALCULATION_POINT_CACHED;
    }

}
