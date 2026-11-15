---

description: "Task list for the consumer catalog and bridge feature"
---

# Tasks: Consumer Catalog and Bridge

**Input**: Design documents from `/specs/001-consumer-catalog-bridge/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included. The constitution's principle VI makes tests-first non-negotiable. In every phase, write the test tasks first, run them, see them fail, and only then do the implementation tasks.

**Organization**: Tasks are grouped by user story so each story can be built, tested and shown on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1 to US5)
- Every task names the files it touches

## Path Conventions

Base package `dev.tonholo.ssrcompose`. These short forms stand for source folders, and test folders mirror them:

| Short form | Folder |
|---|---|
| `<core>` | `core/src/commonMain/kotlin/dev/tonholo/ssrcompose/core/` |
| `<consumer>` | `consumer/src/commonMain/kotlin/dev/tonholo/ssrcompose/consumer/` |
| `<producer>` | `producer/src/main/kotlin/dev/tonholo/ssrcompose/producer/` |
| `<compiler>` | `compiler-plugin/src/main/kotlin/dev/tonholo/ssrcompose/compiler/` |
| `<tooling>` | `tooling/src/main/kotlin/dev/tonholo/ssrcompose/tooling/` |
| `<gradle>` | `gradle-plugin/src/main/kotlin/dev/tonholo/ssrcompose/gradle/` |

Tests: `core/src/commonTest/...`, `consumer/src/commonTest/...` and `consumer/src/androidDeviceTest/...`, `producer/src/test/...`, `compiler-plugin/src/test/...` (compile tests with kctfork), `tooling/src/test/...`, `gradle-plugin/src/functionalTest/...` (Gradle TestKit), `contract-tests/src/androidHostTest/...`.

`poc/` and `.superpowers/` are never touched and never depended on. The experiment code under `.superpowers/spikes/` may be read as a reference, with its `REPORT.md` files, and never copied blindly.

Rules for every task: JVM test names use `given X, when Y, then Z` in backticks, device test names use `given_x__when_y__then_z`, test bodies have Arrange, Act and Assert parts, fakes are hand-written, no `!!`, no wildcard imports, named arguments, no magic numbers, internal by default. A task that settles something marked "not checked" writes the result into `research.md`. A task that cannot be checked says so in its report and is never reported as passing.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the root build and prove the module shape by building it.

- [ ] T001 Create the root Gradle build: `settings.gradle.kts` (includes `build-logic` and the modules `core`, `consumer`, `producer`, `compiler-plugin`, `tooling`, `gradle-plugin`, `contract-tests`), `build.gradle.kts`, `gradle.properties`, the Gradle wrapper, and `gradle/libs.versions.toml` with Kotlin 2.4.21, Android Gradle Plugin 9.5.1, Compose Multiplatform 1.12.2, kotlinx.serialization 1.13.0, kotlinx.coroutines 1.11.0, `kotlin-compiler-embeddable` 2.4.21, `dev.zacsweers.kctfork:core` 0.14.0, Turbine, detekt and Ktor (samples only). No version is written outside the catalog.
- [ ] T002 Create the convention plugins in `build-logic/src/main/kotlin/` named `dev.tonholo.ssrcompose.conventions.<concern>`: `kotlin-jvm`, `kmp-library`, `compose`, `serialization`, `testing` (kotlin.test, coroutines test, Turbine, power-assert), `detekt` and `publishing` (group `dev.tonholo.ssrcompose`). JVM bytecode target 17, `minSdk` 23, `compileSdk` 37.
- [ ] T003 Create the modules with one trivial internal class each: `core/build.gradle.kts` (Kotlin Multiplatform, `jvm` and `android`), `consumer/build.gradle.kts` (Kotlin Multiplatform, `android` only, Compose in common code, depends on `core`), `producer/build.gradle.kts` (Kotlin JVM, Compose runtime, depends on `core`), `compiler-plugin/build.gradle.kts` (Kotlin JVM, compiler API as compileOnly, depends on `core`), `tooling/build.gradle.kts` (Kotlin JVM, depends on `core`), `gradle-plugin/build.gradle.kts` (Gradle plugin, depends on `tooling`).
- [ ] T004 Create `contract-tests/build.gradle.kts` as a test-only module that runs Android host tests and depends on `core`, `consumer` and `producer`, with one trivial test in `contract-tests/src/androidHostTest/kotlin/dev/tonholo/ssrcompose/contracttests/BuildExperimentTest.kt` that uses a class from `consumer` and a class from `producer`.
- [ ] T005 Run `./gradlew build` and record the result in `specs/001-consumer-catalog-bridge/research.md` under R11 and R12. This is a gate: if the module shape cannot be built, stop and report.
- [ ] T006 [P] Add `config/detekt/detekt.yml` (close to defaults, `FunctionNaming` ignores `@Composable`) and wire it in the `detekt` convention plugin so `./gradlew detekt` passes.
- [ ] T007 [P] Create the two sample builds as separate Gradle builds that use the root build as an included build and share nothing else: `samples/app/settings.gradle.kts` and `samples/app/build.gradle.kts` (Android application, cleartext allowed only in `samples/app/src/debug/AndroidManifest.xml`), `samples/server/settings.gradle.kts` and `samples/server/build.gradle.kts` (Kotlin JVM application), and the shared folder `samples/catalogs/` with a `README.md` that says it holds only catalog files. `samples/app` has one flavor dimension, `catalog`, with two flavors: `current` and `catalogV1`.

**Checkpoint**: `./gradlew build detekt` passes with empty modules, and research.md says which build facts are checked.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The shared contract in `core`, the skeleton of the compiler plugin and the Gradle plugin, and the experiments that settle the riskiest unchecked facts inside the real modules.

**CRITICAL**: No user story work can begin until this phase is complete.

### Tests for the contract (write first, see them fail)

- [ ] T008 [P] Test catalog validation in `core/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/catalog/CatalogValidationTest.kt`: a `CatalogVersion` rejects empty text, text over "at most 64 characters" and control characters; component, parameter and choice names must be valid Kotlin identifiers that are not keywords; a catalog rejects more than "at most 1,000" components, a component with more than "at most 50" parameters, repeated names, a choice list with no entry, a choice parameter that names a missing choice list, a callback with more than four arguments, and `bytes` anywhere except a callback argument.
- [ ] T009 [P] Test the catalog file in `core/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/catalog/CatalogFileTest.kt`: the example in `contracts/catalog-file.md` parses; writing then reading gives an equal catalog; the same catalog always gives the same bytes (sorted components and choices, parameters in declaration order, fixed key order, two-space indent, one trailing newline); `nullable` is left out when false; a file with an unknown `format` is refused; the fingerprint is the lowercase hexadecimal SHA-256 of the bytes.
- [ ] T010 [P] Test the handshake types in `core/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/handshake/HandshakeTypesTest.kt`: `Introduction` holds `contractVersion`, `catalogVersion`, `app` and `device` and no catalog; `DeviceContext.orientation` is derived from the width and height; contract versions compare in order and catalog versions only for equality; `RefusalKind` has exactly four cases: contract version too old, contract version too new, catalog version not known or no longer supported, introduction not valid.
- [ ] T011 [P] Test the JSON codec in `core/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/message/JsonMessageCodecTest.kt`: every message of `contracts/messages.md` survives encode then decode, including a `Screen` with nested slots and a `Trigger` with zero, one and four arguments and with bytes; the contract version can be read from the first frame without decoding the rest; a frame that is not a message throws a decoding error and nothing else.
- [ ] T012 [P] Test the in-memory connection in `core/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/connection/InMemoryConnectionTest.kt`: frames arrive in order both ways, and closing one end ends the other end's `incoming` flow.
- [ ] T013 [P] Test the fitting rules in `core/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/data/FittingTest.kt`, one test per rule of data-model.md: a component not in the catalog is missing; a parameter the catalog does not have is left out and listed, for values, callbacks and slots; a required parameter that was not given is missing; a parameter whose kind differs is missing, with kinds compared exactly; a choice entry the catalog's list does not have is missing; slot content is fitted component by component.

### Implementation of the contract

- [ ] T014 [P] Add `@ConsumerComposable` and `@ProducerComposable` in `<core>/annotation/ConsumerComposable.kt` and `<core>/annotation/ProducerComposable.kt`, each allowed on functions and on types, kept in the compiled output.
- [ ] T015 Implement the catalog model and its validation in `<core>/catalog/`: `CatalogVersion.kt`, `ValueKind.kt`, `ChoiceList.kt`, `Parameter.kt` (value, callback with up to four arguments, slot with `accepts` and optional arguments, `required`, `nullable`), `ComponentDescriptor.kt`, `Catalog.kt`.
- [ ] T016 Implement the catalog file reader and writer and the fingerprint in `<core>/catalog/CatalogFile.kt` and `<core>/catalog/Fingerprint.kt` (depends on T015).
- [ ] T017 [P] Implement `ContractVersion`, `DeviceContext` (with `Orientation` and `Theme`), `AppIdentity`, `Introduction`, `Answer` and `RefusalKind` in `<core>/handshake/`, one file per type.
- [ ] T018 [P] Implement `DataValue` in `<core>/data/DataValue.kt` and `PlacedComponent`, `ActionId` and `ScreenContent` in `<core>/message/ScreenContent.kt`.
- [ ] T019 Implement the messages in `<core>/message/ConsumerMessage.kt` and `<core>/message/ProducerMessage.kt`, `MessageCodec` in `<core>/message/MessageCodec.kt`, and the contract version 1 JSON codec in `<core>/message/internal/JsonMessageCodec.kt` (depends on T017, T018).
- [ ] T020 [P] Implement `Connection` in `<core>/connection/Connection.kt` and a public `InMemoryConnection` pair in `<core>/connection/InMemoryConnection.kt`.
- [ ] T021 Implement fitting in `<core>/data/Fitting.kt`, returning either the fitted component with the names left out, or "missing" with the reason (depends on T015, T018).
- [ ] T022 Run `./gradlew :core:build detekt` for both targets and confirm with `./gradlew :core:dependencies` that `core` has no Android, server framework or transport dependency.

### Skeletons and risk experiments

Each experiment below builds the smallest real piece in its real module, proves or disproves one unchecked fact from plan.md, and writes the result into research.md. An experiment whose result changes the design stops the work and is reported.

- [ ] T023 Create the compiler plugin skeleton in `<compiler>/`: `SsrCompilerPluginRegistrar.kt`, `SsrCommandLineProcessor.kt` with the options `mode` (consumer or producer), `fragmentsDir`, `bridgeCatalog` and `bridgePackage`, and a compile test support class in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/testing/CompileTestSupport.kt` that runs kctfork with the Compose compiler registered after this plugin.
- [ ] T024 Create the Gradle plugin skeleton in `<gradle>/SsrComposePlugin.kt` and `<gradle>/SsrComposeExtension.kt` with the `consumer { }` and `producer { }` blocks of `contracts/build-tooling.md`. It applies the compiler plugin to each Kotlin compilation through the Kotlin compilation API, passes `-Xcompiler-plugin-order` so this plugin runs before the Compose compiler, and declares the saved catalog as a compile input. Add a TestKit support class in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/testing/FixtureProject.kt`.
- [ ] T025 Experiment, bridge types (gate): in `<compiler>/producer/` create, from a one-component catalog, a bridge function whose parameters are a text, a `RemoteAction1<String>`, a nullable optional text with `null` as default, and a nullable optional slot, with the order guard from `.superpowers/spikes/k2-bridge`. In `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/BridgeExperimentTest.kt` and `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/producer/BridgeExperimentTest.kt` prove: it compiles and composes; a lambda converts to `RemoteAction1`; leaving out an optional parameter compiles; recomposition after a state change updates the node; and the wrong plugin order fails with the guard's message. Record in research.md R2, R8 and R8a.
- [ ] T026 Experiment, a component that cannot be placed (gate): find how to make every call to a created bridge function a compile error with a chosen message, and prove it with a compile test in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/producer/UnplaceableComponentTest.kt`. Record in research.md R8b.
- [ ] T027 Experiment, catalog from a Kotlin Multiplatform Android module with variants (gate): write the smallest consumer checker and fragment writer in `<compiler>/consumer/` (text parameters only, one fragment folder per compilation), then a TestKit test in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/KmpAndroidFragmentsTest.kt` with a fixture that has two product flavors with different marked composables and one marked `expect` and `actual` pair. Prove: each variant gets its own fragments, the pair counts once, and an edit to one file leaves the merged result equal to a clean build. Record in research.md R4 and R8c.
- [ ] T028 Experiment, an annotation on a slot's type: prove in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/consumer/SlotAnnotationTest.kt` that the checker can read `@ConsumerComposable` and `@ProducerComposable` on a slot parameter's type. Record in research.md R4.
- [ ] T029 Experiment, consumer-side renderers (gate): in `<compiler>/consumer/` create a renderer for one marked composable with a text parameter, and prove in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/consumer/RendererExperimentTest.kt` that it compiles, that calling it calls the real composable with the decoded value, and that an edit to one file does not break another file's renderer. Record in research.md R7. If creating it in the compiler is fragile, report it: the hand-written bindings of `contracts/consumer-api.md` stay, and the created renderers become follow-up work.
- [ ] T030 [P] Experiment, the Kotlin toolchain: in `samples/toolchain-probe/` set up the smallest Kotlin toolchain project that applies the compiler plugin next to the Compose compiler. Find how plugin order is decided there and whether the toolchain can build an Android library. Record in research.md R2 and R5, with the exact commands and output.
- [ ] T031 [P] Experiment, fetching on IDE sync: find how a Gradle plugin makes a task run when the IDE syncs, try it in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/SyncHookTest.kt` as far as TestKit can show it, and record in research.md R6 what was checked and what needs a person in an IDE.

