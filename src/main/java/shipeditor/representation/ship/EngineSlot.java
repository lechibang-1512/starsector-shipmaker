package shipeditor.representation.ship;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import shipeditor.parsing.deserialize.CustomDeserializers.Point2DDeserializer;
import shipeditor.parsing.serialize.CustomSerializers.BaseNumberSerializer;
import shipeditor.parsing.serialize.points.EngineLocationSerializer;
import shipeditor.utility.text.StringConstants;

import java.awt.geom.Point2D;

@Getter @Setter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class EngineSlot {

    @JsonProperty("location")
    @JsonDeserialize(using = Point2DDeserializer.class)
    @JsonSerialize(using = EngineLocationSerializer.class)
    private Point2D.Double location;

    @JsonProperty(StringConstants.LENGTH)
    @JsonSerialize(using = BaseNumberSerializer.class)
    private Double length;

    @JsonProperty(StringConstants.WIDTH)
    @JsonSerialize(using = BaseNumberSerializer.class)
    private Double width;

    @JsonProperty(StringConstants.ANGLE)
    @JsonSerialize(using = BaseNumberSerializer.class)
    private Double angle;

    @JsonProperty("contrailSize")
    @JsonSerialize(using = BaseNumberSerializer.class)
    private Double contrailSize;

    @JsonProperty(StringConstants.STYLE)
    private String style;

    /**
     * This field is used to specify a custom style if style field is set to CUSTOM.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("styleId")
    private String styleId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("styleSpec")
    private EngineStyle styleSpec;

}
