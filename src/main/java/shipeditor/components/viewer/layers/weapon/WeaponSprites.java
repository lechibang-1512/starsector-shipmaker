package shipeditor.components.viewer.layers.weapon;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import shipeditor.representation.weapon.WeaponEnums.WeaponMount;
import shipeditor.utility.graphics.Sprite;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WeaponSprites {

    private Sprite turretSprite;
    private Sprite turretUnderSprite;
    private Sprite turretGunSprite;
    private Sprite turretGlowSprite;

    private Sprite hardpointSprite;
    private Sprite hardpointUnderSprite;
    private Sprite hardpointGunSprite;
    private Sprite hardpointGlowSprite;

    private List<Sprite> turretSpriteFrames = new ArrayList<>();
    private List<Sprite> turretUnderSpriteFrames = new ArrayList<>();
    private List<Sprite> turretGunSpriteFrames = new ArrayList<>();
    private List<Sprite> turretGlowSpriteFrames = new ArrayList<>();

    private List<Sprite> hardpointSpriteFrames = new ArrayList<>();
    private List<Sprite> hardpointUnderSpriteFrames = new ArrayList<>();
    private List<Sprite> hardpointGunSpriteFrames = new ArrayList<>();
    private List<Sprite> hardpointGlowSpriteFrames = new ArrayList<>();

    private int currentFrame;

    public void setTurretSprite(Sprite sprite) {
        this.turretSprite = sprite;
        if (turretSpriteFrames.isEmpty() && sprite != null) {
            turretSpriteFrames.add(sprite);
        }
    }

    public void setTurretUnderSprite(Sprite sprite) {
        this.turretUnderSprite = sprite;
        if (turretUnderSpriteFrames.isEmpty() && sprite != null) {
            turretUnderSpriteFrames.add(sprite);
        }
    }

    public void setTurretGunSprite(Sprite sprite) {
        this.turretGunSprite = sprite;
        if (turretGunSpriteFrames.isEmpty() && sprite != null) {
            turretGunSpriteFrames.add(sprite);
        }
    }

    public void setTurretGlowSprite(Sprite sprite) {
        this.turretGlowSprite = sprite;
        if (turretGlowSpriteFrames.isEmpty() && sprite != null) {
            turretGlowSpriteFrames.add(sprite);
        }
    }

    public void setHardpointSprite(Sprite sprite) {
        this.hardpointSprite = sprite;
        if (hardpointSpriteFrames.isEmpty() && sprite != null) {
            hardpointSpriteFrames.add(sprite);
        }
    }

    public void setHardpointUnderSprite(Sprite sprite) {
        this.hardpointUnderSprite = sprite;
        if (hardpointUnderSpriteFrames.isEmpty() && sprite != null) {
            hardpointUnderSpriteFrames.add(sprite);
        }
    }

    public void setHardpointGunSprite(Sprite sprite) {
        this.hardpointGunSprite = sprite;
        if (hardpointGunSpriteFrames.isEmpty() && sprite != null) {
            hardpointGunSpriteFrames.add(sprite);
        }
    }

    public void setHardpointGlowSprite(Sprite sprite) {
        this.hardpointGlowSprite = sprite;
        if (hardpointGlowSpriteFrames.isEmpty() && sprite != null) {
            hardpointGlowSpriteFrames.add(sprite);
        }
    }

    public Sprite getTurretSprite() {
        if (!turretSpriteFrames.isEmpty()) {
            return turretSpriteFrames.get(Math.floorMod(currentFrame, turretSpriteFrames.size()));
        }
        return turretSprite;
    }

    public Sprite getTurretUnderSprite() {
        if (!turretUnderSpriteFrames.isEmpty()) {
            return turretUnderSpriteFrames.get(Math.floorMod(currentFrame, turretUnderSpriteFrames.size()));
        }
        return turretUnderSprite;
    }

    public Sprite getTurretGunSprite() {
        if (!turretGunSpriteFrames.isEmpty()) {
            return turretGunSpriteFrames.get(Math.floorMod(currentFrame, turretGunSpriteFrames.size()));
        }
        return turretGunSprite;
    }

    public Sprite getTurretGlowSprite() {
        if (!turretGlowSpriteFrames.isEmpty()) {
            return turretGlowSpriteFrames.get(Math.floorMod(currentFrame, turretGlowSpriteFrames.size()));
        }
        return turretGlowSprite;
    }

    public Sprite getHardpointSprite() {
        if (!hardpointSpriteFrames.isEmpty()) {
            return hardpointSpriteFrames.get(Math.floorMod(currentFrame, hardpointSpriteFrames.size()));
        }
        return hardpointSprite;
    }

    public Sprite getHardpointUnderSprite() {
        if (!hardpointUnderSpriteFrames.isEmpty()) {
            return hardpointUnderSpriteFrames.get(Math.floorMod(currentFrame, hardpointUnderSpriteFrames.size()));
        }
        return hardpointUnderSprite;
    }

    public Sprite getHardpointGunSprite() {
        if (!hardpointGunSpriteFrames.isEmpty()) {
            return hardpointGunSpriteFrames.get(Math.floorMod(currentFrame, hardpointGunSpriteFrames.size()));
        }
        return hardpointGunSprite;
    }

    public Sprite getHardpointGlowSprite() {
        if (!hardpointGlowSpriteFrames.isEmpty()) {
            return hardpointGlowSpriteFrames.get(Math.floorMod(currentFrame, hardpointGlowSpriteFrames.size()));
        }
        return hardpointGlowSprite;
    }

    public int getMaxFrames() {
        int max = 1;
        max = Math.max(max, turretSpriteFrames.size());
        max = Math.max(max, hardpointSpriteFrames.size());
        max = Math.max(max, turretGunSpriteFrames.size());
        max = Math.max(max, hardpointGunSpriteFrames.size());
        max = Math.max(max, turretUnderSpriteFrames.size());
        max = Math.max(max, hardpointUnderSpriteFrames.size());
        return max;
    }

    public void cycleNextFrame() {
        int max = getMaxFrames();
        if (max > 1) {
            this.currentFrame = (currentFrame + 1) % max;
        }
    }

    public Sprite getMainSprite(WeaponMount mount) {
        if (mount == WeaponMount.HARDPOINT) {
            return getHardpointSprite();
        } else {
            return getTurretSprite();
        }
    }

    public Sprite getUnderSprite(WeaponMount mount) {
        if (mount == WeaponMount.HARDPOINT) {
            return getHardpointUnderSprite();
        } else {
            return getTurretUnderSprite();
        }
    }

    public Sprite getGunSprite(WeaponMount mount) {
        if (mount == WeaponMount.HARDPOINT) {
            return getHardpointGunSprite();
        } else {
            return getTurretGunSprite();
        }
    }

    public Sprite getGlowSprite(WeaponMount mount) {
        if (mount == WeaponMount.HARDPOINT) {
            return getHardpointGlowSprite();
        } else {
            return getTurretGlowSprite();
        }
    }

    static Point2D getSpriteCenterDifference(RenderedImage sprite, WeaponMount mount) {
        final float centerRatio = 0.5f;
        // Starsector engine uses height/4 from bottom as hardpoint pivot (height * 0.75 in top-down space).
        float yRatio = (mount == WeaponMount.HARDPOINT) ? 0.75f : centerRatio;
        return new Point2D.Double(sprite.getWidth() * centerRatio, sprite.getHeight() * yRatio);
    }

    Point2D getWeaponCenter(WeaponMount mount) {
        switch (mount) {
            case HARDPOINT -> {
                BufferedImage spriteImage;
                Sprite hp = getHardpointSprite();
                Sprite tr = getTurretSprite();
                if (hp != null) {
                    spriteImage = hp.getImage();
                } else if (tr != null) {
                    spriteImage = tr.getImage();
                } else {
                    break;
                }
                return WeaponSprites.getSpriteCenterDifference(spriteImage, mount);
            }
            case TURRET, HIDDEN -> {
                BufferedImage spriteImage;
                Sprite tr = getTurretSprite();
                if (tr != null) {
                    spriteImage = tr.getImage();
                } else {
                    break;
                }
                return WeaponSprites.getSpriteCenterDifference(spriteImage, mount);
            }
        }
        return new Point2D.Double(0, 0);
    }

}