**Checkpoint**: The contract is tested on JVM and Android, the plugin skeletons exist, and every gate experiment has a written result.

---

## Phase 3: User Story 1 - A server screen places an app composable (Priority: P1) MVP

**Goal**: Mark a composable, produce the catalog, sync it into a separate server build, call the bridge, connect, and see the composable on a device with the server's values.

**Independent Test**: quickstart.md, step 2.

### Tests for User Story 1 (write first, see them fail)

- [ ] T032 [P] [US1] Compile tests for the consumer checker in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/consumer/ConsumerCheckerTest.kt`: text, whole number, decimal number, yes or no, a choice from an enum with its entries, and a list of one kind are described; a default makes `required` false; a nullable value sets `nullable`; a modifier parameter is left out; an unsupported type fails at the parameter with a message that names the composable, the parameter and the reason; a marked function that is not `@Composable` fails. Also: a marked member function, a marked extension function and a marked function with type parameters each fail at the function with a message that says only top-level composables without type parameters can be marked.
- [ ] T033 [P] [US1] Test the merge and the check in `tooling/src/test/kotlin/dev/tonholo/ssrcompose/tooling/CatalogMergeTest.kt` and `CatalogCheckTest.kt`: fragments merge into one sorted catalog; a fragment whose source file is gone is ignored; a name used twice fails and names both; fragments that disagree about a choice's entries fail; the check passes when the merged catalog equals the saved file of its version and fails with a readable difference when it does not; the dump writes the same bytes twice. When no saved file exists for the version, the check fails with a message that says to run `ssrCatalogDump`, and writes nothing. Also: an app with no marked composable gives a valid empty catalog, and a catalog over "at most 1,000" components or "at most 50" parameters fails and names the bound.
- [ ] T034 [P] [US1] Test the sync in `tooling/src/test/kotlin/dev/tonholo/ssrcompose/tooling/CatalogSyncTest.kt` with a local test web server and file paths: a catalog is fetched, validated, saved as `<version>.json` and written to the lock file with its location and SHA-256; an invalid catalog or one with a name that cannot be a Kotlin name is rejected and nothing is saved; a location whose content no longer matches the lock file is reported and the saved copy is kept unless update is asked; an unreachable location uses the saved copy, and fails when there is none; an address that is not `https` or a file path is refused unless opted in.
- [ ] T035 [P] [US1] Test the verify step in `tooling/src/test/kotlin/dev/tonholo/ssrcompose/tooling/CatalogVerifyTest.kt`: it fails when a saved catalog does not match its fingerprint, when the bridge catalog or a supported catalog is not saved, and when two saved catalogs share a version.
- [ ] T036 [P] [US1] Compile tests for the bridge in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/producer/BridgeGenerationTest.kt`: one composable function per component and one enum per choice list, in the bridge package; value, choice and list parameters with the right types; a wrong name, a missing required parameter and a value of the wrong kind each fail to compile at the caller's line (SC-004); the created code refers to no class outside `core` and `producer`.
- [ ] T037 [P] [US1] Test the live composition in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/internal/LiveScreenTest.kt`: content with state emits a new `ScreenContent` when the state changes, a collector that cancels disposes the composition, and equal content is not sent twice.
- [ ] T038 [P] [US1] Test `ConsumerComponent` and the session's happy path in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/ConsumerComponentTest.kt` and `ProducerSessionTest.kt` over an `InMemoryConnection`: a placed component carries its name and values in call order; a parameter that was not given is absent; a valid `Introduce` gets `Accepted` with the contract version before anything else; `OpenScreen` gets a `Screen`; a name the producer does not have gets `ScreenUnavailable` and the session stays accepted; a second `OpenScreen` replaces the open screen and disposes the first composition; `LocalConsumer.current` exposes `device`, `app` and `catalogVersion`. A required nullable parameter given `null` is sent as an explicit nothing. A parameter that can be left out and was not given is absent for values, callbacks and slots alike (FR-062).
- [ ] T039 [P] [US1] Test `CatalogSet` in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/CatalogSetTest.kt`: it loads saved catalogs from resources, and fails when versions repeat with different fingerprints, when the bridge version is not held, and when a supported version is not held.
- [ ] T040 [P] [US1] Test the consumer side in `consumer/src/commonTest/kotlin/dev/tonholo/ssrcompose/consumer/ComponentBindingsTest.kt` and `internal/ConsumerSessionTest.kt`: hand-written bindings read text, numbers, flags, choices and lists from a placed component; the session sends `Introduce` first with every value from a fake device values source and the build's catalog version, waits for `Accepted`, sends `OpenScreen`, and exposes each `Screen` as state. The catalog version is read from the generated resource through `CatalogVersionLoader`.
- [ ] T041 [P] [US1] Contract test in `contract-tests/src/androidHostTest/kotlin/dev/tonholo/ssrcompose/contracttests/FirstScreenContractTest.kt`: a real `Producer` and a real consumer session joined by an `InMemoryConnection` end with the consumer holding the screen the producer composed.
- [ ] T042 [P] [US1] Compose UI test in `consumer/src/androidDeviceTest/kotlin/dev/tonholo/ssrcompose/consumer/SsrScreenTest.kt`: `given_a_screen_with_a_placed_component__when_shown__then_the_app_composable_draws_the_values`, with a fake `Connection`, and the default problem message with its retry button.
- [ ] T043 [P] [US1] TestKit tests in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/ConsumerTasksTest.kt` and `ProducerTasksTest.kt`: `ssrCatalogDump` writes `ssr/catalogs/<version>.json`; `ssrCatalogCheck` fails when a marked composable changes without a new version; `ssrCatalogSync` saves and pins; `ssrCatalogVerify` fails on a changed saved file; a build with the network disabled gives the same output as one with it (SC-009); editing a saved catalog causes a recompile. Also: a fixture with the Compose compiler and an Android variant builds and its bridge composes, which proves the plugin sets the order flag and that the Android build keeps it.

