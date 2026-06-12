package shipeditor.persistence;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.icons.FlatAbstractIcon;
import lombok.extern.log4j.Log4j2;
import shipeditor.PrimaryWindow;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.ViewerBackgroundChanged;
import shipeditor.parsing.FileUtilities;
import shipeditor.utility.Errors;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

@Log4j2
public final class Initializations {

    public static final String FILE_CHOOSER_SHORTCUTS_FILES_FUNCTION = "FileChooser.shortcuts.filesFunction";
    public static final String SHELL_FOLDER_0_X_12 = "ShellFolder: 0x12";

    private Initializations() {
    }

    @SuppressWarnings({"ProhibitedExceptionThrown", "CallToPrintStackTrace"})
    public static void updateStateFromSettings(PrimaryWindow window) {
        Settings settings = SettingsManager.getSettings();
        EventBus.publish(new ViewerBackgroundChanged(settings.getBackgroundColor()));
        try {
            Initializations.installGameFolderShortcut(window);
            Initializations.installWindowIcon(window);
        } catch (URISyntaxException | IOException e) {
            log.error("Customization of file chooser failed!", e);
            throw new RuntimeException("Customization of file chooser failed!", e);
        }
    }

    private static void installGameFolderShortcut(PrimaryWindow window) throws URISyntaxException, IOException {
        Class<? extends PrimaryWindow> windowClass = window.getClass();
        ClassLoader classLoader = windowClass.getClassLoader();
        String iconName = "gamefolder_icon64.png";

        File iconFile;
        BufferedImage iconImage;
        URL iconPath = Objects.requireNonNull(classLoader.getResource(iconName));
        URI checked = iconPath.toURI();
        if (checked.isOpaque()) {
            try ( InputStream inputStream = Initializations.class.getResourceAsStream("/" + iconName)) {
                if (inputStream != null) {
                    iconImage = ImageIO.read(inputStream);
                } else {
                    throw new RuntimeException("Game folder icon not found!");
                }
            }
        } else {
            iconFile = new File(checked);
            log.info("Loading game folder icon...");
            iconImage = ImageIO.read(iconFile);
        }

        Settings settings = SettingsManager.getSettings();
        String folderPath = settings.getGameFolderPath();

        UIManager.put(FILE_CHOOSER_SHORTCUTS_FILES_FUNCTION, (Function<File[], File[]>) files -> {
            ArrayList<File> list = new ArrayList<>( Arrays.asList( files ) );
            list.removeIf(next -> SHELL_FOLDER_0_X_12.equals(next.getPath()));
            list.add( 0, new File(folderPath));
            return list.toArray(new File[0]);
        } );
        UIManager.put( "FileChooser.shortcuts.displayNameFunction", (Function<File, String>) file -> {
            String absolutePath = file.getAbsolutePath();
            if (absolutePath.equals(folderPath)) {
                return "Game folder";
            }
            return null;
        } );
        UIManager.put( "FileChooser.shortcuts.iconFunction", (Function<File, Icon>) file -> {
            String absolutePath = file.getAbsolutePath();
            if (absolutePath.equals(folderPath)) {
                return new GameFolderIcon(iconImage);
            }
            return null;
        } );
    }

    private static void installWindowIcon(PrimaryWindow window) throws URISyntaxException, IOException {
        Class<? extends PrimaryWindow> windowClass = window.getClass();
        ClassLoader classLoader = windowClass.getClassLoader();
        String iconName = "icon.png";

        File iconFile;
        BufferedImage iconImage;
        URL iconPath = Objects.requireNonNull(classLoader.getResource(iconName));
        URI checked = iconPath.toURI();
        if (checked.isOpaque()) {
            try ( InputStream inputStream = Initializations.class.getResourceAsStream("/" + iconName)) {
                if (inputStream != null) {
                    iconImage = ImageIO.read(inputStream);
                } else {
                    throw new RuntimeException("Window icon not found!");
                }
            }
        } else {
            iconFile = new File(checked);
            log.info("Loading window icon...");
            iconImage = ImageIO.read(iconFile);
        }

        ImageIcon icon = new ImageIcon(iconImage);
        window.setIconImage(icon.getImage());
    }

    private static class GameFolderIcon extends FlatAbstractIcon {

        private final BufferedImage iconImage;

