package oth.shipeditor;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.components.logging.StandardOutputRedirector;
import oth.shipeditor.parsing.loading.FileLoading;
import oth.shipeditor.persistence.Initializations;
import oth.shipeditor.persistence.Settings;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.utility.Errors;
import oth.shipeditor.utility.text.StringConstants;
import oth.shipeditor.utility.themes.Theme;
import oth.shipeditor.utility.themes.Themes;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Locale;
import java.util.List;

import java.util.function.Function;

@Log4j2
public final class Main {

    public static final String VERSION = "0.0.1c";

    private Main() {}

    private static void checkAndRelaunch(String[] args) {
        if (Boolean.getBoolean("oth.shipeditor.relaunched")) {
            return;
        }

        long maxMemory = Runtime.getRuntime().maxMemory();
        long threshold = 1100L * 1024L * 1024L;

        if (maxMemory > threshold) {
            System.out.println("Max memory available is " + (maxMemory / (1024 * 1024)) + " MB, which exceeds the 1 GB limit. Relaunching JVM with -Xmx1g...");
            System.out.flush();
            log.info("Max memory available is {} MB, which exceeds the 1 GB limit. Relaunching JVM with -Xmx1g...", maxMemory / (1024 * 1024));
            try {
                String javaHome = System.getProperty("java.home");
                String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    javaBin += ".exe";
                }

                List<String> command = new ArrayList<>();
                command.add(javaBin);
                command.add("-Xmx1g");
                command.add("-XX:+UseG1GC");
                command.add("-XX:+UseStringDeduplication");
                command.add("-XX:MinHeapFreeRatio=10");
                command.add("-XX:MaxHeapFreeRatio=20");
                command.add("-Doth.shipeditor.relaunched=true");

                var codeSource = Main.class.getProtectionDomain().getCodeSource();
                if (codeSource != null) {
                    File codeLocation = new File(codeSource.getLocation().toURI());
                    if (codeLocation.isFile() && codeLocation.getName().endsWith(".jar")) {
                        command.add("-jar");
                        command.add(codeLocation.getAbsolutePath());
                    } else {
                        command.add("-cp");
                        command.add(System.getProperty("java.class.path"));
                        command.add("oth.shipeditor.Main");
                    }
                } else {
                    command.add("-cp");
                    command.add(System.getProperty("java.class.path"));
                    command.add("oth.shipeditor.Main");
                }

                command.addAll(Arrays.asList(args));
                System.out.println("Starting child JVM with command: " + String.join(" ", command));
                System.out.flush();

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.inheritIO();
                builder.start();

                System.exit(0);
            } catch (Exception e) {
                log.error("Failed to relaunch JVM with -Xmx1g", e);
            }
        }
    }

    public static void main(String[] args) {
        checkAndRelaunch(args);
        Locale.setDefault(Locale.US);
        SwingUtilities.invokeLater(() -> {
            // These method calls are initialization block; the order of calls is important.
            StandardOutputRedirector.redirectStandardStreams();
            Errors.initGlobalHandler();
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
        });
    }

    private static void configureLaf() {
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
        UIManager.put("SplitPane.dividerSize", 8);
        UIManager.put("SplitPane.oneTouchButtonSize", 10);
        UIManager.put("TitlePane.useWindowDecorations", true);

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
    }

}
