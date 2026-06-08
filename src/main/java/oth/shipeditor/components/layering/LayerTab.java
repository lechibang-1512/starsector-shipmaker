package oth.shipeditor.components.layering;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import oth.shipeditor.components.viewer.layers.ViewerLayer;

import javax.swing.*;
import java.awt.*;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public abstract class LayerTab extends JPanel {

    @Getter
    private final ViewerLayer associatedLayer;

    LayerTab(ViewerLayer layer) {
        this.associatedLayer = layer;
        this.setLayout(new BorderLayout());
    }

    public abstract String getTabTooltip();

}
