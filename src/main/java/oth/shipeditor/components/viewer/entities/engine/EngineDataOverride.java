package oth.shipeditor.components.viewer.entities.engine;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.representation.ship.EngineStyle;
import oth.shipeditor.representation.GameDataRepository;

import java.util.Map;

@Getter @Setter @Builder
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class EngineDataOverride implements EngineData {

    private Integer index;

    private Double angle;

    private Double length;

    private Double width;

    private String styleID;

    private EngineStyle style;

    @Override
    public Double getAngleBoxed() {
        return angle;
    }

    @Override
    public Double getLengthBoxed() {
        return length;
    }

    @Override
    public Double getWidthBoxed() {
        return width;
    }

    @Override
    public Double getContrailSizeBoxed() {
        return null;
    }

    public EngineStyle getStyle() {
        if (style != null) return style;
        GameDataRepository gameData = SettingsManager.getGameData();
        Map<String, EngineStyle> allEngineStyles = gameData.getAllEngineStyles();
        if (allEngineStyles != null) {
            EngineStyle engineStyle = allEngineStyles.get(styleID);
            this.setStyle(engineStyle);
        }
        return style;
    }

}
