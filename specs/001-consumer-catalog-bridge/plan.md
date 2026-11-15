# Implementation Plan: Consumer Catalog and Bridge

**Branch**: `001-consumer-catalog-bridge` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-consumer-catalog-bridge/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

An app developer marks composables with `@ConsumerComposable`. The app's build produces a catalog file that describes them. A server project, which may be in another repository, is given the file's location, fetches it at sync, saves it and pins it. The compiler then creates a bridge in the server project: one `@Composable` function per component and one enum per choice list, with the same names and parameters and none of the app's code. A server developer writes screens with them. The app connects, introduces itself, and renders what the server placed with its own composables, including content inside slots, and sends callbacks back to run lambdas on the server.

The approach rests on two experiments run before the design, both successful (see [research.md](research.md)):

- A K2 compiler plugin can create the bridge from a file, and the Compose compiler treats the result like hand-written code, as long as the bridge plugin runs first.
- A K2 compiler plugin can read marked composables, but under incremental compilation it sees only the changed files. So it writes one fragment per source file, and a build step merges them into the catalog.

This is the first feature, so it also creates the root build and all four deliverables, plus two thin build-tool plugins.

## Technical Context

**Language/Version**: Kotlin 2.4.21 with the K2 compiler. JVM bytecode target 17.

**Primary Dependencies**: Compose Multiplatform 1.12.2 (runtime on the producer; runtime, foundation and UI on the consumer), kotlinx.serialization 1.13.0, kotlinx.coroutines 1.11.0, the Kotlin compiler's plugin API (`kotlin-compiler-embeddable` 2.4.21), Android Gradle Plugin 9.5.1. The samples also use Ktor for a WebSocket. No transport and no logging library in `core`, `consumer` or `producer`.

**Storage**: none at run time. Catalog files and a lock file are kept in the repositories that use them.

**Testing**: `kotlin.test` with power-assert, Turbine and `kotlinx-coroutines-test`, hand-written fakes. Compile tests for the compiler plugin with `dev.zacsweers.kctfork:core` 0.14.0. Gradle TestKit for the Gradle plugin. Compose UI tests on a device for the consumer. Contract tests in their own module. Checks on a device or emulator with the `android` CLI.

**Target Platform**: `core` for JVM and Android. `consumer` as a Kotlin Multiplatform library with an Android target only, `minSdk` 23. `producer`, the compiler plugin and the tooling for a plain JVM.

**Project Type**: libraries, a compiler plugin and build-tool plugins.

**Performance Goals**: an app with a 200 component catalog is accepted and shows its first screen in under 1 second on a local network. A tap shows its result in under 300 milliseconds. A device change reaches the screen in under 1 second.

**Constraints**: the app and the server share only a catalog file. The consumer and producer libraries never depend on each other. `core` depends on no Android, server framework or transport. The compiler never uses the network. Everything the compiler and the build create can be written by hand. Nothing works with only one build tool.

**Scale/Scope**: up to 1,000 components per catalog, 50 parameters per component, 10,000 sessions on one producer, 10 catalog versions and 3 contract versions served at once, callback data up to 1 megabyte.

Checked and not checked, as principle VII asks:

- Checked by experiment: the compiler creating the bridge, the plugin order rule and its guard, the enum workaround, reading marked composables with every parameter kind, precise compile errors, fragments plus merge under incremental compilation, byte-identical output, and the compile-testing library on this Kotlin version.
- Checked against Maven metadata: every version above.
- Not checked: anything in the IDE; a Kotlin Multiplatform module with an Android target for producing a catalog, with variants; composables spread over several modules of one app; running the fetch on IDE sync; the Kotlin toolchain plugin, including plugin order there and building an Android library; creating the consumer-side renderers in the compiler; the `RemoteAction` types as created parameter types, and a lambda converted to one; defaults and nullable parameters in created functions; the compile error for a component that cannot be placed; an annotation on a slot's type; recomposition through a bridge function; the 10,000 session target. Each has a task that settles it, and the riskiest come first.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Checked against constitution v1.2.0.

