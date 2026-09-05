package shipeditor.parsing.saving;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import shipeditor.parsing.FileUtilities;
import shipeditor.parsing.loading.JsonSpecLoader;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

@Log4j2
public final class SaveActionUtilities {

    private SaveActionUtilities() {
    }

    @SuppressWarnings("unchecked")
    public static <T> void mergeUnrecognizedProperties(File file, Map<String, Object> targetMap, Class<T> clazz) {
        if (file == null || !file.isFile() || targetMap == null || clazz == null) {
            return;
        }
        try {
            T existingFile = JsonSpecLoader.parseCorrectableJSON(file, clazz);
            if (existingFile == null) {
                ObjectMapper objectMapper = FileUtilities.getConfigured();
                existingFile = objectMapper.readValue(file, clazz);
            }
            if (existingFile != null) {
                Method method = clazz.getMethod("getUnrecognizedProperties");
                Map<String, Object> unrecognized = (Map<String, Object>) method.invoke(existingFile);
                if (unrecognized != null) {
                    unrecognized.forEach(targetMap::putIfAbsent);
                }
            }
        } catch (Exception e) {
            log.trace("Could not read existing file for unrecognized properties: {}", file, e);
        }
    }
}
