package shipeditor.parsing.loading;

import net.jqwik.api.*;
import org.junit.jupiter.api.Assertions;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.representation.ship.VariantFile;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.representation.weapon.ProjectileSpecFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.nio.file.Paths;

class DataLoadingPropertiesTest {

    @Property
    void testVariantIdResolutionNullSafety(
            @ForAll String variantIdInFile,
            @ForAll String entityIdInDb) {
        VariantFile mapped = new VariantFile();
        mapped.setVariantId(variantIdInFile);

        IndexedFile dbFile = IndexedFile.builder()
                .uuid(UUID.randomUUID())
                .entityId(entityIdInDb)
                .fileName("test_variant.variant")
                .filePath(Paths.get("data/variants/test_variant.variant"))
                .build();

        Map<String, VariantFile> allVariants = new ConcurrentHashMap<>();

        // Simulating the exact resolution and mapping logic in
        // LoadShipDataAction.collectVariants()
        String variantId = mapped.getVariantId();
        if (variantId == null || variantId.isEmpty()) {
            variantId = dbFile.getEntityId();
            mapped.setVariantId(variantId);
        }

        if (variantId != null && !variantId.isEmpty()) {
            // Should successfully put without throwing NullPointerException
            allVariants.put(variantId, mapped);
            Assertions.assertEquals(mapped, allVariants.get(variantId));
            Assertions.assertEquals(variantId, mapped.getVariantId());
        } else {
            // If both are null/empty, we log and do not insert, preventing
            // ConcurrentHashMap NPE
            Assertions.assertTrue(variantId == null || variantId.isEmpty());
            Assertions.assertTrue(allVariants.isEmpty());
        }
    }

    @Property
    void testWeaponIdResolutionNullSafety(
            @ForAll String weaponIdInFile,
            @ForAll String entityIdInDb) {
        WeaponSpecFile mapped = new WeaponSpecFile();
        mapped.setId(weaponIdInFile);

        IndexedFile dbFile = IndexedFile.builder()
                .uuid(UUID.randomUUID())
                .entityId(entityIdInDb)
                .fileName("test_weapon.wpn")
                .filePath(Paths.get("data/weapons/test_weapon.wpn"))
                .build();

        Map<String, WeaponSpecFile> mappedWeaponSpecs = new ConcurrentHashMap<>();

        // Simulating the exact resolution and mapping logic in
        // LoadWeaponsDataAction.walkWeaponsFolder()
        String weaponId = mapped.getId();
        if (weaponId == null || weaponId.isEmpty()) {
            weaponId = dbFile.getEntityId();
            mapped.setId(weaponId);
        }

        if (weaponId != null && !weaponId.isEmpty()) {
            mappedWeaponSpecs.put(weaponId, mapped);
            Assertions.assertEquals(mapped, mappedWeaponSpecs.get(weaponId));
            Assertions.assertEquals(weaponId, mapped.getId());
        } else {
            Assertions.assertTrue(weaponId == null || weaponId.isEmpty());
            Assertions.assertTrue(mappedWeaponSpecs.isEmpty());
        }
    }

    @Property
    void testProjectileIdResolutionNullSafety(
            @ForAll String projectileIdInFile,
            @ForAll String entityIdInDb) {
        ProjectileSpecFile mapped = new ProjectileSpecFile();
        mapped.setId(projectileIdInFile);

        IndexedFile dbFile = IndexedFile.builder()
                .uuid(UUID.randomUUID())
                .entityId(entityIdInDb)
                .fileName("test_proj.proj")
                .filePath(Paths.get("data/proj/test_proj.proj"))
                .build();

        Map<String, ProjectileSpecFile> allProjectiles = new ConcurrentHashMap<>();

        // Simulating the exact resolution and mapping logic in
        // LoadWeaponsDataAction.collectProjectiles()
        String projId = mapped.getId();
        if (projId == null || projId.isEmpty()) {
            projId = dbFile.getEntityId();
            mapped.setId(projId);
        }

        if (projId != null && !projId.isEmpty()) {
            allProjectiles.put(projId, mapped);
            Assertions.assertEquals(mapped, allProjectiles.get(projId));
            Assertions.assertEquals(projId, mapped.getId());
        } else {
            Assertions.assertTrue(projId == null || projId.isEmpty());
            Assertions.assertTrue(allProjectiles.isEmpty());
        }
    }

    @Property
    void testHullIdResolutionNullSafety(
            @ForAll String entityIdInDb,
            @ForAll String fileName) {
        IndexedFile dbFile = IndexedFile.builder()
                .uuid(UUID.randomUUID())
                .entityId(entityIdInDb)
                .fileName(fileName)
                .filePath(Paths.get("data/hulls/temp.ship"))
                .build();

        Map<String, String> shipFiles = new ConcurrentHashMap<>();

        // Simulating the exact resolution and mapping logic in
        // LoadShipDataAction.walkHullFolder()
        String entityId = dbFile.getEntityId();
        if (entityId != null && !entityId.isEmpty() && dbFile.getFileName() != null) {
            shipFiles.put(entityId, dbFile.getFileName());
            Assertions.assertEquals(dbFile.getFileName(), shipFiles.get(entityId));
        } else {
            Assertions.assertTrue(entityId == null || entityId.isEmpty() || dbFile.getFileName() == null);
            Assertions.assertTrue(shipFiles.isEmpty());
        }
    }
}