### Implementation for User Story 1

- [ ] T044 [US1] Complete the consumer checker and the fragment writer for value, choice and list parameters in `<compiler>/consumer/ConsumerComposableChecker.kt`, `ParameterKindMapper.kt`, `ConsumerErrors.kt` and `FragmentWriter.kt`.
- [ ] T045 [P] [US1] Implement the merge, the check and the dump in `<tooling>/CatalogMerge.kt`, `CatalogCheck.kt` and `CatalogDump.kt`.
- [ ] T046 [P] [US1] Implement the sync, the lock file and the verify step in `<tooling>/CatalogSync.kt`, `CatalogLock.kt` and `CatalogVerify.kt`.
- [ ] T047 [US1] Register `ssrCatalogCheck`, `ssrCatalogDump`, `ssrCatalogSync` and `ssrCatalogVerify` in `<gradle>/tasks/`, one file per task, with `ssrCatalogCheck` and `ssrCatalogVerify` part of `check`, per variant as research.md R8c says (depends on T045, T046). The build also writes each variant's catalog version into a small generated resource that `<consumer>/internal/CatalogVersionLoader.kt` reads.
- [ ] T048 [US1] Complete the bridge creation for value, choice and list parameters in `<compiler>/producer/BridgeDeclarationGenerator.kt`, `BridgeBodyGenerator.kt` and `PluginOrderGuard.kt`, passing a readable file name for the created functions.
- [ ] T049 [US1] Implement the producer's tree applier and live composition in `<producer>/internal/ScreenApplier.kt` and `<producer>/internal/LiveScreen.kt`, with one snapshot observer shared by the whole process in `<producer>/internal/SnapshotNotifier.kt`.
- [ ] T050 [US1] Implement `ConsumerComponent` in `<producer>/ConsumerComponent.kt` and `LocalConsumer` with its `Consumer` type in `<producer>/LocalConsumer.kt` (depends on T049).
- [ ] T051 [US1] Implement `CatalogSet` in `<producer>/CatalogSet.kt`, and `Producer`, `ProducerLimits` and `ProducerObserver` in their own files in `<producer>/`, with the session's accept, open-screen, unknown-screen, replace-screen and close paths in `<producer>/internal/Session.kt`, following the state table in data-model.md (depends on T050).
- [ ] T052 [P] [US1] Implement `ComponentBindings` and its `PlacedComponent` reader in `<consumer>/ComponentBindings.kt`.
- [ ] T053 [P] [US1] Implement the device values source in `<consumer>/internal/DeviceValues.kt` with the common Compose APIs; put a value in `consumer/src/androidMain/` behind an internal interface only if common code cannot read it, and record which in research.md.
- [ ] T054 [US1] Implement the consumer session in `<consumer>/internal/ConsumerSession.kt`, and `SsrScreen` and `ScreenProblem` in `<consumer>/SsrScreen.kt` and `<consumer>/ScreenProblem.kt`, rendering `ScreenContent` top to bottom (depends on T052, T053).
- [ ] T055 [US1] Complete the consumer-side renderers for value, choice and list parameters and the renderer index in `<compiler>/consumer/RendererGenerator.kt`, `<tooling>/RendererIndex.kt` and `<consumer>/internal/RendererIndexLoader.kt`, so `SsrScreen` needs no hand-written bindings. If T029 reported this as fragile, skip this task, say so, and use hand-written bindings in the samples.
- [ ] T056 [US1] Write the sample app in `samples/app/src/main/kotlin/dev/tonholo/ssrcompose/samples/app/`: `MainActivity.kt`, `components/Greeting.kt` and `components/Style.kt` marked with `@ConsumerComposable`, `WebSocketConnection.kt`, catalog version `"1"`, showing `SsrScreen(screen = "home", ...)` against `127.0.0.1:8788`; run `ssrCatalogDump` and copy the file to `samples/catalogs/1.json`. `Greeting` and `Style` live in `src/main`, since both flavors have them, and both flavors start at catalog version `"1"`.
- [ ] T057 [US1] Write the sample server in `samples/server/src/main/kotlin/dev/tonholo/ssrcompose/samples/server/`: `Main.kt` (Ktor on port 8788, bound to `127.0.0.1`), `WebSocketConnection.kt`, `HomeScreen.kt` that calls the bridge's `Greeting`, and an observer that prints each event; point it at `samples/catalogs/1.json`, run `ssrCatalogSync`, and confirm with `./gradlew -p samples/server dependencies` that nothing from `samples/app` is on its classpath (SC-003).
- [ ] T058 [US1] Run quickstart.md step 2 on an emulator, including the three deliberate compile failures, look at the screenshot, fix what is wrong, and report the screenshot path, the server's event output, and the time from launch to first screen from the session log (SC-005).
- [ ] T059 [US1] Run a quick load probe in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/SessionProbeTest.kt`: hold 1,000 sessions of the sample home screen over `InMemoryConnection`, and report the memory per session and whether the bound of 10,000 looks reachable. This is an early warning, not the load test of the last phase. Record the result in research.md R14.

**Checkpoint**: User Story 1 works on a device, with two builds that share only a catalog file. This is the MVP.

---

## Phase 4: User Story 2 - A tap in the app runs a lambda on the server (Priority: P2)

**Goal**: Callbacks of an app composable run lambdas on the server, with or without arguments, and the screen updates.

**Independent Test**: quickstart.md, step 3.

### Tests for User Story 2 (write first, see them fail)

- [ ] T060 [P] [US2] Compile tests for callbacks in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/consumer/CallbackKindsTest.kt` and `producer/BridgeCallbacksTest.kt`: callbacks with no argument and with one to four arguments, including `ByteArray`, are described with their `arguments`; a callback with more than four arguments, or with a result other than `Unit`, fails at the parameter; the bridge gives each one the matching type from `RemoteAction0` to `RemoteAction4`; a lambda with the wrong number or kinds of parameters fails to compile.
- [ ] T061 [P] [US2] Test actions on the producer in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/ActionsTest.kt`: a placed component's callbacks carry action IDs unique in the session; a `Trigger` runs the latest lambda the screen gave; arguments reach the lambda in order with their kinds; a state change in the lambda sends a new `Screen`; triggers run in the order they arrive; a trigger for an action the screen no longer has is ignored, reported through `onTriggerIgnored`, and does not end the session; arguments that do not match the callback's kinds in the session's catalog are refused and reported; arguments over "1 megabyte in total by default" are refused without being decoded; a trigger in one session never changes another session. Also: a lambda that throws leaves the screen's state and the last sent `Screen` unchanged, calls `onActionFailed`, and the session carries on; triggers over "100 per second by default" are dropped, call `onTriggerDropped`, and do not end the session.
- [ ] T062 [P] [US2] Test callbacks on the consumer in `consumer/src/commonTest/kotlin/dev/tonholo/ssrcompose/consumer/internal/CallbackTest.kt`: `placed.callback("name")` gives a function that sends a `Trigger` with the action ID and its arguments as values; a callback the screen did not give falls back to the composable's default.
- [ ] T063 [P] [US2] Contract test in `contract-tests/src/androidHostTest/kotlin/dev/tonholo/ssrcompose/contracttests/CallbackContractTest.kt`: a callback triggered on a real consumer session runs the server lambda and the consumer ends with the updated screen.

### Implementation for User Story 2

- [ ] T064 [US2] Add callback kinds to the consumer checker, the fragment writer and the renderers in `<compiler>/consumer/ParameterKindMapper.kt`, `FragmentWriter.kt` and `RendererGenerator.kt`.
- [ ] T065 [P] [US2] Add `RemoteAction0` to `RemoteAction4` as function interfaces with no result type in `<producer>/RemoteAction.kt`.
- [ ] T066 [US2] Add callback parameters to the bridge creation in `<compiler>/producer/BridgeDeclarationGenerator.kt` and `BridgeBodyGenerator.kt` (depends on T065).
- [ ] T067 [US2] Implement the session's actions in `<producer>/internal/Actions.kt` and handle `Trigger` in `<producer>/internal/Session.kt`: assign IDs, always run the latest lambda, validate arguments against the session's catalog, enforce the size limit, and report ignored triggers. Run each lambda in a snapshot that is applied only when it finishes, as research.md R10a says, and enforce the per-session trigger limit from `ProducerLimits`.
- [ ] T068 [US2] Send triggers from the consumer in `<consumer>/internal/ConsumerSession.kt` and expose `callback` on the `PlacedComponent` reader in `<consumer>/ComponentBindings.kt`.
- [ ] T069 [US2] Extend the samples: a marked `CounterCard(count: Int, onAddOne: () -> Unit)` and a marked `NameField(onSubmit: (String) -> Unit)` in `samples/app/.../components/`, catalog version `"2"` dumped and copied to `samples/catalogs/2.json`, and a `CounterScreen.kt` in `samples/server/.../` that holds the count with `remember`. The new marked composables go in `samples/app/src/current/kotlin/dev/tonholo/ssrcompose/samples/app/components/`, so only the `current` flavor has them. Only the `current` flavor's catalog version is raised; `catalogV1` stays at `"1"`.
- [ ] T070 [US2] Run quickstart.md step 3 on an emulator, look at the screenshots, and report their paths, the server's event output, and the time from tap to updated screen from the session log (SC-006).

**Checkpoint**: User Stories 1 and 2 work on a device.

---

## Phase 5: User Story 3 - Server content sits inside an app composable (Priority: P3)

**Goal**: The server fills an app composable's slot with other catalog composables, and the app shows them where it places the slot, nested.

**Independent Test**: quickstart.md, step 4.

### Tests for User Story 3 (write first, see them fail)

- [ ] T071 [P] [US3] Compile tests for slots in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/consumer/SlotKindsTest.kt` and `producer/BridgeSlotsTest.kt`: a slot is described with `accepts` set from the annotation on its type (`any`, `consumer`, `producer`); a slot whose content receives arguments is described with its `arguments`; the bridge gives a plain slot a composable parameter, nullable with `null` as default when it is optional; an optional slot with arguments has no bridge parameter; calling a component that has a required slot with arguments is a compile error that names the later feature.
- [ ] T072 [P] [US3] Test slots on the producer in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/SlotsTest.kt`: content given to a slot becomes that slot's ordered list of placed components; slots inside slots keep their nesting; a slot that was not given is absent; state changes inside slot content send a new `Screen`.
- [ ] T073 [P] [US3] Test slots on the consumer in `consumer/src/commonTest/kotlin/dev/tonholo/ssrcompose/consumer/internal/SlotRenderingTest.kt` and `consumer/src/androidDeviceTest/kotlin/dev/tonholo/ssrcompose/consumer/SlotRenderingTest.kt`: `placed.slot("name")` gives content that renders the slot's components in order; nested slots render nested; a slot the screen did not give falls back to the composable's default.
- [ ] T074 [P] [US3] Contract test in `contract-tests/src/androidHostTest/kotlin/dev/tonholo/ssrcompose/contracttests/SlotContractTest.kt`: a screen with two levels of slots reaches the consumer with the same nesting.

### Implementation for User Story 3

- [ ] T075 [US3] Add slot kinds, `accepts` and slot arguments to the consumer checker and the fragment writer in `<compiler>/consumer/ParameterKindMapper.kt` and `FragmentWriter.kt`, and slot rendering to `<compiler>/consumer/RendererGenerator.kt`.
- [ ] T076 [US3] Add slot parameters to the bridge creation in `<compiler>/producer/BridgeDeclarationGenerator.kt` and `BridgeBodyGenerator.kt`, and the compile error for a component that cannot be placed in `<compiler>/producer/UnplaceableComponents.kt`, as T026 found.
- [ ] T077 [US3] Compose slot content as children in `<producer>/ConsumerComponent.kt` and `<producer>/internal/ScreenApplier.kt`.
- [ ] T078 [US3] Render slot content in `<consumer>/SsrScreen.kt` and expose `slot` on the `PlacedComponent` reader in `<consumer>/ComponentBindings.kt`.
- [ ] T079 [US3] Extend the samples: a marked `Section(title: String, content: @Composable () -> Unit)` in `samples/app/.../components/`, catalog version `"3"` dumped and copied to `samples/catalogs/3.json`, and a `SectionsScreen.kt` in `samples/server/.../` with a section inside a section. The new marked composable goes in `samples/app/src/current/`, and only the `current` flavor's catalog version is raised.
- [ ] T080 [US3] Run quickstart.md step 4 on an emulator, including the compile error for a slot with arguments, look at the screenshots, and report their paths.

**Checkpoint**: User Stories 1 to 3 work on a device.

---

## Phase 6: User Story 4 - Apps that did not update keep working (Priority: P4)

**Goal**: One producer serves several catalog and contract versions at once, gives each app only what it has, and refuses the ones it does not support with a reason the user sees.

**Independent Test**: quickstart.md, step 5.

### Tests for User Story 4 (write first, see them fail)

- [ ] T081 [P] [US4] Test introduction validation in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/internal/IntroductionValidatorTest.kt`, one test per rule of research.md R14: frame "at most 8 KB", density "above 0 and at most 10", width and height "1 to 100,000 pixels", language tag "at most 35 characters", and app version, variant, flavor and catalog version "at most 64 characters each with no control characters".
- [ ] T082 [P] [US4] Test refusals in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/RefusalTest.kt`: a catalog version not held, a catalog version held but not supported, a contract version below and above the supported set, an invalid introduction, a frame over the limit (refused without being decoded), and any message before `Introduce` each get `Refused` with the right kind, a reason with no internal details, no `Screen`, and an `onRefused` event.
- [ ] T083 [P] [US4] Test serving several versions in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/MultiVersionTest.kt`: two sessions with different catalog versions get screens fitted to their own catalogs at the same time; a parameter the older catalog lacks is left out and reported through `onParameterLeftOut`; three contract versions are served at once using two test-only codecs registered through the same internal seam as the real one; what one session introduced never appears in another.
- [ ] T084 [P] [US4] Test missing components in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/MissingComponentTest.kt`: inside `OrElse` a missing component shows the replacement and no trace of itself; without it the session sends `ScreenUnavailable` and no partial `Screen`, and reports `onComponentMissing` with the component, the screen and the catalog version; a missing component inside a slot follows the same rule; `LocalConsumer.current.has("Name")` is true only when the component is in the session's catalog and can be fitted; a screen whose code throws gets `ScreenUnavailable` with no internal details and an `onScreenStopped` event, and the session can open another screen.
- [ ] T085 [P] [US4] Test the consumer's problem handling in `consumer/src/commonTest/kotlin/dev/tonholo/ssrcompose/consumer/internal/ScreenProblemTest.kt`: `Refused` becomes `ScreenProblem.Refused`; `ScreenUnavailable` becomes `ScreenProblem.Unavailable`; something the consumer cannot draw becomes `ScreenProblem.Mismatch` with no crash and no developer placeholder; a producer message it cannot decode is treated as a refusal; the default English message says why in one sentence and what the user can do; `retry` asks for the screen again after `Unavailable` and introduces the app again after `Refused`.
- [ ] T086 [P] [US4] Compatibility matrix contract test in `contract-tests/src/androidHostTest/kotlin/dev/tonholo/ssrcompose/contracttests/CompatibilityMatrixTest.kt`: for every pairing of sample catalog version (1, 2, 3) and sample screen, the consumer ends with a correct screen, a replacement, an unavailable screen or a refusal, and never something it cannot draw (SC-002).
- [ ] T087 [P] [US4] TestKit test in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/VariantCatalogsTest.kt`: a flavor that sets its own catalog version is checked against its own saved file, and two variants that share a version but differ in marked composables fail the check.