| Principle | Result | How the plan meets it |
|---|---|---|
| I. Real SSR, Not SDR | Pass | Screens are composables that run on the producer with state, effects and lambdas. Server content sits inside app composables through slots. Content the app has no component for is left to the remote composables feature, and the catalog already describes where it goes. |
| II. Four Deliverables, One Contract | Pass | `core` holds the one definition of the catalog format, handshake and messages. `consumer` and `producer` depend on `core` only. The build produces the catalog and the compiler plugin creates the bridge, which holds none of the app's code. The two build-tool plugins sit over one tooling library and offer the same steps. Everything can be written by hand. Consumer and producer naming is used throughout, and `RemoteAction` is a name for something that runs across the connection. |
| III. Our Own Document Format | Not in scope | No document is produced, and no dependency on AndroidX Remote Compose is added. |
| IV. Catalogs and Handshake Before Rendering | Pass | The catalog is produced from annotations, is a plain file with a version, and is the only thing two builds share. It is fetched at sync, saved and pinned. The consumer sends its catalog version and never the catalog. All handshake values are sent. Mid-session changes are answered. The producer catalog is a later feature. |
| V. Real-Time, Self-Recovering, Secure Connection | Deviation | This feature ships no transport, so it ships no encryption or recovery. See Complexity Tracking. No public API exposes a transport type. |
| VI. Test-First | Pass | Tests first, hand-written fakes, compile tests for the compiler plugin, a contract test module, and a device check per story. |
| VII. Evidence Over Assumption | Pass | The two riskiest questions were answered by experiment before the design. What is not checked is listed with the task that settles it. Bounds are named and a load test measures them. |
| VIII. Small Public API, Simple Design | Pass, with a noted risk | Each contract file lists the few public types. The compiler plugin uses experimental compiler APIs and one workaround in compiler internals. Neither is in a public API of the libraries, and both are covered by tests that must pass before a new Kotlin version is supported. |
| IX. Versioned Producer, Old Apps Keep Working | Pass | Contract version in the introduction, a set of supported contract and catalog versions, one saved catalog per version, parameters fitted per version, clear refusals, and a compatibility matrix in the contract tests. |

Re-check after Phase 1 design: the same results hold. The design added no dependency between `consumer` and `producer`, no transport type in a public API, and no deviation beyond those in Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/001-consumer-catalog-bridge/
├── plan.md              # This file
├── research.md          # Experiment results, decisions, what was checked
├── data-model.md        # Entities, session states, fitting rules
├── quickstart.md        # How to prove the feature end to end
├── contracts/
│   ├── catalog-file.md  # The file the two builds share
│   ├── build-tooling.md # What the build does on each side
│   ├── messages.md      # The messages both sides share
│   ├── producer-api.md  # What a server developer writes
│   └── consumer-api.md  # What an app developer writes
├── checklists/
│   └── requirements.md
└── tasks.md             # Written by /speckit-tasks, not by this command
```

### Source Code (repository root)

```text
settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
build-logic/                         # convention plugins dev.tonholo.ssrcompose.conventions.*

core/                                # Kotlin Multiplatform: jvm, android
└── src/commonMain/kotlin/dev/tonholo/ssrcompose/core/
    ├── annotation/                  # @ConsumerComposable, @ProducerComposable
    ├── catalog/                     # catalog model, file reader and writer, fingerprint, validation
    ├── handshake/                   # Introduction, DeviceContext, AppIdentity, Answer
    ├── data/                        # DataValue, fitting
    ├── message/                     # messages, MessageCodec, the JSON codec
    └── connection/                  # Connection, InMemoryConnection

consumer/                            # Kotlin Multiplatform: android
└── src/
    ├── commonMain/kotlin/dev/tonholo/ssrcompose/consumer/
    │   ├── SsrScreen.kt
    │   ├── ComponentBindings.kt
    │   ├── ScreenProblem.kt
    │   └── internal/                # session, device values, rendering, renderer index
    ├── androidMain/
    ├── commonTest/
    └── androidDeviceTest/

