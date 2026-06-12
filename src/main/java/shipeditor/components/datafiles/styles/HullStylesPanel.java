package shipeditor.components.datafiles.styles;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.HullStylesLoaded;
import shipeditor.representation.ship.HullStyle;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.nio.file.Path;
import java.util.Map;

public class HullStylesPanel extends AbstractStylesPanel {

    @Override
    protected JPanel createTopPanel() {
        return null;
    }

    @Override
    protected void initListeners() {
        EventBus.subscribe(this, event -> {
            if (event instanceof HullStylesLoaded checked) {
                populatePanel(checked.hullStyles());
            }
        });
    }

    private void populatePanel(Map<String, HullStyle> hullStyles) {
        JPanel scrollerContent = getScrollerContent();
        scrollerContent.removeAll();
        for (HullStyle style : hullStyles.values()) {
            scrollerContent.add(this.createStylePanel(style));
        }
        scrollerContent.revalidate();
        scrollerContent.repaint();
    }

    @Override
    protected  JPanel createStyleTitlePanel(Object style) {
        if (style instanceof HullStyle checked) {
            String hullStyleID = checked.getHullStyleID();
            Path filePath = checked.getFilePath();
            Path containingPackage = checked.getContainingPackage();
            return ComponentUtilities.createFileTitlePanel(filePath, containingPackage, hullStyleID);
        } else {
            throw new IllegalArgumentException(ILLEGAL_STYLE_ARGUMENT);
        }

    }

    @Override
    protected JPanel createStyleContentPanel(Object style) {
        if (style instanceof HullStyle checked) {
            JPanel contentContainer = new JPanel();
            contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.PAGE_AXIS));

            JLabel ringLabel = new JLabel("Shield ring color:");
            JPanel ringColorPanel = ComponentUtilities.createColorPropertyPanel(ringLabel,
                    checked.getShieldRingColor(), CONTENT_SIDE_PAD);
            contentContainer.add(ringColorPanel);

            JLabel innerLabel = new JLabel("Shield inner color:");
            JPanel innerColorPanel = ComponentUtilities.createColorPropertyPanel(innerLabel,
                    checked.getShieldInnerColor(), CONTENT_SIDE_PAD);
            contentContainer.add(innerColorPanel);

            return contentContainer;
        } else {
            throw new IllegalArgumentException(ILLEGAL_STYLE_ARGUMENT);
        }
    }

}