### Implementation for User Story 4

- [ ] T088 [US4] Implement the introduction validator and the frame limit in `<producer>/internal/IntroductionValidator.kt`, configurable through `ProducerLimits`.
- [ ] T089 [US4] Pick the session's catalog and apply fitting when a screen is composed, in `<producer>/internal/Session.kt` and `<producer>/ConsumerComponent.kt`, and implement the refusal paths and one codec per supported contract version in `<producer>/internal/CodecRegistry.kt` (depends on T088).
- [ ] T090 [US4] Implement `OrElse` in `<producer>/OrElse.kt` and `has` on `Consumer` in `<producer>/LocalConsumer.kt`, and stop the screen on a missing component with no replacement, or on a screen whose code throws, in `<producer>/internal/Session.kt`.
- [ ] T091 [US4] Handle `Refused`, `ScreenUnavailable`, undrawable content and undecodable messages in `<consumer>/internal/ConsumerSession.kt`, with the default messages and the `retry` action in `<consumer>/SsrScreen.kt`.
- [ ] T092 [US4] Add per-variant catalog versions to `<gradle>/SsrComposeExtension.kt` and `<gradle>/tasks/`, as T027 found.
- [ ] T093 [US4] Extend the samples: confirm that the `catalogV1` flavor, which has only the composables in `src/main`, still produces a catalog equal to `samples/catalogs/1.json`, the sample server holding `1.json`, `2.json` and `3.json` with bridge catalog `"3"`, supported catalogs taken from a command line argument in `samples/server/.../Main.kt`, and a `StrictScreen.kt` with no replacement next to screens that use `OrElse`.
- [ ] T094 [US4] Run quickstart.md step 5 with both app flavors against one server, then with version 1 unsupported, look at the screenshots, and report their paths and the server's event output.

