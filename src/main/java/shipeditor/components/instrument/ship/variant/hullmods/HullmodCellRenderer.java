package shipeditor.components.instrument.ship.variant.hullmods;

import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.datafiles.entities.OrdnancedCSVEntry;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.utility.components.rendering.OrdnancedEntryCellRenderer;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.themes.Themes;

import javax.swing.JList;
import java.awt.Component;
import java.util.Collections;
import java.util.List;

public class HullmodCellRenderer extends OrdnancedEntryCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<? extends OrdnancedCSVEntry> list, OrdnancedCSVEntry value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof HullmodCSVEntry hullmod) {
            String modId = hullmod.getID();
            
            ShipVariant variant = null;
            var layer = StaticController.getActiveLayer();
            if (layer instanceof shipeditor.components.viewer.layers.ship.ShipLayer shipLayer) {
                variant = shipLayer.getActiveVariant();
            }

            if (variant != null) {
                int count = 0;
                count += countOccurrences(variant.getHullMods(), modId);
                count += countOccurrences(variant.getPermaMods(), modId);
                count += countOccurrences(variant.getSMods(), modId);

                if (count > 1) {
                    if (!isSelected) {
                        this.textLabel.setForeground(Themes.getReddishFontColor());
                    }
                    this.textLabel.setText(hullmod.getEntryName() + " (Duplicate)");
                    this.setToolTipText(hullmod.getEntryName() + " is fitted multiple times! Duplicate hullmods provide no extra benefit.");
                }
            }
        }

        return this;
    }

    private int countOccurrences(List<HullmodCSVEntry> list, String modId) {
        if (list == null) return 0;
        int count = 0;
        for (HullmodCSVEntry entry : list) {
            if (entry != null && modId.equals(entry.getID())) {
                count++;
            }
        }
        return count;
    }
}
