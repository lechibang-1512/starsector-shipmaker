package oth.shipeditor.components.viewer.layers.weapon;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.components.viewer.layers.ViewerLayer;
import oth.shipeditor.components.viewer.painters.points.weapon.ProjectilePainter;
import oth.shipeditor.representation.weapon.ProjectileSpecFile;
import oth.shipeditor.utility.graphics.Sprite;
import oth.shipeditor.utility.objects.Size2D;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class ProjectileLayerPainter extends LayerPainter {

    private final ProjectilePainter corePainter;
    private final Sprite sprite;

    public ProjectileLayerPainter(ViewerLayer layer, Sprite sprite, ProjectileSpecFile specFile) {
        super(layer);
        this.sprite = sprite;
        double width = specFile.getSize()[0];
        double height = specFile.getSize()[1];
        Point2D center = specFile.getCenter();
        this.corePainter = new ProjectilePainter(sprite, center, new Size2D(width, height));
        this.setUninitialized(false);
    }

    @Override
    public Point2D getEntityCenter() {
        return this.getRotationAnchor();
    }

    @Override
    public Sprite getSprite() {
        return this.sprite;
    }

    @Override
    protected void paintContent(Graphics2D g, AffineTransform worldToScreen, double w, double h) {
        corePainter.setPaintAnchor(this.getAnchor());
        corePainter.setRotationRadians(0); // Projectile layers don't have rotation natively yet, maybe add later
        corePainter.setSpriteOpacity(this.getSpriteOpacity());
        corePainter.paint(g, worldToScreen, w, h);
    }

    @Override
    protected Point2D getRotationAnchor() {
        return this.getAnchor();
    }

}
