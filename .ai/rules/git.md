# Git

Commits, branches and what is never committed.

## Rules

1. Use Conventional Commits: `<type>(<scope>): <description>`, in the imperative, with no trailing period.
2. Keep the message to one line. Add a body only when the reason is not obvious, with `-` bullets and no wrapped lines.
3. Never add a footer, a `Co-Authored-By` line or a session trailer.
4. Make small commits, one logical change each. Every commit builds.
5. Scopes: `core`, `core-testing`, `consumer`, `producer`, `plugin`, `poc`, `protocol`, `server`, `app`, `ai`, `spec`, `build-logic`, `compiler-plugin`, `gradle-plugin`, `tooling`, `contract-tests`, `samples`. Leave the scope out for a broad change.
6. Mark a breaking change with `!` after the type or scope.
7. Keep history linear. Rebase, and do not create merge commits. Pull requests are rebase merged, so every commit lands on main as written; before merging, review fix-ups are folded into the commits they fix, and every commit builds and passes detekt on its own.
8. Commit and push only when the owner asks.
9. Never commit plans or specs written by the Superpowers skills (`docs/superpowers/`).
10. Format-only changes go in their own commit.
11. Name branches `<type>/<issue-number>/<brief-description>`, for example `chore/8/detekt-every-source-set`. The type is a Conventional Commit type, the number is the GitHub issue, and the description is a few lowercase words joined with hyphens. Do not add a user name prefix.

## Check

- `git log --oneline -10` shows one-line Conventional Commit messages.
- `git log -10 --format=%b` shows no `Co-Authored-By` and no trailer.
- `git log --merges --oneline -5` shows nothing new.
