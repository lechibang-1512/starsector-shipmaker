package shipeditor.components.instrument;
import shipeditor.components.ComponentEnums.EditorInstrument;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.ViewerRepaintQueued;
import shipeditor.utility.components.MinimizeListener;
import shipeditor.utility.components.MinimizerWidget;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import shipeditor.communication.events.components.ComponentEvents.InstrumentSplitterResized;
import shipeditor.communication.events.viewer.points.PointEvents.InstrumentModeChanged;

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

    protected final Map<JPanel, EditorInstrument> panelMode = new HashMap<>();

    protected AbstractInstrumentsPane() {
        this.minimizer = new MinimizerWidget(this.minimizeTabbedPane(), this.restoreTabbedPane());
        minimizer.setPanelSwitched(false);
        this.initListeners();
        this.setTabPlacement(SwingConstants.TOP);
        this.setMinimumSize(new Dimension(150, 0));
        this.setPreferredSize(preferredSize);
        this.putClientProperty("JTabbedPane.tabType", "card");
        this.putClientProperty("JTabbedPane.tabHeight", 32);
        this.putClientProperty("JTabbedPane.showTabSeparators", true);
        this.putClientProperty("JTabbedPane.hasFullBorder", true);
        this.putClientProperty("JTabbedPane.tabWidthMode", "compact");
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

    protected void dispatchModeChange(JPanel active) {
        EditorInstrument selected = panelMode.get(active);
        if (selected != null) {
            EventBus.publish(new InstrumentModeChanged(selected));
            EventBus.publish(new ViewerRepaintQueued());
        }
    }

    protected void updateTooltipText() {
        String minimizePrompt = getMinimizePrompt();
        int size = this.getTabCount();
        for (int i = 0; i < size; i++) {
            Component comp = this.getComponentAt(i);
            if (comp instanceof JPanel panel) {
                EditorInstrument mode = panelMode.get(panel);
                String title = mode != null ? mode.getTitle() : "";
                String tooltip = shipeditor.utility.Utility.getWithLinebreaks(title, minimizePrompt);
                this.setToolTipTextAt(i, tooltip);
            }
        }
    }

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
