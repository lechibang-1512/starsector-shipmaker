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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

@Log4j2
public final class FramebufferUtilities {

    private FramebufferUtilities() {
    }

    public static void printLayerToImage(ViewerLayer layer, int width, int height, double scale, int padding, File outputFile) {
        printLayerToImage(layer, width, height, scale, padding, outputFile, false, new java.awt.Color(0,0,0,0), false);
    }

    public static void printLayerToImage(ViewerLayer layer, int width, int height, double scale, int padding, File outputFile, boolean bakeCenterline, java.awt.Color bgColor, boolean renderMountsBounds) {
        BufferedImage image = renderLayerToImage(layer, width, height, scale, padding, bakeCenterline, bgColor, renderMountsBounds);
        if (image == null) return;
        
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
    
    public static BufferedImage renderLayerToImage(ViewerLayer layer, int width, int height, double scale, int padding, boolean bakeCenterline, java.awt.Color bgColor, boolean renderMountsBounds) {
        LayerPainter painter = layer.getPainter();
        if (painter == null || painter.isUninitialized()) {
            log.error("Layer painter is uninitialized, aborting render.");
            return null;
        }

        int paddedWidth = width + (padding * 2);
        int paddedHeight = height + (padding * 2);
        int finalWidth = (int) Math.round(paddedWidth * scale);
        int finalHeight = (int) Math.round(paddedHeight * scale);

        // Setup FBO
        int fbo = GL30.glGenFramebuffers();
        int texture = GL11.glGenTextures();

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, finalWidth, finalHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);

        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            log.error("FBO setup failed!");
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glDeleteTextures(texture);
            GL30.glDeleteFramebuffers(fbo);
            return null;
        }

        // Render to FBO
        GL11.glViewport(0, 0, finalWidth, finalHeight);
        GL11.glClearColor(bgColor.getRed() / 255.0f, bgColor.getGreen() / 255.0f, bgColor.getBlue() / 255.0f, bgColor.getAlpha() / 255.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        Matrix4f projection = new Matrix4f().ortho(0.0f, finalWidth, finalHeight, 0.0f, -1.0f, 1.0f);
        Matrix4f view = new Matrix4f();

        java.awt.geom.Rectangle2D bounds = painter.getVisualBounds();
        double boundsCenterX = bounds.getX() + bounds.getWidth() / 2.0;
        double boundsCenterY = bounds.getY() + bounds.getHeight() / 2.0;

        double dx = (finalWidth / 2.0f) - (boundsCenterX * scale);
        double dy = (finalHeight / 2.0f) - (boundsCenterY * scale);

        view.translate((float) dx, (float) dy, 0.0f);
        view.scale((float) scale, (float) scale, 1.0f);

        SpriteRenderer spriteRenderer = StaticController.getViewer().getSpriteRenderer();
        ShapeRenderer shapeRenderer = StaticController.getViewer().getShapeRenderer();

        GL11.glEnable(GL11.GL_BLEND);
        org.lwjgl.opengl.GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        if (renderMountsBounds) {
            PaintOrderController.paintLayer(spriteRenderer, shapeRenderer, projection, view, layer);
        } else {
            PaintOrderController.paintLayerGraphicsOnly(spriteRenderer, shapeRenderer, projection, view, layer);
        }

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Read pixels
        ByteBuffer buffer = BufferUtils.createByteBuffer(finalWidth * finalHeight * 4);
        GL11.glReadPixels(0, 0, finalWidth, finalHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Cleanup FBO
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glDeleteTextures(texture);
        GL30.glDeleteFramebuffers(fbo);

        // Restore viewport
        GL11.glViewport(0, 0, StaticController.getViewer().getWidth(), StaticController.getViewer().getHeight());

        // Process pixels to BufferedImage
        BufferedImage image = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[finalWidth * finalHeight];

        for (int y = 0; y < finalHeight; y++) {
            for (int x = 0; x < finalWidth; x++) {
                int i = (x + (finalWidth * y)) * 4;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                int a = buffer.get(i + 3) & 0xFF;
                if (a > 0 && a < 255) {
                    float alphaFactor = 255.0f / a;
                    r = Math.min(255, (int)(r * alphaFactor));
                    g = Math.min(255, (int)(g * alphaFactor));
                    b = Math.min(255, (int)(b * alphaFactor));
                }
                // Flip vertically since OpenGL reads bottom-to-top
                pixels[(finalHeight - y - 1) * finalWidth + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        image.setRGB(0, 0, finalWidth, finalHeight, pixels, 0, finalWidth);

        if (bakeCenterline) {
            java.awt.Graphics2D g2d = image.createGraphics();
            g2d.setColor(new java.awt.Color(0, 255, 255, 128));
            int centerX = finalWidth / 2;
            int centerY = finalHeight / 2;
            g2d.drawLine(centerX, 0, centerX, finalHeight);
            g2d.drawLine(0, centerY, finalWidth, centerY);
            g2d.dispose();
        }
        
        return image;
    }

    public static void printViewerToImage(int width, int height, File outputFile) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

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
            log.info("Viewer successfully printed to {}", outputFile);
        } catch (IOException e) {
            log.error("Failed to write image to disk", e);
        }
    }
}
