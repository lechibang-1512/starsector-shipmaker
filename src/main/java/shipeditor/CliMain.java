package shipeditor;

import lombok.extern.log4j.Log4j2;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.parsing.loading.IndexScannerTask;
import shipeditor.persistence.Initializations;
import shipeditor.utility.Errors;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class CliMain {

    public static void main(String[] args) {
        // Enforce headless mode
        System.setProperty("java.awt.headless", "true");
        log.info("Starting Starsector Ship Editor CLI...");

        if (args.length == 0 || Arrays.asList(args).contains("--help")) {
            printHelp();
            System.exit(0);
        }

        // Initialize core backend functionality
        Errors.initGlobalHandler();
        Initializations.initializeSettingsFile();
        Initializations.selectGameFolder();

        // Process CLI arguments
        if (Arrays.asList(args).contains("--init-db")) {
            log.info("Initializing and indexing database...");
            try {
                IndexScannerTask.scanAndIndexAll(false);
                log.info("Database initialization completed successfully.");
                System.exit(0);
            } catch (RuntimeException e) {
                log.error("Database initialization failed with an exception:", e);
                System.exit(1);
            }
        } else if (Arrays.asList(args).contains("--validate")) {
            log.info("Running data validation (loading game data headlessly)...");
            try {
                log.info("Checking database index first...");
                IndexScannerTask.scanAndIndexAll(false);
                log.info("Loading game data...");
                CompletableFuture<?> future = FileLoading.loadGameData();
                future.join();
                log.info("Validation completed successfully.");
                System.exit(0);
            } catch (RuntimeException e) {
                log.error("Validation failed with an exception:", e);
                System.exit(1);
            }
        } else {
            log.error("Unknown arguments: {}", Arrays.toString(args));
            printHelp();
            System.exit(1);
        }
    }

    private static void printHelp() {
        log.info("Usage: java -cp ship_editor.jar shipeditor.CliMain [options]");
        log.info("");
        log.info("Options:");
        log.info("  --init-db       Initialize or update the database index of installed mods.");
        log.info("  --validate      Update the database index and validate all game data without launching the GUI.");
        log.info("  --help          Print this help message.");
    }
}
