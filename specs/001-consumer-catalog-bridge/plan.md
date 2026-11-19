# Implementation Plan: Consumer Catalog and Bridge

**Branch**: `001-consumer-catalog-bridge` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-consumer-catalog-bridge/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

An app developer marks composables with `@ConsumerComposable`. The app's build produces a catalog that describes them. A catalog is a bundle: one manifest, and one part for each app module that has marked composables. Each module produces its own part from its own compilation, so the build tool can cache it per module, and one step in the module that sets the catalog version collects the parts and writes the manifest.

A server project, which may be in another repository, is given the address of each catalog's manifest. It fetches the manifest and the parts via `ssrCatalogSync` (wired into IDE sync through JetBrains `idea-ext` `afterSync`, unverified in Android Studio, and runnable directly), checks them, saves them and pins them. A cacheable build step turns each saved catalog into an index, and the compiler reads that index to create one bridge per held catalog, in a package the server developer names for it: one `@Composable` function for each component the module names and one enum per choice list, with the same names and parameters and none of the app's code. Every bridge function carries its catalog version as a constant.

A server developer writes screens per catalog version and registers them under that version. The app connects, introduces itself, and gets the screen registered under the name it asked for, for its own catalog version. It shows the placed components at the top level through an internal provisional fallback (`ProvisionalScreenContent`, FR-034) until server-rendered documents own layout. It renders each one with its own composable, including content inside slots, and sends callbacks back to run lambdas on the server. A guard stops a screen that places a component from the bridge of another catalog version.

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

**Performance Goals**: an app is accepted and shows its first screen in under 1 second on a local network. A tap shows its result in under 300 milliseconds. A device change reaches the screen in under 1 second. With a catalog of 20,000 components, a build with nothing changed does no catalog work, a change in one app module produces again only that module's part and the manifest, catalog outputs are reusable from a build cache shared between machines, and the catalog adds at most 2 seconds to a clean compile of a server module. The owner kept the 2 second figure after T031a measured a prototype, and the real plugin meets it ([research.md R16](research.md#measurements)).

**Constraints**: the app and the server share only a catalog. The consumer and producer libraries never depend on each other, and neither depends on `catalog-model`. `core` depends on no Android, server framework or transport. The compiler never uses the network and never parses the catalog's JSON: it reads only the index. Catalog outputs hold no absolute path and no timestamp. Everything the compiler and the build create can be written by hand. Nothing works with only one build tool.

**Scale/Scope**: no hard limit on components or parameters per catalog; tested at 20,000 components with about 80,000 parameters; the manifest and each part capped at 16 MiB by default; 10,000 sessions on one producer; 10 catalog versions and 3 contract versions served at once; callback data up to 1 megabyte.

Checked and not checked, as principle VII asks:

