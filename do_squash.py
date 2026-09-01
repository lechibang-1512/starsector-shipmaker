import subprocess
import os

# Groups of commits
groups = [
    {
        "message": "docs: update agent guidelines and build dependencies\n\n- Updated agent guidelines and modding documentation\n- Updated dependencies, JVM options, and Java 17 toolchain\n- Removed obsolete utility scripts",
        "commits": ["3702b763", "09e1376b", "205ef3cc"]
    },
    {
        "message": "refactor(graphics): OpenGL rendering and graphics tools\n\n- Standardized cross-platform graphics initialization\n- Corrected weapon recoil and missile sprite scaling\n- Ported experiment tools to native Java 17 graphics pipeline",
        "commits": ["5c667a1c", "1baa61f7", "158ed39c"]
    },
    {
        "message": "fix(data): caching, loading, and serialization\n\n- Fixed hull saving crash and launch bay serialization\n- Resolved ship/weapon loading, tree rendering, and serialization bugs\n- Fixed hullmod, wing, and systems repository caching\n- Implemented lazy sprite loading for performance",
        "commits": ["3574c547", "715a61b4", "862bab60", "ab64d5a9"]
    },
    {
        "message": "feat(interaction): drag interaction and auto-calculate gizmos\n\n- Replaced radius modification with Ctrl+LMB drag interaction\n- Added auto-calculate radius and center-to-sprite buttons",
        "commits": ["1f8c90a3", "0f46f6d0"]
    },
    {
        "message": "feat(weapon): hullmod picker and weapon slot management\n\n- Enhanced weapon slot display and overhauled built-in weapon picker\n- Added interactive weapon slot inspector panel\n- Implemented hullmod picker dialog\n- Reorganized ship instrument tabs and added null checks",
        "commits": ["bf876162", "6f7162f3", "7cf0c883", "1a803b52"]
    },
    {
        "message": "refactor(ui): nested tabs, strings decoupling, and UI fixes\n\n- Disabled Linux popup drop shadows\n- Replaced ShipHullPanel with nested JTabbedPane\n- Decoupled UI strings into external JSON resources",
        "commits": ["a6a5f636", "1ea3b878", "d061e983"]
    }
]

def run(cmd):
    return subprocess.check_output(cmd, shell=True).decode('utf-8').strip()

# Make sure we stash current changes
print("Stashing unstaged changes...")
run("git stash push -m 'pre-squash stash'")

print("Resetting to 85b33c0c...")
run("git reset --soft 85b33c0c")
run("git reset") # unstage all

committed_files = set()

for group in groups:
    files = set()
    for commit in group["commits"]:
        # Get files modified in this commit
        out = run(f"git show --name-only --format='' {commit}").splitlines()
        for f in out:
            if f.strip():
                files.add(f.strip())
    
    # Only add files that exist and haven't been committed yet
    files_to_add = []
    for f in files:
        if f not in committed_files and os.path.exists(f):
            files_to_add.append(f)
            committed_files.add(f)
            
    if files_to_add:
        # Add files in chunks to avoid command line too long
        print(f"Adding {len(files_to_add)} files for group '{group['message'].splitlines()[0]}'")
        for f in files_to_add:
            subprocess.call(['git', 'add', f])
        
        # Commit
        with open("msg.txt", "w") as f:
            f.write(group["message"])
        run("git commit -F msg.txt")
    else:
        print(f"No files to add for group '{group['message'].splitlines()[0]}'")

# Add any remaining files that were changed
print("Adding remaining files...")
out = run("git status --porcelain").splitlines()
has_remaining = False
for line in out:
    if line.startswith(" M") or line.startswith("??"):
        subprocess.call(['git', 'add', '.'])
        run("git commit -m 'chore: miscellaneous squash updates'")
        has_remaining = True
        break

if not has_remaining:
    print("No remaining files to commit.")

print("Restoring stashed changes...")
try:
    run("git stash pop")
except Exception as e:
    print("Note: git stash pop failed or no stashes.")

print("Done.")
