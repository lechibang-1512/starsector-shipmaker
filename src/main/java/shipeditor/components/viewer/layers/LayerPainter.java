package shipeditor.components.viewer.layers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.utility.graphics.opengl.OpenGLPainter;
import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.BusEventListener;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.control.ControlEvents.LayerAnchorDragged;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerRotationQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.ViewerLayerRemovalConfirmed;
import shipeditor.communication.events.viewer.points.PointEvents.AnchorOffsetQueued;
import shipeditor.components.viewer.control.ControlPredicates;
import shipeditor.components.viewer.entities.BaseWorldPoint;
import shipeditor.components.viewer.painters.points.AbstractPointPainter;
import shipeditor.undo.EditDispatch;
import shipeditor.undo.UndoOverseer;
import shipeditor.utility.Utility;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.overseers.StaticController;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
@SuppressWarnings("ClassWithTooManyMethods")
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public abstract class LayerPainter implements OpenGLPainter {

    private final List<AbstractPointPainter> allPainters;

    private Point2D anchor = new Point2D.Double(0, 0);

    private float spriteOpacity = 1.0f;

    private double rotationRadians;

    public void setRotationRadians(double newRotation) {
        double delta = newRotation - this.rotationRadians;

        if (delta != 0.0 && this.allPainters != null && !this.allPainters.isEmpty()) {
            Point2D rotationAnchor = this.getRotationAnchor();
            
            AffineTransform rotationTransform = AffineTransform.getRotateInstance(delta, rotationAnchor.getX(), rotationAnchor.getY());

            for (AbstractPointPainter pointPainter : this.allPainters) {
                for (Object pointObj : pointPainter.getPointsIndex()) {
                    if (pointObj instanceof shipeditor.components.viewer.entities.BaseWorldPoint point) {
                        Point2D oldPos = point.getPosition();
                        Point2D newPos = rotationTransform.transform(oldPos, null);
                        point.setPosition(newPos.getX(), newPos.getY());
                    }
                    if (pointObj instanceof shipeditor.components.viewer.entities.AngledPoint angledPoint) {
                        angledPoint.setAngle(angledPoint.getAngle() - Math.toDegrees(delta));
                    }
                }

                if (pointPainter instanceof shipeditor.components.viewer.painters.points.ship.LaunchBayPainter bayPainter) {
                    for (shipeditor.components.viewer.entities.bays.LaunchBay bay : bayPainter.getBaysList()) {
                        bay.setAngle(bay.getAngle() - Math.toDegrees(delta));
                    }
                }
            }
        }
        
        this.rotationRadians = newRotation;
    }

    private final ViewerLayer parentLayer;

    @Setter
    private Sprite sprite;

    @Setter(AccessLevel.PROTECTED)
    private boolean uninitialized = true;

    @Setter
    private boolean shouldDrawPainter = true;

    private final Rectangle2D.Double spriteRectCache = new Rectangle2D.Double();
    
    private final Point2D.Double unrotatedCache = new Point2D.Double();

    private final org.joml.Vector2f paintPosition = new org.joml.Vector2f();
    private final org.joml.Vector2f paintSize = new org.joml.Vector2f();
    private final org.joml.Vector2f paintRotAnchor = new org.joml.Vector2f();
    private final org.joml.Vector4f paintColor = new org.joml.Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    protected LayerPainter(ViewerLayer layer) {
        this.parentLayer = layer;
        this.allPainters = new ArrayList<>();
        this.initLayerListeners();
    }

    public Dimension getSpriteSize() {
        Sprite spriteContainer = getSprite();
        var spriteImage = spriteContainer.getImage();
        return new Dimension(spriteImage.getWidth(), spriteImage.getHeight());
    }

    public BufferedImage getSpriteImage() {
        Sprite currentSprite = getSprite();
        return currentSprite != null ? currentSprite.getImage() : null;
    }

    private void initLayerListeners() {
        BusEventListener removalListener = event -> {
            if (event instanceof ViewerLayerRemovalConfirmed checked) {
                if (checked.removed() != this.getParentLayer())
                    return;
                this.cleanupForRemoval();
            }
        };
        EventBus.subscribe(this, removalListener);
        BusEventListener anchorDragListener = event -> {
            if (event instanceof LayerAnchorDragged checked && checked.selected() == this) {
                AffineTransform screenToWorld = checked.screenToWorld();
                Point2D difference = checked.difference();
                Point2D wP = screenToWorld.transform(difference, null);
                double roundedX = Math.round(wP.getX() * 2) / 2.0;
                double roundedY = Math.round(wP.getY() * 2) / 2.0;
                Point2D corrected = new Point2D.Double(roundedX, roundedY);
                updateAnchorOffset(corrected);
            }
        };
        EventBus.subscribe(this, anchorDragListener);

        BusEventListener rotationListener = event -> {
            if (event instanceof LayerRotationQueued checked) {
                if (checked.layer() != this)
                    return;
                this.rotateToTarget(checked.worldTarget());
            }
        };
        EventBus.subscribe(this, rotationListener);
    }

    public abstract Point2D getEntityCenter();

    public void setAnchor(Point2D inputAnchor) {
        Point2D oldAnchor = this.getAnchor();

        Point2D difference = new Point2D.Double(oldAnchor.getX() - inputAnchor.getX(),
                oldAnchor.getY() - inputAnchor.getY());
        EventBus.publish(new AnchorOffsetQueued(this, difference));

        this.anchor = inputAnchor;
    }

    /**
     * Is expected to contain a setSprite() call as well.
     * 
     * @param updated new sprite to be used.
     */
    public void reconfigureSpriteCircumstance(Sprite updated) {
        BufferedImage previous = this.getSpriteImage();
        var spriteHeight = previous.getHeight();

        BufferedImage updatedImage = updated.getImage();
        var updatedHeight = updatedImage.getHeight();

        int heightDifference = spriteHeight - updatedHeight;

        var painterList = this.getAllPainters();
        painterList.forEach(abstractPointPainter -> {
            List<? extends BaseWorldPoint> pointsIndex = abstractPointPainter.getPointsIndex();
            pointsIndex.forEach((Consumer<BaseWorldPoint>) baseWorldPoint -> {
                Point2D oldPosition = baseWorldPoint.getPosition();
                baseWorldPoint.setPosition(oldPosition.getX(), oldPosition.getY() - heightDifference);
            });
        });

        this.setSprite(updated);
    }

    public boolean isLayerActive() {
        ViewerLayer layer = this.getParentLayer();
        if (layer == null)
            return false;
        return StaticController.getActiveLayer() == layer;
    }

    public void cleanupForRemoval() {
        cleanupPointPainters();
        EventBus.unsubscribeByParent(this);
        UndoOverseer.cleanupRemovedLayer(this);
    }

    protected void cleanupPointPainters() {
        List<AbstractPointPainter> painters = this.getAllPainters();
        for (AbstractPointPainter pointPainter : painters) {
            pointPainter.cleanupPointPainter();
        }
        this.allPainters.clear();
    }

    public void setSpriteOpacity(float opacity) {
        if (opacity < 0.0f) {
            this.spriteOpacity = 0.0f;
        } else
            this.spriteOpacity = Math.min(opacity, 1.0f);
    }

    @Override
    public String toString() {
        Class<? extends LayerPainter> identity = this.getClass();
        return identity.getSimpleName() + " #" + this.hashCode();
    }

    private void rotateToTarget(Point2D worldTarget) {
        Point2D center = getRotationAnchor();
        double deltaX = worldTarget.getX() - center.getX();
        double deltaY = worldTarget.getY() - center.getY();

        double radians = -Math.atan2(deltaX, deltaY);

        double rotationDegrees = Math.toDegrees(radians) + 180;
        double result = rotationDegrees;
        if (ControlPredicates.isRotationRoundingEnabled()) {
            result = Math.round(rotationDegrees);
        }
        this.rotateLayer(result);
    }

    @SuppressWarnings("WeakerAccess")
    protected Point2D getRotationAnchor() {
        return this.getSpriteCenter();
    }

    public Point2D getCenterAnchorDifference() {
        Point2D layerAnchor = getAnchor();
        Point2D rotationAnchor = this.getRotationAnchor();
        double x = rotationAnchor.getX() - layerAnchor.getX();
        double y = rotationAnchor.getY() - layerAnchor.getY();
        return new Point2D.Double(x, y);
    }

    public void rotateLayer(double rotationDegrees) {
        EditDispatch.postLayerRotated(this, this.getRotationRadians(), Math.toRadians(rotationDegrees));
    }

    public AffineTransform getWithRotation(AffineTransform worldToScreen) {
        AffineTransform transform = new AffineTransform(worldToScreen);
        transform.concatenate(getRotationTransform());
        return transform;
    }

    public AffineTransform getWithRotation(AffineTransform worldToScreen, AffineTransform transformCache) {
        transformCache.setTransform(worldToScreen);
        transformCache.concatenate(getRotationTransform());
        return transformCache;
    }

    public AffineTransform getRotationTransform() {
        double rotation = this.getRotationRadians();
        Point2D center = this.getRotationAnchor();
        double centerX = center.getX();
        double centerY = center.getY();
        return AffineTransform.getRotateInstance(rotation, centerX, centerY);
    }

    public AffineTransform getWithRotationInverse(AffineTransform worldToScreen) {
        AffineTransform transform;
        AffineTransform worldToScreenCopy = new AffineTransform(worldToScreen);
        try {
            AffineTransform inverseRotation = getRotationTransform();
            worldToScreenCopy.concatenate(inverseRotation);
            transform = worldToScreenCopy.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException("Non-invertible rotation transform of layer!", e);
        }
        return transform;
    }

    public int getSpriteWidth() {
        Sprite spriteContainer = getSprite();
        var spriteImage = spriteContainer.getImage();
        return spriteImage.getWidth();
    }

    public int getSpriteHeight() {
        Sprite spriteContainer = getSprite();
        var spriteImage = spriteContainer.getImage();
        return spriteImage.getHeight();
    }

    /**
     * Note: if called programmatically outside of usual user input flow,
     * {@link shipeditor.undo.UndoOverseer} needs to finish all edits
     * programmatically as well,
     * for consistent undo/redo behaviour.
     * 
     * @param updated new position of the anchor offset.
     */
    public void updateAnchorOffset(Point2D updated) {
        EditDispatch.postAnchorOffsetChanged(this, updated);
    }

    /**
     * @return world position of the sprite center. Is dependent on anchor position.
     */
    public Point2D getSpriteCenter() {
        Point2D difference = this.getSpriteCenterDifferenceToAnchor();
        Point2D currentAnchor = this.getAnchor();
        return new Point2D.Double((currentAnchor.getX() + difference.getX()),
                (currentAnchor.getY() + difference.getY()));
    }

    public boolean isWorldCursorInsideSprite(Point2D worldCursor) {
        try {
            Point2D currentAnchor = this.getAnchor();
            int width = this.getSpriteWidth();
            int height = this.getSpriteHeight();
            spriteRectCache.setRect(currentAnchor.getX(), currentAnchor.getY(), width, height);

            double rotation = this.getRotationRadians();
            if (rotation == 0.0) {
                return spriteRectCache.contains(worldCursor);
            }

            AffineTransform inverseRotation = this.getRotationTransform().createInverse();
            Point2D unrotated = inverseRotation.transform(worldCursor, unrotatedCache);

            return spriteRectCache.contains(unrotated);
        } catch (NoninvertibleTransformException e) {
            return false;
        }
    }

    public Point2D getSpriteCenter(Point2D cachingTarget) {
        Point2D difference = this.getSpriteCenterDifferenceToAnchor();

        Point2D currentAnchor = this.getAnchor();
        double x = currentAnchor.getX() + difference.getX();
        double y = currentAnchor.getY() + difference.getY();
        cachingTarget.setLocation(x, y);

        return cachingTarget;
    }

    public Point2D getSpriteCenterDifferenceToAnchor() {
        Sprite spriteContainer = getSprite();
        var spriteImage = spriteContainer.getImage();
        return Utility.getSpriteCenterDifferenceToAnchor(spriteImage);
    }

    protected void paintContent(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        Sprite spriteContainer = getSprite();
        if (spriteContainer == null) return;
        int textureId = spriteContainer.getTextureId();
        if (textureId == 0) return;

        Point2D currentAnchor = this.getAnchor();
        paintPosition.set((float) currentAnchor.getX(), (float) currentAnchor.getY());
        paintSize.set((float) this.getSpriteWidth(), (float) this.getSpriteHeight());
        Point2D rotationAnchor2D = this.getRotationAnchor();
        paintRotAnchor.set((float) rotationAnchor2D.getX(), (float) rotationAnchor2D.getY());
        float rotation = (float) this.getRotationRadians();
        float opacity = this.getSpriteOpacity();
        paintColor.w = opacity;

        spriteRenderer.drawSprite(textureId, paintPosition, paintSize, paintRotAnchor, rotation, paintColor, projection, view);
    }

    @Override
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        if (!shouldDrawPainter)
            return;
        this.paintContent(spriteRenderer, shapeRenderer, projection, view);
    }

    public Rectangle2D getVisualBounds() {
        if (isUninitialized()) {
            return new Rectangle2D.Double();
        }
        Point2D currentAnchor = this.getAnchor();
        int width = this.getSpriteWidth();
        int height = this.getSpriteHeight();
        
        Point2D[] corners = new Point2D[] {
            new Point2D.Double(currentAnchor.getX(), currentAnchor.getY()),
            new Point2D.Double(currentAnchor.getX() + width, currentAnchor.getY()),
            new Point2D.Double(currentAnchor.getX(), currentAnchor.getY() + height),
            new Point2D.Double(currentAnchor.getX() + width, currentAnchor.getY() + height)
        };
        
        double rotation = this.getRotationRadians();
        if (rotation != 0.0) {
            AffineTransform transform = this.getRotationTransform();
            for (int i = 0; i < corners.length; i++) {
                transform.transform(corners[i], corners[i]);
            }
        }
        
        double minX = corners[0].getX();
        double minY = corners[0].getY();
        double maxX = corners[0].getX();
        double maxY = corners[0].getY();
        for (int i = 1; i < corners.length; i++) {
            minX = Math.min(minX, corners[i].getX());
            minY = Math.min(minY, corners[i].getY());
            maxX = Math.max(maxX, corners[i].getX());
            maxY = Math.max(maxY, corners[i].getY());
        }
        
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

}