producer/                            # Kotlin JVM
└── src/
    ├── main/kotlin/dev/tonholo/ssrcompose/producer/
    │   ├── Producer.kt, CatalogSet.kt, ConsumerComponent.kt, RemoteAction.kt
    │   ├── OrElse.kt, LocalConsumer.kt, ProducerObserver.kt
    │   └── internal/                # session, live composition, actions, validation
    ├── test/
    └── loadTest/

compiler-plugin/                     # Kotlin JVM, K2
└── src/
    ├── main/kotlin/dev/tonholo/ssrcompose/compiler/
    │   ├── consumer/                # FIR checker, fragment writer, renderer creation
    │   └── producer/                # FIR declarations and IR bodies for the bridge, order guard
    └── test/                        # compile tests

tooling/                             # Kotlin JVM: merge, check, fetch, pin, lock file
gradle-plugin/                       # thin Gradle wrapper over tooling
toolchain-plugin/                    # thin Kotlin toolchain wrapper over tooling

contract-tests/                      # test-only: producer output through the consumer's reader

samples/
├── catalogs/                        # the only thing the two sample builds share
├── app/                             # its own build: Android app with marked composables
├── server/                          # its own build: JVM server with a WebSocket connection
├── toolchain-probe/                 # the smallest Kotlin toolchain project, for the early experiment
└── toolchain-server/                # the sample server set up for the Kotlin toolchain

poc/                                 # unchanged, reference only
```

**Structure Decision**: four deliverables as modules at the root (`core`, `consumer`, `producer`, `compiler-plugin`), the tooling library and its two build-tool wrappers, one test-only module, and samples as separate builds that share only a folder of catalog files. `poc/` stays a separate Gradle project and nothing depends on it. The reasons are in [research.md](research.md), sections R5 and R11.

Delivery order, with the riskiest unknowns first and something visible on a device early:

1. Root build and the module shape, proved by building it.
2. The riskiest unchecked facts, each as a small experiment inside the real modules: the catalog from a Kotlin Multiplatform Android module with variants, the consumer-side renderers, the `RemoteAction` types, defaults and nullable parameters as created types, recomposition through a bridge function, and a small Kotlin toolchain probe that covers plugin order and whether it can build an Android library.
3. Story 1: mark, produce, sync, bridge, connect, render one composable on a device, with the samples as separate builds.
4. Story 2: callbacks.
5. Story 3: slots.
6. Story 4: several catalog and contract versions, replacements, refusals.
7. Story 5: device values and mid-session changes.
8. The full Kotlin toolchain plugin, the load test, composables in several modules, ADRs, and the real commands in `.ai/guidelines.md` and `CONTRIBUTING.md`.

Decisions that become ADRs when implemented are listed in [research.md](research.md), section R15.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Principle V: no encryption and no connection recovery in this feature | The spec puts transport, recovery and encryption in the later connection feature. The libraries ship only a `Connection` interface and an in-memory one for tests. The sample socket is cleartext, bound to the local machine, for development only. | Adding a secure, self-recovering transport now would pull a whole feature into this one, and would pick a transport before the measurements principle V asks for. |
| The consumer side may work from Gradle only, if the experiment in T030 finds that the Kotlin toolchain cannot build an Android library | The constitution asks for every step to work from every supported build tool. The producer side is expected to work from both. | Holding the whole feature until the Kotlin toolchain can build an Android library was turned down by the owner. If T030 finds it can, this row is removed. |
| Experimental compiler APIs and one workaround in compiler internals for enum entries | The owner's design has a real enum per choice list, and the compiler's typed API cannot create enum entries. Top-level generation is itself marked experimental. | A class with one constant per choice is the fallback if the workaround breaks in the IDE or in a new Kotlin version. It was not chosen first because it does not match the owner's example and loses the exhaustive `when`. |
| A test-only module, `contract-tests`, and samples as two separate builds | Principle VI asks for the producer's output to run through the consumer's reader, and principle II forbids the two libraries from depending on each other. The spec requires proof that a server builds with none of the app's code. | Tests inside either library would put the other on its build. One shared sample build would not prove that the two sides share only a file. |