**Checkpoint**: User Stories 1 to 4 work on a device.

---

## Phase 7: User Story 5 - A screen adapts to the device and follows its changes (Priority: P5)

**Goal**: A screen reads the device values and updates when they change, without losing what the user did.

**Independent Test**: quickstart.md, step 6.

### Tests for User Story 5 (write first, see them fail)

- [ ] T095 [P] [US5] Test device changes on the producer in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/DeviceChangeTest.kt`: a `DeviceChanged` recomposes the open screen with the new `LocalConsumer.current.device`; a value held with `remember` survives; a burst of changes ends with a screen for the last values; an invalid `DeviceChanged` is ignored and reported; it cannot change `app`, `catalogVersion` or the contract version; a callback trigger and a `DeviceChanged` sent one after the other are handled one at a time, in the order they arrive (FR-054).
- [ ] T096 [P] [US5] Test change reporting on the consumer in `consumer/src/commonTest/kotlin/dev/tonholo/ssrcompose/consumer/internal/DeviceChangeReportingTest.kt` and `consumer/src/androidDeviceTest/kotlin/dev/tonholo/ssrcompose/consumer/DeviceChangeTest.kt`: each change sends one `DeviceChanged` after `Accepted`; a change during the introduction is sent once accepted; an unchanged value sends nothing; a configuration change on a device sends one.

### Implementation for User Story 5

- [ ] T097 [US5] Hold `DeviceContext` as state in the session and handle `DeviceChanged` with validation and conflation in `<producer>/internal/Session.kt`.
- [ ] T098 [US5] Observe the device values and send `DeviceChanged` in `<consumer>/internal/ConsumerSession.kt` and `<consumer>/internal/DeviceValues.kt`.
- [ ] T099 [US5] Add an `AdaptiveScreen.kt` in `samples/server/.../` that picks its text by theme and orientation and holds a counter with `remember`, and a way to open it in `samples/app/.../MainActivity.kt`.
- [ ] T100 [US5] Run quickstart.md step 6 on an emulator (dark theme with `adb -s <serial> shell cmd uimode night yes`, then rotate), look at the screenshots, and report their paths and the time from each change to the updated screen from the session log (SC-007).

**Checkpoint**: All five user stories work on a device.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: The second build tool, the measured bounds, the remaining unchecked facts, and docs that are true.

- [ ] T101 Implement the Kotlin toolchain plugin in `toolchain-plugin/` (`module.yaml`, `plugin.yaml` with the tasks `ssrCatalogCheck`, `ssrCatalogDump`, `ssrCatalogSync` and `ssrCatalogVerify`, and `toolchain-plugin/src/dev/tonholo/ssrcompose/toolchain/` calling `tooling`), following what T030 found, and add `samples/toolchain-server/` as the sample server set up for the Kotlin toolchain.
- [ ] T102 Run quickstart.md step 9 and record in research.md R5 whether the Kotlin toolchain path is now checked, for the server and, if the toolchain can build one, for an Android library.
- [ ] T103 Add a `loadTest` source set and task to `producer/build.gradle.kts`, and write `producer/src/loadTest/kotlin/dev/tonholo/ssrcompose/producer/SessionLoadTest.kt`: hold 10,000 sessions over 10 catalog versions of up to 1,000 components with up to 50 parameters each, measure the time to accept an app with a 200 component catalog, and check that 1,000 healthy sessions see no error and get their screens no more than 100 milliseconds later while one session sends invalid and oversized introductions and callback arguments.
- [ ] T104 Run `./gradlew :producer:loadTest`, write the measured numbers into research.md R14, and report any success criterion (SC-010, SC-011) that is not met. The accept time measured here is in memory on the JVM and is not the device number of SC-005.
- [ ] T105 Support marked composables in several modules of one app: each module exposes its merged catalog, and the app's `ssrCatalogCheck` and `ssrCatalogDump` aggregate them and catch a name used in two modules, in `<tooling>/CatalogAggregate.kt` and `<gradle>/tasks/`, with a TestKit test in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/MultiModuleCatalogTest.kt`. Record in research.md R4.
- [ ] T106 [P] Write `docs/editor-setup.md` with the IDE registry setting `kotlin.k2.only.bundled.compiler.plugins.enabled`, and a short list for the owner to check by hand in an IDE, since an agent cannot: the bridge functions and enums resolve, a catalog edit is picked up after a sync, and the enum workaround of research.md R3 holds. Record the owner's result in research.md R13 when it comes.
- [ ] T107 [P] Write the ADRs listed in research.md R15 as `docs/adr/0002-compiler-created-bridge.md`, `docs/adr/0003-catalog-fragments-and-merge.md`, `docs/adr/0004-build-tool-plugins-over-shared-tooling.md`, `docs/adr/0005-catalogs-fetched-at-sync-and-pinned.md` and `docs/adr/0006-module-layout-and-targets.md`, and add them to the index in `docs/adr/README.md`.
- [ ] T108 [P] Add short API documentation to every public type named under "What is public" in `contracts/producer-api.md` and `contracts/consumer-api.md`, and confirm with `grep` that nothing else in `core`, `consumer` and `producer` is public.
- [ ] T109 Update the agent files with the real modules and commands: `.ai/guidelines.md` (module map, commands, QA targets), `.ai/registry.md`, `.ai/rules/architecture.md` (the catalog is produced, two catalogs, consumer and producer naming), the test table in `.ai/rules/testing.md` and `.ai/skills/build-and-verify/SKILL.md`, then run `python3 .ai/scripts/check-links.py` and `python3 .ai/scripts/sync-agents.py --check`.
- [ ] T110 [P] Update `README.md` (status and a short usage example), `CONTRIBUTING.md` (setup commands for the root build), `CHANGELOG.md`, and write `docs/getting-started.md` that takes a new developer from marking one composable to seeing it on a device, including the rule that the producer is released before or together with the app (SC-001, FR-064), and the rule that a composable is kept under the same name until the catalog versions that need it are no longer supported (FR-020). The getting started page also says that a catalog file shows component names to anyone who can read its location, and which Kotlin version the release supports.
- [ ] T111 Check the whole diff against `.ai/rules/security.md`, rule by rule, and FR-059: no reason or message that reaches the user carries a stack trace, a class name or another app's values. Fix what breaks one.
- [ ] T112 Run every step of `specs/001-consumer-catalog-bridge/quickstart.md` from a clean checkout, including step 7 with no network, then `./gradlew build detekt`, and report each step's result, including any that could not run.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies. T005 is a gate.
- **Foundational (Phase 2)**: needs Setup. Blocks every user story. T025, T026, T027 and T029 are gates: a result that changes the design stops the work and is reported.
- **User Story 1 (Phase 3)**: needs Foundational. Its last task, T059, is an early load probe.
- **User Story 2 (Phase 4)**: needs User Story 1.
- **User Story 3 (Phase 5)**: needs User Story 1. It does not need User Story 2.
- **User Story 4 (Phase 6)**: needs User Stories 1, 2 and 3, since the compatibility matrix uses all three sample catalogs.
- **User Story 5 (Phase 7)**: needs User Story 1 only.
- **Polish (Phase 8)**: needs all five stories. T101, T106, T107 and T110 can start earlier.

