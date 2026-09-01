package shipeditor.parsing.loading;

import shipeditor.utility.text.StringManager;

import lombok.extern.log4j.Log4j2;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.files.FileEvents.HullFileOpened;
import shipeditor.parsing.FileUtilities;
import shipeditor.representation.ship.HullSpecFile;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

@Log4j2
public class OpenHullAction extends AbstractAction {

    static void openHullAndDo(ActionListener action) {
        JFileChooser shipDataChooser = FileUtilities.getHullFileChooser();
        int returnVal = shipDataChooser.showOpenDialog(shipeditor.PrimaryWindow.getInstance());
        FileUtilities.setLastShipDirectory(shipDataChooser.getCurrentDirectory());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            ActionEvent event = new ActionEvent(shipDataChooser, ActionEvent.ACTION_PERFORMED, null);
            action.actionPerformed(event);
        }
        else {
            log.info(FileUtilities.OPEN_COMMAND_CANCELLED_BY_USER);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OpenHullAction.openHullAndDo(event -> {
            JFileChooser shipDataChooser = (JFileChooser) event.getSource();
            File file = shipDataChooser.getSelectedFile();
            HullSpecFile hullSpecFile = FileLoading.loadHullFile(file);
            if (hullSpecFile != null) {
                EventBus.publish(new HullFileOpened(hullSpecFile, file.getName()));
            }
            else {
                log.error(StringManager.getString("FAILURE_TO_LOAD_HULL_CANCELLING_ACTION"), file);
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        StringManager.getString("FAILURE_TO_LOAD_HULL_CANCELLING_ACTION_ALT") + file,
                        StringManager.getString("FILE_LOADING_ERROR"),
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

}
