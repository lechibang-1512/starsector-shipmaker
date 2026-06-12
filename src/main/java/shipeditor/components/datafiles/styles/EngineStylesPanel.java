package shipeditor.components.datafiles.styles;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.EngineStylesLoaded;
import shipeditor.representation.ship.EngineStyle;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.nio.file.Path;
import java.util.Map;

public class EngineStylesPanel extends AbstractStylesPanel {

    @Override
    protected JPanel createTopPanel() {
        return null;
    }

    @Override
    protected void initListeners() {
        EventBus.subscribe(this, event -> {
            if (event instanceof EngineStylesLoaded checked) {
                populatePanel(checked.engineStyles());
            }
        });
    }

    private void populatePanel(Map<String, EngineStyle> engineStyles) {
        JPanel scrollerContent = getScrollerContent();
        scrollerContent.removeAll();
        for (EngineStyle style : engineStyles.values()) {
            scrollerContent.add(createStylePanel(style));
        }
        scrollerContent.revalidate();
        scrollerContent.repaint();
    }

    protected JPanel createStyleTitlePanel(Object style) {
        if (style instanceof EngineStyle checked) {
            String styleID = checked.getEngineStyleID();
            Path filePath = checked.getFilePath();
            Path containingPackage = checked.getContainingPackage();
            return ComponentUtilities.createFileTitlePanel(filePath, containingPackage, styleID);
        } else {
            throw new IllegalArgumentException(ILLEGAL_STYLE_ARGUMENT);
        }
    }

    protected JPanel createStyleContentPanel(Object style) {
        if (style instanceof EngineStyle checked) {
            JPanel contentContainer = new JPanel();
            contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.PAGE_AXIS));

            JLabel engineLabel = new JLabel("Engine color:");
            JPanel engineColorPanel = ComponentUtilities.createColorPropertyPanel(engineLabel,
                    checked.getEngineColor(), CONTENT_SIDE_PAD);
            contentContainer.add(engineColorPanel);

            JLabel contrailLabel = new JLabel("Contrail color:");
            JPanel contrailColorPanel = ComponentUtilities.createColorPropertyPanel(contrailLabel,
                    checked.getContrailColor(), CONTENT_SIDE_PAD);
            contentContainer.add(contrailColorPanel);

            return contentContainer;
        } else {
            throw new IllegalArgumentException(ILLEGAL_STYLE_ARGUMENT);
        }
    }

}
