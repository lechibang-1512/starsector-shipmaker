package shipeditor.parsing.loading;

import shipeditor.utility.text.StringManager;

import lombok.extern.log4j.Log4j2;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.overseers.ImageCache;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

@Log4j2
public final class SpriteLoader {

    private SpriteLoader() {
    }

    @SuppressWarnings("NestedTryStatement")
    public static BufferedImage loadImageResource(String imageFilename) {
        if (imageFilename == null) {
            return null;
        }
        Class<SpriteLoader> loadingClass = SpriteLoader.class;
        ClassLoader classLoader = loadingClass.getClassLoader();

        URL spritePath = classLoader != null ? classLoader.getResource(imageFilename) : loadingClass.getResource("/" + imageFilename);
        if (spritePath == null) {
            log.warn("Sprite resource not found: {}", imageFilename);
            return null;
        }
        File spriteFile;
        try {
            URI pathURI = spritePath.toURI();
            if (pathURI.isOpaque()) {
                try (InputStream inputStream = loadingClass.getResourceAsStream("/" + imageFilename)) {
                    if (inputStream != null) {
                        return ImageIO.read(inputStream);
                    } else {
                        log.warn(StringManager.getString("RESOURCE_NOT_FOUND") + ": " + imageFilename);
                        return null;
                    }
                } catch (IOException e) {
                    log.error("Failed to read sprite input stream: {}", imageFilename, e);
                    return null;
                }
            }
            spriteFile = new File(pathURI);
        } catch (URISyntaxException e) {
            String errorMsg = StringManager.getString("IMAGE_RESOURCE_LOAD_FAILED").replace("{}", String.valueOf(spritePath));
            log.error(errorMsg, e);
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        errorMsg,
                        StringManager.getString("FILE_LOADING_ERROR"),
                        JOptionPane.ERROR_MESSAGE);
            }
            return null;
        }
        return loadSpriteAsImage(spriteFile);
    }

    public static BufferedImage loadSpriteAsImage(File file) {
        return ImageCache.loadImage(file);
    }

    public static Sprite loadSprite(File file) {
        if (file == null) {
            return null;
        }
        BufferedImage spriteImage = loadSpriteAsImage(file);
        if (spriteImage == null) return null;
        String name = file.getName();
        Path path = file.toPath();
        return new Sprite(spriteImage, path, name);
    }
}
