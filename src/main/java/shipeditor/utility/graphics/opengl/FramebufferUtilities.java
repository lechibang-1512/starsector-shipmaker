package shipeditor.utility.graphics.opengl;

import lombok.extern.log4j.Log4j2;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import shipeditor.components.viewer.PaintOrderController;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.utility.overseers.StaticController;

import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

@Log4j2
public final class FramebufferUtilities {

    private FramebufferUtilities() {
    }

    public static void printLayerToImage(ViewerLayer layer, int width, int height, File outputFile) {
        LayerPainter painter = layer.getPainter();
        if (painter == null || painter.isUninitialized()) {
            log.error("Layer painter is uninitialized, aborting print.");
            return;
        }

        // Setup FBO
        int fbo = GL30.glGenFramebuffers();
        int texture = GL11.glGenTextures();

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);

        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            log.error("FBO setup failed!");
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glDeleteTextures(texture);
            GL30.glDeleteFramebuffers(fbo);
            return;
        }

        // Render to FBO
        GL11.glViewport(0, 0, width, height);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // Transparent background
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        Matrix4f projection = new Matrix4f().ortho(0.0f, width, height, 0.0f, -1.0f, 1.0f);
        Matrix4f view = new Matrix4f();

        Point2D spriteCenter = painter.getSpriteCenter();
        double dx = (width / 2.0f) - spriteCenter.getX();
        double dy = (height / 2.0f) - spriteCenter.getY();

        view.translate((float) dx, (float) dy, 0.0f);

        SpriteRenderer spriteRenderer = StaticController.getViewer().getSpriteRenderer();
        ShapeRenderer shapeRenderer = StaticController.getViewer().getShapeRenderer();

        PaintOrderController.paintLayer(spriteRenderer, shapeRenderer, projection, view, layer);

        // Read pixels
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Cleanup FBO
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glDeleteTextures(texture);
        GL30.glDeleteFramebuffers(fbo);

        // Restore viewport
        GL11.glViewport(0, 0, StaticController.getViewer().getWidth(), StaticController.getViewer().getHeight());

        // Process pixels to BufferedImage
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (x + (width * y)) * 4;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                int a = buffer.get(i + 3) & 0xFF;
                // Flip vertically since OpenGL reads bottom-to-top
                pixels[(height - y - 1) * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);

        try {
            String extension = "png";
            String name = outputFile.getName();
            int i = name.lastIndexOf('.');
            if (i > 0) {
                extension = name.substring(i+1);
            }
            ImageIO.write(image, extension, outputFile);
            log.info("Layer successfully printed to {}", outputFile);
        } catch (IOException e) {
            log.error("Failed to write image to disk", e);
        }
    }
}
