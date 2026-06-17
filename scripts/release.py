#!/usr/bin/env python3
import os
import sys
import re
import shutil
import zipfile
import subprocess
import argparse
import datetime

# Files to update version in
POM_PATH = "pom.xml"
SETTINGS_MANAGER_PATH = "src/main/java/shipeditor/persistence/SettingsManager.java"
MAIN_PATH = "src/main/java/shipeditor/Main.java"
CHANGELOG_PATH = "CHANGELOG.md"

# Files/folders to package in release zip
MANDATORY_RELEASE_FILES = {
    "ship_editor.bat": "ship_editor.bat",
    "ship_editor.sh": "ship_editor.sh",
    "ship_editor.command": "ship_editor.command",
    "CHANGELOG.md": "CHANGELOG.md",
    "LICENSE": "LICENSE",
    "README.md": "README.md"
}

def check_command(cmd):
    """Check if a command is available on the system."""
    return shutil.which(cmd) is not None

def run_cmd(cmd, check=True, capture_output=True):
    """Helper to run a shell command."""
    res = subprocess.run(cmd, shell=True, capture_output=capture_output, text=True)
    if check and res.returncode != 0:
        print(f"Error executing command: {cmd}")
        if capture_output:
            print(f"Stdout:\n{res.stdout}")
            print(f"Stderr:\n{res.stderr}")
        sys.exit(res.returncode)
    return res

def is_git_clean():
    """Check if the git repository has no uncommitted changes."""
    res = run_cmd("git status --porcelain")
    return len(res.stdout.strip()) == 0

def get_current_version():
    """Extract current project version from pom.xml."""
    if not os.path.exists(POM_PATH):
        print("Error: pom.xml not found in current directory.", file=sys.stderr)
        sys.exit(1)
    with open(POM_PATH, "r", encoding="utf-8") as f:
        content = f.read()
    
    match = re.search(
        r'<artifactId>ship_editor</artifactId>\s*<version>([^<]+)</version>', 
        content
    )
    if match:
        return match.group(1)
    
    # Fallback to general project version if the specific group match fails
    match = re.search(r'<version>([^<]+)</version>', content)
    if match:
        return match.group(1)
    
    return None

def update_version_in_files(new_version):
    """Replace version strings in pom.xml and Java source files."""
    # 1. pom.xml
    replace_in_file(
        POM_PATH,
        r'(<artifactId>ship_editor</artifactId>\s*<version>)([^<]+)(</version>)',
        rf'\g<1>{new_version}\g<3>'
    )
    
    # 2. SettingsManager.java
    replace_in_file(
        SETTINGS_MANAGER_PATH,
        r'(private static final String projectVersion = ")([^"]+)(";)',
        rf'\g<1>{new_version}\g<3>'
    )
    
    # 3. Main.java
    replace_in_file(
        MAIN_PATH,
        r'(public static final String VERSION = ")([^"]+)(";)',
        rf'\g<1>{new_version}\g<3>'
    )

def replace_in_file(filepath, pattern, replacement):
    if not os.path.exists(filepath):
        raise FileNotFoundError(f"Source file not found for version update: {filepath}")
    
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    new_content, count = re.subn(pattern, replacement, content)
    if count == 0:
        raise ValueError(f"Could not locate version target in file: {filepath}")
    
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(new_content)

def update_changelog(target_version, target_date, changelog_lines=None):
    """Replaces ## [Unreleased] with ## [target_version] - target_date."""
    if not os.path.exists(CHANGELOG_PATH):
        print(f"Warning: {CHANGELOG_PATH} not found.")
        return
    with open(CHANGELOG_PATH, "r", encoding="utf-8") as f:
        content = f.read()
    
    # If the target version is already there, don't inject again
    if re.search(rf"##\s*\[{re.escape(target_version)}\]", content):
        print(f"CHANGELOG.md already has section for {target_version}.")
        return

    if "## [Unreleased]" in content:
        changelog_body = ""
        if changelog_lines:
            changelog_body = "\n\n### Features\n" + "\n".join(changelog_lines)
            
        new_header = f"## [Unreleased]\n\n## [{target_version}] - {target_date}{changelog_body}"
        content = content.replace("## [Unreleased]", new_header, 1)
        with open(CHANGELOG_PATH, "w", encoding="utf-8") as f:
            f.write(content)
        print("Automatically updated CHANGELOG.md with version and date.")
    else:
        print("Warning: '## [Unreleased]' not found in CHANGELOG.md. Skipping automated update.")

