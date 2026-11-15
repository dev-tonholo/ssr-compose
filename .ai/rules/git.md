# Git

Commits, branches and what is never committed.

## Rules

1. Use Conventional Commits: `<type>(<scope>): <description>`, in the imperative, with no trailing period.
2. Keep the message to one line. Add a body only when the reason is not obvious, with `-` bullets and no wrapped lines.
3. Never add a footer, a `Co-Authored-By` line or a session trailer.
4. Make small commits, one logical change each. Every commit builds.
5. Scopes: `core`, `consumer`, `producer`, `plugin`, `poc`, `protocol`, `server`, `app`, `ai`, `spec`, `build-logic`, `compiler-plugin`, `gradle-plugin`, `tooling`, `contract-tests`, `samples`. Leave the scope out for a broad change.
6. Mark a breaking change with `!` after the type or scope.
7. Keep history linear. Rebase, and do not create merge commits. Pull requests are squash merged, so the pull request title becomes the commit on main and must itself be a one-line Conventional Commit.
8. Commit and push only when the owner asks.
9. Never commit plans or specs written by the Superpowers skills (`docs/superpowers/`).
10. Format-only changes go in their own commit.

## Check

- `git log --oneline -10` shows one-line Conventional Commit messages.
- `git log -10 --format=%b` shows no `Co-Authored-By` and no trailer.
- `git log --merges --oneline -5` shows nothing new.