- Checked by experiment: the compiler creating the bridge, the plugin order rule and its guard, the enum workaround, reading marked composables with every parameter kind, precise compile errors, the compile error for a component that cannot be placed (in the module that creates the bridge, and in a module that only reads it when the plugin runs there with `mode=check`), creating a consumer-side renderer for a marked composable with a text parameter, fragments plus merge under incremental compilation, byte-identical output, and the compile-testing library on this Kotlin version.
- Checked against Maven metadata: every version above.
- Checked in the code already written for T011 and T019: the first frame's 4-byte contract version prefix, `Answer` wrapped by `ProducerMessage.Answered`, `MessageDecodingException`, and `MessageCodec.json()`.
- Not checked: the IDE beyond the owner's test of named bridges on IntelliJ IDEA 2026.3 ([research.md R16](research.md#named-bridges-in-the-real-plugin)); a Kotlin Multiplatform module with an Android target for producing a part, with variants; one module collecting the parts of the modules it depends on; the build cache and the configuration cache for the catalog steps; running the fetch on IDE sync; the Kotlin toolchain plugin itself; the effect of the plugin order flag and the default plugin order in the Kotlin toolchain; consumer-side renderers against the real `:consumer` library (`PlacedComponentScope` and `PlacedValueException`), for the remaining parameter kinds, defaults, a user function named `<Composable>Renderer` in the same package, the renderer index and real Gradle incremental builds (T055, T064, T075); `RemoteAction0` and `RemoteAction2` to `RemoteAction4` as created parameter types; a required nullable parameter and an optional callback in created functions; the removal of `mode=check`, which research.md R16 found is no longer needed for created bridges (T047, T101); an annotation on a slot's type; the 10,000 session target. Each has a task that settles it, and the riskiest come first. T031a, the scale gate, is done: the owner decided the index format, bridge declarations on demand and the 2 second figure, and the real plugin meets that figure ([research.md R16](research.md#r16-scale-and-build-performance)). T030 checked that the Kotlin toolchain accepts the plugin order flag and can compile an Android library with the plugin (research.md [R2](research.md#r2-the-order-of-the-two-compiler-plugins-is-a-correctness-matter) and [R5](research.md#the-kotlin-toolchain-probe-t030)); the effect of the flag and the default order wait for the IR step.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Checked against constitution v1.2.0.

| Principle | Result | How the plan meets it |
|---|---|---|
| I. Real SSR, Not SDR | Partial | Screens are composables that run on the producer with state, effects and lambdas. Server content sits inside app composables through slots. The principle is only partly met here: content the app has no component for needs server-rendered documents, which Feature 2 adds, and the catalog already describes where it goes. |
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
    └── connection/                  # Connection

core-testing/                        # Kotlin Multiplatform: jvm, android. Published test support for core
└── src/
    ├── commonMain/kotlin/dev/tonholo/ssrcompose/core/testing/   # InMemoryConnection and its pair()
    └── commonTest/kotlin/dev/tonholo/ssrcompose/core/testing/   # InMemoryConnectionTest

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
    │   └── internal/                # session/, screen/, read/ and device/ by role, renderer index
    ├── androidMain/
    ├── commonTest/
    └── androidDeviceTest/

producer/                            # Kotlin JVM
└── src/
    ├── main/kotlin/dev/tonholo/ssrcompose/producer/
    │   ├── Producer.kt, ProducerBuilder.kt, ConsumerComponent.kt
    │   ├── RemoteAction1.kt, Callback.kt, Consumer.kt, LocalConsumer.kt   # T065 folds RemoteAction1 into RemoteAction.kt with the other four
    │   ├── ProducerLimits.kt, ProducerObserver.kt, UnplaceableBridge.kt   # UnplaceableBridge is read by the compiler plugin
    │   ├── ListValues.kt, InternalSsrComposeApi.kt   # list helpers and the opt-in for bridge entry points (research.md R18)
    │   └── internal/                # PlacedNode (stand-in from T025), ScreenApplier, live screen, snapshot notifier, session, screen registry, actions, guard, validation
    ├── test/
    └── loadTest/

compiler-plugin/                     # Kotlin JVM, K2
└── src/
    ├── main/kotlin/dev/tonholo/ssrcompose/compiler/   # SsrCompilerPluginRegistrar, SsrCommandLineProcessor, SsrNames
    │   ├── fir/                     # ComposableAnnotation shared by consumer and producer FIR generators
    │   ├── ir/                      # PluginOrderGuard and PluginOrderDiagnostics shared by consumer and producer IR steps
    │   ├── consumer/                # FIR checker, fragment writer
    │   │   ├── slot/                # SlotParameterMapper
    │   │   └── renderer/            # RendererKey, RendererNames
    │   │       ├── diagnostic/      # RendererDiagnostics
    │   │       ├── fir/             # RendererDeclarationGenerator
    │   │       │   └── checker/     # RendererCheckersExtension, RendererComposableChecker
    │   │       └── ir/              # RendererBodyGenerator, RendererIrGenerationExtension
    │   └── producer/                # BridgeCatalog, BridgeCatalogLoader, BridgeClassRule, BridgeComponent, BridgeKey, BridgeLimits, BridgeParameterDescription, BridgeParameterMapping, BridgeRequest, NamedBridgeComponents, ProducerNames, UnplaceableComponent, BridgeExtensionRegistration
    │       ├── scan/                # ComponentNameSource, ExcludedSources, ScanRoots, SourceNameScan
    │       ├── fir/                 # BridgeDeclarationGenerator, BridgeFirExtensionRegistrar, RefusedBridgeCheckers, UnplaceableBridgeMarker, UnplaceableBridgeCheckers, UnplaceableBridgeCheckRegistrar
    │       ├── ir/                  # BridgeBodyGenerator, BridgeIrGenerationExtension
    │       └── diagnostic/          # the compile error of a component that cannot be placed
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
| `core-testing` | `core`. Published, and added by other modules and by library users as a test dependency only. `core` never depends on it |
| `catalog-model` | `core`, for `CatalogVersion` and `ValueKind` only |
| `consumer`, `producer` | `core` |
| `compiler-plugin`, `tooling` | `catalog-model`. The tests of `compiler-plugin` also use `producer`, as a test dependency only, because the compile tests of the bridges need `ConsumerComponent` and `RemoteAction1` on their classpath |
| `gradle-plugin`, `toolchain-plugin` | `tooling`, and `catalog-model` for the compiler plugin's id and option names |

How the build is written, following the owner's pattern in the thunderbird repository (its `build-plugin` and the pii-safe compiler plugin):

- Convention plugins in `build-logic` are Plugin classes, each registered in the `gradlePlugin { plugins { } }` block of `build-logic/build.gradle.kts` with an id `dev.tonholo.ssrcompose.conventions.<concern>` and its implementation class. Each id is listed in `gradle/libs.versions.toml` under `[plugins]` with no version, and a module applies it with `alias(libs.plugins...)`. There is no precompiled script plugin, and no module build file writes a plugin id as a string.
- The compiler plugin id has one home: the `ssrcompose.compiler.plugin.id` property in the root `gradle.properties`. The buildconfig Gradle plugin (`com.github.gmazzo.buildconfig`, listed in the catalog) turns it into one generated constant in `catalog-model`, next to the option names. `compiler-plugin`, `gradle-plugin` and `toolchain-plugin` all read that one constant. It is not a constant in `core`, and no literal compiler plugin id appears anywhere else, including the toolchain plugin's yaml files.
- The option names of the compiler plugin are written once, in `catalog-model` (`options/CompilerPluginOptions.kt`). It is the one module that `compiler-plugin` and both build-tool plugins can see without touching `core`: `compiler-plugin` depends on it already, and `gradle-plugin` and `toolchain-plugin` depend on it for the id and these names, as the module table shows. T113 creates both.
- The Gradle plugin `dev.tonholo.ssrcompose` is one `KotlinCompilerPluginSupportPlugin` class. It adds `core`, which holds `@ConsumerComposable` and `@ProducerComposable`, to the compile classpath of each compilation by itself, at the plugin's own version, so a developer applies this one plugin and adds nothing else to mark composables. It is registered in the `gradlePlugin` block of `gradle-plugin/build.gradle.kts` and listed in the catalog as `ssrcompose`.
- Compile tests use kotlin-compile-testing. Their sources are `*.fixture.kt` files under `compiler-plugin/src/test/resources/fixtures/`. That folder is added as a test source folder, so the IDE reads the fixtures as Kotlin, and `*.fixture.kt` is excluded from test compilation.
- Our own samples apply the Gradle plugin through the catalog with `alias(libs.plugins.ssrcompose)`, from the first task that needs it (T056 and T057); T007 creates the sample builds without it, since the plugin exists only after T024. Each sample build reads the root `gradle/libs.versions.toml` as its `libs` catalog.

Not checked: the buildconfig plugin's version against Maven metadata, which T001 checks when it adds it.

**Structure Decision**: four deliverables as modules at the root (`core`, `consumer`, `producer`, `compiler-plugin`), the published `core-testing` test-support library, the build-time `catalog-model` library, the tooling library and its two build-tool wrappers, one test-only module, and samples as separate builds that share none of their own code: each uses only the root build, its version catalog and the shared folder of catalogs. `poc/` stays a separate Gradle project and nothing depends on it. The reasons are in [research.md](research.md), sections R5, R11 and R17.

How the build handles the catalog:

- On the app side, the compiler plugin writes one fragment per source file. A cacheable step in each module with marked composables, `ssrCatalogPart`, merges that module's fragments into its part. A step in the module that sets the catalog version, `ssrCatalogBundle`, collects the parts of the modules it depends on, runs the checks across parts, and writes the manifest. `ssrCatalogCheck` compares the bundle with the saved one of the same version, and `ssrCatalogDump` saves it.
- On the server side, `ssrCatalogSync` fetches and pins, and `ssrCatalogVerify`, part of the normal check, fails when a saved catalog no longer matches its lock file or its manifest. A cacheable step per held catalog, `ssrCatalogIndex`, turns the saved bundle into an index. The compiler plugin reads only the index. The index is internal, is not shared between builds, and its format is binary ([research.md R16](research.md#r16-scale-and-build-performance)).
- A bridge declaration is created only for a component the server module names, so a module that calls 20 of 20,000 composables does not pay for the rest. Each server module that calls bridges holds the catalogs it needs and creates the bridges it names ([research.md R9](research.md#r9-one-bridge-per-held-catalog)). The plugin finds the names by scanning the source folders of the module on every compile. A real use of a named component with more parameters than a created bridge can compile with, or in a class over the function limit, is a clear compile error. A name that only appears in a comment or a string does not fail the build ([research.md R16](research.md#named-bridges-in-the-real-plugin)).
- Every step declares its inputs and outputs with relative paths and no timestamps, so the build cache can reuse its results across machines.

Delivery order, with the riskiest unknowns first and something visible on a device early:

1. Root build and the module shape, proved by building it, then the `catalog-model` module and the move of the catalog code out of `core`.
2. The riskiest unchecked facts, each as a small experiment inside the real modules: the part from a Kotlin Multiplatform Android module with variants, the consumer-side renderers, the `RemoteAction` types, defaults and nullable parameters as created types, two bridges with the same names in one module, recomposition through a bridge function, and the scale gate at 20,000 components. The small Kotlin toolchain probe, which covered plugin order and whether the toolchain can build an Android library, is done (T030, [research.md R5](research.md#the-kotlin-toolchain-probe-t030)).
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
| The consumer side may work from Gradle only, if the Kotlin toolchain cannot do what T030 left unproven for an Android library | The constitution asks for every step to work from every supported build tool. The producer side is expected to work from both. | T030 found that the Kotlin toolchain builds a debug Android library with the compiler plugin (a `kmp/lib` module with the `android` platform, see [research.md R5](research.md#the-kotlin-toolchain-probe-t030)). Still unproven: the release variant, `kotlin package`, publishing the AAR, and a Kotlin Multiplatform library with other targets next to Android. Holding the whole feature until the toolchain can do these was turned down by the owner. This row is removed when they are proven. |
| Experimental compiler APIs and one workaround in compiler internals for enum entries | The owner's design has a real enum per choice list, and the compiler's typed API cannot create enum entries. Top-level generation is itself marked experimental. | A class with one constant per choice is the fallback if the workaround breaks in the IDE or in a new Kotlin version. It was not chosen first because it does not match the owner's example and loses the exhaustive `when`. |
| A test-only module, `contract-tests`, and samples as two separate builds | Principle VI asks for the producer's output to run through the consumer's reader, and principle II forbids the two libraries from depending on each other. The spec requires proof that a server builds with none of the app's code. | Tests inside either library would put the other on its build. One shared sample build would not prove that the two sides share only a catalog. |
| Principle II: an extra library, `catalog-model`, holds the catalog format instead of `core` | The owner decided the catalog is build time only (FR-069). The catalog model, its validation, the manifest and part reader and writer, the fingerprint and the index are used only by `tooling` and `compiler-plugin`. The two build-tool plugins also depend on `catalog-model`, but only for the compiler plugin's id and option names. Keeping them in `core` would ship a JSON reader, a hashing step and validation code in every app and every server that never use them. `catalog-model` counts as part of the compiler plugin deliverable, like the tooling library. | Keeping the catalog code in `core`, as principle II's wording asks, was turned down because `core` is a run-time dependency of `consumer` and `producer`. Putting it inside `tooling` was turned down because `compiler-plugin` must not depend on the build-tool library. |
