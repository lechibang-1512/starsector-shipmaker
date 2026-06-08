package oth.shipeditor.components;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.components.layering.ViewerLayersPanel;
import oth.shipeditor.components.viewer.LayerViewer;
import oth.shipeditor.components.viewer.PrimaryViewer;
import oth.shipeditor.undo.UndoOverseer;
import oth.shipeditor.utility.themes.Themes;

import javax.swing.*;
import java.awt.*;

@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public final class WindowContentPanes {

    /**
     * Complex component responsible for ship layers display.
     */
    @Getter
    private LayerViewer shipView;

    /**
     * Status line panel for ship sprite viewer.
     */
    @Getter
    private ViewerStatusPanel statusPanel;

    private final Container primaryContentPane;

    /**
     * Holds datafile panel on the left and secondary split pane on the right.
     */
    @Getter
    private TripleSplitContainer tripleSplitter;

    public WindowContentPanes(Container pane) {
        this.primaryContentPane = pane;
    }

    public void loadLayerHandling() {
        JPanel northPane = new JPanel();
        northPane.setLayout(new BorderLayout());
        northPane.setBorder(null);
        if (shipView == null) {
            // We want to fail fast here, just to be safe and find out quick.
            throw new IllegalStateException("Ship view was null at the time of layer panel initialization!");
        }
        ViewerLayersPanel layersPanel = new ViewerLayersPanel(shipView.getLayerManager());
        northPane.add(layersPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton undo = new JButton();
        undo.setAction(UndoOverseer.getUndoAction());
        undo.setHideActionText(false);
        undo.setToolTipText("Undo");
        toolBar.add(undo);

        JButton redo = new JButton();
        redo.setAction(UndoOverseer.getRedoAction());
        redo.setHideActionText(false);
        redo.setToolTipText("Redo");
        toolBar.add(redo);
        
        northPane.add(toolBar, BorderLayout.NORTH);

        northPane.setBackground(Themes.getTabBackgroundColor());

        primaryContentPane.add(northPane, BorderLayout.PAGE_START);
    }

    public void loadShipView() {
        this.shipView = new PrimaryViewer().commenceInitialization();
        this.statusPanel = new ViewerStatusPanel(this.shipView);
        primaryContentPane.add(this.statusPanel, BorderLayout.PAGE_END);
        this.refreshContent();
    }

    public void refreshContent() {
        primaryContentPane.revalidate();
        primaryContentPane.repaint();
    }

    public void loadEditingPanes() {
        tripleSplitter = new TripleSplitContainer();
        tripleSplitter.loadContentPanes(shipView);
        primaryContentPane.add(tripleSplitter, BorderLayout.CENTER);
        this.refreshContent();
    }

}
