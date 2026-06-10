package shipeditor.undo.edits;

import shipeditor.components.viewer.layers.LayerPainter;

public interface LayerEdit {

    LayerPainter getLayerPainter();

    void cleanupReferences();

}
