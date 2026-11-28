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
| [0009](0009-androidx-compose-test-libraries-for-device-tests.md) | Use the AndroidX Compose test libraries for consumer device tests | Superseded by 0027 |
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
| [0021](0021-producer-runs-every-scene-on-one-scene-thread.md) | The producer runs every scene on one scene thread | Proposed |
| [0022](0022-android-variant-api-for-generated-resources.md) | Add generated resources to Android variants through the Android Gradle plugin's variant API | Proposed |
| [0023](0023-producer-converts-text-sizes-with-the-font-scale-curve.md) | The producer converts text sizes with the font scale curve | Proposed, amended by 0025 |
| [0024](0024-text-gate-producer-half-in-contract-tests-jvm.md) | The text gate's producer half runs in a JVM target of contract-tests | Rejected |
| [0025](0025-device-measures-server-text.md) | The device measures server text and reports its sizes | Not chosen; fallback D of 0026 |
| [0026](0026-producer-measures-text-with-layoutlib.md) | The producer measures text with layoutlib's Android text classes | Accepted (L2b) |
| [0027](0027-compose-decides-the-versions-of-libraries-tied-to-it.md) | Let Compose decide the versions of the libraries tied to it | Accepted |
| [0028](0028-live-session-wakes-on-its-own-scene-invalidations.md) | A live session wakes only when its own scene reports a change | Accepted |
