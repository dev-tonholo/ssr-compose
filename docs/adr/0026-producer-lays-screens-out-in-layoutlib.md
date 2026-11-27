# 0026. The producer lays screens out in layoutlib

- Status: Proposed
- Date: 2026-11-27
- Modules: `producer`, `compiler-plugin`, `core`, `consumer`, `contract-tests`

## Context

The producer lays every screen out and the device draws each node in the box it was sent ([ADR 0015](0015-producer-renders-with-compose-ui-foundation.md)). Desktop Compose on Skia breaks and rounds text lines differently from Android: probe P21 missed the text gate's rule in 10 of 60 texts ([research.md R20](../../specs/001-consumer-catalog-bridge/research.md#text-gate-t123)). [ADR 0025](0025-device-measures-server-text.md) proposed that the device measures each text and reports its sizes.

Then the owner asked how Compose previews match the emulator, which led to layoutlib, the Android renderer Android Studio previews use. A replay of 360 recorded texts per device through layoutlib 16.2.1 (Compose 1.12.1, layoutlib reports API 36) matched every check, including line count, height, box width, line ends, line widths, bottoms, baselines and the ellipsis flag, on API 33 (physical Motorola), API 35 (emulator), API 36 (emulator and physical tablet) and API 37 (emulator and physical Pixel 7 Pro). On API 30 to 32 only line widths differed, in 90 Roboto rows, by at most 0.02 px. On API 23 to 29, line counts and heights still matched, but Roboto line widths differed by up to 1.73 px, 24 to 28 one-line Roboto boxes were 1 px wider in layoutlib, and API 23 cut 2 ellipsis texts differently. Linux x86_64 gave values byte for byte equal to macOS. Every replay had a control, each text 1 sp larger, that failed. The records are in R20, "Text laid out by layoutlib (#194)".

The owner decided on 2026-11-27: principle III may change to allow layoutlib on the server, only if tests prove it works, keeping real devices and emulators ruled out; API 30 is the line for exact text, older versions stay supported with a documented note; the target is AOSP and Google Android, not other makers' changes; and ADR 0025 is a candidate, not a decision.

## Options

| | L1: the whole scene in layoutlib | L2: desktop scene, text sizes from layoutlib | D: the device measures text (ADR 0025) |
|---|---|---|---|
| How | The producer composes, lays out and reads each screen with Android Compose inside layoutlib, in place of desktop Compose and Skiko | The scene stays desktop Compose; the measure policy of `ScaledBasicText` asks layoutlib for each text's size, in the same process | Each text waits for sizes the device reports |
| Exact text | API 30 and later on AOSP and Google Android, by the replays of R20 (P32) | The same as L1 for text; the rest of the layout is Compose's common code on both sides | Every device and version, by construction |
| Round trips before a screen shows | None more than today | None more than today | 1, or 2 when a wrapping text depends on a sibling (P25) |
| What the producer adds | layoutlib 60.3 MB, layoutlib-runtime for the server's OS (80.7 MB Linux, 79.3 MB macOS arm64), layoutlib-resources 34.8 MB, layoutlib-api 0.1 MB, about 11 MB of Android Compose and AndroidX jars, and generated Android resource classes; desktop Compose and Skiko (38.5 MB) go | All of L1, plus desktop Compose and Skiko stay, plus a separate class loader, since both define the same `androidx.compose.ui.*` class names (P31) | Nothing |
| Licences | Apache 2.0 (layoutlib, its runtime, AndroidX); the runtime carries the ICU licence and its fonts' licences (Noto under OFL 1.1) | The same | No change |
| Server cost, macOS M3 Pro, warm (P29) | A new 50-row screen 9.5 ms median, an update 6.7 ms per frame, about 1.3 MB of heap per live screen, about 2 s to start a process, about 340 new screens per second at most from 4 to 8 processes | Desktop scene costs (P17: 2.3 ms per frame, 297 KB per screen) plus about 0.04 ms per new text measured (P28) | Desktop scene costs; the device spends 1.3 to 2.0 ms per new text on its main thread (M0) |
| Threads | One render at a time per process: every layoutlib render takes `Bridge.getLock()` (P31), the same shape as one scene thread ([ADR 0021](0021-producer-runs-every-scene-on-one-scene-thread.md)) | Text measuring is no faster on more threads (P28) | No change |
| Material `Text` and text from modules without the plugin | Supported: Android Compose scales text itself | Refused, as now | Refused, as now |
| Merged work | The rewrite, `ScaledBasicText`, the font scale curve and the desktop reader's scene go; the reader's walk moves to Android Compose's same semantics API | All kept; `ScaledBasicText` becomes the hook to layoutlib | All kept |
| Principle III | Needs the amendment the owner allowed | Needs the same amendment | Unchanged |
| Principle VIII | Paparazzi, which is alpha, is not shipped: the producer drives layoutlib through its own small driver over `layoutlib-api` | The same | No change |
| Main risk | Shipping Android jars to plain JVM servers (proven only with a classpath a build prepared, P30), and memory at 10,000 screens | Two Compose runtimes in one process, in separate class loaders (unproven) | Device time on large screens, protocol size, and the rounds |
| Testability | Replays of recorded device layouts run in the JVM build and on Linux CI | The same for text | Needs devices for every proof |

