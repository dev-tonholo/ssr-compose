# Blueprint: architecture decision record

Rules: [architecture](../rules/architecture.md), [documentation](../rules/documentation.md).

## Steps

1. Copy `docs/adr/0000-template.md` to `docs/adr/NNNN-short-title.md`, with the next free number.
2. Fill in Status, Date, Modules, Context, Options, Decision and Consequences.
3. Give at least two options, each with what it costs.
4. Add a row to the index in `docs/adr/README.md`.
5. Never rewrite an accepted ADR. Write a new one that replaces it and link both ways.
6. Run `python3 .ai/scripts/check-links.py`.

## Template

See [docs/adr/0000-template.md](../../docs/adr/0000-template.md).

## Filled example from the code

[docs/adr/0001-keep-the-poc-in-a-folder.md](../../docs/adr/0001-keep-the-poc-in-a-folder.md) records the decision behind the commit "chore: move the proof of concept into the poc folder".
