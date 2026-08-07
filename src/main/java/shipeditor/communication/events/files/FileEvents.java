package shipeditor.communication.events.files;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import shipeditor.communication.events.BusEvent;
import shipeditor.components.datafiles.entities.CSVEntry;
import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.datafiles.entities.ShipSystemCSVEntry;
import shipeditor.components.datafiles.entities.WingCSVEntry;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.layers.weapon.ProjectileLayer;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.representation.ship.EngineStyle;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.representation.ship.HullStyle;
import shipeditor.representation.ship.SkinSpecFile;
import shipeditor.utility.graphics.Sprite;

public class FileEvents {
    public static record SpriteOpened(Sprite sprite) implements BusEvent {

    }

    public static record HullmodDataSet() implements BusEvent {

    }

    public static record HullTreeReloadQueued() implements BusEvent {

    }

    public static record WingDataSet() implements BusEvent {

    }

    public static record WeaponTreeReloadQueued() implements BusEvent {

    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    public static record WeaponSaveQueued(WeaponLayer weaponLayer) implements BusEvent {
    }

    /**
     * @author Ontarget (or Antigravity)
     */
    public static record CSVSaveQueued(CSVEntry entry) implements BusEvent {
    }

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    public static record ProjectileSaveQueued(ProjectileLayer projectileLayer) implements BusEvent {
    }

    public static record HullStylesLoaded(Map<String, HullStyle> hullStyles) implements BusEvent {

}


    public static record HullmodFoldersWalked(Map<Path, List<HullmodCSVEntry>> hullmodsByPackage) implements BusEvent {

}


    public static record EngineStylesLoaded(Map<String, EngineStyle> engineStyles) implements BusEvent {

}


    public static record WingDataLoaded(Map<Path, List<WingCSVEntry>> wingsByPackage) implements BusEvent {

}


    public static record SkinFileOpened(SkinSpecFile skinSpecFile, boolean setAsActive) implements BusEvent {

}


    public static record ShipSystemsLoaded(Map<Path, List<ShipSystemCSVEntry>> systemsByPackage) implements BusEvent {

}


    public static record HullFileOpened(HullSpecFile hullSpecFile, String hullFileName) implements BusEvent {

}


    public static record HullSaveQueued(ShipLayer shipLayer) implements BusEvent {

}


    public static record VariantSaveQueued(ShipVariant variant) implements BusEvent {

}

}
