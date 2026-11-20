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
| [0010](0010-per-module-choice-enums-in-bridge-subpackage.md) | Place per-module choice enums in a bridge subpackage | Accepted |
| [0011](0011-replaceable-text-through-a-string-manager.md) | Replace the consumer's text through a string manager | Accepted |
| [0012](0012-problem-message-style-parameters.md) | Style the default problem message with Material's shape of style parameters | Accepted |
| [0013](0013-preview-annotation-as-compile-only.md) | Set up Compose previews as the Kotlin Multiplatform guide shows, off what apps receive | Accepted |
| [0014](0014-one-opt-in-marker-for-internal-api.md) | One opt-in marker for the library's internal API, in core | Accepted |