package shipeditor.components.viewer.painters.points.ship;

import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import shipeditor.components.viewer.entities.BaseWorldPoint;
import shipeditor.components.viewer.entities.WorldPoint;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.painters.points.AbstractPointPainter;


@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public abstract class SinglePointPainter extends AbstractPointPainter {

    private final ShipPainter parentLayer;

    SinglePointPainter(ShipPainter parent) {
        this.parentLayer = parent;
    }

    @Override
    protected boolean isParentLayerActive() {
        return this.parentLayer.isLayerActive();
    }

    @SuppressWarnings("NoopMethodInAbstractClass")
    @Override
    protected void handlePointRemovalEvent(BaseWorldPoint point, boolean removalViaListPanel) {
        // Not relevant for center points.
    }

    @Override
    public boolean isMirrorable() {
        return false;
    }

    /**
     * Conceptually irrelevant for center points.
     * @return null.
     */
    @Override
    public BaseWorldPoint getMirroredCounterpart(WorldPoint inputPoint) {
        throw new UnsupportedOperationException("Mirrored operations unsupported by SinglePointPainters!");
    }

    @Override
    protected void selectPointConditionally() {
        this.selectPointClosest();
    }

    @SuppressWarnings("NoopMethodInAbstractClass")
    protected void paintPainterContent(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {}

    @Override
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        if (!checkVisibility()) return;

        shapeRenderer.begin(projection, new Matrix4f());
        this.paintPainterContent(spriteRenderer, shapeRenderer, projection, view);
        this.paintDelegates(spriteRenderer, shapeRenderer, projection, view);
        shapeRenderer.end();
    }

}
