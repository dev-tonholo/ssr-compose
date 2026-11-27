# 0026. The producer lays screens out in layoutlib

- Status: Proposed
- Date: 2026-11-27
- Modules: `producer`, `compiler-plugin`, `core`, `consumer`, `contract-tests`

## Context

The producer lays every screen out and the device draws each node in the box it was sent ([ADR 0015](0015-producer-renders-with-compose-ui-foundation.md)). Desktop Compose on Skia breaks and rounds text lines differently from Android: probe P21 missed the text gate's rule in 10 of 60 texts ([research.md R20](../../specs/001-consumer-catalog-bridge/research.md#text-gate-t123)). [ADR 0025](0025-device-measures-server-text.md) proposed that the device measures each text and reports its sizes.

Then the owner asked how Compose previews match the emulator, which led to layoutlib, the Android renderer Android Studio previews use. A replay of 360 recorded texts per device through layoutlib 16.2.1 (Compose 1.12.1, layoutlib reports API 36) matched every check, including line count, height, box width, line ends, line widths, bottoms, baselines and the ellipsis flag, on API 33 (physical Motorola), API 35 (emulator), API 36 (emulator and physical tablet) and API 37 (emulator and physical Pixel 7 Pro). On API 30 to 32 only line widths differed, in 90 Roboto rows, by at most 0.0156 px. On API 23 to 29, line counts and heights still matched, but Roboto line widths differed by up to 1.73 px, 24 to 28 one-line Roboto boxes were 1 px wider in layoutlib, and API 23 cut 2 ellipsis texts differently. Linux x86_64, in a container under Rosetta on a Mac, gave values byte for byte equal to macOS. Every replay had a control, each text 1 sp larger, that failed. The records are in R20, "Text laid out by layoutlib (#194)".

The owner decided on 2026-11-27: principle III may change to allow layoutlib on the server, only if tests prove it works, keeping real devices and emulators ruled out; API 30 is the line for exact text, older versions stay supported with a documented note; the target is AOSP and Google Android, not other makers' changes; and ADR 0025 is a candidate, not a decision.

## Options

| | L1: the whole scene in layoutlib | L2: desktop scene, text sizes from layoutlib | D: the device measures text (ADR 0025) |
|---|---|---|---|
| How | The producer composes, lays out and reads each screen with Android Compose inside layoutlib, in place of desktop Compose and Skiko | The scene stays desktop Compose; the measure policy of `ScaledBasicText` asks layoutlib for each text's size, in the same process | Each text waits for sizes the device reports |
| Exact text | Every value from API 33; every box, line break, height and baseline from API 30, with line widths within 0.0156 px on API 30 to 32; AOSP and Google Android only (P32) | The same as L1 for text; the rest of the layout is Compose's common code on both sides | Every device and version, by construction |
| Round trips before a screen shows | None more than today | None more than today | 1, or 2 when a wrapping text depends on a sibling (P25) |
| What the producer adds | layoutlib 60.3 MB, layoutlib-runtime for the server's OS (80.7 MB Linux, 79.3 MB macOS arm64), layoutlib-resources 34.8 MB, layoutlib-api 0.1 MB, about 11 MB of Android Compose and AndroidX jars repackaged in a jar of ours with generated `R` classes and our own `android.os.Build`, so no Android SDK file (P30); desktop Compose and Skiko (38.5 MB) go | All of L1, plus desktop Compose and Skiko stay, plus a separate class loader, since both define the same `androidx.compose.ui.*` class names (P31) | Nothing |
| Licences | Apache 2.0 (layoutlib, its runtime, AndroidX); the runtime carries the ICU licence and its fonts' licences (Noto under OFL 1.1) | The same | No change |
| Server cost, macOS M3 Pro, warm, the same 50-row screen (P29) | A new screen 9.5 ms median, an update 6.7 ms per frame, 0.72 to 0.93 MB of heap per live screen, about 2 s to start a process, about 340 new screens per second at most from 4 to 8 processes | Desktop scene costs (a new screen 22.3 ms, an update 4.0 ms, 0.76 MB per live screen) plus about 0.04 ms per new text measured (P28) | Desktop scene costs; the device spends 1.3 to 2.0 ms per new text on its main thread (M0) |
| Threads | One render at a time per process: every layoutlib render takes `Bridge.getLock()` (P31), the same shape as one scene thread ([ADR 0021](0021-producer-runs-every-scene-on-one-scene-thread.md)) | Text measuring is no faster on more threads (P28) | No change |
| Material `Text` and text from modules without the plugin | Text from modules without the plugin is allowed; Material `Text` needs Material in the jar and #224 first | Refused, as now | Refused, as now |
| Principle I | The producer composes, measures and arranges every screen and sends a positioned document; the device draws boxes and reports no text size | The same | The producer arranges every screen; the device reports only text sizes under constraints the producer chose |
| Merged work | The rewrite, `ScaledBasicText`, the font scale curve and the desktop reader's scene go; the reader's walk moves to Android Compose's same semantics API | All kept; `ScaledBasicText` becomes the hook to layoutlib | All kept |
| Principle III | Needs the amendment the owner allowed | Needs the same amendment | Unchanged |
| Principle VIII | Paparazzi, which is alpha, is not shipped: the producer drives layoutlib through its own small driver over `layoutlib-api` | The same | No change |
| Main risk | Shipping Android jars to plain JVM servers (proven only with a classpath a build prepared, P30); many live screens, each its own composition, in one process (unproven) | Two Compose runtimes in one process, in separate class loaders (unproven) | Device time on large screens, protocol size, and the rounds |
| Testability | Replays of recorded device layouts run in the JVM build and on Linux CI | The same for text | Needs devices for every proof |

