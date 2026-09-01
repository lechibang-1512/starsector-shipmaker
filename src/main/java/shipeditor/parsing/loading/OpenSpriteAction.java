package shipeditor.parsing.loading;

import shipeditor.utility.text.StringManager;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.parsing.FileUtilities;
import shipeditor.utility.graphics.Sprite;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.function.Consumer;
import shipeditor.communication.events.files.FileEvents.SpriteOpened;

@Log4j2
public class OpenSpriteAction extends AbstractAction {

    public static void openSpriteAndDo(Consumer<Sprite> action) {
        JFileChooser spriteChooser = FileUtilities.getImageChooser();

        int returnVal = spriteChooser.showOpenDialog(shipeditor.PrimaryWindow.getInstance());
        FileUtilities.setLastSpriteDirectory(spriteChooser.getCurrentDirectory());

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            log.info(FileUtilities.OPEN_COMMAND_CANCELLED_BY_USER);
            return;
        }

        File file = spriteChooser.getSelectedFile();

        if (FileUtilities.isFileWithinGamePackages(file)) {
            Sprite sprite = FileLoading.loadSprite(file);
            action.accept(sprite);
        } else {
            log.error(StringManager.getString("OUTSIDE_GAME_PACKAGES_LOG"));
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    StringManager.getString("OUTSIDE_GAME_PACKAGES_ERROR") + file,
                    StringManager.getString("FILE_LOADING_ERROR"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OpenSpriteAction.openSpriteAndDo(sprite -> EventBus.publish(new SpriteOpened(sprite)));
    }

}
