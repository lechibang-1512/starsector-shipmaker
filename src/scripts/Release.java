package scripts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Release {

    private static final String POM_PATH = "pom.xml";
    private static final String SETTINGS_MANAGER_PATH = "src/main/java/shipeditor/persistence/SettingsManager.java";
    private static final String MAIN_PATH = "src/main/java/shipeditor/Main.java";
    private static final String CHANGELOG_PATH = "CHANGELOG.md";

    private static final Map<String, String> MANDATORY_RELEASE_FILES = Map.of(
            "ship_editor.bat", "ship_editor.bat",
            "ship_editor.sh", "ship_editor.sh",
            "ship_editor.command", "ship_editor.command",
            "CHANGELOG.md", "CHANGELOG.md",
            "LICENSE", "LICENSE",
            "README.md", "README.md"
    );

    public static void main(String[] args) {
        boolean isDryRun = Arrays.asList(args).contains("--dry-run");
        boolean noGit = Arrays.asList(args).contains("--no-git");
        boolean allowDirty = Arrays.asList(args).contains("--allow-dirty");
        
        String versionArg = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--version") && i + 1 < args.length) {
                versionArg = args[i + 1];
                break;
            } else if (args[i].startsWith("--version=")) {
                versionArg = args[i].substring("--version=".length());
                break;
            }
        }

        // 1. Dependency Checks
        if (!checkCommand("git")) {
            System.err.println("Error: git is not installed or not in PATH.");
            System.exit(1);
        }
        if (!checkCommand("mvn")) {
            System.err.println("Error: maven (mvn) is not installed or not in PATH.");
            System.exit(1);
        }

        // 2. Check Git Clean status
        if (!allowDirty && !isDryRun && !isGitClean()) {
            System.err.println("Error: Git repository has uncommitted changes. Please commit, stash, or run with --allow-dirty.");
            System.exit(1);
        }

        // 3. Determine versions
        String currentVersion = getCurrentVersion();
        if (currentVersion == null) {
            System.err.println("Error: Could not extract current version from pom.xml.");
            System.exit(1);
        }

        String targetVersion = null;
        String targetDate = null;
        List<String> changelogLines = new ArrayList<>();

        if (versionArg != null) {
            targetVersion = versionArg;
            targetDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            System.out.println("Current version: " + currentVersion);
        } else {
            ReleaseConfig config = interactiveConsole(currentVersion);
            if (config == null || config.version == null || config.version.trim().isEmpty()) {
                System.out.println("\nAborted by user.");
                System.exit(1);
                return;
            }
            targetVersion = config.version;
            targetDate = config.date;
            changelogLines = config.changelog;
        }

        System.out.println("Target release version: " + targetVersion);
        System.out.println("Target release date: " + targetDate);

        Map<String, String> originalFilesBackup = new HashMap<>();

        try {
            // 4. Update Versions
            System.out.println("Bumping version from " + currentVersion + " to " + targetVersion + "...");
            for (String path : Arrays.asList(POM_PATH, SETTINGS_MANAGER_PATH, MAIN_PATH, CHANGELOG_PATH)) {
                File f = new File(path);
                if (f.exists()) {
                    originalFilesBackup.put(path, Files.readString(f.toPath(), StandardCharsets.UTF_8));
                }
            }

            updateVersionInFiles(targetVersion);
            updateChangelog(targetVersion, targetDate, changelogLines);

            // 5. Build
            buildProject();

            // 6. Package
            packageRelease(targetVersion);

        } catch (Exception e) {
            System.err.println("\nAn error occurred during build/packaging: " + e.getMessage());
            e.printStackTrace();
            restoreBackups(originalFilesBackup);
            System.exit(1);
        }

        // 7. Git Operations / Reverts
        if (isDryRun) {
            System.out.println("\nDry-run mode active.");
            restoreBackups(originalFilesBackup);
            System.out.println("Dry-run complete. Built archive is preserved in releases/.");
        } else if (noGit) {
            System.out.println("\nSkipping Git operations as requested (--no-git).");
        } else {
            commitChanges(targetVersion);
        }

        System.out.println("\nRelease management workflow completed successfully!");
    }

    private static boolean checkCommand(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void runCmd(String[] cmdArray, boolean check, boolean captureOutput) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null || javaHome.isEmpty()) {
            javaHome = System.getProperty("java.home");
        }
        if (javaHome != null && !javaHome.isEmpty()) {
            pb.environment().put("JAVA_HOME", javaHome);
        }
        if (!captureOutput) {
            pb.inheritIO();
        } else {
            pb.redirectErrorStream(true);
        }
        
        Process p = pb.start();
        
        String output = "";
        if (captureOutput) {
            try (Scanner s = new Scanner(p.getInputStream(), StandardCharsets.UTF_8)) {
                s.useDelimiter("\\A");
                output = s.hasNext() ? s.next() : "";
            }
        }
        
        int exitCode = p.waitFor();
        if (check && exitCode != 0) {
            System.out.println("Error executing command: " + String.join(" ", cmdArray));
            if (captureOutput) {
                System.out.println("Output:\n" + output);
            }
            System.exit(exitCode);
        }
    }

    private static boolean isGitClean() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
            Process p = pb.start();
            try (Scanner s = new Scanner(p.getInputStream(), StandardCharsets.UTF_8)) {
                s.useDelimiter("\\A");
                String output = s.hasNext() ? s.next() : "";
                return output.trim().isEmpty();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static String getCurrentVersion() {
        File pomFile = new File(POM_PATH);
        if (!pomFile.exists()) {
            System.err.println("Error: pom.xml not found in current directory.");
            return null;
        }
        try {
            String content = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
            Matcher m1 = Pattern.compile("<artifactId>ship_editor</artifactId>\\s*<version>([^<]+)</version>").matcher(content);
            if (m1.find()) {
                return m1.group(1);
            }
            Matcher m2 = Pattern.compile("<version>([^<]+)</version>").matcher(content);
            if (m2.find()) {
                return m2.group(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void updateVersionInFiles(String newVersion) throws IOException {
        replaceInFile(POM_PATH, 
                "(<artifactId>ship_editor</artifactId>\\s*<version>)([^<]+)(</version>)", 
                "$1" + newVersion + "$3");
        
        replaceInFile(SETTINGS_MANAGER_PATH, 
                "(private static final String projectVersion = \")([^\"]+)(\";)", 
                "$1" + newVersion + "$3");
        
        replaceInFile(MAIN_PATH, 
                "(public static final String VERSION = \")([^\"]+)(\";)", 
                "$1" + newVersion + "$3");
    }

    private static void replaceInFile(String filepath, String regex, String replacement) throws IOException {
        File f = new File(filepath);
        if (!f.exists()) {
            throw new RuntimeException("Source file not found for version update: " + filepath);
        }
        String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
        
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(content);
        if (!m.find()) {
            throw new RuntimeException("Could not locate version target in file: " + filepath);
        }
        
        String newContent = m.replaceAll(replacement);
        Files.writeString(f.toPath(), newContent, StandardCharsets.UTF_8);
    }

    private static void updateChangelog(String targetVersion, String targetDate, List<String> changelogLines) throws IOException {
        File f = new File(CHANGELOG_PATH);
        if (!f.exists()) {
            System.out.println("Warning: " + CHANGELOG_PATH + " not found.");
            return;
        }
        
        String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
        if (Pattern.compile("##\\s*\\[" + Pattern.quote(targetVersion) + "\\]").matcher(content).find()) {
            System.out.println("CHANGELOG.md already has section for " + targetVersion + ".");
            return;
        }

        if (content.contains("## [Unreleased]")) {
            StringBuilder body = new StringBuilder();
            if (changelogLines != null && !changelogLines.isEmpty()) {
                body.append("\n\n### Features\n");
                for (String line : changelogLines) {
                    body.append(line).append("\n");
                }
            } else {
                body.append("\n");
            }
            
            String newHeader = "## [Unreleased]\n\n## [" + targetVersion + "] - " + targetDate + body.toString();
            String newContent = content.replaceFirst("## \\[Unreleased\\]", Matcher.quoteReplacement(newHeader));
            Files.writeString(f.toPath(), newContent, StandardCharsets.UTF_8);
            System.out.println("Automatically updated CHANGELOG.md with version and date.");
        } else {
            System.out.println("Warning: '## [Unreleased]' not found in CHANGELOG.md. Skipping automated update.");
        }
    }

    private static void buildProject() throws Exception {
        System.out.println("Building application with Maven...");
        String[] cmdArray;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            cmdArray = new String[]{"cmd.exe", "/c", "mvn clean package -DskipTests"};
        } else {
            cmdArray = new String[]{"sh", "-c", "mvn clean package -DskipTests"};
        }
        runCmd(cmdArray, true, false);
    }

    private static void packageRelease(String version) throws IOException {
        File releasesDir = new File("releases");
        if (!releasesDir.exists()) {
            releasesDir.mkdirs();
        }
        
        String zipFilename = "releases/ship-editor-" + version + ".zip";
        System.out.println("Packaging release artifacts to " + zipFilename + "...");
        
        String folderPrefix = "ship-editor-" + version + "/";
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilename))) {
            String jarSrc = "ship_editor.jar";
            if (!new File(jarSrc).exists()) {
                jarSrc = "target/ship_editor-" + version + ".jar";
            }
            if (!new File(jarSrc).exists()) {
                System.err.println("Error: Built JAR '" + jarSrc + "' is missing. Did the build fail?");
                System.exit(1);
            }
            
            addFileToZip(zos, jarSrc, folderPrefix + "ship_editor.jar");
            System.out.println("  + Added: " + jarSrc + " -> " + folderPrefix + "ship_editor.jar");
            
            for (Map.Entry<String, String> entry : MANDATORY_RELEASE_FILES.entrySet()) {
                String srcPath = entry.getKey();
                String relPath = entry.getValue();
                
                if (!new File(srcPath).exists()) {
                    System.err.println("Error: Mandatory release component '" + srcPath + "' is missing.");
                    System.exit(1);
                }
                
                String targetPath = folderPrefix + (relPath != null ? relPath : new File(srcPath).getName());
                addFileToZip(zos, srcPath, targetPath);
                System.out.println("  + Added: " + srcPath + " -> " + targetPath);
            }
        }
        
        System.out.println("Successfully created release package: " + zipFilename);
    }

    private static void addFileToZip(ZipOutputStream zos, String fileToZip, String zipEntryName) throws IOException {
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        }
    }

    private static void restoreBackups(Map<String, String> backups) {
        System.out.println("\nRestoring original version strings...");
        for (Map.Entry<String, String> entry : backups.entrySet()) {
            String path = entry.getKey();
            String content = entry.getValue();
            if (new File(path).exists() || content != null) {
                try {
                    Files.writeString(Paths.get(path), content, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    System.err.println("Failed to restore backup for " + path);
                }
            }
        }
    }

    private static void commitChanges(String targetVersion) {
        System.out.println("\nCommitting version changes...");
        try {
            runCmd(new String[]{"git", "add", POM_PATH, SETTINGS_MANAGER_PATH, MAIN_PATH, CHANGELOG_PATH}, true, true);
            
            ProcessBuilder pb = new ProcessBuilder("git", "diff", "--cached", "--name-only");
            Process p = pb.start();
            Scanner s = new Scanner(p.getInputStream(), StandardCharsets.UTF_8);
            s.useDelimiter("\\A");
            String diffOutput = s.hasNext() ? s.next() : "";
            s.close();
            p.waitFor();
            
            if (!diffOutput.trim().isEmpty()) {
                runCmd(new String[]{"git", "commit", "-m", "Release v" + targetVersion}, true, true);
                System.out.println("Successfully committed v" + targetVersion + " in git.");
            } else {
                System.out.println("No changes to commit (files already match target version).");
            }
        } catch (Exception e) {
            System.err.println("Error committing changes: " + e.getMessage());
        }
    }

    private static class ReleaseConfig {
        String version;
        String date;
        List<String> changelog = new ArrayList<>();
    }

    private static ReleaseConfig interactiveConsole(String currentVersion) {
        ReleaseConfig config = new ReleaseConfig();
        CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Release Configuration");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(500, 450);
            frame.setLocationRelativeTo(null);

            JPanel panel = new JPanel(null);

            JLabel currVerLabel = new JLabel("Current version: " + currentVersion);
            currVerLabel.setFont(currVerLabel.getFont().deriveFont(java.awt.Font.BOLD));
            currVerLabel.setBounds(20, 20, 400, 25);
            panel.add(currVerLabel);

            JLabel targetVerLabel = new JLabel("Target Version (e.g. x.y.z-[suffix]):");
            targetVerLabel.setBounds(20, 60, 400, 25);
            panel.add(targetVerLabel);

            String suggested = currentVersion;
            Matcher m1 = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)-(.*)$").matcher(currentVersion);
            if (m1.matches()) {
                suggested = m1.group(1) + "." + m1.group(2) + "." + m1.group(3) + "-" + m1.group(4);
            } else {
                Matcher m2 = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)([a-zA-Z]*)$").matcher(currentVersion);
                if (m2.matches()) {
                    suggested = m2.group(1) + "." + m2.group(2) + "." + m2.group(3) + "-" + m2.group(4);
                }
            }

            JTextField targetVerField = new JTextField(suggested);
            targetVerField.setBounds(20, 85, 440, 25);
            panel.add(targetVerField);

            JLabel dateLabel = new JLabel("Release Date (YYYY-MM-DD):");
            dateLabel.setBounds(20, 120, 400, 25);
            panel.add(dateLabel);

            JTextField dateField = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            dateField.setBounds(20, 145, 440, 25);
            panel.add(dateField);

            JLabel changelogLabel = new JLabel("Changelog Entries (each line will be bulleted):");
            changelogLabel.setBounds(20, 180, 400, 25);
            panel.add(changelogLabel);

            JTextArea changelogArea = new JTextArea();
            JScrollPane scrollPane = new JScrollPane(changelogArea);
            scrollPane.setBounds(20, 205, 440, 130);
            panel.add(scrollPane);

            JButton submitBtn = new JButton("Submit");
            submitBtn.setBounds(370, 355, 90, 30);
            submitBtn.addActionListener(e -> {
                String v = targetVerField.getText().trim();
                String d = dateField.getText().trim();
                if (v.isEmpty() || d.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Version and Date cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                config.version = v;
                config.date = d;
                String[] lines = changelogArea.getText().split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        if (!line.startsWith("-")) {
                            line = "- " + line;
                        }
                        config.changelog.add(line);
                    }
                }

                frame.dispose();
                latch.countDown();
            });
            panel.add(submitBtn);

            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.setBounds(270, 355, 90, 30);
            cancelBtn.addActionListener(e -> {
                frame.dispose();
                latch.countDown();
            });
            panel.add(cancelBtn);

            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    latch.countDown();
                }
            });

            frame.setContentPane(panel);
            frame.setVisible(true);
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return config;
    }
}