## Decision

Proposed: option L1. It uses one engine for the whole screen, the same Android Compose the device runs, so text and every other measurement follow Android's code, and the replay tests that proved layoutlib become the build's own test. It removes the workarounds that desktop Compose needed: the compiler plugin's rewrite of `BasicText`, `ScaledBasicText` and the font scale curve. It adds no round trip, and for the same screen it holds about the same memory as desktop Compose and builds a new screen faster, at the cost of slower updates. L2 keeps all of layoutlib's weight and adds a second Compose runtime and a class loader split. D stays the fallback if layoutlib cannot be shipped to a plain JVM server.

Three proofs come first, in slices L1-0 to L1-1b of R20, before anything is removed: our own layoutlib driver holds several screens, each its own composition, and renders one without the others; a plain JVM server, built without the Android Gradle plugin, renders a screen from our published jars on Linux, with no Paparazzi at run time; and the cost run on the owner's Linux server (package ready, `.crew/reports-layoutlib/cost/`). If either of the first two fails, stop and fall back to D.

The fix found for M0's failure holds in every option: when a text's overflow is `Ellipsis`, the document carries as `maxLines` the line count its layout ended with, so the device, drawing in the fixed box, keeps the same lines (P27 on the tablet, P28 in layoutlib: 0 mismatches in 2,880 each, measured with `TextMeasurer`; that it equals what `BasicText` draws rests on M0).

## Consequences

- Good: text boxes, line breaks, heights and baselines are exact from API 30 and every value from API 33, on AOSP and Google Android, with no round trip, and a screen shows as soon as the producer laid it out.
- Good: server code can use text from library modules; the compiler plugin no longer rewrites Compose calls. Material's `Text` becomes possible once Material is in the jar and #224 is settled.
- Good: the proof is a JVM test that replays committed device recordings, so it runs in every build and on Linux CI.
- Bad: the producer depends on about 190 MB of Android artifacts and runs Android framework classes in its process, which needs the constitution's principle III and its platform constraint changed (R20 gives the wording).
- Bad: an update of a screen costs 6.7 ms against desktop Compose's 4.0 ms for the same screen, on macOS; memory per live screen is about the same (0.72 to 0.93 MB against 0.76 MB); native memory per screen and the Linux numbers are pending.
- Bad: layoutlib renders one screen at a time per process, so scale comes from more processes.
- Bad: devices on API 29 and older can differ by up to 1.73 px in line width and 1 px in box width, and every device draws text the bundled fonts lack with its own fallback fonts. Both are documented, not prevented.
- Bad: the producer's Android Compose version and the app's should match for text; a different version is not tested.
- Bad: merged work becomes unused: the plugin's rewrite (T130), `ScaledBasicText`, the reader's `UnscaledText` checks, the font scale curve in the handshake, and the desktop scene of T122.
- Follow-up: [ADR 0015](0015-producer-renders-with-compose-ui-foundation.md) is replaced by this record if it is accepted; ADR 0023 and ADR 0025 stay as records of the options before it.
