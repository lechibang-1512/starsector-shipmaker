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

### Alternative: Starsector's Built-in JRE (No Installation Required)
If you place the Ship Editor inside your Starsector installation directory (for example, in the `mods` folder or the main `Starsector` folder), the launcher scripts will **automatically detect and use Starsector's built-in Java**.

You don't need to install anything! Just double-click `ship_editor.bat` (Windows) or run `ship_editor.sh` (Mac/Linux), and it will automatically look for the `jre` folder that came with your game.

---

### Alternative: Local JRE (Portable Setup)
If you prefer not to install Java system-wide and are running the editor outside of the Starsector directory, you can run the editor using a local folder:
1. Go to the **[Eclipse Temurin Java 21 Releases](https://adoptium.net/temurin/releases/?version=21)** page.
2. Select your Operating System and download the `.zip` archive (Windows) or `.tar.gz` archive (Linux/macOS). Make sure the package type is set to **JRE**.
3. Extract the downloaded archive.
4. Rename the extracted folder (e.g., `jdk-21.0.x+xx-jre` or `jre-21.x.x`) to exactly **`jre`**.
5. Place this **`jre`** folder directly in the root directory of the application (alongside `ship_editor.jar`).
6. Launch using the startup scripts (`ship_editor.bat` or `ship_editor.sh`), which will automatically detect and run from the local folder.

---

## Building from Source (For Developers)

For detailed instructions on compiling, running from source, and managing releases, please refer to the [BUILD.md](BUILD.md) file.

### Troubleshooting & Platform Notes
- **Crash Errors & Logs**: Should there be any crash errors, check the `log` folder, which will have a file with stack trace lines.
  > [!NOTE]
  > File loading failures that do not have a modal popup and appear exclusively in log lines are generally to be expected; they are usually the result of inputs that do not conform to the Starsector spec JSON layouts.
- **Linux Wayland Rendering Issues**: If you are running Linux (such as Arch, Garuda, or Fedora) with a Wayland session, the editor may launch with a black, unrendered workspace and "not initialized" side panels. This is caused by a `Failed to query GLX version` OpenGL crash on Wayland's default display server. To fix this, run the application in X11/XWayland mode by launching the script via terminal with the GDK backend variable set:
  ```bash
  GDK_BACKEND=x11 ./ship_editor.sh
  ```
  Alternatively, you may switch your desktop environment session from Wayland to X11/Xorg from your login screen.
- **macOS Startup Issues**: If the editor fails to launch on macOS, try the following steps (see [PR 52](https://github.com/Ontheheavens/Ship-Editor/pull/52)):
  1. Open a Terminal window.
  2. Type: `chmod +x ` (make sure to include the trailing space).
  3. Drag the `.command` file onto the Terminal window.
  4. Press `Enter` to execute.