        GameFolderIcon(BufferedImage image) {
            super(64, 64, Color.WHITE);
            this.iconImage = image;
        }

        @Override
        protected void paintIcon(Component c, Graphics2D g2) {
            g2.drawImage(iconImage, 0, 0, width, height, null);
        }

    }

    public static void initializeSettingsFile() {
        ObjectMapper mapper = SettingsManager.getMapperForSettingsFile();
        Settings loaded;
        Path workingDirectory = Paths.get("").toAbsolutePath();
        log.info("Current folder: {}", workingDirectory);
        File settingsFile = SettingsManager.getSettingsPath();
        try {
            if (settingsFile.exists()) {
                log.info("Reading existing settings file...");
                loaded = mapper.readValue(settingsFile, Settings.class);
                if (loaded != null) {
                    String coreFolder = loaded.getCoreFolderPath();
                    if (coreFolder != null && !coreFolder.isEmpty()) {
                        SettingsManager.setCoreFolderName(FileUtilities.extractFolderName(coreFolder));
                    }
                    log.info("Settings read successful.");
                }
            } else {
                log.info("Settings file not found, creating default...");
                loaded = SettingsManager.createDefault();
                mapper.writeValue(settingsFile, loaded);
                if (settingsFile.exists()) {
                    log.info("Default settings file creation successful.");
                }
            }
        } catch (IOException e) {
            Errors.printToStream(e);
            log.error("Failed to resolve settings file, writing default one.", e);
            loaded = SettingsManager.createDefault();
            SettingsManager.writeSettingsToFile(mapper, settingsFile, loaded);
        }
        SettingsManager.setSettings(loaded);
        SettingsManager.getCorePackage();
    }

    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private static List<Path> getPotentialGameFolders() {
        List<Path> paths = new ArrayList<>();

        String os = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
        boolean isWindows = os.contains("win");
        boolean isMac = os.contains("mac");

        if (isWindows) {
            paths.add(Paths.get("C:\\Games\\Starsector"));
            paths.add(Paths.get("C:\\Program Files (x86)\\Fractal Softworks\\Starsector"));
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty()) {
            paths.add(Paths.get(userHome, "Games", "Starsector"));
            paths.add(Paths.get(userHome, "Games", "starsector"));
            paths.add(Paths.get(userHome, ".local", "share", "starsector"));
            paths.add(Paths.get(userHome, ".local", "share", "Starsector"));
            paths.add(Paths.get(userHome, "starsector"));
            paths.add(Paths.get(userHome, "Starsector"));
        }

        if (!isWindows && !isMac) {
            paths.add(Paths.get("/opt/starsector"));
            paths.add(Paths.get("/opt/Starsector"));
        }

        if (isMac) {
            paths.add(Paths.get("/Applications/Starsector.app"));
        }

        if (isWindows) {
            File[] roots = File.listRoots();
            if (roots != null) {
                for (File root : roots) {
                    try {
                        Path rootPath = root.toPath();
                        paths.add(rootPath.resolve("Games/Starsector"));
                        paths.add(rootPath.resolve("Games/starsector"));
                        paths.add(rootPath.resolve("Starsector"));
                        paths.add(rootPath.resolve("starsector"));
                    } catch (Throwable t) {
                        log.warn("Failed to resolve path for drive root: {}", root, t);
                    }
                }
            }
        }

        if (!isWindows) {
            String[] mountDirs = {"/media", "/mnt", "/Volumes", "/run/media"};
            for (String mountDir : mountDirs) {
                File mDir = new File(mountDir);
                if (mDir.exists() && mDir.isDirectory()) {
                    File[] userMounts = mDir.listFiles();
                    if (userMounts != null) {
                        for (File uMount : userMounts) {
                            if (uMount.isDirectory()) {
                                try {
                                    Path uMountPath = uMount.toPath();
                                    paths.add(uMountPath.resolve("Games/Starsector"));
                                    paths.add(uMountPath.resolve("Games/starsector"));
                                    paths.add(uMountPath.resolve("Starsector"));
                                    paths.add(uMountPath.resolve("starsector"));

                                    File[] subMounts = uMount.listFiles();
                                    if (subMounts != null) {
                                        for (File sub : subMounts) {
                                            if (sub.isDirectory()) {
                                                try {
                                                    Path subPath = sub.toPath();
                                                    paths.add(subPath.resolve("Games/Starsector"));
                                                    paths.add(subPath.resolve("Games/starsector"));
                                                    paths.add(subPath.resolve("Starsector"));
                                                    paths.add(subPath.resolve("starsector"));
                                                } catch (Throwable t) {
                                                    log.warn("Failed to resolve path for submount: {}", sub, t);
                                                }
                                            }
                                        }
                                    }
                                } catch (Throwable t) {
                                    log.warn("Failed to resolve path for mount: {}", uMount, t);
                                }
                            }
                        }
                    }
                }
            }
        }

        return paths;
    }

