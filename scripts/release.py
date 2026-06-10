#!/usr/bin/env python3
import os
import sys
import re
import shutil
import zipfile
import subprocess
import argparse

# Files to update version in
POM_PATH = "pom.xml"
SETTINGS_MANAGER_PATH = "src/main/java/oth/shipeditor/persistence/SettingsManager.java"
MAIN_PATH = "src/main/java/oth/shipeditor/Main.java"
CHANGELOG_PATH = "CHANGELOG.md"

# Files/folders to package in release zip
MANDATORY_RELEASE_FILES = {
    "ship_editor.jar": "",  # from project root, to zip root
    "scripts/launchers/ship_editor.bat": "ship_editor.bat",
    "scripts/launchers/ship_editor.sh": "ship_editor.sh",
    "scripts/launchers/ship_editor.command": "ship_editor.command",
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
        r'<groupId>oth\.shipeditor</groupId>\s*<artifactId>ship_editor</artifactId>\s*<version>([^<]+)</version>', 
        content
    )
    if match:
        return match.group(1)
    
    # Fallback to general project version if the specific group match fails
    match = re.search(r'<version>([^<]+)</version>', content)
    if match:
        return match.group(1)
    
    return None

def suggest_next_version(current):
    """Suggest a logical next version (e.g. 0.0.1c -> 0.0.1d, 1.2.3 -> 1.2.4)."""
    # 1. Match trailing letter (e.g. 0.0.1c)
    match_letter = re.match(r"^(.*)([a-zA-Z])$", current)
    if match_letter:
        prefix, letter = match_letter.groups()
        next_letter = chr(ord(letter.lower()) + 1)
        if letter.isupper():
            next_letter = next_letter.upper()
        return prefix + next_letter
    
    # 2. Match trailing integer (e.g. 1.2.3)
    match_number = re.match(r"^(.*?)(\d+)$", current)
    if match_number:
        prefix, number = match_number.groups()
        return prefix + str(int(number) + 1)
    
    return current

def update_version_in_files(new_version):
    """Replace version strings in pom.xml and Java source files."""
    # 1. pom.xml
    replace_in_file(
        POM_PATH,
        r'(<groupId>oth\.shipeditor</groupId>\s*<artifactId>ship_editor</artifactId>\s*<version>)([^<]+)(</version>)',
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

def check_changelog(version):
    """Check if CHANGELOG.md contains the target version section."""
    if not os.path.exists(CHANGELOG_PATH):
        print(f"Warning: {CHANGELOG_PATH} not found.")
        return False
    with open(CHANGELOG_PATH, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Matches line like ## [0.0.1c] or ## [0.0.1c] - 2026-06-08
    pattern = rf"##\s*\[{re.escape(version)}\]"
    if re.search(pattern, content):
        return True
    return False

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

def main():
    parser = argparse.ArgumentParser(description="Manage project releases locally without GitHub dependency.")
    parser.add_argument("--version", help="The target release version (e.g. 0.0.1d).")
    parser.add_argument("--dry-run", action="store_true", help="Compile and package the zip, but revert version changes and skip Git operations.")
    parser.add_argument("--no-git", action="store_true", help="Bump version and build/package, but skip Git commit and tag operations.")
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
    if not args.allow_dirty and not args.dry_run:
        if not is_git_clean():
            print("Error: Git repository has uncommitted changes. Please commit, stash, or run with --allow-dirty.", file=sys.stderr)
            sys.exit(1)
            
    # 3. Determine versions
    current_version = get_current_version()
    if not current_version:
        print("Error: Could not extract current version from pom.xml.", file=sys.stderr)
        sys.exit(1)
        
    print(f"Current version: {current_version}")
    
    if args.version:
        target_version = args.version
    else:
        suggested = suggest_next_version(current_version)
        try:
            user_input = input(f"Enter target release version [{suggested}]: ").strip()
            target_version = user_input if user_input else suggested
        except (KeyboardInterrupt, EOFError):
            print("\nAborted.")
            sys.exit(1)
            
    if not target_version:
        print("Error: Target version cannot be empty.", file=sys.stderr)
        sys.exit(1)
        
    # 4. Check CHANGELOG.md
    if not check_changelog(target_version):
        print(f"Warning: No changelog section found in {CHANGELOG_PATH} for version '{target_version}'.")
        try:
            confirm = input("Do you want to proceed anyway? (y/N): ").strip().lower()
            if confirm != 'y':
                print("Aborted. Please update CHANGELOG.md first.")
                sys.exit(0)
        except (KeyboardInterrupt, EOFError):
            print("\nAborted.")
            sys.exit(1)

    original_files_backup = {}
    
    try:
        # 5. Update Versions
        print(f"Bumping version from {current_version} to {target_version}...")
        for path in [POM_PATH, SETTINGS_MANAGER_PATH, MAIN_PATH]:
            with open(path, "r", encoding="utf-8") as f:
                original_files_backup[path] = f.read()
                
        update_version_in_files(target_version)
        
        # 6. Build
        build_project()
        
        # 7. Package
        package_release(target_version)
        
    except Exception as e:
        print(f"\nAn error occurred during build/packaging: {e}", file=sys.stderr)
        # Restore backups
        print("Restoring original source files...")
        for path, content in original_files_backup.items():
            with open(path, "w", encoding="utf-8") as f:
                f.write(content)
        sys.exit(1)
        
    # 8. Git Operations / Reverts
    if args.dry_run:
        print("\nDry-run mode: Restoring original version strings...")
        for path, content in original_files_backup.items():
            with open(path, "w", encoding="utf-8") as f:
                f.write(content)
        print("Dry-run complete. Built archive is preserved in releases/.")
    else:
        if args.no_git:
            print("\nSkipping Git operations as requested (--no-git).")
        else:
            print("\nCommitting version changes and creating Git tag...")
            # Git add
            run_cmd(f"git add {POM_PATH} {SETTINGS_MANAGER_PATH} {MAIN_PATH}")
            # Check if there are staged changes to commit
            diff_res = run_cmd("git diff --cached --name-only")
            if diff_res.stdout.strip():
                run_cmd(f'git commit -m "Release v{target_version}"')
            else:
                print("No changes to commit (files already match target version).")
            # Git tag (check if tag already exists and handle/re-tag)
            tag_check = run_cmd(f'git tag -l "v{target_version}"')
            if tag_check.stdout.strip():
                print(f"Warning: Git tag v{target_version} already exists. Re-tagging...")
                run_cmd(f'git tag -d "v{target_version}"')
            run_cmd(f'git tag -a "v{target_version}" -m "Release v{target_version}"')
            print(f"Successfully committed and tagged v{target_version} in git.")
            
    print("\nRelease management workflow completed successfully!")

if __name__ == "__main__":
    main()
