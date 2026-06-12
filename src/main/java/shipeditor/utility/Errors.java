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
            log.error(StringValues.FILE_ERROR_HEADLESS, message);
            if (exception != null && SettingsManager.isDeveloperModeEnabled()) {
                Errors.printToStream(exception);
            }
            return;
        }

        Object[] options = {StringValues.OPTION_OK, StringValues.OPTION_HIDE_FILE_ERRORS};
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
            if (SettingsManager.isDeveloperModeEnabled()) {
                log.info(StringValues.FILE_ERRORS_DISABLED_LOG);
            }
        }

        if (exception != null && SettingsManager.isDeveloperModeEnabled()) {
            Errors.printToStream(exception);
        }
    }

    public static void showFileOpeningError(File toOpen, Throwable exception) {
        String filePath = toOpen.getAbsolutePath();

        if (SettingsManager.isDeveloperModeEnabled()) {
            log.error(StringValues.FAILED_TO_OPEN_IN_EXPLORER, filePath);
        }
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    StringValues.FAILED_TO_OPEN_IN_EXPLORER_UI + filePath,
                    StringValues.FILE_LOADING_ERROR,
                    JOptionPane.ERROR_MESSAGE);
        }
        if (SettingsManager.isDeveloperModeEnabled()) {
            Errors.printToStream(exception);
        }
    }

    static void showSpriteNotFound(String filePath) {
        String report = StringValues.IMAGE_FILE_NOT_FOUND + filePath;
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    report,
                    StringValues.FILE_LOADING_ERROR,
                    JOptionPane.ERROR_MESSAGE);
        } else {
            log.error(report);
        }
        if (SettingsManager.isDeveloperModeEnabled()) {
            FileNotFoundException notFoundException = new FileNotFoundException(report);
            Errors.printToStream(notFoundException);
        }
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
                log.error(StringValues.EXCEPTION_GLOBAL_HANDLER, e);
                Errors.printToStream(e);
            } catch (Throwable fallback) {
                System.err.println(StringValues.UNCAUGHT_EXCEPTION_FAILED_LOG);
                e.printStackTrace(System.err);
                fallback.printStackTrace(System.err);
            }
        }
    }

}
