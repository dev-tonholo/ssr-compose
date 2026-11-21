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
| [0015](0015-producer-renders-with-compose-ui-foundation.md) | The producer renders with Compose UI foundation and sends a positioned document | Accepted |
| [0016](0016-app-components-measured-on-the-device.md) | App components are measured on the device and reported to the producer | Accepted |
| [0017](0017-app-fonts-travel-in-a-pinned-font-bundle.md) | The app's fonts travel in a pinned font bundle | Accepted |
| [0018](0018-local-switcher-for-interactions-without-a-round-trip.md) | A local switcher is the minimal interaction with no round trip | Accepted |
| [0019](0019-contract-version-1-carries-the-document.md) | Contract version 1 carries the server-rendered document | Accepted |
| [0020](0020-bundle-roboto-as-the-default-font.md) | Bundle Roboto as the default font | Accepted |
| [0022](0022-android-variant-api-for-generated-resources.md) | Add generated resources to Android variants through the Android Gradle plugin's variant API | Proposed |
