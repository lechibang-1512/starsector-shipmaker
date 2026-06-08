package oth.shipeditor.components.datafiles.entities;

import oth.shipeditor.representation.ship.HullSize;

import javax.swing.*;

public interface OrdnancedCSVEntry extends CSVEntry {

    JLabel getIconLabel();

    JLabel getIconLabel(int maxSize);

    int getOrdnanceCost(HullSize size);

    String getEntryName();

}
