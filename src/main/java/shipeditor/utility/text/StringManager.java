package shipeditor.utility.text;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public final class StringManager {

    private static final Map<String, String> STRINGS;

    static {
        Map<String, String> loadedStrings = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        String[] groups = {
            "ui_general", "system_messages", "editor_controls",
            "skin_overrides", "weapon_properties", "export_dialog", "misc_labels"
        };
        for (String group : groups) {
            String resourcePath = "/" + group + ".json";
            try (InputStream is = StringManager.class.getResourceAsStream(resourcePath)) {
                if (is != null) {
                    Map<String, String> map = mapper.readValue(is, new TypeReference<Map<String, String>>() {});
                    if (map != null) {
                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            if (loadedStrings.containsKey(entry.getKey())) {
                                log.warn("Duplicate string key '{}' in {} overrides existing value",
                                        entry.getKey(), resourcePath);
                            }
                            loadedStrings.put(entry.getKey(), entry.getValue());
                        }
                    }
                } else {
                    log.error("Could not find {}", resourcePath);
                }
            } catch (Exception e) {
                log.error("Failed to load string group: {}", group, e);
            }
        }
        STRINGS = Collections.unmodifiableMap(loadedStrings);
    }

    private StringManager() {
    }

    public static String getString(String key) {
        String value = STRINGS.get(key);
        if (value == null) {
            log.trace("Missing localization key: '{}'", key);
            return key;
        }
        return value;
    }

    public static String getString(String key, Object... args) {
        String template = getString(key);
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return MessageFormat.format(template, args);
        } catch (Exception e) {
            log.warn("Failed to format string for key '{}' with args: {}", key, e.getMessage());
            return template;
        }
    }

    public static boolean containsKey(String key) {
        return STRINGS.containsKey(key);
    }
}
