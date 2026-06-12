package shipeditor.menubar;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.LayerWasSelected;
import shipeditor.communication.events.viewer.layers.LayerEvents.ViewerLayerRemovalConfirmed;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.parsing.FileUtilities;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.StringValues;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
        JMenuItem printViewer = new JMenuItem("Print viewer to image");

        printViewer.addActionListener(event -> {
            var chooser = FileUtilities.getImageChooser();

            File directory = FileUtilities.getLastGeneralDirectory();
            if (directory != null) {
                chooser.setCurrentDirectory(directory);
            }

            chooser.setDialogTitle("Print viewer content to image file");

            int returnVal = chooser.showSaveDialog(shipeditor.PrimaryWindow.getInstance());
            FileUtilities.setLastGeneralDirectory(chooser.getCurrentDirectory());

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String extension = ((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions()[0];
                File result = FileUtilities.ensureFileExtension(chooser, extension);
                log.info("Commencing viewer printing: {}", result);

                var viewer = StaticController.getViewer();
                int width = viewer.getWidth();
                int height = viewer.getHeight();
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = image.createGraphics();

                viewer.print(g2d);
                g2d.dispose();
                try {
                    javax.imageio.ImageIO.write(image , extension, result);
                } catch (IOException e) {
                    log.error("Viewer printing failed", e);
                    JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                            "Viewer printing failed, exception thrown at: " + result,
                            StringValues.FILE_SAVING_ERROR,
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return printViewer;
    }

}
