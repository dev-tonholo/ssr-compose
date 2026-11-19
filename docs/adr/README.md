# Architecture decision records

This folder holds the decisions that shape <project>. Each file is one decision.

## Rules

- File name: `NNNN-short-title.md` (four digits, next free number, lowercase words with hyphens).
- Start from [0000-template.md](0000-template.md). Copy it; do not edit the template.
- Status is one of `Proposed`, `Accepted`, `Rejected`, `Deprecated`, `Superseded by NNNN`.
- Do not rewrite an accepted ADR. Write a new one and change only the old status line.
- Add every new ADR to the index below.

How to write one: [.ai/blueprints/adr.md](../../.ai/blueprints/adr.md).

## Index

| Number | Title | Status |
|--------|-------|--------|
| [0001](0001-keep-the-poc-in-a-folder.md) | Keep the proof of concept in a folder and build the library from a clean root | Accepted |
| [0002](0002-ksp-for-tests-only.md) | Use KSP in tests only | Accepted |
| [0009](0009-androidx-compose-test-libraries-for-device-tests.md) | Use the AndroidX Compose test libraries for consumer device tests | Accepted |
| [0012](0012-problem-message-style-parameters.md) | Style the default problem message with Material's shape of style parameters | Accepted |
