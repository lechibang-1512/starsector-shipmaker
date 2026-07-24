package shipeditor.components.datafiles.trees;

import shipeditor.components.viewer.layers.ship.FeaturesOverseer;
import shipeditor.representation.ship.VariantFile;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.MouseoverLabelListener;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Enumeration;

public class DataTreeVariantPanelBuilder {

    public static JPanel createVariantsPanel(Collection<VariantFile> variantFiles, boolean withSelector) {
        JPanel variantsPanel = new JPanel();
        variantsPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        ComponentUtilities.outfitPanelWithTitle(variantsPanel, new Insets(1, 0, 0, 0), "Variants");
        variantsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel labelContainer = new JPanel();
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelContainer.setBorder(new EmptyBorder(2, 0, 0, 0));
        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.PAGE_AXIS));

        if (variantFiles.isEmpty()) throw new IllegalArgumentException("Empty variants list!");

        ButtonGroup group = new ButtonGroup();
        variantFiles.forEach(variant -> {
            JPanel variantLine = new JPanel();
            variantLine.setAlignmentX(Component.LEFT_ALIGNMENT);
            variantLine.setLayout(new BoxLayout(variantLine, BoxLayout.LINE_AXIS));

            JLabel variantFileLabel = createVariantFileLabel(variant);
            if (withSelector) {
                outfitVariantLabelWithSelector(variant, variantLine, group, variantFileLabel);
            }
            variantLine.add(variantFileLabel);
            labelContainer.add(variantLine);
            labelContainer.add(Box.createVerticalStrut(2));
        });

        if (withSelector) {
            Enumeration<AbstractButton> elements = group.getElements();
            if (elements.hasMoreElements()) {
                AbstractButton abstractButton = elements.nextElement();
                abstractButton.doClick();
            }
        }

        variantsPanel.add(labelContainer);
        return variantsPanel;
    }

    private static JLabel createVariantFileLabel(VariantFile variantFile) {
        Path variantFilePath = variantFile.getVariantFilePath();
        Path fileNamePath = variantFilePath != null ? variantFilePath.getFileName() : null;
        String fileName = fileNamePath != null ? fileNamePath.toString() : "Unknown";
        JLabel variantLabel = new JLabel("Variant file : " + fileName);
        variantLabel.setToolTipText(variantFilePath != null ? variantFilePath.toString() : "Unknown");
        variantLabel.setBorder(ComponentUtilities.createLabelSimpleBorder(ComponentUtilities.createLabelInsets()));
        if (variantFilePath != null) {
            JPopupMenu pathContextMenu = ComponentUtilities.createPathContextMenu(variantFilePath);
            variantLabel.addMouseListener(new MouseoverLabelListener(pathContextMenu, variantLabel));
        }
        return variantLabel;
    }

    private static void outfitVariantLabelWithSelector(VariantFile variant, JPanel variantLine,
                                                       ButtonGroup group, JLabel variantFileLabel) {
        JRadioButton selector = new JRadioButton();
        selector.setBorder(new EmptyBorder(0, 0, 2, 4));
        selector.addActionListener(e -> FeaturesOverseer.setModuleForInstall(variant));
        selector.setToolTipText("Select variant or drag label to be installed as module");
        group.add(selector);
        variantLine.add(selector);

        DragSource dragSource = DragSource.getDefaultDragSource();
        DragGestureListener gestureListener = new LabelDragListener(variant, variantFileLabel);
        dragSource.createDefaultDragGestureRecognizer(variantFileLabel,
                DnDConstants.ACTION_COPY, gestureListener);
    }
}
