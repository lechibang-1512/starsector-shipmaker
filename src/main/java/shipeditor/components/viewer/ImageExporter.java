package shipeditor.components.viewer;

import lombok.extern.log4j.Log4j2;
import org.lwjgl.opengl.GL11;
import shipeditor.PrimaryWindow;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.components.viewer.layers.ViewerLayer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import shipeditor.utility.graphics.opengl.ViewerTransform;

@Log4j2
public class ImageExporter {

    public static void exportCurrentLayerToPNG(PrimaryViewer viewer) {
        LayerManager layerManager = viewer.getLayerManager();
        ViewerLayer activeLayer = layerManager.getActiveLayer();
        if (activeLayer == null || activeLayer.getPainter() == null) {
            JOptionPane.showMessageDialog(PrimaryWindow.getInstance(), "No active layer to export.", "Export Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export as Transparent PNG");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        if (fileChooser.showSaveDialog(PrimaryWindow.getInstance()) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".png");
        }
        final File finalFile = fileToSave;

        viewer.queueGLTask(() -> {
            try {
                int width = viewer.getWidth();
                int height = viewer.getHeight();

                // Clear with transparent color
                GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

                // Render layer graphics only
                Matrix4f projection = new Matrix4f().setOrtho(0.0f, width, height, 0.0f, -1.0f, 1.0f);
                Matrix4f viewMatrix = ViewerTransform.convertToMatrix4f(viewer.getWorldToScreen());

                PaintOrderController.paintLayerGraphicsOnly(viewer.getSpriteRenderer(), viewer.getShapeRenderer(), projection, viewMatrix, activeLayer);

                // Read pixels
                GL11.glReadBuffer(GL11.GL_BACK);
                int bpp = 4; // RGBA
                ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * bpp);
                GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

                // Need to flip image vertically because OpenGL Y is bottom-up, BufferedImage is top-down
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int i = (x + (width * y)) * bpp;
                        int r = buffer.get(i) & 0xFF;
                        int g = buffer.get(i + 1) & 0xFF;
                        int b = buffer.get(i + 2) & 0xFF;
                        int a = buffer.get(i + 3) & 0xFF;
                        image.setRGB(x, height - (y + 1), (a << 24) | (r << 16) | (g << 8) | b);
                    }
                }

                // Restore clear color
                java.awt.Color bg = viewer.getBackground();
                GL11.glClearColor(bg.getRed() / 255.0f, bg.getGreen() / 255.0f, bg.getBlue() / 255.0f, 1.0f);

                ImageIO.write(image, "png", finalFile);
                log.info("Exported layer to PNG: {}", finalFile.getAbsolutePath());

            } catch (java.io.IOException e) {
                log.error("Failed to export PNG", e);
                javax.swing.SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(PrimaryWindow.getInstance(), "Failed to export image: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
                );
            }
        });
    }
}
