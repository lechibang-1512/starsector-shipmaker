package shipeditor.components.instrument.ship.variant;

import shipeditor.communication.BusEventListener;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.BusEvent;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerUpdated;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.layers.ship.data.ShipHull;
import shipeditor.utility.themes.Themes;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;

public class OPSummaryBar extends JPanel implements BusEventListener {

    private final JProgressBar progressBar;
    private final JLabel label;

    public OPSummaryBar() {
        this.setLayout(new BorderLayout(10, 0));
        this.setBorder(new EmptyBorder(4, 6, 4, 6));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(false);
        this.add(progressBar, BorderLayout.CENTER);

        label = new JLabel("OP: 0/0");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        this.add(label, BorderLayout.LINE_END);

        EventBus.subscribe(this);
    }

    @Override
    public void handleEvent(BusEvent event) {
        if (event instanceof ActiveLayerUpdated checked) {
            refresh(checked.updated());
        }
    }

    public void refresh(ViewerLayer layer) {
        if (!(layer instanceof ShipLayer shipLayer)) {
            clear();
            return;
        }
        ShipPainter painter = shipLayer.getPainter();
        if (painter == null || painter.isUninitialized()) {
            clear();
            return;
        }
        ShipVariant variant = painter.getActiveVariant();
        ShipHull hull = shipLayer.getHull();
        if (variant == null || variant.isEmpty() || hull == null) {
            clear();
            return;
        }

        int totalOP = variant.getTotalUsedOP(shipLayer);
        int maxOP = shipLayer.getTotalOP();

        progressBar.setMaximum(maxOP);
        progressBar.setValue(totalOP);

        label.setText("OP: " + totalOP + " / " + maxOP);

        if (totalOP > maxOP) {
            progressBar.setForeground(Themes.getReddishFontColor());
            label.setForeground(Themes.getReddishFontColor());
        } else if (totalOP == maxOP) {
            progressBar.setForeground(Themes.getSuccessColor());
            label.setForeground(Themes.getSuccessColor());
        } else {
            progressBar.setForeground(null); // default
            label.setForeground(Themes.getTextColor());
        }
    }

    private void clear() {
        progressBar.setValue(0);
        progressBar.setMaximum(100);
        progressBar.setForeground(null);
        label.setText("OP: - / -");
        label.setForeground(Themes.getTextColor());
    }
}
