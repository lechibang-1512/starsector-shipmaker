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
public class SmokeSpec {

    @JsonProperty(StringConstants.PARTICLE_SIZE_MIN)
    private double particleSizeMin;

    @JsonProperty(StringConstants.PARTICLE_SIZE_RANGE)
    private double particleSizeRange;

    @JsonProperty("cloudParticleCount")
    private int cloudParticleCount;

    @JsonProperty("cloudDuration")
    private double cloudDuration;

    @JsonProperty("cloudRadius")
    private double cloudRadius;

    @JsonProperty("blowbackParticleCount")
    private int blowbackParticleCount;

    @JsonProperty("blowbackDuration")
    private double blowbackDuration;

    @JsonProperty("blowbackLength")
    private double blowbackLength;

    @JsonProperty("blowbackSpread")
    private double blowbackSpread;

    @JsonDeserialize(using = ColorArrayRGBADeserializer.class)
    @JsonSerialize(using = ColorArrayRGBASerializer.class)
    @JsonProperty(StringConstants.PARTICLE_COLOR)
    private Color particleColor;

}