def build_project():
    """Run Maven clean package."""
    print("Building application with Maven...")
    # Using skipTests for faster packaging, similar to clean build guidelines
    run_cmd("mvn clean package -DskipTests", capture_output=False)

def package_release(version):
    """Create the zip package inside releases/ directory."""
    releases_dir = "releases"
    os.makedirs(releases_dir, exist_ok=True)
    
    zip_filename = os.path.join(releases_dir, f"ship-editor-{version}.zip")
    print(f"Packaging release artifacts to {zip_filename}...")
    
    folder_prefix = f"ship-editor-{version}"
    
    with zipfile.ZipFile(zip_filename, 'w', zipfile.ZIP_DEFLATED) as zipf:
        # Dynamically add the freshly built jar
        jar_src = "ship_editor.jar"
        if not os.path.exists(jar_src):
            jar_src = f"target/ship_editor-{version}.jar"
        if not os.path.exists(jar_src):
            print(f"Error: Built JAR '{jar_src}' is missing. Did the build fail?")
            sys.exit(1)
        zipf.write(jar_src, os.path.join(folder_prefix, "ship_editor.jar"))
        print(f"  + Added: {jar_src} -> {folder_prefix}/ship_editor.jar")
        
        for src_path, zip_rel_path in MANDATORY_RELEASE_FILES.items():
            if not os.path.exists(src_path):
                print(f"Error: Mandatory release component '{src_path}' is missing.")
                sys.exit(1)
            
            # Destination path inside zip
            target_path = os.path.join(folder_prefix, zip_rel_path if zip_rel_path else os.path.basename(src_path))
            
            # Write file
            zipf.write(src_path, target_path)
            
            # Log packaging
            print(f"  + Added: {src_path} -> {target_path}")
            
    print(f"Successfully created release package: {zip_filename}")
    return zip_filename

def restore_backups(backups: dict):
    """Restores original file contents from backups."""
    print("\nRestoring original version strings...")
    for path, content in backups.items():
        if os.path.exists(path) or content is not None:
            with open(path, "w", encoding="utf-8") as f:
                f.write(content)

def commit_changes(target_version: str):
    """Commits the bumped version files to git."""
    print("\nCommitting version changes...")
    run_cmd(f"git add {POM_PATH} {SETTINGS_MANAGER_PATH} {MAIN_PATH} {CHANGELOG_PATH}")
    
    diff_res = run_cmd("git diff --cached --name-only")
    if diff_res.stdout.strip():
        run_cmd(f'git commit -m "Release v{target_version}"')
        print(f"Successfully committed v{target_version} in git.")
    else:
        print("No changes to commit (files already match target version).")

def interactive_console(current_version):
    import tkinter as tk
    from tkinter import ttk
    from tkinter import messagebox
    
    root = tk.Tk()
    root.title("Release Configuration")
    
    window_width = 500
    window_height = 450
    screen_width = root.winfo_screenwidth()
    screen_height = root.winfo_screenheight()
    x_cordinate = int((screen_width/2) - (window_width/2))
    y_cordinate = int((screen_height/2) - (window_height/2))
    root.geometry(f"{window_width}x{window_height}+{x_cordinate}+{y_cordinate}")
    
    target_version_var = tk.StringVar()
    target_date_var = tk.StringVar()
    
    suggested = ""
    m = re.match(r"^(\d+)\.(\d+)\.(\d+)-(.*)$", current_version)
    if m:
        suggested = f"{m.group(1)}.{m.group(2)}.{m.group(3)}-{m.group(4)}"
    else:
        m = re.match(r"^(\d+)\.(\d+)\.(\d+)([a-zA-Z]*)$", current_version)
        if m:
            suggested = f"{m.group(1)}.{m.group(2)}.{m.group(3)}-{m.group(4)}"
            
    target_version_var.set(suggested if suggested else current_version)
    target_date_var.set(datetime.datetime.now().strftime("%Y-%m-%d"))
    
    frame = ttk.Frame(root, padding="15")
    frame.pack(fill=tk.BOTH, expand=True)
    
    ttk.Label(frame, text=f"Current version: {current_version}", font=("Helvetica", 10, "bold")).pack(anchor=tk.W, pady=(0, 15))
    
    ttk.Label(frame, text="Target Version (e.g. x.y.z-[suffix]):").pack(anchor=tk.W)
    version_entry = ttk.Entry(frame, textvariable=target_version_var, width=40)
    version_entry.pack(anchor=tk.W, pady=(0, 10))
    
    ttk.Label(frame, text="Release Date (YYYY-MM-DD):").pack(anchor=tk.W)
    date_entry = ttk.Entry(frame, textvariable=target_date_var, width=40)
    date_entry.pack(anchor=tk.W, pady=(0, 10))
    
    ttk.Label(frame, text="Changelog Entries (each line will be bulleted):").pack(anchor=tk.W)
    changelog_text = tk.Text(frame, height=10, width=50)
    changelog_text.pack(fill=tk.BOTH, expand=True, pady=(0, 15))
    
    result = {'version': None, 'date': None, 'changelog': []}
    
    def on_submit():
        v = target_version_var.get().strip()
        d = target_date_var.get().strip()
        if not v or not d:
            messagebox.showerror("Error", "Version and Date cannot be empty.")
            return
            
        lines = changelog_text.get("1.0", tk.END).strip().split('\n')
        parsed_lines = []
        for line in lines:
            line = line.strip()
            if line:
                if not line.startswith("-"):
                    line = f"- {line}"
                parsed_lines.append(line)
                
        result['version'] = v
        result['date'] = d
        result['changelog'] = parsed_lines
        root.destroy()
        
    def on_cancel():
        root.destroy()
        
    btn_frame = ttk.Frame(frame)
    btn_frame.pack(fill=tk.X)
    
    ttk.Button(btn_frame, text="Submit", command=on_submit).pack(side=tk.RIGHT, padx=(5, 0))
    ttk.Button(btn_frame, text="Cancel", command=on_cancel).pack(side=tk.RIGHT)
    
    version_entry.focus()
    root.bind('<Escape>', lambda e: on_cancel())
    
    root.mainloop()
    
    return result['version'], result['date'], result['changelog']

