package oth.shipeditor.components.datafiles.entities;

import oth.shipeditor.components.viewer.layers.ViewerLayer;

public interface LayerableEntry extends CSVEntry{

    ViewerLayer loadLayerFromEntry();

}
