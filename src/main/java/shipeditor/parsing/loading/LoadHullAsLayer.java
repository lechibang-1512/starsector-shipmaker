package shipeditor.parsing.loading;

import shipeditor.utility.text.StringManager;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.HullFileOpened;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.utility.graphics.Sprite;
import shipeditor.persistence.SettingsManager;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import shipeditor.communication.events.files.FileEvents.SpriteOpened;
import shipeditor.communication.events.viewer.layers.LayerEvents.ShipLayerCreationQueued;
import shipeditor.communication.events.viewer.layers.LayerEvents.LastLayerSelectQueued;

@Log4j2
public class LoadHullAsLayer extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        OpenHullAction.openHullAndDo(e1 -> {
            JFileChooser shipDataChooser = (JFileChooser) e1.getSource();
            File file = shipDataChooser.getSelectedFile();
            HullSpecFile hullSpecFile = FileLoading.loadHullFile(file);
            if (hullSpecFile == null) {
                log.error(StringManager.getString("FAILURE_TO_LOAD_HULL_CANCELLING_ACTION"), file);
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        StringManager.getString("FAILURE_TO_LOAD_HULL_CANCELLING_ACTION_ALT") + file,
                        StringManager.getString("FILE_LOADING_ERROR"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String spriteName = hullSpecFile.getSpriteName();

            Path spriteFilePath = Path.of(spriteName);
            File spriteFile = FileLoading.fetchDataFile(spriteFilePath, null);
            if (spriteFile == null) {
                if (SettingsManager.isDeveloperModeEnabled()) {
                    log.error(StringManager.getString("FAILED_TO_FIND_SPRITE"), file);
                }
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        StringManager.getString("SPRITE_NOT_FOUND_MSG"),
                        StringManager.getString("SPRITE_NOT_FOUND_TITLE"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            EventBus.publish(new ShipLayerCreationQueued());
            EventBus.publish(new LastLayerSelectQueued());
            Sprite sprite = FileLoading.loadSprite(spriteFile);
            EventBus.publish(new SpriteOpened(sprite));
            EventBus.publish(new HullFileOpened(hullSpecFile, file.getName()));
        });
    }

}
