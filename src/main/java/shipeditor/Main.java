package shipeditor;

import lombok.extern.log4j.Log4j2;
import shipeditor.components.logging.StandardOutputRedirector;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.persistence.Initializations;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.Errors;
import shipeditor.utility.text.StringConstants;
import shipeditor.utility.UtilityEnums.Theme;
import shipeditor.utility.themes.Themes;

import javax.swing.JPopupMenu;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Locale;
import java.util.List;

import java.util.function.Function;

@Log4j2
public final class Main {

    public static final String VERSION = "0.0.1g";

    private Main() {}

    private static void checkAndRelaunch(String[] args) {
        if (Boolean.getBoolean("shipeditor.relaunched")) {
            return;
        }

        long maxMemory = Runtime.getRuntime().maxMemory();
        long threshold = 3900L * 1024L * 1024L;

        if (maxMemory < threshold) {
            log.info("Max memory available is {} MB, which is less than the 4 GB required. Relaunching JVM with -Xmx4g...", maxMemory / (1024 * 1024));
            try {
                String javaHome = System.getProperty("java.home");
                String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
                if (System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win")) {
                    javaBin += ".exe";
                }

                List<String> command = new ArrayList<>();
                command.add(javaBin);
                command.add("-Xmx4g");
                command.add("-XX:+UseG1GC");
                command.add("-XX:+UseStringDeduplication");
                command.add("-XX:MinHeapFreeRatio=10");
                command.add("-XX:MaxHeapFreeRatio=20");
                command.add("-Dsun.java2d.opengl=false");
                command.add("-Dsun.java2d.d3d=false");
                command.add("-Dsun.java2d.noddraw=true");
                command.add("-Dsun.awt.noerasebackground=true");
                command.add("-Dshipeditor.relaunched=true");

                var codeSource = Main.class.getProtectionDomain().getCodeSource();
                if (codeSource != null) {
                    File codeLocation = new File(codeSource.getLocation().toURI());
                    if (codeLocation.isFile() && codeLocation.getName().endsWith(".jar")) {
                        command.add("-jar");
                        command.add(codeLocation.getAbsolutePath());
                    } else {
                        command.add("-cp");
                        command.add(System.getProperty("java.class.path"));
                        command.add("shipeditor.Main");
                    }
                } else {
                    command.add("-cp");
                    command.add(System.getProperty("java.class.path"));
                    command.add("shipeditor.Main");
                }

                command.addAll(Arrays.asList(args));
                log.info("Starting child JVM with command: {}", String.join(" ", command));

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.inheritIO();
                Process process = builder.start();
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.exit(process.exitValue());
            } catch (java.io.IOException | java.net.URISyntaxException | SecurityException e) {
                log.error("Failed to relaunch JVM with -Xmx4g", e);
            }
        }
    }

    public static void main(String[] args) {
        checkAndRelaunch(args);
        
        System.setProperty("sun.java2d.opengl", "false");
        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.awt.noerasebackground", "true");
        
        Locale.setDefault(Locale.US);
        SwingUtilities.invokeLater(() -> {
            // These method calls are initialization block; the order of calls is important.
            Initializations.initializeSettingsFile();
            configureLaf();
            PrimaryWindow window = PrimaryWindow.create();
            Initializations.updateStateFromSettings(window);

            Settings settings = SettingsManager.getSettings();

            if (settings.isLoadDataAtStart()) {
                window.showGUI();
                FileLoading.loadGameData();
            } else {
                // No data preload — show window immediately.
                window.showGUI();
            }

            // Bind the error streams AFTER the UI is fully initialized and visible
            // to prevent silent layout crashes on startup!
            StandardOutputRedirector.redirectStandardStreams();
            Errors.initGlobalHandler();
        });
    }

    private static void configureLaf() {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
        UIManager.put("SplitPane.dividerSize", 8);
        UIManager.put("SplitPane.oneTouchButtonSize", 10);
        boolean isLinux = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux");
        if (!isLinux) {
            UIManager.put("TitlePane.useWindowDecorations", true);
        }

        UIManager.put(StringConstants.TREE_PAINT_LINES, true);
        UIManager.put("Tree.showDefaultIcons", true);
        UIManager.put("TitlePane.showIcon", true);
        UIManager.put("TitlePane.showIconInDialogs", true);
        UIManager.put("FileChooser.readOnly", true);

        UIManager.put(Initializations.FILE_CHOOSER_SHORTCUTS_FILES_FUNCTION, (Function<File[], File[]>) files -> {
            ArrayList<File> list = new ArrayList<>( Arrays.asList( files ) );
            list.removeIf(next -> Initializations.SHELL_FOLDER_0_X_12.equals(next.getPath()));
            return list.toArray(new File[0]);
        } );

        Settings settings = SettingsManager.getSettings();
        Theme settingsTheme = settings.getTheme();
        Runnable setterMethod = settingsTheme.getSetterMethod();
        setterMethod.run();

        Themes.setupColors();

        // Force early initialization of ToolTipUI to avoid lazy-loading classloader issues later on EDT
        try {
            new JToolTip().updateUI();
        } catch (RuntimeException ignored) {
        }
    }

}
