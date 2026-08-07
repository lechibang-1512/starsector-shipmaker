package shipeditor.menubar.actions;

import lombok.extern.log4j.Log4j2;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.parsing.FileUtilities;
import shipeditor.utility.graphics.opengl.FramebufferUtilities;
import shipeditor.utility.overseers.StaticController;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JViewport;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

@Log4j2
public final class PrintSpriteAction {

    private PrintSpriteAction() {}

    @SuppressWarnings("CallToPrintStackTrace")
    public static void showPrintDialog() {
        ViewerLayer activeLayer = null;
        LayerManager layerManager = StaticController.getLayerManager();
        if (layerManager != null) {
            activeLayer = layerManager.getActiveLayer();
        }
        if (activeLayer == null) {
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(), "No active layer selected to print.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final ViewerLayer finalLayer = activeLayer;

        JDialog dialog = new JDialog(shipeditor.PrimaryWindow.getInstance(), "Export Sprite to Image", true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setSize(new Dimension((int)(screenSize.width * 0.9), (int)(screenSize.height * 0.9)));
        dialog.setLocationRelativeTo(null);

        JPanel previewWrapper = new JPanel(new GridBagLayout());
        JLabel previewLabel = new JLabel("No sprite image");
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewWrapper.add(previewLabel);
        
        JScrollPane scrollPane = new JScrollPane(previewWrapper);
        scrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));
        scrollPane.setWheelScrollingEnabled(true);
        
        MouseAdapter panAdapter = new MouseAdapter() {
            private Point origin;
            @Override
            public void mousePressed(MouseEvent e) {
                origin = e.getPoint();
                if (e.getSource() == previewLabel) {
                    origin = SwingUtilities.convertPoint(previewLabel, origin, previewWrapper);
                }
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                if (origin != null) {
                    Point current = e.getPoint();
                    if (e.getSource() == previewLabel) {
                        current = SwingUtilities.convertPoint(previewLabel, current, previewWrapper);
                    }
                    int deltaX = origin.x - current.x;
                    int deltaY = origin.y - current.y;
                    
                    JViewport viewPort = scrollPane.getViewport();
                    if (viewPort != null) {
                        Rectangle view = viewPort.getViewRect();
                        view.x += deltaX;
                        view.y += deltaY;
                        previewWrapper.scrollRectToVisible(view);
                    }
                }
            }
        };
        previewWrapper.addMouseListener(panAdapter);
        previewWrapper.addMouseMotionListener(panAdapter);
        previewLabel.addMouseListener(panAdapter);
        previewLabel.addMouseMotionListener(panAdapter);
        
        previewLabel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        previewWrapper.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout(0, 10));
        rightPanel.setPreferredSize(new Dimension(300, 0));
        rightPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));

        JComboBox<String> scaleCombo = new JComboBox<>(new String[]{"1x", "2x", "3x", "4x", "5x", "6x", "8x", "10x", "16x"});
        JSpinner paddingSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 2000, 10));
        JComboBox<String> bgCombo = new JComboBox<>(new String[]{"Transparent", "Solid Black", "Solid White", "Solid Gray"});
        JCheckBox renderMountsCheck = new JCheckBox("Render Mounts & Bounds", false);
        JLabel dimensionLabel = new JLabel("Output Size: ");
        
        Component[] componentsToAdd = {
            new JLabel("Output Scale:"), scaleCombo, Box.createVerticalStrut(15),
            new JLabel("Padding (px):"), paddingSpinner, Box.createVerticalStrut(15),
            new JLabel("Background:"), bgCombo, Box.createVerticalStrut(15),
            renderMountsCheck, Box.createVerticalStrut(25),
            dimensionLabel
        };
        
        for (Component c : componentsToAdd) {
            if (c instanceof JComponent) ((JComponent) c).setAlignmentX(Component.CENTER_ALIGNMENT);
            controlsPanel.add(c);
        }
        
        rightPanel.add(controlsPanel, BorderLayout.NORTH);

        LayerPainter painter = finalLayer.getPainter();
        Runnable updatePreview = () -> {
            String scaleStr = (String) scaleCombo.getSelectedItem();
            double tempScale = Double.parseDouble(scaleStr.replace("x", ""));
            int pad = (Integer) paddingSpinner.getValue();
            boolean drawMounts = renderMountsCheck.isSelected();
            
            int bgIdx = bgCombo.getSelectedIndex();
            Color bgCol = new Color(0,0,0,0);
            if (bgIdx == 1) bgCol = Color.BLACK;
            if (bgIdx == 2) bgCol = Color.WHITE;
            if (bgIdx == 3) bgCol = Color.GRAY;
            final Color finalBgCol = bgCol;
            
            if (painter != null) {
                Rectangle2D bounds = painter.getVisualBounds();
                int baseW = (int) Math.ceil(bounds.getWidth());
                int baseH = (int) Math.ceil(bounds.getHeight());
                if (baseW <= 0 || baseH <= 0) return;
                
                int outW = (int) Math.round((baseW + pad * 2) * tempScale);
                int outH = (int) Math.round((baseH + pad * 2) * tempScale);
                dimensionLabel.setText("Output Size: " + outW + " x " + outH + " px");
                
                StaticController.getViewer().queueGLTask(() -> {
                    BufferedImage fboImg = FramebufferUtilities.renderLayerToImage(
                        finalLayer, baseW, baseH, tempScale, pad, finalBgCol, drawMounts
                    );
                    if (fboImg != null) {
                        SwingUtilities.invokeLater(() -> {
                            previewLabel.setIcon(new ImageIcon(fboImg));
                            previewLabel.setText("");
                            previewWrapper.revalidate();
                            previewWrapper.repaint();
                            
                            Dimension viewSize = scrollPane.getViewport().getExtentSize();
                            Dimension viewSizeTotal = previewWrapper.getSize();
                            int x = (viewSizeTotal.width - viewSize.width) / 2;
                            int y = (viewSizeTotal.height - viewSize.height) / 2;
                            scrollPane.getViewport().setViewPosition(new Point(Math.max(0, x), Math.max(0, y)));
                        });
                    }
                });
            }
        };
        
        scaleCombo.addActionListener(e -> updatePreview.run());
        paddingSpinner.addChangeListener(e -> updatePreview.run());
        bgCombo.addActionListener(e -> updatePreview.run());
        renderMountsCheck.addActionListener(e -> updatePreview.run());

        updatePreview.run();

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        JButton exportBtn = new JButton("Export...");
        JButton cancelBtn = new JButton("Cancel");
        buttonPanel.add(exportBtn);
        buttonPanel.add(cancelBtn);
        
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(rightPanel, BorderLayout.EAST);

        cancelBtn.addActionListener(e -> dialog.dispose());
        
        exportBtn.addActionListener(e -> {
            var chooser = FileUtilities.getImageChooser();
            File directory = FileUtilities.getLastGeneralDirectory();
            if (directory != null) {
                chooser.setCurrentDirectory(directory);
            }
            chooser.setDialogTitle("Save Sprite Image");
            
            int returnVal = chooser.showSaveDialog(dialog);
            
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                FileUtilities.setLastGeneralDirectory(chooser.getCurrentDirectory());
                String extension = ((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions()[0];
                File result = FileUtilities.ensureFileExtension(chooser, extension);
                log.info("Commencing sprite printing: {}", result);

                int width = 0;
                int height = 0;
                if (painter != null && !painter.isUninitialized()) {
                    Rectangle2D bounds = painter.getVisualBounds();
                    width = (int) Math.ceil(bounds.getWidth());
                    height = (int) Math.ceil(bounds.getHeight());
                }
                if (width <= 0 || height <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Layer is empty or invalid size.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double tempScale = Double.parseDouble(((String) scaleCombo.getSelectedItem()).replace("x", ""));
                final double finalScale = tempScale;
                final int finalPadding = (Integer) paddingSpinner.getValue();
                final int finalWidth = width;
                final int finalHeight = height;
                final boolean renderMounts = renderMountsCheck.isSelected();
                
                Color bgCol = new Color(0,0,0,0);
                int bgIdx = bgCombo.getSelectedIndex();
                if (bgIdx == 1) bgCol = Color.BLACK;
                if (bgIdx == 2) bgCol = Color.WHITE;
                if (bgIdx == 3) bgCol = Color.GRAY;
                final Color finalBgCol = bgCol;

                StaticController.getViewer().queueGLTask(() -> FramebufferUtilities.printLayerToImage(finalLayer, finalWidth, finalHeight, finalScale, finalPadding, result, finalBgCol, renderMounts));
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }
}
