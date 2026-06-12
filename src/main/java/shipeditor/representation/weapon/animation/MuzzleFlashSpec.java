package shipeditor.representation.weapon.animation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import shipeditor.parsing.deserialize.CustomDeserializers.ColorArrayRGBADeserializer;
import shipeditor.parsing.serialize.CustomSerializers.ColorArrayRGBASerializer;
import shipeditor.utility.text.StringConstants;

import java.awt.Color;

@Getter
@Setter
public class MuzzleFlashSpec {

    @JsonProperty(StringConstants.LENGTH)
    private double length;

    @JsonProperty("spread")
    private double spread;

    @JsonProperty(StringConstants.PARTICLE_SIZE_MIN)
    private double particleSizeMin;

    @JsonProperty(StringConstants.PARTICLE_SIZE_RANGE)
    private double particleSizeRange;

    @JsonProperty("particleDuration")
    private double particleDuration;

    @JsonProperty("particleCount")
    private int particleCount;

    @JsonDeserialize(using = ColorArrayRGBADeserializer.class)
    @JsonSerialize(using = ColorArrayRGBASerializer.class)
    @JsonProperty(StringConstants.PARTICLE_COLOR)
    private Color particleColor;

}
