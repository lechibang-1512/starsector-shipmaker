package shipeditor.components.dialogs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shipeditor.persistence.GameDataPackage;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;

import javax.swing.JCheckBox;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModSelectionDialogTest {

    @TempDir
    Path tempDir;

    private Settings originalSettings;
    private File originalSettingsFile;

    @BeforeEach
    void setUp() throws IOException {
        originalSettings = SettingsManager.getSettings();
        originalSettingsFile = SettingsManager.getSettingsPath();
        
        // Setup a dummy settings file so updateFileFromRuntime doesn't throw or overwrite real data
        File tempSettingsFile = tempDir.resolve("ship_editor_settings.json").toFile();
        
        // Use reflection to set settingsFilePath
        try {
            Field pathField = SettingsManager.class.getDeclaredField("settingsFilePath");
            pathField.setAccessible(true);
            pathField.set(null, tempSettingsFile.toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Settings testSettings = null;
        try {
            java.lang.reflect.Constructor<Settings> constructor = Settings.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            testSettings = constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        Path modsFolder = tempDir.resolve("mods");
        Files.createDirectories(modsFolder);
        try {
            Method setModFolderPath = Settings.class.getDeclaredMethod("setModFolderPath", String.class);
            setModFolderPath.setAccessible(true);
            setModFolderPath.invoke(testSettings, modsFolder.toAbsolutePath().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            Method setCoreFolderName = SettingsManager.class.getDeclaredMethod("setCoreFolderName", String.class);
            setCoreFolderName.setAccessible(true);
            setCoreFolderName.invoke(null, "starsector-core");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SettingsManager.setSettings(testSettings);

        // Create fake mods
        Path modA = modsFolder.resolve("mod-a-folder");
        Files.createDirectories(modA);
        Files.writeString(modA.resolve("mod_info.json"), "{\"id\": \"mod_a_id\"}");

        Path modB = modsFolder.resolve("mod-b-folder");
        Files.createDirectories(modB);
        Files.writeString(modB.resolve("mod_info.json"), "{\"id\": \"mod_b_id\"}");

        // Create enabled_mods.json enabling ONLY mod-a
        Files.writeString(modsFolder.resolve("enabled_mods.json"), "{\"enabledMods\": [\"mod_a_id\"]}");
    }

    @AfterEach
    void tearDown() {
        SettingsManager.setSettings(originalSettings);
        try {
            Field pathField = SettingsManager.class.getDeclaredField("settingsFilePath");
            pathField.setAccessible(true);
            pathField.set(null, originalSettingsFile != null ? originalSettingsFile.toPath() : null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testModSelectionDialogInitializesAndAppliesCorrectly() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(), "Skip UI dialog test in headless mode");

        final ModSelectionDialog[] dialogHolder = new ModSelectionDialog[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            dialogHolder[0] = new ModSelectionDialog(null);
        });
        ModSelectionDialog dialog = dialogHolder[0];

        try {
            // Access the modCheckboxes field
            Field checkboxesField = ModSelectionDialog.class.getDeclaredField("modCheckboxes");
            checkboxesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, JCheckBox> modCheckboxes = (Map<String, JCheckBox>) checkboxesField.get(dialog);

            // Assert that both folders were found and checkboxes created
            assertNotNull(modCheckboxes.get("mod-a-folder"), "Checkbox for mod-a-folder should exist");
            assertNotNull(modCheckboxes.get("mod-b-folder"), "Checkbox for mod-b-folder should exist");

            // Wait for the background worker to finish loading mod states
            Field workerField = ModSelectionDialog.class.getDeclaredField("fetchWorker");
            workerField.setAccessible(true);
            javax.swing.SwingWorker<?, ?> worker = (javax.swing.SwingWorker<?, ?>) workerField.get(dialog);
            if (worker != null) {
                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                worker.addPropertyChangeListener(evt -> {
                    if ("state".equals(evt.getPropertyName()) && javax.swing.SwingWorker.StateValue.DONE == evt.getNewValue()) {
                        latch.countDown();
                    }
                });
                worker.get();
                latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
                // Flush EDT to ensure SwingWorker.done() completes updating checkboxes
                javax.swing.SwingUtilities.invokeAndWait(() -> {});
            }

            // Assert that fetchFromEnabledMods properly checked mod-a based on mod_id and unchecked mod-b
            assertTrue(modCheckboxes.get("mod-a-folder").isSelected(), "mod-a-folder should be selected because mod_a_id is in enabled_mods.json");
            assertFalse(modCheckboxes.get("mod-b-folder").isSelected(), "mod-b-folder should NOT be selected");

            // Manually flip selection to test applySelection
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                modCheckboxes.get("mod-a-folder").setSelected(false);
                modCheckboxes.get("mod-b-folder").setSelected(true);
            });

            // Call applySelection()
            Method applyMethod = ModSelectionDialog.class.getDeclaredMethod("applySelection");
            applyMethod.setAccessible(true);
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                try {
                    applyMethod.invoke(dialog);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(dialog::dispose);
        }

        Settings settings = SettingsManager.getSettings();
        
        // mod-a was deselected, so it should be disabled
        GameDataPackage pkgA = settings.getPackage("mod-a-folder");
        assertNotNull(pkgA);
        assertTrue(pkgA.isDisabled(), "pkgA should be disabled after applySelection");

        // mod-b was selected, so it should NOT be disabled
        GameDataPackage pkgB = settings.getPackage("mod-b-folder");
        assertNotNull(pkgB);
        assertFalse(pkgB.isDisabled(), "pkgB should NOT be disabled after applySelection");

        // Core should always be enabled
        GameDataPackage corePkg = settings.getPackage("starsector-core");
        assertNotNull(corePkg);
        assertFalse(corePkg.isDisabled(), "Core package should always be enabled");
    }
}
