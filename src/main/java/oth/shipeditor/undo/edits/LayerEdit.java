package oth.shipeditor.undo.edits;

import oth.shipeditor.components.viewer.layers.LayerPainter;

public interface LayerEdit {

    LayerPainter getLayerPainter();

    void cleanupReferences();

}
