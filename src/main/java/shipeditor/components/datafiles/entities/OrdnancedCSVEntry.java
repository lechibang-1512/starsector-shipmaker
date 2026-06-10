package shipeditor.components.datafiles.entities;

import shipeditor.representation.ship.HullSize;

import javax.swing.JLabel;

public interface OrdnancedCSVEntry extends CSVEntry {

    JLabel getIconLabel();

    JLabel getIconLabel(int maxSize);

    int getOrdnanceCost(HullSize size);

    String getEntryName();

}
