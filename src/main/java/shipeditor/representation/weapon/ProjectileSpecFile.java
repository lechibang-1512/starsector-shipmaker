package shipeditor.representation.weapon;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import shipeditor.parsing.deserialize.Point2DDeserializer;
import shipeditor.utility.text.StringConstants;

import java.awt.geom.Point2D;
import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class ProjectileSpecFile {

    @Setter
    @JsonIgnore
    private Path projectileSpecFilePath;

    @Setter
    @JsonIgnore
    private Path containingPackage;

    @JsonProperty("id")
    private String id;

    @JsonProperty(StringConstants.SPEC_CLASS)
    private String specClass;

    @JsonProperty("missileType")
    private String missileType;

    @JsonProperty(StringConstants.SPRITE)
    private String sprite;

    @JsonProperty("size")
    private int[] size;

    @JsonDeserialize(using = Point2DDeserializer.class)
    @JsonProperty(StringConstants.CENTER)
    private  Point2D.Double center;

}
