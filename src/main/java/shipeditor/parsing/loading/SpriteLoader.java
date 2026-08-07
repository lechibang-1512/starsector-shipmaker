package shipeditor.parsing.loading;

import lombok.extern.log4j.Log4j2;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.overseers.ImageCache;
import shipeditor.utility.text.StringValues;

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
import java.util.Objects;

@Log4j2
public final class SpriteLoader {

    private SpriteLoader() {
    }

    @SuppressWarnings("NestedTryStatement")
    public static BufferedImage loadImageResource(String imageFilename) {
        Class<SpriteLoader> loadingClass = SpriteLoader.class;
        ClassLoader classLoader = loadingClass.getClassLoader();

        URL spritePath = Objects.requireNonNull(classLoader.getResource(imageFilename));
        File spriteFile;
        try {
            URI pathURI = spritePath.toURI();
            if (pathURI.isOpaque()) {
                try (InputStream inputStream = loadingClass.getResourceAsStream("/" + imageFilename)) {
                    if (inputStream != null) {
                        return ImageIO.read(inputStream);
                    } else {
                        throw new RuntimeException(StringValues.RESOURCE_NOT_FOUND);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            spriteFile = new File(pathURI);
        } catch (URISyntaxException e) {
            String errorMsg = StringValues.IMAGE_RESOURCE_LOAD_FAILED.replace("{}", String.valueOf(spritePath));
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        errorMsg,
                        StringValues.FILE_LOADING_ERROR,
                        JOptionPane.ERROR_MESSAGE);
            } else {
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.error(errorMsg, e);
                } else {
                    log.error(errorMsg);
                }
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
