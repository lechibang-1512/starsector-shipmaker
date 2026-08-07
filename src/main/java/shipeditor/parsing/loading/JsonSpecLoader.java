package shipeditor.parsing.loading;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.log4j.Log4j2;
import shipeditor.parsing.FileUtilities;
import shipeditor.parsing.JsonProcessor;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.representation.ship.SkinSpecFile;
import shipeditor.representation.ship.VariantFile;
import shipeditor.representation.weapon.ProjectileSpecFile;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.Errors;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.text.StringValues;

import java.io.File;
import java.io.IOException;

@Log4j2
public final class JsonSpecLoader {

    private JsonSpecLoader() {
    }

    public static HullSpecFile loadHullFile(File file) {
        if (file == null) {
            return null;
        }
        HullSpecFile hullSpecFile = loadDataFile(file, StringConstants.SHIP_EXTENSION, HullSpecFile.class);
        if (hullSpecFile != null) {
            hullSpecFile.setFilePath(file.toPath());
            GameDataRepository.putSpec(hullSpecFile);
        }
        return hullSpecFile;
    }

    public static WeaponSpecFile loadWeaponFile(File file) {
        if (file == null) {
            return null;
        }
        WeaponSpecFile weaponSpecFile = loadDataFile(file, StringConstants.WEAPON_EXTENSION, WeaponSpecFile.class);
        if (weaponSpecFile != null) {
            weaponSpecFile.setWeaponSpecFilePath(file.toPath());

            if (weaponSpecFile.getType() == null) {
                log.error(StringValues.WEAPON_TYPE_NULL, file.getName());
            }

        }
        return weaponSpecFile;
    }

    public static SkinSpecFile loadSkinFile(File file) {
        if (file == null) {
            return null;
        }
        SkinSpecFile skinSpecFile = loadDataFile(file, StringConstants.SKIN_EXTENSION, SkinSpecFile.class);
        if (skinSpecFile != null) {
            skinSpecFile.setFilePath(file.toPath());
            GameDataRepository.putSpec(skinSpecFile);
        }
        return skinSpecFile;
    }

    public static VariantFile loadVariantFile(File file) {
        if (file == null) {
            return null;
        }
        VariantFile variantFile = loadDataFile(file, StringConstants.VARIANT_EXTENSION, VariantFile.class);
        if (variantFile != null) {
            variantFile.setVariantFilePath(file.toPath());
            if (variantFile.getVariantId() == null) {
                variantFile.setVariantId(file.getName().replace(StringConstants.VARIANT_EXTENSION, ""));
            }
        }
        return variantFile;
    }

    public static ProjectileSpecFile loadProjectileFile(File file) {
        if (file == null) {
            return null;
        }
        ProjectileSpecFile projectileFile = loadDataFile(file, StringConstants.PROJECTILE_EXTENSION, ProjectileSpecFile.class);
        if (projectileFile != null) {
            projectileFile.setProjectileSpecFilePath(file.toPath());
        }
        return projectileFile;
    }

    public static <T> T loadDataFile(File file, String extension, Class<T> dataClass) {
        if (file == null || !file.exists()) {
            log.error(StringValues.DATA_FILE_NOT_EXIST, file != null ? file.getPath() : "null");
            return null;
        }
        String toString = file.getPath();
        if (extension == null || !toString.endsWith(extension)) {
            throw new IllegalArgumentException(StringValues.INVALID_FILE_EXTENSION);
        }

        if (file.length() == 0) {
            log.warn(StringValues.DATA_FILE_EMPTY, file.getName());
            return null;
        }

        if (SettingsManager.isDeveloperModeEnabled()) {
            log.trace(StringValues.OPENING_DATA_FILE, file.getName());
        }

        T dataFile = parseCorrectableJSON(file, dataClass);
        if (dataFile == null) {
            log.error(StringValues.DATA_FILE_PARSE_FAILED, file.getName());
            if (SettingsManager.areFileErrorPopupsEnabled()) {
                Errors.showFileError(StringValues.DATA_FILE_PARSE_EXCEPTION + file, new Exception("Failed to parse correctable JSON"));
            }
        }
        return dataFile;
    }

    public static <T> T parseCorrectableJSON(File file, Class<T> target) {
        if (file == null || target == null) {
            return null;
        }
        ObjectMapper objectMapper = FileUtilities.getConfigured();

        TypeFactory typeFactory = objectMapper.getTypeFactory();
        JavaType javaType = typeFactory.constructType(target);

        return parseCorrectableJSON(file, javaType);
    }

    public static <T> T parseCorrectableJSON(File file, JavaType targetType) {
        if (file == null || targetType == null) {
            return null;
        }
        T result;
        ObjectMapper objectMapper = FileUtilities.getConfigured();

        String content = JsonProcessor.straightenMalformed(file);
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        try (JsonParser parser = objectMapper.createParser(content)) {
            result = objectMapper.readValue(parser, targetType);
        } catch (IOException e) {
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.error(StringValues.CORRECTED_JSON_PARSE_FAILED, file.getName(), e);
                Errors.printToStream(e);
            } else {
                log.error(StringValues.CORRECTED_JSON_PARSE_FAILED, file.getName());
            }
            result = null;
        }
        return result;
    }
}