## Decision

Proposed: option L1. It uses one engine for the whole screen, the same Android Compose the device runs, so text and every other measurement follow Android's code, and the replay tests that proved layoutlib become the build's own test. It removes the workarounds that desktop Compose needed: the compiler plugin's rewrite of `BasicText`, `ScaledBasicText`, the font scale curve and the refusal of Material's `Text`. It adds no round trip. L2 keeps all of layoutlib's weight and adds a second Compose runtime and a class loader split. D stays the fallback if layoutlib cannot be shipped to a plain JVM server.

Two proofs come first, in slices L1-0 and L1-1 of R20, before anything is removed: a plain JVM server, built without the Android Gradle plugin, renders a screen from our published jars on Linux, with our own layoutlib driver and no Paparazzi at run time; and the cost run on the owner's Linux server (package ready, `.crew/reports-layoutlib/cost/`). If the first fails, stop and fall back to D.

The fix found for M0's failure holds in every option: when a text's overflow is `Ellipsis`, the document carries as `maxLines` the line count its layout ended with, so the device, drawing in the fixed box, keeps the same lines (P27 on the tablet, P28 in layoutlib: 0 mismatches in 2,880 each).

## Consequences

- Good: text is exact on API 30 and later on AOSP and Google Android, with no round trip, and a screen shows as soon as the producer laid it out.
- Good: server code can use Material's `Text` and text from library modules; the compiler plugin no longer rewrites Compose calls.
- Good: the proof is a JVM test that replays committed device recordings, so it runs in every build and on Linux CI.
- Bad: the producer depends on about 190 MB of Android artifacts and runs Android framework classes in its process, which needs the constitution's principle III and its platform constraint changed (R20 gives the wording).
- Bad: memory per live screen is about 4 times desktop Compose's (1.3 MB against 297 KB), and an update about 3 times slower (6.7 ms against 2.3 ms), on macOS; the Linux numbers are pending.
- Bad: layoutlib renders one screen at a time per process, so scale comes from more processes.
- Bad: devices on API 29 and older can differ by up to 1.73 px in line width and 1 px in box width, and every device draws text the bundled fonts lack with its own fallback fonts. Both are documented, not prevented.
- Bad: the producer's Android Compose version and the app's should match for text; a different version is not tested.
- Bad: merged work becomes unused: the plugin's rewrite (T130), `ScaledBasicText`, the reader's `UnscaledText` checks, the font scale curve in the handshake, and the desktop scene of T122.
- Follow-up: [ADR 0015](0015-producer-renders-with-compose-ui-foundation.md) is replaced by this record if it is accepted; ADR 0023 and ADR 0025 stay as records of the options before it.
