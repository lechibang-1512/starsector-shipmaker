# Ship-Editor 
**Developed and maintained by:** thevolkflower

Visualizer and editor of object data in JSON and CSV format. Developed as utility tool for the purposes of working with data files of a game Starsector.

Clean repository based on ontheheaven's original repo: https://github.com/Ontheheavens/Ship-Editor/


## Stack:

 - Java 17
 - Swing
 - Maven
 - Jackson
 - Lombok
 - Log4j2

## Used libraries:

 - JavaGL: https://github.com/javagl/Viewer
 - Ikonli: https://github.com/kordamp/ikonli
 - Flatlaf: https://github.com/JFormDesigner/FlatLaf

## Installation & Running (For Modders)

To run the Ship Editor, you need a **Java Runtime Environment (JRE) version 21**.

### Step 1: Install Java 21 (Recommended)
You need to install Java 21 using a standard setup wizard. Any of the following trusted, open-source distributions will work:

*   **Option A: Eclipse Temurin (Adoptium)** — Highly recommended and lightweight:
    1. Go to the **[Eclipse Temurin Java 21 Releases](https://adoptium.net/temurin/releases/?version=21)** page.
    2. Select your Operating System (Windows, macOS, or Linux).
    3. Set the **Package Type** filter to **JRE** (this contains just what is needed to run the app, keeping the download small).
    4. Download the installer package: `.msi` for Windows, or `.pkg` for macOS.
*   **Option B: Microsoft Build of OpenJDK** — Extremely stable, great for Windows & macOS:
    1. Go to the **[Microsoft Build of OpenJDK 21](https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-21)** page.
    2. Locate the download section for **OpenJDK 21**.
    3. Download the installer package: `.msi` for Windows, or `.pkg` for macOS.

*Note: During installation, make sure the option to **"Add to PATH"** or **"Set JAVA_HOME"** is checked (these are usually checked by default). This registers Java with your operating system.*

### Step 2: Run the Editor
Once Java is installed, download the release files and run the appropriate startup script:
*   **Windows**: Double-click **`ship_editor.bat`**.
*   **Linux / macOS**: Run **`ship_editor.sh`** or **`ship_editor.command`**.

---

### Alternative: Local JRE (Portable Setup)
If you prefer not to install Java system-wide, you can run the editor using a local folder:
1. Go to the **[Eclipse Temurin Java 21 Releases](https://adoptium.net/temurin/releases/?version=21)** page.
2. Select your Operating System and download the `.zip` archive (Windows) or `.tar.gz` archive (Linux/macOS). Make sure the package type is set to **JRE**.
3. Extract the downloaded archive.
4. Rename the extracted folder (e.g., `jdk-21.0.x+xx-jre` or `jre-21.x.x`) to exactly **`jre`**.
5. Place this **`jre`** folder directly in the root directory of the application (alongside `ship_editor.jar`).
6. Launch using the startup scripts (`ship_editor.bat` or `ship_editor.sh`), which will automatically detect and run from the local folder.

---

## Building from Source (For Developers)

If you wish to compile the project yourself:

### Prerequisites
- **Java Development Kit (JDK)**: JDK 17 or higher (JDK 21 recommended).
- **Maven**: Ensure Maven is installed on your system.

### Compiling
To compile the project and generate the executable fat JAR, run:
```bash
mvn clean package -DskipTests
```
This builds the application and outputs the executable `ship_editor.jar` directly into the project root directory.

### Running Developer Build
To run the compiled JAR:
```bash
java -jar ship_editor.jar
```

### Managing Releases
To automate a new release locally without relying on GitHub:
```bash
python3 scripts/release.py
```
This script will:
1. Extract the current version from `pom.xml` and prompt you for the target release version.
2. Verify that `CHANGELOG.md` has an entry for the target version.
3. Automatically bump the version in `pom.xml` and Java source files (`Main.java`, `SettingsManager.java`).
4. Compile and package the application using Maven.
5. Create a standalone portable ZIP archive (e.g., `releases/ship-editor-0.0.1d.zip`) containing the fat JAR, launchers, `CHANGELOG.md`, `LICENSE`, and `README.md`.
6. Commit the version bump and create a local Git release tag (e.g., `v0.0.1d`).

**Available options:**
*   `--dry-run`: Performs compilation, packages the zip, but reverts all code modifications and skips Git operations. Useful for testing the release build.
*   `--no-git`: Bumps version and builds/packages, but skips Git commit and tag creation.
*   `--allow-dirty`: Allows running the script even if there are uncommitted changes in the working directory.

### Troubleshooting & Platform Notes
- **Crash Errors & Logs**: Should there be any crash errors, check the `log` folder, which will have a file with stack trace lines.
  > [!NOTE]
  > File loading failures that do not have a modal popup and appear exclusively in log lines are generally to be expected; they are usually the result of inputs that do not conform to the Starsector spec JSON layouts.
- **macOS Startup Issues**: If the editor fails to launch on macOS, try the following steps (see [PR 52](https://github.com/Ontheheavens/Ship-Editor/pull/52)):
  1. Open a Terminal window.
  2. Type: `chmod +x ` (make sure to include the trailing space).
  3. Drag the `.command` file onto the Terminal window.
  4. Press `Enter` to execute.
