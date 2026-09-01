package shipeditor.utility.overseers;

import shipeditor.utility.text.StringManager;

import lombok.extern.log4j.Log4j2;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public final class ImageCache {

    private static final ImageCache INSTANCE = new ImageCache();

    private final Map<File, SoftReference<BufferedImage>> cache;

    private ImageCache() {
        cache = new ConcurrentHashMap<>();
    }

    public static BufferedImage loadImage(File file) {
        if (file == null) {
            return null;
        }
        SoftReference<BufferedImage> ref = INSTANCE.cache.get(file);
        if (ref != null) {
            BufferedImage sprite = ref.get();
            if (sprite != null) {
                return sprite;
            }
            // SoftReference was cleared by GC, will re-load below.
            log.trace("Sprite was garbage-collected, reloading: {}.", file.getName());
        }
        BufferedImage sprite;
        try {
            sprite = ImageIO.read(file);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    StringManager.getString("IMAGE_FILE_LOADING_FAILED_MSG") + file,
                    StringManager.getString("FILE_LOADING_ERROR"),
                    JOptionPane.ERROR_MESSAGE);
            throw new UncheckedIOException("Failed to load sprite: " + file.getName(), ex);
        }
        log.trace("Opening sprite: {}.", file.getName());
        INSTANCE.cache.put(file, new SoftReference<>(sprite));
        return sprite;
    }

    public static void clearCache() {
        INSTANCE.cache.clear();
    }

}
