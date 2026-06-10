package shipeditor.components.datafiles.entities;

import shipeditor.components.viewer.layers.ViewerLayer;

public interface LayerableEntry extends CSVEntry{

    ViewerLayer loadLayerFromEntry();

}
