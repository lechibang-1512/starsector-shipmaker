package shipeditor.persistence;
 
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameFolderDetectionTest {

    private String originalCoreFolderName;

    @BeforeEach
    public void setUp() {
        originalCoreFolderName = SettingsManager.getCoreFolderName();
    }

    @AfterEach
    public void tearDown() {
        SettingsManager.setCoreFolderName(originalCoreFolderName);
    }

    @Test
    public void testLinuxStructure(@TempDir Path tempDir) throws IOException {
        Settings settings = new Settings();
        
        Path linuxRoot = tempDir.resolve("Linux_Starsector_0.96");
        Files.createDirectories(linuxRoot.resolve("data").resolve("hulls"));
        Files.write(linuxRoot.resolve("data").resolve("hulls").resolve("ship_data.csv"), "".getBytes());
        Files.createDirectories(linuxRoot.resolve("graphics"));
        Files.createDirectories(linuxRoot.resolve("mods"));
        Files.createDirectories(linuxRoot.resolve("mods").resolve("SomeMod").resolve("data"));
        Files.createDirectories(linuxRoot.resolve("mods").resolve("SomeMod").resolve("graphics"));
        Files.write(linuxRoot.resolve("mods").resolve("SomeMod").resolve("mod_info.json"), "{}".getBytes());
        Files.write(linuxRoot.resolve("starfarer.api.jar"), "".getBytes());
        
        boolean valid = Initializations.checkGameFolderEligibility(linuxRoot, settings);
        assertTrue(valid, "Linux structure should be detected as a valid game folder");
    }

    @Test
    public void testWindowsStructure(@TempDir Path tempDir) throws IOException {
        Settings settings = new Settings();
        
        Path winRoot = tempDir.resolve("Windows_Starsector");
        Path winCore = winRoot.resolve("starsector-core");
        Files.createDirectories(winCore.resolve("data").resolve("hulls"));
        Files.write(winCore.resolve("data").resolve("hulls").resolve("ship_data.csv"), "".getBytes());
        Files.createDirectories(winCore.resolve("graphics"));
        Files.write(winCore.resolve("starfarer.api.jar"), "".getBytes());
        Files.createDirectories(winRoot.resolve("mods"));
        Files.createDirectories(winRoot.resolve("mods").resolve("SomeMod").resolve("data"));
        Files.createDirectories(winRoot.resolve("mods").resolve("SomeMod").resolve("graphics"));
        Files.write(winRoot.resolve("mods").resolve("SomeMod").resolve("mod_info.json"), "{}".getBytes());
        
        boolean valid = Initializations.checkGameFolderEligibility(winRoot, settings);
        assertTrue(valid, "Windows structure should be detected as a valid game folder");
    }

    @Test
    public void testMacStructure(@TempDir Path tempDir) throws IOException {
        Settings settings = new Settings();
        
        Path macRoot = tempDir.resolve("Starsector.app");
        Path macCore = macRoot.resolve("Contents").resolve("Resources").resolve("Java");
        Files.createDirectories(macCore.resolve("data").resolve("hulls"));
        Files.write(macCore.resolve("data").resolve("hulls").resolve("ship_data.csv"), "".getBytes());
        Files.createDirectories(macCore.resolve("graphics"));
        Files.write(macCore.resolve("starfarer.api.jar"), "".getBytes());
        Files.createDirectories(macRoot.resolve("Contents").resolve("Resources").resolve("mods"));
        Files.createDirectories(macRoot.resolve("Contents").resolve("Resources").resolve("mods").resolve("SomeMod").resolve("data"));
        Files.createDirectories(macRoot.resolve("Contents").resolve("Resources").resolve("mods").resolve("SomeMod").resolve("graphics"));
        Files.write(macRoot.resolve("Contents").resolve("Resources").resolve("mods").resolve("SomeMod").resolve("mod_info.json"), "{}".getBytes());
        
        boolean valid = Initializations.checkGameFolderEligibility(macRoot, settings);
        assertTrue(valid, "Mac structure should be detected as a valid game folder");
    }
}
