package shipeditor.utility.graphics;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import shipeditor.utility.Utility;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

/** * Convenience container for BufferedImage sprites, allows additional information such as path and filename.
 **/
@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public final class Sprite {

    private final BufferedImage image;

    private final Path path;

    private final String filename;

    private String pathFromPackage;

    private int textureId = 0;

    public Sprite(BufferedImage bufferedImage, Path inputPath, String shortFilename) {
        this.image = bufferedImage;
        this.path = inputPath;
        this.filename = shortFilename;
    }

    public int getTextureId() {
        if (textureId == 0 && image != null) {
            textureId = shipeditor.utility.graphics.opengl.TextureLoader.loadTexture(image);
        }
        return textureId;
    }

    public void cleanup() {
        if (textureId != 0) {
            org.lwjgl.opengl.GL11.glDeleteTextures(textureId);
            textureId = 0;
        }
    }

    public String getPathFromPackage() {
        if (pathFromPackage == null || pathFromPackage.isEmpty()) {
            pathFromPackage = Utility.computeRelativePathFromPackage(path);
        }
        return pathFromPackage;
    }

}
