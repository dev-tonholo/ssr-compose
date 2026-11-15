# Implementation Plan: Consumer Catalog and Bridge

**Branch**: `001-consumer-catalog-bridge` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-consumer-catalog-bridge/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

An app developer marks composables with `@ConsumerComposable`. The app's build produces a catalog that describes them. A catalog is a bundle: one manifest, and one part for each app module that has marked composables. Each module produces its own part from its own compilation, so the build tool can cache it per module, and one step in the module that sets the catalog version collects the parts and writes the manifest.

A server project, which may be in another repository, is given the address of each catalog's manifest. It fetches the manifest and the parts at sync, checks them, saves them and pins them. A cacheable build step turns each saved catalog into an index, and the compiler reads that index to create one bridge per held catalog, in a package the server developer names for it: one `@Composable` function per component and one enum per choice list, with the same names and parameters and none of the app's code. Every bridge function carries its catalog version as a constant.

A server developer writes screens per catalog version and registers them under that version. The app connects, introduces itself, and gets the screen registered under the name it asked for, for its own catalog version. It renders what the server placed with its own composables, including content inside slots, and sends callbacks back to run lambdas on the server. A guard stops a screen that places a component from the bridge of another catalog version.

The catalog is build time only. Its model, validation, reader, writer, fingerprint and index live in a JVM library, `catalog-model`, that `tooling`, `compiler-plugin` and the two build-tool plugins (for the compiler plugin's id and option names only) use. `core`, `consumer` and `producer` never depend on it, and the running producer holds no catalog.

The approach rests on two experiments run before the design, both successful (see [research.md](research.md)):

- A K2 compiler plugin can create the bridge from a file, and the Compose compiler treats the result like hand-written code, as long as the bridge plugin runs first.
- A K2 compiler plugin can read marked composables, but under incremental compilation it sees only the changed files. So it writes one fragment per source file, and a build step merges them, here into the module's part.

This is the first feature, so it also creates the root build, all four deliverables, the build-time catalog library, and two thin build-tool plugins.

## Technical Context

**Language/Version**: Kotlin 2.4.21 with the K2 compiler. JVM bytecode target 17.

**Primary Dependencies**: Compose Multiplatform 1.12.2 (runtime on the producer; runtime, foundation and UI on the consumer), kotlinx.serialization 1.13.0, kotlinx.coroutines 1.11.0, the Kotlin compiler's plugin API (`kotlin-compiler-embeddable` 2.4.21), Android Gradle Plugin 9.5.1. The samples also use Ktor for a WebSocket. No transport and no logging library in `core`, `consumer` or `producer`.

**Storage**: none at run time. Catalogs (a manifest and its parts per version) and a lock file are kept in the repositories that use them. The catalog index is a build output and is never committed or shared.

**Testing**: `kotlin.test` with power-assert, Turbine and `kotlinx-coroutines-test`, hand-written fakes. Compile tests for the compiler plugin with kotlin-compile-testing (`dev.zacsweers.kctfork:core` 0.14.0), reading their sources from `*.fixture.kt` files under `compiler-plugin/src/test/resources/fixtures/`. Gradle TestKit for the Gradle plugin, including build cache checks. Compose UI tests on a device for the consumer. Contract tests in their own module. Checks on a device or emulator with the `android` CLI.

**Target Platform**: `core` for JVM and Android. `consumer` as a Kotlin Multiplatform library with an Android target only, `minSdk` 23. `producer`, `catalog-model`, the compiler plugin and the tooling for a plain JVM.

**Project Type**: libraries, a compiler plugin and build-tool plugins.

**Performance Goals**: an app is accepted and shows its first screen in under 1 second on a local network. A tap shows its result in under 300 milliseconds. A device change reaches the screen in under 1 second. With a catalog of 20,000 components, a build with nothing changed does no catalog work, a change in one app module produces again only that module's part and the manifest, catalog outputs are reusable from a build cache shared between machines, and the catalog adds at most 2 seconds to a clean compile of a server module. The 2 second figure is provisional until T031a measures it.

**Constraints**: the app and the server share only a catalog. The consumer and producer libraries never depend on each other, and neither depends on `catalog-model`. `core` depends on no Android, server framework or transport. The compiler never uses the network and never parses the catalog's JSON: it reads only the index. Catalog outputs hold no absolute path and no timestamp. Everything the compiler and the build create can be written by hand. Nothing works with only one build tool.

**Scale/Scope**: no hard limit on components or parameters per catalog; tested at 20,000 components with about 80,000 parameters; the manifest and each part capped at 16 MiB by default; 10,000 sessions on one producer; 10 catalog versions and 3 contract versions served at once; callback data up to 1 megabyte.

Checked and not checked, as principle VII asks:

- Checked by experiment: the compiler creating the bridge, the plugin order rule and its guard, the enum workaround, reading marked composables with every parameter kind, precise compile errors, fragments plus merge under incremental compilation, byte-identical output, and the compile-testing library on this Kotlin version.
- Checked against Maven metadata: every version above.
- Checked in the code already written for T011 and T019: the first frame's 4-byte contract version prefix, `Answer` wrapped by `ProducerMessage.Answered`, `MessageDecodingException`, and `MessageCodec.json()`.
- Not checked: anything in the IDE; a Kotlin Multiplatform module with an Android target for producing a part, with variants; one module collecting the parts of the modules it depends on; the build cache and the configuration cache for the catalog steps; the index format; creating a bridge declaration only when code names it; several bridges with the same function names in different packages of one module; the time and memory to compile against a 20,000 component catalog, and the 2 second figure; running the fetch on IDE sync; the Kotlin toolchain plugin, including plugin order there and building an Android library; creating the consumer-side renderers in the compiler; the `RemoteAction` types as created parameter types, and a lambda converted to one; defaults and nullable parameters in created functions; the compile error for a component that cannot be placed; an annotation on a slot's type; recomposition through a bridge function; the 10,000 session target. Each has a task that settles it, and the riskiest come first. T031a, the scale gate, settles the index format, bridge declarations on demand and the 2 second figure.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Checked against constitution v1.2.0.

| Principle | Result | How the plan meets it |
|---|---|---|
| I. Real SSR, Not SDR | Pass | Screens are composables that run on the producer with state, effects and lambdas. Server content sits inside app composables through slots. Content the app has no component for is left to the remote composables feature, and the catalog already describes where it goes. |
| II. Four Deliverables, One Contract | Deviation | The handshake and messages have one definition, in `core`. The catalog format has one definition too, but it lives in `catalog-model`, not in `core`, because it is needed only at build time. `catalog-model` counts as part of the compiler plugin deliverable, next to the tooling library. `consumer` and `producer` depend on `core` only. The build produces the catalog and the compiler plugin creates the bridges, which hold none of the app's code. The two build-tool plugins sit over one tooling library and offer the same steps. Everything can be written by hand. Consumer and producer naming is used throughout, and `RemoteAction` is a name for something that runs across the connection. See Complexity Tracking. |
| III. Our Own Document Format | Not in scope | No document is produced, and no dependency on AndroidX Remote Compose is added. |
| IV. Catalogs and Handshake Before Rendering | Pass, with a wording conflict | The catalog is produced from annotations, has a version, and is the only thing two builds share. It is a bundle of plain descriptor files (a manifest and its parts), where the principle speaks of "a plain descriptor file". It is fetched at sync, saved and pinned. The consumer sends its catalog version and never the catalog. The producer serves only catalog versions it was built with, through their bridges, and holds no catalog at run time. A guard stops any screen that would send a component the session's catalog does not have. All handshake values are sent. Mid-session changes are answered. The producer catalog is a later feature. |
| V. Real-Time, Self-Recovering, Secure Connection | Deviation | This feature ships no transport, so it ships no encryption or recovery. See Complexity Tracking. No public API exposes a transport type. |
| VI. Test-First | Pass | Tests first, hand-written fakes, compile tests for the compiler plugin, a contract test module, build cache tests, and a device check per story. |
| VII. Evidence Over Assumption | Pass | The two riskiest questions were answered by experiment before the design. What is not checked is listed with the task that settles it. Bounds are named, the scale gate measures the build at 20,000 components, and a load test measures the producer. |
| VIII. Small Public API, Simple Design | Pass, with a noted risk | Each contract file lists the few public types. The compiler plugin uses experimental compiler APIs and one workaround in compiler internals. Neither is in a public API of the libraries, and both are covered by tests that must pass before a new Kotlin version is supported. The extra `catalog-model` module is justified in Complexity Tracking. |
| IX. Versioned Producer, Old Apps Keep Working | Pass | Contract version in the introduction, one saved catalog and one bridge per catalog version, screens registered per catalog version, a version served only while it has screens, clear refusals, and a compatibility matrix in the contract tests. |

Principles II and IV still use wording from the earlier design: "a catalog file", and the catalog format living in the core library. The owner decides whether to amend them. This plan does not change the constitution.

Re-check after Phase 1 design: the same results hold. The design added no dependency between `consumer` and `producer`, no dependency from `core`, `consumer` or `producer` on `catalog-model`, no transport type in a public API, and no deviation beyond those in Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/001-consumer-catalog-bridge/
├── plan.md              # This file
├── research.md          # Experiment results, decisions, what was checked
├── data-model.md        # Entities, session states, the guard
├── quickstart.md        # How to prove the feature end to end
├── contracts/
│   ├── catalog-bundle.md # The catalog the two builds share: manifest and parts
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
gradle.properties                    # ssrcompose.compiler.plugin.id: the one home of the compiler plugin id
gradle/libs.versions.toml            # versions, libraries, and every plugin id (convention plugins with no version)
build-logic/                         # convention plugins as Plugin classes, registered in gradlePlugin { }

core/                                # Kotlin Multiplatform: jvm, android
└── src/commonMain/kotlin/dev/tonholo/ssrcompose/core/
    ├── annotation/                  # @ConsumerComposable, @ProducerComposable
    ├── catalog/                     # CatalogVersion only: a label
    ├── handshake/                   # Introduction, DeviceContext, AppIdentity, Answer
    ├── data/                        # DataValue, ValueKind (the one definition of value kinds)
    ├── message/                     # messages, MessageFrame, MessageCodec, the JSON codec
    └── connection/                  # Connection, InMemoryConnection

catalog-model/                       # Kotlin JVM, build time only
└── src/main/kotlin/dev/tonholo/ssrcompose/catalog/
    ├── model/                       # ChoiceList, Parameter, ComponentDescriptor, PartName (kinds from core)
    ├── bundle/                      # CatalogManifest, CatalogPart, reader, writer, fingerprint, size cap
    ├── validation/                  # one part, and the checks across parts
    ├── index/                       # the index the compiler plugin reads, format from T031a
    └── options/                     # CompilerPluginOptions and the generated plugin id (buildconfig)

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
    │   ├── Producer.kt, ProducerBuilder.kt, ConsumerComponent.kt
    │   ├── RemoteAction.kt, Callback.kt, LocalConsumer.kt
    │   ├── ProducerLimits.kt, ProducerObserver.kt
    │   └── internal/                # session, screen registry, live composition, actions, guard, validation
    ├── test/
    └── loadTest/

compiler-plugin/                     # Kotlin JVM, K2
└── src/
    ├── main/kotlin/dev/tonholo/ssrcompose/compiler/
    │   ├── consumer/                # FIR checker, fragment writer, renderer creation
    │   └── producer/                # FIR declarations and IR bodies for each bridge, order guard
    └── test/
        ├── kotlin/                  # compile tests with kotlin-compile-testing
        └── resources/fixtures/      # *.fixture.kt sources, a source folder for the IDE, not compiled

tooling/                             # Kotlin JVM: part, bundle, check, dump, sync, verify, lock file, index step
gradle-plugin/                       # thin Gradle wrapper over tooling: one KotlinCompilerPluginSupportPlugin
toolchain-plugin/                    # thin Kotlin toolchain wrapper over tooling

contract-tests/                      # test-only: producer output through the consumer's reader

samples/
├── catalogs/                        # the only files the two sample builds share: one folder per version
├── app/                             # its own build: Android app with marked composables
├── server/                          # its own build: JVM server with a WebSocket connection
├── toolchain-probe/                 # the smallest Kotlin toolchain project, for the early experiment
└── toolchain-server/                # the sample server set up for the Kotlin toolchain

poc/                                 # unchanged, reference only
```

Module dependencies:

| Module | Depends on |
|---|---|
| `core` | nothing in this repo |
| `catalog-model` | `core`, for `CatalogVersion` and `ValueKind` only |
| `consumer`, `producer` | `core` |
| `compiler-plugin`, `tooling` | `catalog-model` |
| `gradle-plugin`, `toolchain-plugin` | `tooling`, and `catalog-model` for the compiler plugin's id and option names |

How the build is written, following the owner's pattern in the thunderbird repository (its `build-plugin` and the pii-safe compiler plugin):

- Convention plugins in `build-logic` are Plugin classes, each registered in the `gradlePlugin { plugins { } }` block of `build-logic/build.gradle.kts` with an id `dev.tonholo.ssrcompose.conventions.<concern>` and its implementation class. Each id is listed in `gradle/libs.versions.toml` under `[plugins]` with no version, and a module applies it with `alias(libs.plugins...)`. There is no precompiled script plugin, and no module build file writes a plugin id as a string.
- The compiler plugin id has one home: the `ssrcompose.compiler.plugin.id` property in the root `gradle.properties`. The buildconfig Gradle plugin (`com.github.gmazzo.buildconfig`, listed in the catalog) turns it into one generated constant in `catalog-model`, next to the option names. `compiler-plugin`, `gradle-plugin` and `toolchain-plugin` all read that one constant. It is not a constant in `core`, and no literal id appears anywhere else, including the toolchain plugin's yaml files.
- The option names of the compiler plugin are written once, in `catalog-model` (`options/CompilerPluginOptions.kt`). It is the one module that `compiler-plugin` and both build-tool plugins can see without touching `core`: `compiler-plugin` depends on it already, and `gradle-plugin` and `toolchain-plugin` depend on it for the id and these names, as the module table shows. T113 creates both.
- The Gradle plugin `dev.tonholo.ssrcompose` is one `KotlinCompilerPluginSupportPlugin` class. It adds `core`, which holds `@ConsumerComposable` and `@ProducerComposable`, to the compile classpath of each compilation by itself, at the plugin's own version, so a developer applies this one plugin and adds nothing else to mark composables. It is registered in the `gradlePlugin` block of `gradle-plugin/build.gradle.kts` and listed in the catalog as `ssrcompose`.
- Compile tests use kotlin-compile-testing. Their sources are `*.fixture.kt` files under `compiler-plugin/src/test/resources/fixtures/`. That folder is added as a test source folder, so the IDE reads the fixtures as Kotlin, and `*.fixture.kt` is excluded from test compilation.
- Our own samples apply the Gradle plugin through the catalog with `alias(libs.plugins.ssrcompose)`, from the first task that needs it (T056 and T057); T007 creates the sample builds without it, since the plugin exists only after T024. Each sample build reads the root `gradle/libs.versions.toml` as its `libs` catalog.

Not checked: the buildconfig plugin's version against Maven metadata, which T001 checks when it adds it.

**Structure Decision**: four deliverables as modules at the root (`core`, `consumer`, `producer`, `compiler-plugin`), the build-time `catalog-model` library, the tooling library and its two build-tool wrappers, one test-only module, and samples as separate builds that share none of their own code: each uses only the root build, its version catalog and the shared folder of catalogs. `poc/` stays a separate Gradle project and nothing depends on it. The reasons are in [research.md](research.md), sections R5, R11 and R17.

How the build handles the catalog:

- On the app side, the compiler plugin writes one fragment per source file. A cacheable step in each module with marked composables, `ssrCatalogPart`, merges that module's fragments into its part. A step in the module that sets the catalog version, `ssrCatalogBundle`, collects the parts of the modules it depends on, runs the checks across parts, and writes the manifest. `ssrCatalogCheck` compares the bundle with the saved one of the same version, and `ssrCatalogDump` saves it.
- On the server side, `ssrCatalogSync` fetches and pins, and `ssrCatalogVerify`, part of the normal check, fails when a saved catalog no longer matches its lock file or its manifest. A cacheable step per held catalog, `ssrCatalogIndex`, turns the saved bundle into an index. The compiler plugin reads only the index. The index is internal, is not shared between builds, and its format is settled by the scale gate, T031a. This plan does not fix that format.
- A bridge declaration should be created only when code names it, so a server module that calls 20 of 20,000 composables does not pay for the rest. This is not checked. T031a proves or disproves it, and reports the cost if it does not hold.
- Every step declares its inputs and outputs with relative paths and no timestamps, so the build cache can reuse its results across machines.

Delivery order, with the riskiest unknowns first and something visible on a device early:

1. Root build and the module shape, proved by building it, then the `catalog-model` module and the move of the catalog code out of `core`.
2. The riskiest unchecked facts, each as a small experiment inside the real modules: the part from a Kotlin Multiplatform Android module with variants, the consumer-side renderers, the `RemoteAction` types, defaults and nullable parameters as created types, two bridges with the same names in one module, recomposition through a bridge function, the scale gate at 20,000 components, and a small Kotlin toolchain probe that covers plugin order and whether it can build an Android library.
3. Story 1: mark, produce, sync, index, bridge, connect, render one composable on a device, with the samples as separate builds, parts collected from several modules, and the build cache checks.
4. Story 2: callbacks.
5. Story 3: slots.
6. Story 4: several catalog and contract versions, screens per catalog version, the guard, refusals.
7. Story 5: device values and mid-session changes.
8. The full Kotlin toolchain plugin, the load test, the build measurements at 20,000 components, ADRs, and the real commands in `.ai/guidelines.md` and `CONTRIBUTING.md`.

Decisions that become ADRs when implemented are listed in [research.md](research.md), section R15.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Principle V: no encryption and no connection recovery in this feature | The spec puts transport, recovery and encryption in the later connection feature. The libraries ship only a `Connection` interface and an in-memory one for tests. The sample socket is cleartext, bound to the local machine, for development only. | Adding a secure, self-recovering transport now would pull a whole feature into this one, and would pick a transport before the measurements principle V asks for. |
| The consumer side may work from Gradle only, if the experiment in T030 finds that the Kotlin toolchain cannot build an Android library | The constitution asks for every step to work from every supported build tool. The producer side is expected to work from both. | Holding the whole feature until the Kotlin toolchain can build an Android library was turned down by the owner. If T030 finds it can, this row is removed. |
| Experimental compiler APIs and one workaround in compiler internals for enum entries | The owner's design has a real enum per choice list, and the compiler's typed API cannot create enum entries. Top-level generation is itself marked experimental. | A class with one constant per choice is the fallback if the workaround breaks in the IDE or in a new Kotlin version. It was not chosen first because it does not match the owner's example and loses the exhaustive `when`. |
| A test-only module, `contract-tests`, and samples as two separate builds | Principle VI asks for the producer's output to run through the consumer's reader, and principle II forbids the two libraries from depending on each other. The spec requires proof that a server builds with none of the app's code. | Tests inside either library would put the other on its build. One shared sample build would not prove that the two sides share only a catalog. |
| Principle II: an extra library, `catalog-model`, holds the catalog format instead of `core` | The owner decided the catalog is build time only (FR-069). The catalog model, its validation, the manifest and part reader and writer, the fingerprint and the index are used only by `tooling` and `compiler-plugin`. The two build-tool plugins also depend on `catalog-model`, but only for the compiler plugin's id and option names. Keeping them in `core` would ship a JSON reader, a hashing step and validation code in every app and every server that never use them. `catalog-model` counts as part of the compiler plugin deliverable, like the tooling library. | Keeping the catalog code in `core`, as principle II's wording asks, was turned down because `core` is a run-time dependency of `consumer` and `producer`. Putting it inside `tooling` was turned down because `compiler-plugin` must not depend on the build-tool library. |
