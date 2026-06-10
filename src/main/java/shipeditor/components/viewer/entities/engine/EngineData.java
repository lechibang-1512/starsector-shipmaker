package shipeditor.components.viewer.entities.engine;

import shipeditor.representation.ship.EngineStyle;

public interface EngineData {

    Double getAngleBoxed();

    Double getLengthBoxed();

    Double getWidthBoxed();

    Double getContrailSizeBoxed();

    String getStyleID();

    EngineStyle getStyle();

}
