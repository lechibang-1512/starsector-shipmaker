package oth.shipeditor.communication.events.components;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.awt.*;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record GameDataPanelResized(Dimension newMinimum) implements ComponentEvent {
}
