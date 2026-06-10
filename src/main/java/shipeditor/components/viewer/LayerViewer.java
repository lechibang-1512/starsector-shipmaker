package shipeditor.components.viewer;

import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.utility.graphics.Sprite;

import java.awt.geom.AffineTransform;

public interface LayerViewer {

    ViewerLayer loadLayer(ViewerLayer layer, Sprite sprite);

    void centerViewpoint();

    LayerPainter getSelectedLayer();

    AffineTransform getTransformWorldToScreen();

    LayerManager getLayerManager();

}
