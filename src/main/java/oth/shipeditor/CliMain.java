package oth.shipeditor;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.parsing.loading.FileLoading;
import oth.shipeditor.parsing.loading.IndexScannerTask;
import oth.shipeditor.persistence.Initializations;
import oth.shipeditor.utility.Errors;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class CliMain {

    public static void main(String[] args) {
        // Enforce headless mode
        System.setProperty("java.awt.headless", "true");
        System.out.println("Starting Starsector Ship Editor CLI...");

        if (args.length == 0 || Arrays.asList(args).contains("--help")) {
            printHelp();
            System.exit(0);
        }

        // Initialize core backend functionality
        Errors.initGlobalHandler();
        Initializations.initializeSettingsFile();

        // Process CLI arguments
        if (Arrays.asList(args).contains("--init-db")) {
            System.out.println("Initializing and indexing database...");
            try {
                IndexScannerTask.scanAndIndexAll();
                System.out.println("Database initialization completed successfully.");
                System.exit(0);
            } catch (Exception e) {
                System.err.println("Database initialization failed with an exception:");
                e.printStackTrace();
                System.exit(1);
            }
        } else if (Arrays.asList(args).contains("--validate")) {
            System.out.println("Running data validation (loading game data headlessly)...");
            try {
                System.out.println("Checking database index first...");
                IndexScannerTask.scanAndIndexAll();
                System.out.println("Loading game data...");
                CompletableFuture<?> future = FileLoading.loadGameData();
                future.join();
                System.out.println("Validation completed successfully.");
                System.exit(0);
            } catch (Exception e) {
                System.err.println("Validation failed with an exception:");
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            System.err.println("Unknown arguments: " + Arrays.toString(args));
            printHelp();
            System.exit(1);
        }
    }

    private static void printHelp() {
        System.out.println("Usage: java -cp ship_editor.jar oth.shipeditor.CliMain [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --init-db       Initialize or update the database index of installed mods.");
        System.out.println("  --validate      Update the database index and validate all game data without launching the GUI.");
        System.out.println("  --help          Print this help message.");
    }
}
