package shipeditor.parsing.loading;

import shipeditor.components.viewer.PrimaryViewer;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.utility.graphics.Sprite;
import shipeditor.utility.overseers.StaticController;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class LoadSpriteAsNewHull extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        Consumer<Sprite> layerCreator = sprite -> {
            LayerManager layerManager = StaticController.getLayerManager();
            if (layerManager == null) return;

            ShipLayer shipLayer = layerManager.createShipLayer();
            layerManager.activateLastLayer();

            PrimaryViewer viewer = StaticController.getViewer();
            viewer.loadSpriteToLayer(shipLayer, sprite);

            HullSpecFile created = new HullSpecFile();
            shipLayer.initializeHullData(created);
        };
        OpenSpriteAction.openSpriteAndDo(layerCreator);
    }

}
