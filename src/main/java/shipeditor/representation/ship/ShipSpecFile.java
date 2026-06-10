package shipeditor.representation.ship;

import java.nio.file.Path;

public interface ShipSpecFile {

    String getHullId();

    String getHullName();

    Path getFilePath();

    String getSpriteName();

}
