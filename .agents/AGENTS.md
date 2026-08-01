# Workspace Agent Rules

## GitHub Operations
- **Use GitHub CLI (`gh`)**: For all interactions with GitHub (e.g., checking issue statuses, creating PRs, fetching data), prioritize the `gh` CLI (e.g., `gh issue view`, `gh api`) over raw `curl` requests or standard `git` commands.
- **Pushing changes**: If `git push` encounters authentication issues, verify the `gh` authentication status and ensure `gh auth setup-git` is used to sync credentials, or fallback to using `gh api` for operations.
