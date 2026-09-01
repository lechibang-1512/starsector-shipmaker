package shipeditor.utility.text;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public final class StringManager {
    private static final Map<String, String> STRINGS = new HashMap<>();

    static {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] groups = {
                "ui_general", "system_messages", "editor_controls", 
                "skin_overrides", "weapon_properties", "export_dialog", "misc_labels"
            };
            for (String group : groups) {
                try (InputStream is = StringManager.class.getResourceAsStream("/" + group + ".json")) {
                    if (is != null) {
                        Map<String, String> map = mapper.readValue(is, new TypeReference<Map<String, String>>() {});
                        STRINGS.putAll(map);
                    } else {
                        log.error("Could not find /" + group + ".json");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to load strings json files", e);
        }
    }

    private StringManager() {}

    public static String getString(String key) {
        return STRINGS.getOrDefault(key, key);
    }
}
