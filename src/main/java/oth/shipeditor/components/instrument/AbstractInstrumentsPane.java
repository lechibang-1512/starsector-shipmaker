package oth.shipeditor.components.instrument;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.components.InstrumentSplitterResized;
import oth.shipeditor.utility.components.MinimizeListener;
import oth.shipeditor.utility.components.MinimizerWidget;

import javax.swing.*;
import java.awt.*;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public abstract class AbstractInstrumentsPane extends JTabbedPane {

    /**
     * Panel that is currently selected; depending on which panel it is interactivity of certain entities is resolved.
     */
    private JPanel activePanel;

    @Getter
    private final MinimizerWidget minimizer;

    @Getter @Setter
    private boolean instrumentPaneMinimized;

    private final Dimension preferredSize = new Dimension(300, 0);

    protected AbstractInstrumentsPane() {
        this.minimizer = new MinimizerWidget(this.minimizeTabbedPane(), this.restoreTabbedPane());
        minimizer.setPanelSwitched(false);
        this.initListeners();
        this.setTabPlacement(SwingConstants.LEFT);
        this.setMinimumSize(new Dimension(150, 0));
        this.setPreferredSize(preferredSize);
    }

    public void setTargetWidth(int width) {
        preferredSize.width = width;
    }

    public int getTargetWidth() {
        return preferredSize.width;
    }

    protected String getMinimizePrompt() {
        String minimizePrompt = "Left-click to minimize panel";
        if (minimizer.isMinimized()) {
            minimizePrompt = "Left-click to expand panel";
        }
        return minimizePrompt;
    }

    protected abstract void dispatchModeChange(JPanel active);

    protected abstract void updateTooltipText();

    private void initListeners() {
        this.addChangeListener(event -> {
            Component selected = getSelectedComponent();
            if (selected instanceof JPanel panel) {
                activePanel = panel;
                this.dispatchModeChange(activePanel);
            } else if (selected instanceof JTabbedPane subPane) {
                Component subSelected = subPane.getSelectedComponent();
                if (subSelected instanceof JPanel panel) {
                    activePanel = panel;
                    this.dispatchModeChange(activePanel);
                }
            }
            
            if (minimizer.isMinimized()) {
                minimizer.setRestorationQueued(true);
            }
            minimizer.setPanelSwitched(true);
        });
        this.addMouseListener(new MinimizeListener(this, this.minimizer));
    }

    protected void addInnerTabChangeListener(JTabbedPane innerPane) {
        innerPane.addChangeListener(e -> {
            if (getSelectedComponent() == innerPane) {
                Component subSelected = innerPane.getSelectedComponent();
                if (subSelected instanceof JPanel panel) {
                    activePanel = panel;
                    this.dispatchModeChange(activePanel);
                }
            }
        });
    }

    private Runnable minimizeTabbedPane() {
        return () -> {
            minimizer.setMinimized(true);
            Dimension preferred = this.getPreferredSize();
            Dimension minimizedSize = new Dimension(0, preferred.height);
            this.setPreferredSize(minimizedSize);
            updateTooltipText();
            EventBus.publish(new InstrumentSplitterResized(this, true));
        };
    }

    private Runnable restoreTabbedPane() {
        return () -> {
            minimizer.setMinimized(false);
            this.setPreferredSize(preferredSize);
            updateTooltipText();
            EventBus.publish(new InstrumentSplitterResized(this, false));
        };
    }

}