    public static void selectGameFolder() {
        Settings settings = SettingsManager.getSettings();
        String gameFolderPath = settings.getGameFolderPath();
        if (gameFolderPath != null && !gameFolderPath.isEmpty()) {
            Path gameFolder = Paths.get(gameFolderPath);
            if (Files.exists(gameFolder) && Files.isDirectory(gameFolder) && checkGameFolderEligibility(gameFolder, settings)) {
                return;
            } else {
                log.warn("Configured game folder path is invalid or missing: {}", gameFolderPath);
                settings.setGameFolderPath("");
            }
        }

        List<Path> potentialPaths = Initializations.getPotentialGameFolders();

        String detectedPath = null;
        for (Path potentialFolderPath : potentialPaths) {
            if (Files.exists(potentialFolderPath) && Files.isDirectory(potentialFolderPath)) {
                if (Initializations.checkGameFolderEligibility(potentialFolderPath, settings)) {
                    log.info("Auto-detected valid path: {}", potentialFolderPath);
                    detectedPath = potentialFolderPath.toAbsolutePath().toString();
                    break;
                }
            }
        }

        if (java.awt.GraphicsEnvironment.isHeadless()) {
            if (detectedPath != null) {
                log.info("Saving game folder path from predefined paths in headless mode: {}", detectedPath);
                settings.setGameFolderPath(detectedPath);
                return;
            }
            settings.setGameFolderPath("");
            throw new RuntimeException("Game folder selection failed! No predefined path found and cannot open setup dialog in headless mode.");
        }

        String confirmedPath = shipeditor.components.dialogs.FirstTimeSetupDialog.promptForGameFolder(detectedPath, settings);
        log.info("Saving game folder path: {}", confirmedPath);
        settings.setGameFolderPath(confirmedPath);
    }

    public static boolean checkGameFolderEligibility(Path filePath, Settings settings) {
        boolean folderHasCore = false;
        boolean folderHasMods = false;
        try (Stream<Path> pathStream = Files.walk(filePath, 5)) {
            Stream<Path> filtered = pathStream.filter(Files::isDirectory);
            for (Path path : filtered.toArray(Path[]::new)) {
                Path fileNamePath = path.getFileName();
                if (fileNamePath == null) continue;
                String folderName = fileNamePath.toString();
                String coreFolderPath = path.toString();

                if (Initializations.isCoreFolder(path)) {
                    settings.setCoreFolderPath(coreFolderPath);
                    SettingsManager.setCoreFolderName(FileUtilities.extractFolderName(coreFolderPath));
                    folderHasCore = true;
                }

                if ("mods".equals(folderName)) {
                    settings.setModFolderPath(coreFolderPath);
                    folderHasMods = true;
                }
            }
        } catch (IOException e) {
            Errors.printToStream(e);
            return false;
        }
        return folderHasCore && folderHasMods;
    }

    private static boolean isCoreFolder(Path folderPath) {
        if (!Files.isDirectory(folderPath)) {
            return false;
        }
        
        Path fileNamePath = folderPath.getFileName();
        if (fileNamePath == null) return false;

        String[] validParentFolderNames = {"starsector-core", "Java", "starsector"};
        String parentFolderName = fileNamePath.toString();
        boolean isValidParentFolder = false;

        for (String validName : validParentFolderNames) {
            if (parentFolderName.equals(validName)) {
                isValidParentFolder = true;
                break;
            }
        }

        if (!isValidParentFolder) {
            return false;
        }

        String[] childFolderNames = {"data", "graphics"};

        for (String childFolderName : childFolderNames) {
            Path childFolderPath = folderPath.resolve(childFolderName);
            if (!Files.isDirectory(childFolderPath)) {
                return false;
            }
        }

        return true;
    }

}
