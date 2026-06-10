package shipeditor.utility;

import lombok.extern.log4j.Log4j2;
import shipeditor.components.logging.StandardOutputRedirector;
import shipeditor.persistence.Settings;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.text.StringValues;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileNotFoundException;

@Log4j2
public final class Errors {

    private Errors() {
    }

    public static void initGlobalHandler() {
        Thread.UncaughtExceptionHandler globalExceptionHandler = new Handler();
        Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
    }

    public static void showFileError(String message) {
        Errors.showFileError(message, null);
    }

    public static void showFileError(String message, Throwable exception) {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            log.error("File error (headless): {}", message);
            if (exception != null) {
                Errors.printToStream(exception);
            }
            return;
        }

        Object[] options = {"OK", "Hide file errors"};
        int result = JOptionPane.showOptionDialog(
                null,
                message,
                StringValues.FILE_LOADING_ERROR,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0]);

        if (result == 1) {
            Settings settings = SettingsManager.getSettings();
            settings.setShowLoadingErrors(false);
            log.info("File error pop-ups are disabled by user.");
        }

        if (exception != null) {
            Errors.printToStream(exception);
        }
    }

    public static void showFileOpeningError(File toOpen, Throwable exception) {
        String filePath = toOpen.getAbsolutePath();

        log.error("Failed to open {} in Explorer!", filePath);
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    "Failed to open file in Explorer, exception thrown at: " + filePath,
                    StringValues.FILE_LOADING_ERROR,
                    JOptionPane.ERROR_MESSAGE);
        }
        Errors.printToStream(exception);
    }

    static void showSpriteNotFound(String filePath) {
        String report = "Image file not found: " + filePath;
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    report,
                    StringValues.FILE_LOADING_ERROR,
                    JOptionPane.ERROR_MESSAGE);
        } else {
            log.error(report);
        }
        FileNotFoundException notFoundException = new FileNotFoundException(report);
        Errors.printToStream(notFoundException);
    }

    public static void printToStream(Throwable throwable) {
        java.io.PrintStream errorStream = StandardOutputRedirector.getErrorStreamProxy();
        if (errorStream != null) {
            throwable.printStackTrace(errorStream);
        } else {
            throwable.printStackTrace(System.err);
        }
    }

    private static class Handler implements Thread.UncaughtExceptionHandler {

        public void uncaughtException(Thread t, Throwable e) {
            try {
                log.error("Exception caught with global handler!", e);
                Errors.printToStream(e);
            } catch (Throwable fallback) {
                System.err.println("Uncaught exception failed to log!");
                e.printStackTrace(System.err);
                fallback.printStackTrace(System.err);
            }
        }
    }

}