def main():
    parser = argparse.ArgumentParser(description="Manage project releases locally without GitHub dependency.")
    parser.add_argument("--version", help="The target release version (e.g. 0.0.1-d). Skips interactive console.")
    parser.add_argument("--dry-run", action="store_true", help="Compile and package the zip, but revert version changes and skip Git operations.")
    parser.add_argument("--no-git", action="store_true", help="Bump version and build/package, but skip Git commit operations.")
    parser.add_argument("--allow-dirty", action="store_true", help="Allow release script to run even with uncommitted changes.")
    
    args = parser.parse_args()
    
    # 1. Dependency Checks
    if not check_command("git"):
        print("Error: git is not installed or not in PATH.", file=sys.stderr)
        sys.exit(1)
    if not check_command("mvn"):
        print("Error: maven (mvn) is not installed or not in PATH.", file=sys.stderr)
        sys.exit(1)
        
    # 2. Check Git Clean status
    if not args.allow_dirty and not args.dry_run and not is_git_clean():
        print("Error: Git repository has uncommitted changes. Please commit, stash, or run with --allow-dirty.", file=sys.stderr)
        sys.exit(1)
            
    # 3. Determine versions
    current_version = get_current_version()
    if not current_version:
        print("Error: Could not extract current version from pom.xml.", file=sys.stderr)
        sys.exit(1)
        
    if args.version:
        target_version = args.version
        target_date = datetime.datetime.now().strftime("%Y-%m-%d")
        changelog_lines = []
        print(f"Current version: {current_version}")
    else:
        target_version, target_date, changelog_lines = interactive_console(current_version)
        if not target_version:
            print("\nAborted by user.")
            sys.exit(1)
            
    print(f"Target release version: {target_version}")
    print(f"Target release date: {target_date}")

    original_files_backup = {}
    
    try:
        # 4. Update Versions
        print(f"Bumping version from {current_version} to {target_version}...")
        for path in [POM_PATH, SETTINGS_MANAGER_PATH, MAIN_PATH, CHANGELOG_PATH]:
            if os.path.exists(path):
                with open(path, "r", encoding="utf-8") as f:
                    original_files_backup[path] = f.read()
                
        update_version_in_files(target_version)
        update_changelog(target_version, target_date, changelog_lines)
        
        # 5. Build
        build_project()
        
        # 6. Package
        package_release(target_version)
        
    except Exception as e:
        print(f"\nAn error occurred during build/packaging: {e}", file=sys.stderr)
        restore_backups(original_files_backup)
        sys.exit(1)
        
    # 7. Git Operations / Reverts
    if args.dry_run:
        print("\nDry-run mode active.")
        restore_backups(original_files_backup)
        print("Dry-run complete. Built archive is preserved in releases/.")
    elif args.no_git:
        print("\nSkipping Git operations as requested (--no-git).")
    else:
        commit_changes(target_version)
            
    print("\nRelease management workflow completed successfully!")

if __name__ == "__main__":
    main()
