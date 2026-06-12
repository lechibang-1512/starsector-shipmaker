package shipeditor.components.viewer.layers.weapon;
import shipeditor.components.viewer.ViewerEnums.WeaponRenderOrdering;


import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import shipeditor.components.viewer.entities.weapon.OffsetPoint;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.painters.points.weapon.ProjectilePainter;
import shipeditor.components.viewer.painters.points.weapon.WeaponOffsetPainter;
import shipeditor.representation.weapon.WeaponEnums.WeaponMount;
import shipeditor.representation.weapon.WeaponEnums.WeaponRenderHints;
import shipeditor.utility.graphics.Sprite;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;

/** * Sprite field of superclass (which is an image layer painter is initialized with)
 * is assumed to be turret-version of main weapon image.*/
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WeaponPainter extends LayerPainter {

    @Getter @Setter
    private WeaponMount mount = WeaponMount.TURRET;

    @Getter @Setter
    private WeaponSprites weaponSprites;

    @Getter @Setter
    private String weaponID;

    private final WeaponOffsetPainter turretOffsetPainter;

    private final WeaponOffsetPainter hardpointOffsetPainter;

    private final WeaponOffsetPainter hiddenOffsetPainter;

    @Getter @Setter
    private List<WeaponRenderHints> renderHints;

    @Getter @Setter
    private WeaponRenderOrdering renderOrderType;

    /**
     * Stamp-pattern: single instance is mutated and painted for each offset point.
     */
    @Getter @Setter
    private ProjectilePainter projectilePainter;

    private final org.joml.Vector2f paintPosition = new org.joml.Vector2f();
    private final org.joml.Vector2f paintSize = new org.joml.Vector2f();
    private final org.joml.Vector2f paintRotAnchor = new org.joml.Vector2f();
    private final org.joml.Vector4f paintColor = new org.joml.Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public WeaponPainter(ViewerLayer layer) {
        super(layer);
        this.weaponSprites = new WeaponSprites();


        this.turretOffsetPainter = new WeaponOffsetPainter(this, WeaponMount.TURRET);
        this.hardpointOffsetPainter = new WeaponOffsetPainter(this, WeaponMount.HARDPOINT);
        this.hiddenOffsetPainter = new WeaponOffsetPainter(this, WeaponMount.HIDDEN);
        var allPainters = getAllPainters();
        allPainters.add(turretOffsetPainter);
        allPainters.add(hardpointOffsetPainter);
        allPainters.add(hiddenOffsetPainter);

        this.setUninitialized(false);
    }

    public WeaponOffsetPainter getOffsetPainter() {
        if (mount == WeaponMount.HARDPOINT) {
            return hardpointOffsetPainter;
        } else if (mount == WeaponMount.HIDDEN) {
            return hiddenOffsetPainter;
        } else {
            return turretOffsetPainter;
        }
    }

    @Override
    public Point2D getEntityCenter() {
        return this.getRotationAnchor();
    }

    @Override
    public Sprite getSprite() {
        return weaponSprites.getMainSprite(mount);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean hasHint(WeaponRenderHints hint) {
        if (renderHints == null || renderHints.isEmpty()) return false;
        return renderHints.contains(hint);
    }

    @Override
    protected void paintContent(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        if (mount != WeaponMount.HIDDEN) {
            boolean isAdditive = hasHint(WeaponRenderHints.RENDER_ADDITIVE);

            this.drawSpritePartGL(spriteRenderer, projection, view, weaponSprites.getUnderSprite(mount), false);

            if (hasHint(WeaponRenderHints.RENDER_BARREL_BELOW)) {
                this.drawSpritePartGL(spriteRenderer, projection, view, weaponSprites.getGunSprite(mount), 0.0, isAdditive);
                this.drawSpritePartGL(spriteRenderer, projection, view, weaponSprites.getMainSprite(mount), isAdditive);
            } else {
                this.drawSpritePartGL(spriteRenderer, projection, view, weaponSprites.getMainSprite(mount), isAdditive);
                this.drawSpritePartGL(spriteRenderer, projection, view, weaponSprites.getGunSprite(mount), 0.0, isAdditive);
            }

            this.paintLoadedMissilesGL(spriteRenderer, shapeRenderer, projection, view);
        }
    }

    private void paintLoadedMissilesGL(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        boolean renderLoadedMissiles = hasHint(WeaponRenderHints.RENDER_LOADED_MISSILES);
        boolean renderMissilesNotHidden = mount != WeaponMount.HIDDEN
                && hasHint(WeaponRenderHints.RENDER_LOADED_MISSILES_UNLESS_HIDDEN);
        boolean render = renderLoadedMissiles || renderMissilesNotHidden;

        if (!render || projectilePainter == null) return;
        var offsetPainter = this.getOffsetPainter();
        var offsets = offsetPainter.getOffsetPoints();
        if (offsets.isEmpty()) return;

        for (int i = 0; i < offsets.size(); i++) {
            OffsetPoint offsetPoint = offsets.get(i);
            projectilePainter.setPaintAnchor(offsetPoint.getPosition());
            projectilePainter.setRotationRadians(Math.toRadians(-offsetPoint.getAngle()));
            projectilePainter.setSpriteOpacity(this.getSpriteOpacity());
            projectilePainter.paint(spriteRenderer, shapeRenderer, projection, view);
        }
    }

    private void drawSpritePartGL(SpriteRenderer spriteRenderer, Matrix4f projection, Matrix4f view, Sprite part, boolean additive) {
        drawSpritePartGL(spriteRenderer, projection, view, part, 0.0, additive);
    }

    private void drawSpritePartGL(SpriteRenderer spriteRenderer, Matrix4f projection, Matrix4f view, Sprite part, double recoilOffset, boolean additive) {
        if (part == null) return;
        int textureId = part.getTextureId();
        if (textureId == 0) return;

        Point2D rotationAnchor2D = this.getRotationAnchor();
        BufferedImage spriteImage = part.getImage();

        Point2D center = WeaponSprites.getSpriteCenterDifference(spriteImage, this.getMount());
        double positionX = rotationAnchor2D.getX() - center.getX();
        double positionY = rotationAnchor2D.getY() - center.getY();

        double rotRads = this.getRotationRadians();
        double offsetX = -Math.cos(rotRads) * recoilOffset;
        double offsetY = -Math.sin(rotRads) * recoilOffset;

        double finalPosX = positionX + offsetX;
        double finalPosY = positionY + offsetY;
        double finalAnchorX = rotationAnchor2D.getX() + offsetX;
        double finalAnchorY = rotationAnchor2D.getY() + offsetY;

        paintPosition.set((float) finalPosX, (float) finalPosY);
        paintSize.set(spriteImage.getWidth(), spriteImage.getHeight());
        paintRotAnchor.set((float) finalAnchorX, (float) finalAnchorY);
        float rotation = (float) rotRads;
        float opacity = this.getSpriteOpacity();
        paintColor.w = opacity;

        if (additive) {
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE);
        }
        spriteRenderer.drawSprite(textureId, paintPosition, paintSize, paintRotAnchor, rotation, paintColor, projection, view);
        if (additive) {
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    @Override
    protected Point2D getRotationAnchor() {
        Point2D anchor = this.getAnchor();
        Point2D weaponCenter = weaponSprites.getWeaponCenter(mount);
        return new Point2D.Double(anchor.getX() + weaponCenter.getX(), anchor.getY() + weaponCenter.getY());
    }

}
