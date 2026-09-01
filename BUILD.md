# Building Ship-Editor from Source

If you wish to compile the project yourself, follow these instructions.

## Prerequisites
- **Java Development Kit (JDK)**: JDK 17.
- **Maven**: Ensure Maven is installed on your system.

## Compiling
To compile the project and generate the executable fat JAR, run:
```bash
mvn clean package -DskipTests
```
This builds the application and outputs the executable JAR file in the `target/` directory (e.g. `target/ship_editor-0.0.1f.jar`).

## Running Developer Build
To run the compiled JAR, execute:
```bash
java -jar target/ship_editor-*.jar
```

## Running from Source Code
To compile and run the application directly from source using Maven without building a JAR:
```bash
MAVEN_OPTS="-Xmx4g" mvn compile exec:java -Dexec.mainClass="shipeditor.Main"
```

## Managing Releases
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


# Development Environment Setup

When setting up or building the Starsector Ship Editor project, please follow these guidelines and known environment quirks:

## 1. Java Version Requirement
- **JDK 17 is strictly required.**
- **Do not use Java 25 or newer.** The Lombok library used in this project crashes on JDK 25+ with a `TypeTag :: UNKNOWN ExceptionInInitializerError`.

## 2. Linux (Fedora) Maven Quirks
- On Fedora, the system `mvn` wrapper ignores the `update-alternatives` system and defaults to the system's latest Java (often Java 25).
- **Fix:** You must force Maven to use JDK 17 by setting `JAVA_HOME`. For users running `mvn` commands locally on Linux, create or update `~/.mavenrc` with the path to a local JDK 17 installation.
  - Example: `export JAVA_HOME=$HOME/.jdk/jdk-17.0.20+8`

## 3. Ubuntu/Debian IDE Quirks
- On Ubuntu/Debian, internal IDE terminals (like VS Code) may completely ignore `~/.mavenrc`.
- **Fix:** The `JAVA_HOME` must be explicitly exported in `~/.bashrc` (e.g., `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`) to prevent the environment from defaulting to the wrong Java version and crashing during compilation.

## 4. Concurrent Maven Executions
- When running `mvn clean compile exec:java`, **never** execute concurrent background `mvn clean` tasks.
- Maven's `exec:java` loads classes dynamically. A concurrent `clean` goal will purge the `target/classes` directory right as the application JVM initializes, throwing a `java.lang.NoClassDefFoundError` and crashing the startup sequence.

## 5. Memory Allocation for Maven
- The `Main.java` class includes a `checkAndRelaunch` method that forks a new JVM with `-Xmx4g` if the available heap is less than 4GB.
- When launching via `mvn exec:java`, the classpath only contains the Plexus Launcher. Therefore, Maven itself must be allocated sufficient memory to avoid triggering this relaunch fallback, which would fail to find the project classpath.
- **Fix:** Provide Maven with enough heap memory: `MAVEN_OPTS="-Xmx4g" mvn compile exec:java -Dexec.mainClass="shipeditor.Main"`


<details>
<summary>Click here for a detailed step-by-step Building Guide for Beginners</summary>

# Building Ship Editor from Source — Complete Guide

This guide walks you through compiling Ship Editor from its source code on **Windows**, **macOS**, or **Linux**. No prior programming experience is assumed — every step is explained in full.

---

## Table of Contents

