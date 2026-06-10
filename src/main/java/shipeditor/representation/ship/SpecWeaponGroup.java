package shipeditor.representation.ship;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import shipeditor.utility.text.StringConstants;

import java.util.Map;

@Getter @Setter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class SpecWeaponGroup {

    @JsonProperty("autofire")
    private boolean autofire;

    @JsonProperty("mode")
    private String mode;

    @JsonProperty(StringConstants.WEAPONS)
    private Map<String, String> weapons;

}
