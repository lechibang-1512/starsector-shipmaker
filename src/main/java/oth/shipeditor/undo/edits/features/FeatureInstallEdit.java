package oth.shipeditor.undo.edits.features;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.components.datafiles.entities.InstallableEntry;
import oth.shipeditor.components.datafiles.entities.ShipCSVEntry;
import oth.shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import oth.shipeditor.undo.AbstractEdit;
import oth.shipeditor.utility.overseers.StaticController;

import java.util.Map;

@Log4j2
@AllArgsConstructor
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class FeatureInstallEdit<T extends InstallableEntry> extends AbstractEdit {

    private final Map<String, T> collection;

    private final String slotID;

    private final T feature;

    /**
     * Can be null, needed for skin built-ins reloading.
     */
    private final Runnable afterAction;

    @Override
    public void undo() {
        collection.remove(slotID, feature);
        if (afterAction != null) {
            afterAction.run();
        }
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        if (feature instanceof InstalledFeature installed && installed.getDataEntry() instanceof ShipCSVEntry) {
            repainter.queueModulesRepaint();
        } else {
            repainter.queueBuiltInsRepaint();
            repainter.queueVariantsRepaint();
        }
    }

    @Override
    public void redo() {
        collection.put(slotID, feature);
        if (afterAction != null) {
            afterAction.run();
        }
        var repainter = StaticController.getScheduler();
        repainter.queueViewerRepaint();
        if (feature instanceof InstalledFeature installed && installed.getDataEntry() instanceof ShipCSVEntry) {
            repainter.queueModulesRepaint();
        } else {
            repainter.queueBuiltInsRepaint();
            repainter.queueVariantsRepaint();
        }
    }
    @Override

    public String getName() {
        return "Install Feature";
    }

}
