# Workspace Agent Rules

## GitHub Operations
- **Use GitHub CLI (`gh`)**: For all interactions with GitHub (e.g., checking issue statuses, creating PRs, fetching data), prioritize the `gh` CLI (e.g., `gh issue view`, `gh api`) over raw `curl` requests or standard `git` commands.
- **Authentication Issues**: If `git push` or `gh` commands encounter `HTTP 401: Bad credentials`, run `gh auth status`. If it indicates that `GITHUB_TOKEN` is invalid but a valid keyring login exists, execute git/gh commands by explicitly unsetting the environment variable (e.g., `env -u GITHUB_TOKEN git push origin main` or `env -u GITHUB_TOKEN gh release list`). Use `env -u GITHUB_TOKEN gh auth setup-git` to sync credentials if needed.