1. [What You're Doing](#what-youre-doing)
2. [Prerequisites](#prerequisites)
   - [Install Git](#step-1-install-git)
   - [Install JDK 17](#step-2-install-jdk-21)
   - [Install Maven](#step-3-install-maven)
3. [Download the Source Code](#download-the-source-code)
4. [Build the Application](#build-the-application)
5. [Run the Application](#run-the-application)
6. [Troubleshooting](#troubleshooting)

---

## What You're Doing

The Ship Editor source code is written in **Java**. To turn it into a runnable application, you need three tools:

| Tool | What It Does |
|------|-------------|
| **Git** | Downloads the source code from GitHub |
| **JDK (Java Development Kit)** | The Java compiler that translates source code into a runnable program |
| **Maven** | A build tool that downloads all required libraries and assembles the final `.jar` file |

The end result is a single file called `ship_editor.jar` that you can double-click or run from a terminal — exactly like the one in the official release.

---

## Prerequisites

### Step 1: Install Git

Git is used to download the source code.

#### Windows
1. Go to [https://git-scm.com/download/win](https://git-scm.com/download/win).
2. Download the **64-bit installer**.
3. Run the installer. Accept all default options.
4. Once installed, open the **Start Menu**, search for **"Git Bash"**, and open it.

#### macOS
1. Open **Terminal** (search for it in Spotlight with ⌘+Space).
2. Type `git --version` and press Enter.
3. If Git is not installed, macOS will prompt you to install the **Xcode Command Line Tools**. Click **"Install"** and wait.

#### Linux (Ubuntu/Debian)
```bash
sudo apt update && sudo apt install git -y
```

#### Linux (Fedora)
```bash
sudo dnf install git -y
```

> **Verify:** Open a terminal and run `git --version`. You should see something like `git version 2.x.x`.

---

### Step 2: Install JDK 17

You need the **Java Development Kit (JDK)**, not just the JRE. Version **21** is recommended.

> [!CAUTION]
> **Do NOT use JDK 25 or newer.** The Lombok library used by this project crashes on JDK 25+. Stick to JDK 17, 18, 19, 20, or **21** (recommended).

#### Windows

1. Go to the [Eclipse Temurin JDK 17 download page](https://adoptium.net/temurin/releases/?version=21).
2. Make sure the filters are set to:
   - **Operating System:** Windows
   - **Architecture:** x64
   - **Package Type:** JDK (not JRE!)
3. Download the **`.msi`** installer.
4. Run the installer. **Important:** On the "Custom Setup" screen, make sure **both** of these options are enabled:
   - ✅ **Add to PATH**
   - ✅ **Set JAVA_HOME**
5. Click through to finish the installation.

#### macOS

1. Go to the [Eclipse Temurin JDK 17 download page](https://adoptium.net/temurin/releases/?version=21).
2. Set the filters to:
   - **Operating System:** macOS
   - **Architecture:** aarch64 (Apple Silicon M1/M2/M3) or x64 (Intel)
   - **Package Type:** JDK
3. Download the **`.pkg`** installer.
4. Run the installer and follow the prompts.

#### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install temurin-21-jdk -y
```

If the `temurin-21-jdk` package is not found, add the Adoptium repository first:

```bash
sudo mkdir -p /etc/apt/keyrings
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo tee /etc/apt/keyrings/adoptium.asc
echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install temurin-21-jdk -y
```

#### Linux (Fedora)

```bash
sudo dnf install java-21-openjdk-devel -y
```

If you have multiple Java versions installed:
```bash
sudo alternatives --config java
```
Select the Java 21 entry from the list.

> **Verify:** Open a **new** terminal window (important — this reloads the PATH) and run:
> ```bash
> java -version
> ```
> You should see output containing `openjdk version "21.x.x"` or similar.
>
> Also run:
> ```bash
> javac -version
> ```
> You should see `javac 21.x.x`. If you only see `java` but not `javac`, you installed the **JRE** instead of the **JDK**. Go back and install the JDK.

---

### Step 3: Install Maven

Maven is the build tool that compiles the project and downloads all required libraries automatically.

#### Windows

1. Go to [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi).
2. Under **"Files"**, download the **Binary zip archive** (the file ending in `-bin.zip`).
3. Extract the downloaded zip to a permanent location, for example: `C:\Program Files\Maven`.
4. **Add Maven to your system PATH:**
   1. Press **Win + S**, search for **"Environment Variables"**, and click **"Edit the system environment variables"**.
   2. Click **"Environment Variables..."** at the bottom.
   3. Under **"System variables"**, find the variable named **`Path`** and click **"Edit..."**.
   4. Click **"New"** and add the path to Maven's `bin` folder, e.g.: `C:\Program Files\Maven\apache-maven-3.9.9\bin`
   5. Click **OK** on all dialogs.

#### macOS

Using Homebrew (if you have it):
```bash
brew install maven
```

Without Homebrew:
1. Download the Binary zip from [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi).
2. Extract it to `~/maven`.
3. Add to your shell profile (`~/.zshrc` or `~/.bash_profile`):
   ```bash
   export PATH="$HOME/maven/apache-maven-3.9.9/bin:$PATH"
   ```
4. Restart your terminal.

#### Linux (Ubuntu/Debian)

```bash
sudo apt update && sudo apt install maven -y
```

> [!WARNING]
> **Ubuntu/Debian users:** Your system's Maven package may use the wrong Java version by default. If the build fails later, add this line to your `~/.bashrc` file:
> ```bash
> export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
> ```
> Then run `source ~/.bashrc` and try again.

#### Linux (Fedora)

```bash
sudo dnf install maven -y
```

> [!WARNING]
> **Fedora users:** Fedora's `mvn` wrapper ignores `update-alternatives` and uses `/etc/java/maven.conf`. If the build fails, create a file at `~/.mavenrc` with this single line:
> ```bash
> JAVA_HOME=/usr/lib/jvm/java-21-openjdk
> ```

> **Verify:** Open a **new** terminal and run:
> ```bash
> mvn -version
> ```
> You should see output showing both Maven's version **and** the Java version it's using. The Java version should say **21**.
>
> Example good output:
> ```
> Apache Maven 3.9.9
> Java version: 21.0.x, vendor: Eclipse Adoptium
> ```

---

## Download the Source Code

Open a terminal (Git Bash on Windows) and run:

```bash
git clone https://github.com/thevolkflower/Ship-Editor.git
cd Ship-Editor
```

This creates a folder called `Ship-Editor` containing all the source code.

> **Note:** If the repository URL is different, replace it with the correct URL.

---

## Build the Application

From inside the `Ship-Editor` folder, run:

```bash
mvn clean package -DskipTests
```

**What this does:**
1. `clean` — Deletes any previous build output.
2. `package` — Compiles all Java source files, downloads all required libraries from the internet, and assembles everything into a single executable JAR file.
3. `-DskipTests` — Skips running the test suite to save time.

> [!IMPORTANT]
> The **first time** you run this command, Maven will download all the project's dependencies (~200 MB). This may take several minutes depending on your internet speed. Subsequent builds will be much faster because the downloads are cached.

If the build succeeds, you will see:

```
[INFO] BUILD SUCCESS
```

The compiled application is now at:
```
ship_editor.jar    (in the project root folder)
```

---

## Run the Application

### Option A: Using the Launcher Scripts (Recommended)

The project includes ready-made launcher scripts that set up the optimal memory and performance settings:

- **Windows:** Double-click `ship_editor.bat`
- **Linux/macOS:** Run `./ship_editor.sh` in a terminal (you may need to run `chmod +x ship_editor.sh` first)

### Option B: Using the Command Line

```bash
java -Xmx4g -jar ship_editor.jar
```

The `-Xmx4g` flag allocates up to 4 GB of memory to the application, which is recommended for large mod data sets.

---

## Troubleshooting

### Build fails with "javac: invalid release: 17"

**Cause:** Maven is using a Java version older than 17.

**Fix:** Make sure JDK 17 is installed and that Maven is using it. Run `mvn -version` and check the Java version line. If it shows an older version:
- **Windows:** Reinstall JDK 17 with "Set JAVA_HOME" enabled.
- **Linux:** Set `JAVA_HOME` in `~/.bashrc` or `~/.mavenrc` as described above.

---

### Build fails with "ExceptionInInitializerError" from Lombok

**Cause:** You are using JDK 25 or newer, which is incompatible with the Lombok version used by this project.

**Fix:** Install and use JDK 17 instead. Do not use JDK 25+.

---

### "mvn: command not found"

**Cause:** Maven is not installed or not in your system PATH.

**Fix:**
- **Windows:** Make sure you added Maven's `bin` folder to your system PATH (see Step 3).
- **macOS:** Install via `brew install maven`.
- **Linux:** Install via `sudo apt install maven` or `sudo dnf install maven`.

---

### "java: command not found" or "javac: command not found"

**Cause:** JDK is not installed or not in your system PATH.

**Fix:** Reinstall JDK 17 and make sure the "Add to PATH" option was checked during installation. Open a **new** terminal window after installation.

---

### Build succeeds but the application window doesn't appear (Linux)

**Cause:** On some Linux setups (especially when running from an IDE terminal or over SSH), the `DISPLAY` environment variable may not be set.

**Fix:** Add this to your `~/.bashrc`:
```bash
export DISPLAY=:0
```
Then run `source ~/.bashrc` and try again.

---

### Application starts but the window is invisible or blank (Linux/XFCE)

**Cause:** The saved window bounds may match the exact screen resolution, causing X11 to fail rendering.

**Fix:** Delete the settings file to reset window position:
```bash
rm settings.json
```
Then restart the application.

---

### Build takes a very long time

The first build downloads all dependencies from the internet. This is normal and can take 5–10 minutes. Subsequent builds will be much faster (typically under 20 seconds).

---

### I want to make changes and rebuild

After editing source files, simply run the build command again:
```bash
mvn clean package -DskipTests
```

The new `ship_editor.jar` will be generated with your changes.

</details>