### User Story Dependencies

```text
Setup -> Foundational -> US1 -> US2 --\
                           |-> US3 ---+-> US4
                           \-> US5
```

US2, US3 and US5 can be built side by side once US1 is done, as long as changes to `<producer>/internal/Session.kt`, `<consumer>/internal/ConsumerSession.kt`, `<consumer>/SsrScreen.kt` and the files under `<compiler>/` are made one at a time.

### Within Each User Story

- Tests first, and they must fail before the implementation starts.
- `core` first, then the compiler plugin and tooling, then the libraries, then the samples, and the device check last.
- A story is done when its device check task is done and reported.

### Parallel Opportunities

- Phase 1: T006 and T007 after T005.
- Phase 2: the tests T008 to T013 together; then T014, T017, T018 and T020 together; T030 and T031 next to the other experiments.
- Each story: all of its test tasks together. In US1 also T045, T046, T052 and T053.
- Phase 8: T106, T107, T108 and T110 together.

---

## Parallel Example: User Story 1

```text
Write the tests together:
  T032 consumer checker       T037 live composition      T041 contract test
  T033 merge and check        T038 component and session T042 Compose UI test
  T034 sync                   T039 catalog set           T043 Gradle tasks
  T035 verify                 T040 consumer side
  T036 bridge

Then in parallel, since they touch different modules:
  T045 merge, check, dump     T052 ComponentBindings
  T046 sync, lock, verify     T053 device values
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1, with the build as the gate.
2. Phase 2: the contract, the skeletons, and the experiments. Each gate has a written result before the next phase.
3. Phase 3, User Story 1.
4. Stop and check on a device (T058). Show it to the owner.

### Incremental Delivery

1. Setup and Foundational: the contract is tested and the risky facts are settled.
2. User Story 1: an app composable placed by a separate server build, on a device. MVP.
3. User Story 2: taps run server lambdas.
4. User Story 3: server content inside app composables.
5. User Story 4: old apps keep working, and unsupported ones are refused clearly.
6. User Story 5: screens follow the device.
7. Polish: the second build tool, the measured bounds, and true docs.

Each story ends with a device check, so the feature is visible on a device from the first story on.

---

## Notes

- [P] tasks touch different files and have no unfinished dependency.
- Every task is one small Conventional Commit, made as the owner's commit rules and crew rules say.
- Anything in research.md marked "not checked" stays that way until the task that settles it has run.
- The samples of this feature use components with values, callbacks and plain slots. The owner's `PaymentSection` with a title slot and `CameraScanner` need the later remote composables feature.
