package oth.shipeditor.representation.weapon;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.parsing.deserialize.Point2DArrayDeserializer;
import oth.shipeditor.parsing.serialize.BaseNumberSerializer;
import oth.shipeditor.parsing.serialize.points.SlotLocationsSerializer;
import oth.shipeditor.utility.text.StringConstants;

import java.awt.geom.Point2D;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WeaponSlot {

    @JsonProperty("id")
    private String id;

    @JsonProperty("size")
    private String size;

    @JsonProperty("type")
    private String type;

    @JsonProperty("mount")
    private String mount;

    @JsonProperty("renderOrderMod")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer renderOrderMod;

    @JsonProperty("arc")
    @JsonSerialize(using = BaseNumberSerializer.class)
    private Double arc;

    @JsonProperty(StringConstants.ANGLE)
    @JsonSerialize(using = BaseNumberSerializer.class)
    private Double angle;

    @JsonProperty("locations")
    @JsonDeserialize(using = Point2DArrayDeserializer.class)
    @JsonSerialize(using = SlotLocationsSerializer.class)
    private Point2D.Double[] locations;

}
