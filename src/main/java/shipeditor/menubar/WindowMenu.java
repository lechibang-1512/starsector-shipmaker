package shipeditor.menubar;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.communication.events.viewer.layers.LayerEvents.ViewerLayerRemovalConfirmed;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.parsing.FileUtilities;
import shipeditor.utility.overseers.StaticController;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import shipeditor.communication.events.viewer.control.ControlEvents.ViewerTransformsReset;
import shipeditor.communication.events.viewer.layers.LayerEvents.ShipLayerCreationQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.WeaponLayerCreationQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerRemovalQueued;

@Log4j2
public class WindowMenu extends JMenu {

    private JMenuItem removeLayer;

    WindowMenu() {
        super("Window");
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    void initialize() {
        JMenuItem createLayer = WindowMenu.createAddLayerOption();
        this.add(createLayer);

        removeLayer = new JMenuItem("Remove selected layer");
        removeLayer.addActionListener(event -> EventBus.publish(new ActiveLayerRemovalQueued()));

        EventBus.subscribe(this, event -> {
            LayerManager layerManager = StaticController.getLayerManager();
            if (layerManager != null) {
                if (event instanceof ViewerLayerRemovalConfirmed && layerManager.isEmpty()) {
                    removeLayer.setEnabled(false);
                } else if (event instanceof LayerWasSelected && !layerManager.isEmpty()) {
                    removeLayer.setEnabled(true);
                }
            }
        });

        this.add(removeLayer);
        this.addSeparator();

        JMenuItem resetTransform = new JMenuItem("Center on selected layer");
        resetTransform.addActionListener(event ->
                EventBus.publish(new ViewerTransformsReset())
        );
        this.add(resetTransform);

        this.addSeparator();

        JMenuItem printViewer = WindowMenu.getViewerPrintOption();
        this.add(printViewer);
    }

    public static JMenuItem createAddLayerOption() {
        JMenuItem createLayer = new JMenuItem("Create new layer");
        createLayer.addActionListener(event -> {
            Object[] options = {"Ship Layer", "Weapon Layer"};
            int result = JOptionPane.showOptionDialog(null,
                    "Select new layer type:",
                    "Create New Layer",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (result == 0) {
                EventBus.publish(new ShipLayerCreationQueued());
            } else {
                EventBus.publish(new WeaponLayerCreationQueued());
            }
        });
        return createLayer;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private static JMenuItem getViewerPrintOption() {
        JMenuItem printViewer = new JMenuItem("Print sprite to image");

        printViewer.addActionListener(event -> {
            shipeditor.components.viewer.layers.ViewerLayer activeLayer = null;
            LayerManager layerManager = StaticController.getLayerManager();
            if (layerManager != null) {
                activeLayer = layerManager.getActiveLayer();
            }
            if (activeLayer == null) {
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(), "No active layer selected to print.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            final shipeditor.components.viewer.layers.ViewerLayer finalLayer = activeLayer;

            javax.swing.JDialog dialog = new javax.swing.JDialog(shipeditor.PrimaryWindow.getInstance(), "Export Sprite to Image", true);
            dialog.setLayout(new java.awt.BorderLayout(10, 10));
            
            java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            dialog.setSize(new java.awt.Dimension((int)(screenSize.width * 0.9), (int)(screenSize.height * 0.9)));
            dialog.setLocationRelativeTo(null);

            javax.swing.JPanel previewWrapper = new javax.swing.JPanel(new java.awt.GridBagLayout());
            javax.swing.JLabel previewLabel = new javax.swing.JLabel("No sprite image");
            previewLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            previewWrapper.add(previewLabel);
            
            javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(previewWrapper);
            scrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));
            scrollPane.setWheelScrollingEnabled(true);
            
            java.awt.event.MouseAdapter panAdapter = new java.awt.event.MouseAdapter() {
                private java.awt.Point origin;
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    origin = e.getPoint();
                    if (e.getSource() == previewLabel) {
                        origin = javax.swing.SwingUtilities.convertPoint(previewLabel, origin, previewWrapper);
                    }
                }
                @Override
                public void mouseDragged(java.awt.event.MouseEvent e) {
                    if (origin != null) {
                        java.awt.Point current = e.getPoint();
                        if (e.getSource() == previewLabel) {
                            current = javax.swing.SwingUtilities.convertPoint(previewLabel, current, previewWrapper);
                        }
                        int deltaX = origin.x - current.x;
                        int deltaY = origin.y - current.y;
                        
                        javax.swing.JViewport viewPort = scrollPane.getViewport();
                        if (viewPort != null) {
                            java.awt.Rectangle view = viewPort.getViewRect();
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
            
            previewLabel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));
            previewWrapper.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));
            
            dialog.add(scrollPane, java.awt.BorderLayout.CENTER);

            javax.swing.JPanel rightPanel = new javax.swing.JPanel();
            rightPanel.setLayout(new java.awt.BorderLayout(0, 10));
            rightPanel.setPreferredSize(new java.awt.Dimension(300, 0));
            rightPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));

            javax.swing.JPanel controlsPanel = new javax.swing.JPanel();
            controlsPanel.setLayout(new javax.swing.BoxLayout(controlsPanel, javax.swing.BoxLayout.Y_AXIS));

            javax.swing.JComboBox<String> scaleCombo = new javax.swing.JComboBox<>(new String[]{"1x", "2x", "3x", "4x", "5x", "6x", "8x", "10x", "16x"});
            javax.swing.JSpinner paddingSpinner = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(0, 0, 2000, 10));
            javax.swing.JComboBox<String> bgCombo = new javax.swing.JComboBox<>(new String[]{"Transparent", "Solid Black", "Solid White", "Solid Gray"});
            javax.swing.JCheckBox centerlineCheck = new javax.swing.JCheckBox("Bake Guide Centerline", true);
            javax.swing.JCheckBox renderMountsCheck = new javax.swing.JCheckBox("Render Mounts & Bounds", false);
            javax.swing.JLabel dimensionLabel = new javax.swing.JLabel("Output Size: ");
            
            java.awt.Component[] componentsToAdd = {
                new javax.swing.JLabel("Output Scale:"), scaleCombo, javax.swing.Box.createVerticalStrut(15),
                new javax.swing.JLabel("Padding (px):"), paddingSpinner, javax.swing.Box.createVerticalStrut(15),
                new javax.swing.JLabel("Background:"), bgCombo, javax.swing.Box.createVerticalStrut(15),
                centerlineCheck, javax.swing.Box.createVerticalStrut(15),
                renderMountsCheck, javax.swing.Box.createVerticalStrut(25),
                dimensionLabel
            };
            
            for (java.awt.Component c : componentsToAdd) {
                if (c instanceof javax.swing.JComponent) ((javax.swing.JComponent) c).setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
                controlsPanel.add(c);
            }
            
            rightPanel.add(controlsPanel, java.awt.BorderLayout.NORTH);

            shipeditor.components.viewer.layers.LayerPainter painter = finalLayer.getPainter();
            Runnable updatePreview = () -> {
                String scaleStr = (String) scaleCombo.getSelectedItem();
                double tempScale = Double.parseDouble(scaleStr.replace("x", ""));
                int pad = (Integer) paddingSpinner.getValue();
                boolean drawCenter = centerlineCheck.isSelected();
                boolean drawMounts = renderMountsCheck.isSelected();
                
                int bgIdx = bgCombo.getSelectedIndex();
                java.awt.Color bgCol = new java.awt.Color(0,0,0,0);
                if (bgIdx == 1) bgCol = java.awt.Color.BLACK;
                if (bgIdx == 2) bgCol = java.awt.Color.WHITE;
                if (bgIdx == 3) bgCol = java.awt.Color.GRAY;
                final java.awt.Color finalBgCol = bgCol;
                
                if (painter != null) {
                    java.awt.geom.Rectangle2D bounds = painter.getVisualBounds();
                    int baseW = (int) Math.ceil(bounds.getWidth());
                    int baseH = (int) Math.ceil(bounds.getHeight());
                    if (baseW <= 0 || baseH <= 0) return;
                    
                    int outW = (int) Math.round((baseW + pad * 2) * tempScale);
                    int outH = (int) Math.round((baseH + pad * 2) * tempScale);
                    dimensionLabel.setText("Output Size: " + outW + " x " + outH + " px");
                    
                    StaticController.getViewer().queueGLTask(() -> {
                        java.awt.image.BufferedImage fboImg = shipeditor.utility.graphics.opengl.FramebufferUtilities.renderLayerToImage(
                            finalLayer, baseW, baseH, tempScale, pad, drawCenter, finalBgCol, drawMounts
                        );
                        if (fboImg != null) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                previewLabel.setIcon(new javax.swing.ImageIcon(fboImg));
                                previewLabel.setText("");
                                previewWrapper.revalidate();
                                previewWrapper.repaint();
                                
                                java.awt.Dimension viewSize = scrollPane.getViewport().getExtentSize();
                                java.awt.Dimension viewSizeTotal = previewWrapper.getSize();
                                int x = (viewSizeTotal.width - viewSize.width) / 2;
                                int y = (viewSizeTotal.height - viewSize.height) / 2;
                                scrollPane.getViewport().setViewPosition(new java.awt.Point(Math.max(0, x), Math.max(0, y)));
                            });
                        }
                    });
                }
            };
            
            scaleCombo.addActionListener(e -> updatePreview.run());
            paddingSpinner.addChangeListener(e -> updatePreview.run());
            bgCombo.addActionListener(e -> updatePreview.run());
            centerlineCheck.addActionListener(e -> updatePreview.run());
            renderMountsCheck.addActionListener(e -> updatePreview.run());

            updatePreview.run();

            javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 10));
            javax.swing.JButton exportBtn = new javax.swing.JButton("Export...");
            javax.swing.JButton cancelBtn = new javax.swing.JButton("Cancel");
            buttonPanel.add(exportBtn);
            buttonPanel.add(cancelBtn);
            
            rightPanel.add(buttonPanel, java.awt.BorderLayout.SOUTH);

            dialog.add(rightPanel, java.awt.BorderLayout.EAST);

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
                        java.awt.geom.Rectangle2D bounds = painter.getVisualBounds();
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
                    final boolean bakeCen = centerlineCheck.isSelected();
                    final boolean renderMounts = renderMountsCheck.isSelected();
                    
                    java.awt.Color bgCol = new java.awt.Color(0,0,0,0);
                    int bgIdx = bgCombo.getSelectedIndex();
                    if (bgIdx == 1) bgCol = java.awt.Color.BLACK;
                    if (bgIdx == 2) bgCol = java.awt.Color.WHITE;
                    if (bgIdx == 3) bgCol = java.awt.Color.GRAY;
                    final java.awt.Color finalBgCol = bgCol;

                    StaticController.getViewer().queueGLTask(() -> shipeditor.utility.graphics.opengl.FramebufferUtilities.printLayerToImage(finalLayer, finalWidth, finalHeight, finalScale, finalPadding, result, bakeCen, finalBgCol, renderMounts));
                    dialog.dispose();
                }
            });

            dialog.setVisible(true);
        });
        return printViewer;
    }

}
