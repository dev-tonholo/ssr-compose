# Documentation

Comments, writing style and where docs live.

## Rules

1. Code explains itself. If a comment would explain how code works, refactor the code instead.
2. A comment is only allowed for these four things: short API documentation, logic that truly cannot be simplified, a formula used in a calculation, and an attribution.
3. Write plain, simple English with short sentences and everyday words.
4. Never use an em dash. Use a comma, a period, a semicolon or a hyphen.
5. Never start a paragraph or a bullet with a bold label. Use a real heading, or write the sentence so it stands on its own.
6. Each fact lives in one place. Link to it from other files and do not repeat it.
7. Use relative links so they work on GitHub and in every editor.

| What | Where |
|---|---|
| What the project is, for people | `README.md` |
| The rules the project is built by | `.specify/memory/constitution.md` |
| Feature specs, plans and tasks | `specs/`, written by Spec Kit |
| Decisions | `docs/adr/` |
| Changes per release | `CHANGELOG.md` |
| How to contribute and report problems | `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md` |
| Instructions for agents | `.ai/` |
| The proof of concept | `poc/README.md` |

## Check

- `grep -rn $'\xe2\x80\x94' --include='*.md' --include='*.kt' --include='*.kts' . --exclude-dir=.git --exclude-dir=build` finds nothing in files you wrote. `CODE_OF_CONDUCT.md` and `LICENSE` are standard texts and are left as they are.
- `python3 .ai/scripts/check-links.py` passes.

Templates: [adr](../blueprints/adr.md).
