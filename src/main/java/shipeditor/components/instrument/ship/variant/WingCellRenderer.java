package shipeditor.components.instrument.ship.variant;

import shipeditor.components.datafiles.entities.OrdnancedCSVEntry;
import shipeditor.components.datafiles.entities.WingCSVEntry;
import shipeditor.utility.components.rendering.OrdnancedEntryCellRenderer;

import javax.swing.JList;
import java.awt.Component;

public class WingCellRenderer extends OrdnancedEntryCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<? extends OrdnancedCSVEntry> list, OrdnancedCSVEntry value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof WingCSVEntry wing) {
            int ordnanceCost = wing.getOrdnanceCost(null);
            this.ordnanceLabel.setText("OP: " + ordnanceCost);
        }

        return this;
    }
}
