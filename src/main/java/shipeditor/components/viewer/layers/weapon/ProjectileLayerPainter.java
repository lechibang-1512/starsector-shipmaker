package shipeditor.components.viewer.layers.weapon;

import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.painters.points.weapon.ProjectilePainter;
import shipeditor.representation.weapon.ProjectileSpecFile;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.objects.Size2D;

import java.awt.geom.Point2D;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class ProjectileLayerPainter extends LayerPainter {

    private final ProjectilePainter corePainter;

    public ProjectileLayerPainter(ViewerLayer layer, Sprite sprite, ProjectileSpecFile specFile) {
        super(layer);
        this.setSprite(sprite);
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
    protected void paintContent(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        corePainter.setPaintAnchor(this.getAnchor());
        corePainter.setRotationRadians(this.getRotationRadians());
        corePainter.setSpriteOpacity(this.getSpriteOpacity());
        corePainter.paint(spriteRenderer, shapeRenderer, projection, view);
    }

    @Override
    protected Point2D getRotationAnchor() {
        return this.getAnchor();
    }

}
