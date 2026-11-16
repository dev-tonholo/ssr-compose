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
| `<catalog>` | `catalog-model/src/main/kotlin/dev/tonholo/ssrcompose/catalog/` |
| `<consumer>` | `consumer/src/commonMain/kotlin/dev/tonholo/ssrcompose/consumer/` |
| `<producer>` | `producer/src/main/kotlin/dev/tonholo/ssrcompose/producer/` |
| `<compiler>` | `compiler-plugin/src/main/kotlin/dev/tonholo/ssrcompose/compiler/` |
| `<tooling>` | `tooling/src/main/kotlin/dev/tonholo/ssrcompose/tooling/` |
| `<gradle>` | `gradle-plugin/src/main/kotlin/dev/tonholo/ssrcompose/gradle/` |

Tests: `core/src/commonTest/...`, `catalog-model/src/test/...`, `consumer/src/commonTest/...` and `consumer/src/androidDeviceTest/...`, `producer/src/test/...`, `compiler-plugin/src/test/...` (compile tests with kotlin-compile-testing, `dev.zacsweers.kctfork`, whose sources are `*.fixture.kt` files; each compile test reads its sources from `compiler-plugin/src/test/resources/fixtures/*.fixture.kt`), `tooling/src/test/...`, `gradle-plugin/src/functionalTest/...` (Gradle TestKit), `contract-tests/src/androidHostTest/...`.

`poc/` and `.superpowers/` are never touched and never depended on. The experiment code under `.superpowers/spikes/` may be read as a reference, with its `REPORT.md` files, and never copied blindly.

Rules for every task: JVM test names use `given X, when Y, then Z` in backticks, device test names use `given_x__when_y__then_z`, test bodies have Arrange, Act and Assert parts, fakes are hand-written, no `!!`, no wildcard imports, named arguments, no magic numbers, internal by default. A task that settles something marked "not checked" writes the result into `research.md`. A task that cannot be checked says so in its report and is never reported as passing.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the root build and prove the module shape by building it.

- [X] T001 Create the root Gradle build: `settings.gradle.kts` (includes `build-logic` and the modules `core`, `consumer`, `producer`, `compiler-plugin`, `tooling`, `gradle-plugin`, `contract-tests`), `build.gradle.kts`, `gradle.properties` (with `ssrcompose.compiler.plugin.id=dev.tonholo.ssrcompose`, the one home of the compiler plugin id), the Gradle wrapper, and `gradle/libs.versions.toml` with Kotlin 2.4.21, Android Gradle Plugin 9.5.1, Compose Multiplatform 1.12.2, kotlinx.serialization 1.13.0, kotlinx.coroutines 1.11.0, `kotlin-compiler-embeddable` 2.4.21, `dev.zacsweers.kctfork:core` 0.14.0 under the alias `kotlin-compile-testing`, the buildconfig Gradle plugin `com.github.gmazzo.buildconfig` under the alias `buildconfig` (version checked against Maven metadata when added), Turbine, detekt and Ktor (samples only), and a `[plugins]` entry with an id and no version for each convention plugin of T002 and for the Gradle plugin `dev.tonholo.ssrcompose` (`ssrcompose`). No version is written outside the catalog, and no plugin is applied by a string id in a module build file.
- [X] T002 Create the convention plugins as Plugin classes in `build-logic/src/main/kotlin/dev/tonholo/ssrcompose/conventions/`, one per concern: `kotlin-jvm`, `kmp-library`, `compose`, `serialization`, `testing` (kotlin.test, coroutines test, Turbine, power-assert), `detekt` and `publishing` (group `dev.tonholo.ssrcompose`). Register each in the `gradlePlugin { plugins { } }` block of `build-logic/build.gradle.kts` with the id `dev.tonholo.ssrcompose.conventions.<concern>` and its implementation class, list each id in `gradle/libs.versions.toml` under `[plugins]` with no version, and apply them in modules with `alias(libs.plugins...)`. No precompiled script plugin. JVM bytecode target 17, `minSdk` 23, `compileSdk` 37.
- [X] T003 Create the modules with one trivial internal class each, each build file applying its convention plugins with `alias(libs.plugins...)`: `core/build.gradle.kts` (Kotlin Multiplatform, `jvm` and `android`), `consumer/build.gradle.kts` (Kotlin Multiplatform, `android` only, Compose in common code, depends on `core`), `producer/build.gradle.kts` (Kotlin JVM, Compose runtime, depends on `core`), `compiler-plugin/build.gradle.kts` (Kotlin JVM, compiler API as compileOnly, depends on `core`), `tooling/build.gradle.kts` (Kotlin JVM, depends on `core`), `gradle-plugin/build.gradle.kts` (Gradle plugin, depends on `tooling`).
- [X] T004 Create `contract-tests/build.gradle.kts` as a test-only module that runs Android host tests and depends on `core`, `consumer` and `producer`, with one trivial test in `contract-tests/src/androidHostTest/kotlin/dev/tonholo/ssrcompose/contracttests/BuildExperimentTest.kt` that uses a class from `consumer` and a class from `producer`.
- [X] T005 Run `./gradlew build` and record the result in `specs/001-consumer-catalog-bridge/research.md` under R11 and R12. This is a gate: if the module shape cannot be built, stop and report.
- [X] T006 [P] Add `config/detekt/detekt.yml` (close to defaults, `FunctionNaming` ignores `@Composable`) and wire it in the `detekt` convention plugin so `./gradlew detekt` passes.
- [ ] T007 [P] Create the two sample builds as separate Gradle builds that share none of their own code: each uses only the root build (as an included build), its version catalog and the shared folder of catalogs. Write `samples/app/settings.gradle.kts` and `samples/app/build.gradle.kts` (Android application, cleartext allowed only in `samples/app/src/debug/AndroidManifest.xml`), `samples/server/settings.gradle.kts` and `samples/server/build.gradle.kts` (Kotlin JVM application), each sample reading the root `gradle/libs.versions.toml` as its `libs` catalog in its `settings.gradle.kts`, and the shared folder `samples/catalogs/` with a `README.md` that says it holds only catalogs, one folder per catalog version. `samples/app` has one flavor dimension, `catalog`, with two flavors: `current` and `catalogV1`. This task does not apply our Gradle plugin, since that plugin only exists after T024; T056 and T057 apply it.

**Checkpoint**: `./gradlew build detekt` passes with empty modules, and research.md says which build facts are checked.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The shared contract in `core`, the build-time catalog in `catalog-model`, the skeleton of the compiler plugin and the Gradle plugin, and the experiments that settle the riskiest unchecked facts inside the real modules.

**CRITICAL**: No user story work can begin until this phase is complete.

### The build-time catalog module

- [ ] T113 Start from the catalog code on the branch `rafaeltonholo/ssr-10-core-catalog`, which is not merged yet; the engineering manager brings that code in first. Then create `catalog-model/build.gradle.kts` (Kotlin JVM and serialization through its convention plugins with `alias(libs.plugins...)`, depends on `core` for `CatalogVersion` and `ValueKind` only), include it in `settings.gradle.kts`, make `compiler-plugin/build.gradle.kts` and `tooling/build.gradle.kts` depend on `catalog-model` instead of `core`, and make `gradle-plugin/build.gradle.kts` depend on `catalog-model` for the compiler plugin's id and option names. Create, test first, `catalog-model/src/test/kotlin/dev/tonholo/ssrcompose/catalog/options/CompilerPluginOptionsTest.kt` (the option names are not empty, are unique and hold only letters and digits; the generated id is not empty), then `<catalog>/options/CompilerPluginOptions.kt` with the option names `mode`, `fragmentsDir` and `catalog`, and apply the buildconfig plugin in `catalog-model/build.gradle.kts` with `alias(libs.plugins.buildconfig)` so it generates the compiler plugin id constant, in the package `dev.tonholo.ssrcompose.catalog.options`, from the `ssrcompose.compiler.plugin.id` property of the root `gradle.properties`. This is the only place the constant is generated. Move every catalog type, reader, writer, fingerprint and validation already written in `<core>/catalog/`, with its tests in `core/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/catalog/`, into `<catalog>/` and `catalog-model/src/test/kotlin/dev/tonholo/ssrcompose/catalog/`, so that only `<core>/catalog/CatalogVersion.kt` stays there. `ValueKind` is the one exception: it stays in `core` and moves from `<core>/catalog/ValueKind.kt` to `<core>/data/ValueKind.kt` (package `dev.tonholo.ssrcompose.core.data`), with its test of the list rule, while its helper that collects choice list names moves into `<catalog>/`. Run `./gradlew build` and confirm with `./gradlew :core:dependencies :consumer:dependencies :producer:dependencies` that none of them reaches `catalog-model`.

### Tests for the contract (write first, see them fail)

- [ ] T008 [P] Test catalog validation in `catalog-model/src/test/kotlin/dev/tonholo/ssrcompose/catalog/validation/CatalogValidationTest.kt`: component, parameter and choice names must be valid Kotlin identifiers that are not keywords; a part name holds only lowercase letters, digits and hyphens, "at most 64 characters", and an empty name, one with any other character, and a Windows device name such as `con`, `nul` or `com1` are rejected; a part rejects repeated names, a choice list with no entry, a choice parameter that names a choice list the part does not list, a callback with more than four arguments, and `bytes` anywhere except a callback argument; a part with 20,000 components and a component with 100 parameters are accepted, since there is no hard limit; across parts, a component name used twice and a choice list that differs between two parts are each rejected with both part names; a manifest is rejected when an entry's `file` is not its `name` followed by `.part.json` (for example `../x.part.json`, `https://x/y.part.json` or `a/b.part.json`), is empty, is `.`, is `manifest.json`, or is the same as another entry's `file`. Also, in `core/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/catalog/CatalogVersionTest.kt`: a `CatalogVersion` rejects empty text, text over "at most 64 characters" and control characters; and in `core/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/data/ValueKindTest.kt`: a list of bytes or a list of lists is rejected.
- [ ] T009 [P] Test the manifest and part files in `catalog-model/src/test/kotlin/dev/tonholo/ssrcompose/catalog/bundle/`: `CatalogBundleReaderTest.kt`, `CatalogBundleWriterTest.kt`, `SizeCapTest.kt` and `ContractExampleTest.kt` in `bundle/`, `io/CatalogManifestReaderTest.kt`, `io/CatalogManifestGoldenTest.kt`, `io/CatalogPartReaderTest.kt`, `io/CatalogPartStrictTest.kt` and `io/CatalogPartGoldenTest.kt` in `bundle/io/`, and `fingerprint/FingerprintTest.kt` in `bundle/fingerprint/`: the example in `contracts/catalog-bundle.md` parses as a manifest and two parts; writing then reading gives an equal catalog; the same catalog always gives the same bytes in every file (sorted parts, components and choices, parameters in declaration order, fixed key order, two-space indent, one trailing newline); `nullable` is left out when false; a file with an unknown `format` is refused; a part's fingerprint is the lowercase hexadecimal SHA-256 of its bytes, and the catalog's fingerprint is that of the manifest; a part that does not match its fingerprint in the manifest is refused; a manifest or a part over its size cap is refused before it is parsed, with 16 MiB as the default and a smaller cap set in the test.
- [ ] T010 [P] Test the handshake types in `core/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/handshake/HandshakeTypesTest.kt`: `Introduction` holds `contractVersion`, `catalogVersion`, `app` and `device` and no catalog; `DeviceContext.orientation` is derived from the width and height; contract versions compare in order and catalog versions only for equality; `RefusalKind` has exactly four cases: contract version too old, contract version too new, catalog version not known or no longer supported, introduction not valid.
- [ ] T011 [P] Test the JSON codec in `core/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/message/JsonMessageCodecTest.kt`: every message of `contracts/messages.md` survives encode then decode, including a `Screen` with nested slots and a `Trigger` with zero, one and four arguments and with bytes; the first frame is a 4-byte big-endian contract version followed by the payload, and no other frame has a prefix; `MessageFrame` reads the contract version of a first frame without decoding the payload; a first frame whose prefix differs from the contract version inside `Introduce` is refused; `decodeConsumerMessage` rejects an `Introduce`; a `ProducerMessage.Answered` encodes as a top-level `Accepted` or `Refused` message and decodes back; every decoding failure, including a frame that is not a message, throws `MessageDecodingException` and nothing else; `MessageCodec.json()` returns the codec for contract version 1.
- [ ] T012 [P] Test the in-memory connection in `core-testing/src/commonTest/kotlin/dev/tonholo/ssrcompose/core/testing/InMemoryConnectionTest.kt`: frames arrive in order both ways, and closing one end ends the other end's `incoming` flow.

### Implementation of the contract

- [ ] T014 [P] Add `@ConsumerComposable` and `@ProducerComposable` in `<core>/annotation/ConsumerComposable.kt` and `<core>/annotation/ProducerComposable.kt`, each allowed on functions and on types, kept in the compiled output.
- [ ] T015 Implement the catalog model and its validation in `<catalog>/model/` and `<catalog>/validation/`: `ChoiceList.kt`, `Parameter.kt` (value, callback with up to four arguments, slot with `accepts` and optional arguments, `required`, `nullable`), `ComponentDescriptor.kt`, `PartName.kt`, `PartEntry.kt`, `CatalogPart.kt`, `CatalogManifest.kt` and `Catalog.kt` in `model/`, whose constructors hold the checks of one part, and `Identifier.kt` and `CrossPartValidation.kt` in `validation/`, using `CatalogVersion` and `ValueKind` from `core` and defining no kind type of its own (depends on T113, T017, T018).
- [ ] T016 Implement the manifest and part reader and writer, the fingerprint and the size cap in `<catalog>/bundle/CatalogBundleReader.kt`, `CatalogBundleWriter.kt` and `SizeCap.kt`, `<catalog>/bundle/io/CatalogManifestReader.kt`, `CatalogManifestWriter.kt`, `CatalogPartReader.kt` and `CatalogPartWriter.kt`, and `<catalog>/bundle/fingerprint/Fingerprint.kt`; the manifest and part models, `CatalogManifest.kt` and `CatalogPart.kt`, are in `<catalog>/model/` (depends on T015).
- [ ] T017 [P] Implement `CatalogVersion` in `<core>/catalog/CatalogVersion.kt`, and `ContractVersion`, `DeviceContext` (with `Orientation` and `Theme`), `AppIdentity`, `Introduction`, the sealed `Answer` with `Accepted` and `Refused`, and `RefusalKind` in `<core>/handshake/`, one file per type.
- [ ] T018 [P] Implement `DataValue` in `<core>/data/DataValue.kt`, `ValueKind` in `<core>/data/ValueKind.kt` (the one definition of value kinds, used by `catalog-model` and `producer`; where it already exists in `<core>/catalog/ValueKind.kt`, T113 moves it here), and `PlacedComponent`, `ActionId` and `ScreenContent` in `<core>/message/ScreenContent.kt`.
- [ ] T019 Implement the messages in `<core>/message/ConsumerMessage.kt` and `<core>/message/ProducerMessage.kt` (with `Answered` wrapping an `Answer`), the first frame layout in `<core>/message/MessageFrame.kt`, `MessageCodec` with `MessageCodec.json()` in `<core>/message/MessageCodec.kt`, `MessageDecodingException` in `<core>/message/MessageDecodingException.kt`, and the contract version 1 JSON codec in `<core>/message/internal/JsonMessageCodec.kt`, which shows `Accepted` and `Refused` as top-level messages (depends on T017, T018).
- [ ] T020 [P] Implement `Connection` in `<core>/connection/Connection.kt` and a public `InMemoryConnection` pair in `core-testing/src/commonMain/kotlin/dev/tonholo/ssrcompose/core/testing/InMemoryConnection.kt`.
- [ ] T022 Run `./gradlew :core:build :catalog-model:build detekt` for every target, and confirm with `./gradlew :core:dependencies :consumer:dependencies :producer:dependencies` that `core` has no Android, server framework or transport dependency, and that none of the three depends on `catalog-model`.

### Skeletons and risk experiments

Each experiment below builds the smallest real piece in its real module, proves or disproves one unchecked fact from plan.md, and writes the result into research.md. An experiment whose result changes the design stops the work and is reported.

- [ ] T023 Create the compiler plugin skeleton in `<compiler>/`: `SsrCompilerPluginRegistrar.kt` and `SsrCommandLineProcessor.kt`. Their plugin id is the constant `catalog-model` generates from the `ssrcompose.compiler.plugin.id` property of the root `gradle.properties` (T113), never a constant in `core` and never a literal. Their options, `mode` (consumer or producer), `fragmentsDir`, and a repeated `catalog` option that gives, for each held catalog, its version, the path of its index and its bridge package, take their names from `<catalog>/options/CompilerPluginOptions.kt` in `catalog-model`, the one module the compiler plugin and both build-tool plugins see without `core`. In `compiler-plugin/build.gradle.kts`, add `src/test/resources/fixtures` as a test source folder so the IDE reads the fixtures, and exclude `*.fixture.kt` from test compilation. Add a compile test support class in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/testing/CompileTestSupport.kt` that runs kotlin-compile-testing with the Compose compiler registered after this plugin and reads its sources from `*.fixture.kt` files, and, written first, `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/SsrCommandLineProcessorTest.kt`: a mode that is neither consumer nor producer and a malformed `catalog` value each fail with a message that names the problem; and `SsrCompilerPluginTest.kt`: a fixture compiled with the options reaches the plugin with the mode, the fragments folder and every `catalog` value, and a compile without a mode fails.
- [ ] T024 Create the Gradle plugin skeleton in `<gradle>/`: `SsrComposePlugin.kt`, one `KotlinCompilerPluginSupportPlugin` class registered in the `gradlePlugin` block of `gradle-plugin/build.gradle.kts` with the id `dev.tonholo.ssrcompose`, and `SsrComposeExtension.kt` with the `catalogFileSizeLimit` setting (16 MiB by default), the `consumer { }` block (`catalogVersion`, `catalogDir`, `partName`) and the `producer { catalog(version) { location; bridgePackage } }` block of `contracts/build-tooling.md`. Its compiler plugin id is the same constant from `catalog-model` (T113), and its options use the names from `<catalog>/options/CompilerPluginOptions.kt`. In `applyToCompilation` it adds `core`, which holds the annotations, to the compile classpath of each compilation (the `implementation` dependencies of its default source set), at the plugin's own version, so a developer applies this one plugin and adds nothing else to mark composables. It passes `-Xcompiler-plugin-order` so this plugin runs before the Compose compiler, and declares each held catalog's index as a compile input. Add a TestKit support class in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/testing/FixtureProject.kt`, whose fixture builds apply the plugin with `alias(libs.plugins.ssrcompose)`, and, written first, `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/SsrComposePluginTest.kt`: a fixture that applies only this plugin and declares no dependency compiles a function marked `@ConsumerComposable`, the added `core` dependency has the plugin's own version, and the compilation receives the mode, the fragments folder or each `catalog` value under the compiler plugin id from `catalog-model`.
- [ ] T025 Experiment, bridge types (gate): in `<compiler>/producer/` create, from a one-component catalog in a throwaway index format of this experiment's own (T031a replaces it with the chosen format and deletes it), a bridge function whose parameters are a text, a `RemoteAction1<String>`, a nullable optional text with `null` as default, and a nullable optional slot, whose body passes its catalog version as a constant, with the order guard from `.superpowers/spikes/k2-bridge`. From a second one-component catalog with the same component name, create a second bridge in another package of the same module. In `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/BridgeExperimentTest.kt` and `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/producer/BridgeExperimentTest.kt` prove: it compiles and composes; the two bridges compile side by side and each passes its own catalog version; a lambda converts to `RemoteAction1`; leaving out an optional parameter compiles; recomposition after a state change updates the node; and the wrong plugin order fails with the guard's message. Record in research.md R1, R2, R8, R8a and R9.
- [ ] T026 Experiment, a component that cannot be placed (gate): find how to make every call to a created bridge function a compile error with a chosen message, and prove it with a compile test in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/producer/UnplaceableComponentTest.kt`. Record in research.md R8b.
- [ ] T027 Experiment, a part from a Kotlin Multiplatform Android module with variants (gate): write the smallest consumer checker and fragment writer in `<compiler>/consumer/` (text parameters only, one fragment folder per compilation), then a TestKit test in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/KmpAndroidFragmentsTest.kt` with a fixture that has two product flavors with different marked composables and one marked `expect` and `actual` pair. Prove: each variant gets its own fragments, the pair counts once, and an edit to one file leaves the module's part equal to a clean build. Record in research.md R4 and R8c.
- [ ] T028 Experiment, an annotation on a slot's type: prove in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/consumer/SlotAnnotationTest.kt` that the checker can read `@ConsumerComposable` and `@ProducerComposable` on a slot parameter's type. Record in research.md R4.
- [ ] T029 Experiment, consumer-side renderers (gate): in `<compiler>/consumer/` create a renderer for one marked composable with a text parameter, and prove in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/consumer/RendererExperimentTest.kt` that it compiles, that calling it calls the real composable with the decoded value, and that an edit to one file does not break another file's renderer. Record in research.md R7. If creating it in the compiler is fragile, report it: the hand-written bindings of `contracts/consumer-api.md` stay, and the created renderers become follow-up work.
- [ ] T030 [P] Experiment, the Kotlin toolchain: in `samples/toolchain-probe/` set up the smallest Kotlin toolchain project that applies the compiler plugin next to the Compose compiler. The Gradle samples use the root build as an included build; the Kotlin toolchain cannot, so first publish the artifacts to the local Maven repository with `./gradlew publishToMavenLocal`, and record in research.md R5 how the toolchain finds them there. Find how plugin order is decided there, whether the toolchain can build an Android library, and how the toolchain names its modules. Record in research.md R2 and R5, with the exact commands and output, and in R5 map the toolchain's module naming to the part name rule of `contracts/catalog-bundle.md`, so both build tools give the same part names.
- [ ] T031 [P] Experiment, fetching on IDE sync: find how a Gradle plugin makes a task run when the IDE syncs, try it in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/SyncHookTest.kt` as far as TestKit can show it, and record in research.md R6 what was checked and what needs a person in an IDE.
- [ ] T031a Experiment, scale (gate): after T016, T025 and T113, create a generator in `tooling/src/test/kotlin/dev/tonholo/ssrcompose/tooling/testing/SyntheticCatalog.kt` that writes a catalog bundle of 20,000 components and about 80,000 parameters over about 50 parts, with the mix real apps have (most components with 2 to 4 parameters, a few with 15 or more). In `tooling/src/test/kotlin/dev/tonholo/ssrcompose/tooling/CatalogScaleTest.kt` and `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/producer/BridgeScaleTest.kt` measure: the time to read and validate the bundle, each file within the 16 MiB cap; for each candidate index format, the time and memory to build it and to read it in the compiler plugin; the time and memory to compile a server module that calls 20 of the 20,000 bridge functions, with one held catalog and with 10, against the same module without a catalog; whether a bridge declaration can be created only when code names it; and what is recompiled or taken from the build cache when one component in one part changes. Choose the index format, replace the throwaway index format of T025 with it and delete the throwaway one, and record it with the numbers, the machine and the commands in research.md R16, and say whether the 2 second figure of SC-016 holds. This is a gate: if the catalog adds clearly more than 2 seconds to a clean compile with every index format tried, or a change to one component makes the whole server module recompile with no way around it, stop and report.

**Checkpoint**: The contract is tested on JVM and Android, `catalog-model` builds and nothing that runs depends on it, the plugin skeletons exist, and every gate experiment has a written result.

---

## Phase 3: User Story 1 - A server screen places an app composable (Priority: P1) MVP

**Goal**: Mark a composable, produce the catalog bundle, sync it into a separate server build, index it, call the bridge, connect, and see the composable on a device with the server's values.

**Independent Test**: quickstart.md, step 2.

### Tests for User Story 1 (write first, see them fail)

- [ ] T032 [P] [US1] Compile tests for the consumer checker in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/consumer/ConsumerCheckerTest.kt`: text, whole number, decimal number, yes or no, a choice from an enum with its entries, and a list of one kind are described; a default makes `required` false; a nullable value sets `nullable`; a modifier parameter is left out; an unsupported type fails at the parameter with a message that names the composable, the parameter and the reason; a marked function that is not `@Composable` fails. Also: a marked member function, a marked extension function and a marked function with type parameters each fail at the function with a message that says only top-level composables without type parameters can be marked.
- [ ] T033 [P] [US1] Test the part, the bundle and the check in `tooling/src/test/kotlin/dev/tonholo/ssrcompose/tooling/CatalogPartTest.kt`, `CatalogBundleTest.kt` and `CatalogCheckTest.kt`: fragments merge into one sorted part; a fragment whose source file is gone is ignored; a name used twice in a module fails and names both; fragments that disagree about a choice's entries fail; a part's name comes from the module path (`:feature:home` and `:feature_home` give `feature-home`, `:featureHome` gives `featurehome`, the root module `:` gives `root`) or from the name the developer set, and a derived name over 64 characters fails and asks for a name to be set; the bundle step writes a manifest that lists every part, sorted by name, with its file and fingerprint; a component name used in two parts, a choice list that differs between two parts, and two modules with the same part name each fail and name both places; the check passes when the bundle equals the saved bundle of its version byte for byte, and fails naming each file that differs when it does not, including when a composable moved to another module; the dump writes `manifest.json` and one `<name>.part.json` file per part, the same bytes twice, and removes a part file the manifest no longer lists. When no saved bundle exists for the version, the check fails with a message that says to run `ssrCatalogDump`, and writes nothing. Also: an app with no marked composable gives a valid manifest with no parts, a part of 20,000 components is not refused, and no output holds an absolute path or a timestamp.
- [ ] T034 [P] [US1] Test the sync in `tooling/src/test/kotlin/dev/tonholo/ssrcompose/tooling/CatalogSyncTest.kt` with a local test web server and file paths: the manifest is fetched and validated, then the parts not already saved for that version are fetched, each is checked against its fingerprint in the manifest, and all are saved in `ssr/catalogs/<version>/`, with the location and the manifest's SHA-256 written to the lock file as `fingerprint`; a sync after one saved part file was removed by hand fetches only that part, and a sync with update keeps the parts whose fingerprint did not change and fetches only the others; a manifest that loads while one of its parts cannot be fetched saves nothing and keeps the saved copy as it was; a manifest whose `file` is not its `name` followed by `.part.json`, is empty, is `.`, is `manifest.json`, or repeats another entry's `file` is rejected before any part is fetched; a manifest or a part over the size cap is refused before it is read; a manifest whose version differs from the declared one, a part that does not match its fingerprint, an invalid catalog, or one with a name that cannot be a Kotlin name, is rejected and nothing of it is saved; a location whose manifest no longer matches the lock file is reported and the saved copy is kept unless update is asked; an unreachable location uses the saved copy, and fails when there is none; an address that is not `https` or a file path is refused unless opted in. Every fetched manifest and part is read through the stream overload of `CatalogBundleReader`, so the size cap applies before a file is read in full.
- [ ] T035 [P] [US1] Test the verify step in `tooling/src/test/kotlin/dev/tonholo/ssrcompose/tooling/CatalogVerifyTest.kt`: it fails when a saved manifest does not match its fingerprint in the lock file, when a saved part does not match its fingerprint in the manifest, when a part file the manifest lists is missing from the saved folder, when a declared catalog has no saved bundle, when a saved manifest's version differs from its declared version, and when the saved folder holds a part file the manifest does not list.
- [ ] T116 [P] [US1] Test the catalog index in `catalog-model/src/test/kotlin/dev/tonholo/ssrcompose/catalog/index/CatalogIndexTest.kt`, in the format T031a chose: the same saved catalog always gives the same index bytes; the index holds the catalog version and every component, parameter and choice list a bridge needs; it holds no absolute path and no timestamp; reading it back gives what was written.
- [ ] T036 [P] [US1] Compile tests for the bridges in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/producer/BridgeGenerationTest.kt`: for each held catalog, one composable function per component and one enum per choice list, in that catalog's bridge package; two held catalogs with the same component names give two bridges that compile side by side in one module; each created body passes its own catalog version to `ConsumerComponent`; value, choice and list parameters with the right types; a wrong name, a missing required parameter and a value of the wrong kind each fail to compile at the caller's line (SC-004); the plugin reads only the index and never a catalog's JSON; the created code refers to no class outside `core` and `producer`.
- [ ] T037 [P] [US1] Test the live composition in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/internal/LiveScreenTest.kt`: content with state emits a new `ScreenContent` when the state changes, a collector that cancels disposes the composition, and equal content is not sent twice.
- [ ] T038 [P] [US1] Test `ConsumerComponent` and the session's happy path in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/ConsumerComponentTest.kt` and `ProducerSessionTest.kt` over an `InMemoryConnection` from `core-testing`, added as a test dependency: a placed component carries its name and values in call order, and its node carries the catalog version the bridge passed; a parameter that was not given is absent; a valid `Introduce` for a served catalog version gets `Accepted` with the contract version before anything else; `OpenScreen` gets the `Screen` registered under that name for the session's catalog version; a name with no screen for that version gets `ScreenUnavailable` and the session stays accepted; a second `OpenScreen` replaces the open screen and disposes the first composition; `LocalConsumer.current` exposes `device`, `app` and `catalogVersion`. A required nullable parameter given `null` is sent as an explicit nothing. A parameter that can be left out and was not given is absent for values, callbacks and slots alike (FR-062).
- [ ] T118 [P] [US1] Test screen registration in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/ProducerRegistrationTest.kt`: `Producer { catalog(version) { screen(name) { } } }` serves exactly the catalog versions that have screens; the same screen name under two versions gives two different screens; a version registered twice, or a screen name registered twice in one version, makes `Producer` fail at once with a message that names it; by default `supportedContractVersions` holds every contract version the library has a codec for, and a server developer can narrow it; a `supportedContractVersions` that is empty or holds a version the library has no codec for makes `Producer` fail at once; a producer starts with no catalog and reads none.
- [ ] T040 [P] [US1] Test the consumer side in `consumer/src/commonTest/kotlin/dev/tonholo/ssrcompose/consumer/ComponentBindingsTest.kt` and `internal/ConsumerSessionTest.kt`: hand-written bindings read text, numbers, flags, choices and lists from a placed component; the session sends `Introduce` first with every value from a fake device values source and the build's catalog version, waits for `Accepted`, sends `OpenScreen`, and exposes each `Screen` as state. The catalog version is read from the generated resource through `CatalogVersionLoader`.
- [ ] T041 [P] [US1] Contract test in `contract-tests/src/androidHostTest/kotlin/dev/tonholo/ssrcompose/contracttests/FirstScreenContractTest.kt`: a real `Producer` and a real consumer session joined by an `InMemoryConnection` from `core-testing`, added to `contract-tests` as a test dependency, end with the consumer holding the screen the producer composed.
- [ ] T042 [P] [US1] Compose UI test in `consumer/src/androidDeviceTest/kotlin/dev/tonholo/ssrcompose/consumer/SsrScreenTest.kt`: `given_a_screen_with_a_placed_component__when_shown__then_the_app_composable_draws_the_values`, with a fake `Connection`, and the default problem message with its retry button.
- [ ] T043 [P] [US1] TestKit tests in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/ConsumerTasksTest.kt`, `ProducerTasksTest.kt` and `MultiModuleCatalogTest.kt`: `ssrCatalogPart` writes the module's part; `ssrCatalogDump` writes `ssr/catalogs/<version>/` with `manifest.json` and the parts; `ssrCatalogCheck` fails when a marked composable changes without a new version; in a fixture with an app module and two feature modules, the app module's bundle collects all three parts, and a name used in two modules fails and names both; `ssrCatalogSync` saves and pins, and refuses a part larger than a `catalogFileSizeLimit` set below its size; `ssrCatalogIndex` runs once for each held catalog; `ssrCatalogVerify` fails on a changed saved file; a build with the network disabled gives the same output as one with it (SC-009); editing a saved part runs that catalog's index again and causes a recompile. Also: a fixture with the Compose compiler and an Android variant builds and its bridges compose, which proves the plugin sets the order flag and that the Android build keeps it.
- [ ] T114 [P] [US1] TestKit tests for build performance in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/CatalogBuildCacheTest.kt`, with a fixture of an app module, three feature modules and a server module that holds two catalogs: a second build with nothing changed runs no catalog step (FR-070); a change to one marked composable in one feature module runs only that module's `ssrCatalogPart` and the `ssrCatalogBundle` (FR-071); the outputs of `ssrCatalogPart`, `ssrCatalogBundle` and `ssrCatalogIndex` hold no absolute path and no timestamp, and a copy of the fixture in another folder that shares a local build cache takes every one of them from the cache (FR-072); the configuration cache is reused on the second build. The copy in another folder stands in for the second machine of SC-015. SC-015 on two real machines with a shared remote cache is not checked by this task. Known limit: `FilesOptionKind` has only `INTERNAL` in Kotlin 2.4.21, so the plugin passes absolute paths (each index, each fragments folder) as `InternalSubpluginOption`, which keeps them out of the `compileKotlin` inputs, and tracks the index files as relative-path inputs.

### Implementation for User Story 1

- [ ] T044 [US1] Complete the consumer checker and the fragment writer for value, choice and list parameters in `<compiler>/consumer/ConsumerComposableChecker.kt`, `ParameterKindMapper.kt`, `ConsumerErrors.kt` and `FragmentWriter.kt`.
- [ ] T045 [P] [US1] Implement the part, the bundle, the check and the dump in `<tooling>/CatalogPartStep.kt`, `CatalogBundleStep.kt`, `PartNames.kt`, `CatalogCheck.kt` and `CatalogDump.kt`.
- [ ] T046 [P] [US1] Implement the sync, the lock file and the verify step in `<tooling>/CatalogSync.kt`, `CatalogLock.kt` and `CatalogVerify.kt`, with the reader and the size cap from `catalog-model`. Every fetched manifest and part is read through the stream overload of `CatalogBundleReader`, so the size cap applies before a file is read in full.
- [ ] T117 [US1] Implement the catalog index in `<catalog>/index/CatalogIndexWriter.kt` and `<catalog>/index/CatalogIndexReader.kt`, in the format T031a chose, and the index step in `<tooling>/CatalogIndexStep.kt` (depends on T016, T031a).
- [ ] T047 [US1] Register `ssrCatalogPart`, `ssrCatalogBundle`, `ssrCatalogCheck`, `ssrCatalogDump`, `ssrCatalogSync`, `ssrCatalogIndex` and `ssrCatalogVerify` in `<gradle>/tasks/`, one file per task, with `ssrCatalogCheck` and `ssrCatalogVerify` part of `check`, per variant as research.md R8c says (depends on T045, T046, T117). `ssrCatalogPart`, `ssrCatalogBundle` and `ssrCatalogIndex` are cacheable, track their inputs by relative path, and write no absolute path or timestamp. The build also writes each variant's catalog version into a small generated resource that `<consumer>/internal/CatalogVersionLoader.kt` reads.
- [ ] T105 [US1] Collect the parts of several modules: the module that sets the catalog version finds the parts of the project modules it depends on that apply the plugin, through the build tool's own dependency resolution and with no absolute path, in `<tooling>/PartCollection.kt` and `<gradle>/tasks/BundleInputs.kt`, so `MultiModuleCatalogTest.kt` (T043) and `CatalogBuildCacheTest.kt` (T114) pass. Record in research.md R4 how the parts are found (depends on T047).
- [ ] T048 [US1] Complete the bridge creation for value, choice and list parameters in `<compiler>/producer/BridgeDeclarationGenerator.kt`, `BridgeBodyGenerator.kt` and `PluginOrderGuard.kt`: one bridge per held catalog, read from its index, in its bridge package, each body passing its catalog version as a constant, with a readable file name for the created functions. Create a declaration only when code names it if T031a found that possible; otherwise create them all, as T031a measured (depends on T117).
- [ ] T049 [US1] Implement the producer's tree applier and live composition in `<producer>/internal/ScreenApplier.kt` and `<producer>/internal/LiveScreen.kt`, with one snapshot observer shared by the whole process in `<producer>/internal/SnapshotNotifier.kt`.
- [ ] T050 [US1] Implement `ConsumerComponent` in `<producer>/ConsumerComponent.kt`, taking the bridge's catalog version and keeping it on the placed node, and `LocalConsumer` with its `Consumer` type (`device`, `app`, `catalogVersion`) in `<producer>/LocalConsumer.kt` (depends on T049).
- [ ] T051 [US1] Implement `Producer` and its registration builder, with `catalog(version) { screen(name) { } }`, `supportedContractVersions`, `limits` and `observer`, in `<producer>/Producer.kt` and `<producer>/ProducerBuilder.kt`, the screen registry in `<producer>/internal/ScreenRegistry.kt`, and `ProducerLimits` and `ProducerObserver` in their own files in `<producer>/`, with the session's accept, open-screen, screen-not-registered, replace-screen and close paths in `<producer>/internal/Session.kt`, following the state table in data-model.md (depends on T050).
- [ ] T052 [P] [US1] Implement `ComponentBindings` in `<consumer>/ComponentBindings.kt`, and `PlacedComponentScope` in `<consumer>/PlacedComponentScope.kt`: the consumer type a binding receives, which wraps core's `PlacedComponent` with the session and reads its values (`text`, `whole`, `decimal`, `flag`, `choice`, `list`).
- [ ] T053 [P] [US1] Implement the device values source in `<consumer>/internal/DeviceValues.kt` with the common Compose APIs; put a value in `consumer/src/androidMain/` behind an internal interface only if common code cannot read it, and record which in research.md.
- [ ] T054 [US1] Implement the consumer session in `<consumer>/internal/ConsumerSession.kt`, and `SsrScreen` and `ScreenProblem` in `<consumer>/SsrScreen.kt` and `<consumer>/ScreenProblem.kt`, rendering `ScreenContent` top to bottom (depends on T052, T053).
- [ ] T055 [US1] Complete the consumer-side renderers for value, choice and list parameters and the renderer index in `<compiler>/consumer/RendererGenerator.kt`, `<tooling>/RendererIndex.kt` and `<consumer>/internal/RendererIndexLoader.kt`, so `SsrScreen` needs no hand-written bindings. If T029 reported this as fragile, skip this task, say so, and use hand-written bindings in the samples.
- [ ] T056 [US1] Write the sample app in `samples/app/src/main/kotlin/dev/tonholo/ssrcompose/samples/app/`: `MainActivity.kt`, `components/Greeting.kt` and `components/Style.kt` marked with `@ConsumerComposable`, `WebSocketConnection.kt`, catalog version `"1"`, showing `SsrScreen(screen = "home", ...)` against `127.0.0.1:8788`; apply our Gradle plugin in `samples/app/build.gradle.kts` with `alias(libs.plugins.ssrcompose)`, the first time a sample needs it; run `ssrCatalogDump` and copy the folder `samples/app/ssr/catalogs/1/` to `samples/catalogs/1/`. `Greeting` and `Style` live in `src/main`, since both flavors have them, and both flavors start at catalog version `"1"`.
- [ ] T057 [US1] Write the sample server in `samples/server/src/main/kotlin/dev/tonholo/ssrcompose/samples/server/`: `Main.kt` (Ktor on port 8788, bound to `127.0.0.1`), `WebSocketConnection.kt`, `HomeScreenV1.kt` that calls the version 1 bridge's `Greeting` and is registered under catalog version `"1"`, and an observer that prints each event; apply our Gradle plugin in `samples/server/build.gradle.kts` with `alias(libs.plugins.ssrcompose)`, the first time the server sample needs it; declare catalog `"1"` at `samples/catalogs/1/manifest.json` with the bridge package `dev.tonholo.ssrcompose.samples.server.bridge.v1`, run `ssrCatalogSync`, and confirm with `./gradlew -p samples/server dependencies` that nothing from `samples/app` is on its classpath (SC-003).
- [ ] T058 [US1] Run quickstart.md step 2 on an emulator, including the three deliberate compile failures, look at the screenshot, fix what is wrong, and report the screenshot path, the server's event output, and the time from launch to first screen from the session log (SC-005).
- [ ] T059 [US1] Run a quick load probe in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/SessionProbeTest.kt`: hold 1,000 sessions of the sample home screen over `InMemoryConnection` from `core-testing` (a test dependency), and report the memory per session and whether the bound of 10,000 looks reachable. This is an early warning, not the load test of the last phase. Record the result in research.md R14.

**Checkpoint**: User Story 1 works on a device, with two builds that share only a catalog, and the catalog steps are cached per module. This is the MVP.

---

## Phase 4: User Story 2 - A tap in the app runs a lambda on the server (Priority: P2)

**Goal**: Callbacks of an app composable run lambdas on the server, with or without arguments, and the screen updates.

**Independent Test**: quickstart.md, step 3.

### Tests for User Story 2 (write first, see them fail)

- [ ] T060 [P] [US2] Compile tests for callbacks in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/consumer/CallbackKindsTest.kt` and `producer/BridgeCallbacksTest.kt`: callbacks with no argument and with one to four arguments, including `ByteArray`, are described with their `arguments`; a callback with more than four arguments, or with a result other than `Unit`, fails at the parameter; the bridge gives each one the matching type from `RemoteAction0` to `RemoteAction4`, and its body gives `ConsumerComponent` a `Callback` with the argument kinds as core's `ValueKind` and a conversion to the lambda's types; a lambda with the wrong number or kinds of parameters fails to compile. Running the created conversion: an unknown choice entry, a whole number out of range for an `Int` parameter, and a list with one element of the wrong kind each fail the conversion, and a known entry, an in-range number and a good list each give the lambda its typed values.
- [ ] T061 [P] [US2] Test actions on the producer in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/ActionsTest.kt`: a placed component's callbacks carry action IDs unique in the session; a `Trigger` runs the latest lambda the screen gave; arguments reach the lambda in order with their kinds; a state change in the lambda sends a new `Screen`; triggers run in the order they arrive; a trigger for an action the screen no longer has is ignored, reported through `onTriggerIgnored`, and does not end the session; arguments whose number or kinds do not match what the bridge declared for that callback are refused and reported through `onTriggerRefused`; with a hand-written bridge, a choice entry the enum does not have, a whole number out of range for an `Int` argument, and a list with one wrong element each fail the conversion, run no lambda, change no state, call `onTriggerRefused` and never `onActionFailed`, and the session carries on; arguments over "1 megabyte in total by default" are refused without being decoded; a trigger in one session never changes another session. Also: a lambda that throws leaves the screen's state and the last sent `Screen` unchanged, calls `onActionFailed`, and the session carries on; triggers over "100 per second by default" are dropped, call `onTriggerDropped`, and do not end the session.
- [ ] T062 [P] [US2] Test callbacks on the consumer in `consumer/src/commonTest/kotlin/dev/tonholo/ssrcompose/consumer/internal/CallbackTest.kt`: `placed.callback("name")` gives a function that sends a `Trigger` with the action ID and its arguments as values; a callback the screen did not give falls back to the composable's default.
- [ ] T063 [P] [US2] Contract test in `contract-tests/src/androidHostTest/kotlin/dev/tonholo/ssrcompose/contracttests/CallbackContractTest.kt`: a callback triggered on a real consumer session runs the server lambda and the consumer ends with the updated screen.

### Implementation for User Story 2

- [ ] T064 [US2] Add callback kinds to the consumer checker, the fragment writer and the renderers in `<compiler>/consumer/ParameterKindMapper.kt`, `FragmentWriter.kt` and `RendererGenerator.kt`.
- [ ] T065 [P] [US2] Add `RemoteAction0` to `RemoteAction4` as function interfaces with no result type in `<producer>/RemoteAction.kt`, and `Callback` with its value readers in `<producer>/Callback.kt`, holding the argument kinds as core's `ValueKind` and the conversion that wraps the `RemoteAction`, as `contracts/producer-api.md` sketches. No kind type is defined in `producer`.
- [ ] T066 [US2] Add callback parameters to the bridge creation in `<compiler>/producer/BridgeDeclarationGenerator.kt` and `BridgeBodyGenerator.kt`, each passed to `ConsumerComponent` as a `Callback` with its argument kinds as core's `ValueKind` and a conversion from the received values to the lambda's typed arguments: a choice entry to its enum, a whole number to `Int` or `Long`, a decimal to `Float` or `Double`, a list to a typed list. The conversion fails on an unknown choice entry, a whole number out of range for `Int`, and a list with one wrong element, as T060 tests (depends on T065).
- [ ] T067 [US2] Implement the session's actions in `<producer>/internal/Actions.kt` and handle `Trigger` in `<producer>/internal/Session.kt`: assign IDs, always run the latest lambda, check the number and kinds of the arguments against the `Callback`, then run its conversion, and treat a failure in either step as a refused trigger (nothing runs, `onTriggerRefused`, never `onActionFailed`), enforce the size limit, and report ignored and refused triggers. Run each lambda in a snapshot that is applied only when it finishes, as research.md R10a says, and enforce the per-session trigger limit from `ProducerLimits`.
- [ ] T068 [US2] Send triggers from the consumer in `<consumer>/internal/ConsumerSession.kt` and expose `callback` on `PlacedComponentScope` in `<consumer>/PlacedComponentScope.kt`.
- [ ] T069 [US2] Extend the samples: a marked `CounterCard(count: Int, onAddOne: () -> Unit)` and a marked `NameField(onSubmit: (String) -> Unit)` in `samples/app/.../components/`, catalog version `"2"` dumped and copied to `samples/catalogs/2/`, and in `samples/server/.../` catalog `"2"` declared with the bridge package `...bridge.v2`, with `HomeScreenV2.kt` and `CounterScreenV2.kt` registered under version `"2"`: their own screen functions, written with the version 2 bridge, the count held with `remember` in a plain Kotlin state class in `CounterState.kt` that later versions share. The new marked composables go in `samples/app/src/current/kotlin/dev/tonholo/ssrcompose/samples/app/components/`, so only the `current` flavor has them. Only the `current` flavor's catalog version is raised; `catalogV1` stays at `"1"`, and the version `"1"` registration stays on the server.
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
- [ ] T078 [US3] Render slot content in `<consumer>/SsrScreen.kt` and expose `slot` on `PlacedComponentScope` in `<consumer>/PlacedComponentScope.kt`.
- [ ] T079 [US3] Extend the samples: a marked `Section(title: String, content: @Composable () -> Unit)` in `samples/app/.../components/`, catalog version `"3"` dumped and copied to `samples/catalogs/3/`, and in `samples/server/.../` catalog `"3"` declared with the bridge package `...bridge.v3`, with `HomeScreenV3.kt`, `CounterScreenV3.kt` and `SectionsScreenV3.kt` registered under version `"3"`: their own screen functions, written with the version 3 bridge, reusing `CounterState.kt`, with a section inside a section in `SectionsScreenV3.kt`. The version `"1"` and `"2"` screens stay registered. The new marked composable goes in `samples/app/src/current/`, and only the `current` flavor's catalog version is raised.
- [ ] T080 [US3] Run quickstart.md step 4 on an emulator, including the compile error for a slot with arguments, look at the screenshots, and report their paths.

**Checkpoint**: User Stories 1 to 3 work on a device.

---

## Phase 6: User Story 4 - Apps that did not update keep working (Priority: P4)

**Goal**: One producer serves several catalog and contract versions at once, gives each app the screens registered for its own catalog version, stops any screen that mixes bridges, and refuses the versions it does not serve with a reason the user sees.

**Independent Test**: quickstart.md, step 5.

### Tests for User Story 4 (write first, see them fail)

- [ ] T081 [P] [US4] Test introduction validation in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/internal/IntroductionValidatorTest.kt`, one test per rule of research.md R14: frame "at most 8 KB", density "above 0 and at most 10", width and height "1 to 100,000 pixels", language tag "at most 35 characters", and app version, variant, flavor and catalog version "at most 64 characters each with no control characters".
- [ ] T082 [P] [US4] Test refusals in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/RefusalTest.kt`: a catalog version with no registered screens, a contract version below the set (too old), above its highest version (too new), and in a gap of the set, such as 2 when the set is `{1, 3}` (too old), an invalid introduction, a frame over the limit (refused without being decoded), and any message before `Introduce` each get `Refused` with the right kind, a reason with no internal details, no `Screen`, and an `onRefused` event.
- [ ] T083 [P] [US4] Test serving several versions in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/MultiVersionTest.kt`: two sessions with different catalog versions that ask for the same screen name each get the screen registered for their own version, at the same time; a composable that only the older catalog has is placed for older sessions through the older bridge; a screen name registered for one version only gets `ScreenUnavailable` and an `onScreenNotRegistered` event for a session of another version, which stays accepted; three contract versions are served at once using two test-only codecs registered through the same internal seam as the real one; what one session introduced never appears in another.
- [ ] T084 [P] [US4] Test the guard and failing screens in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/ScreenGuardTest.kt`: a screen registered for one version that places a component from another version's bridge gets `ScreenUnavailable` and no partial `Screen`, keeps the session accepted, and calls `onComponentFromOtherCatalog` with the component, the screen and both versions; such a component inside a slot follows the same rule; a screen that places such a component only after a state change gets `ScreenUnavailable` at that change and its composition is disposed; a screen whose code throws gets `ScreenUnavailable` with no internal details and an `onScreenStopped` event, and the session can open another screen.
- [ ] T085 [P] [US4] Test the consumer's problem handling in `consumer/src/commonTest/kotlin/dev/tonholo/ssrcompose/consumer/internal/ScreenProblemTest.kt`: `Refused` becomes `ScreenProblem.Refused`; `ScreenUnavailable` becomes `ScreenProblem.Unavailable`; something the consumer cannot draw becomes `ScreenProblem.Mismatch` with no crash and no developer placeholder; a producer message it cannot decode is treated as a refusal; the default English message says why in one sentence and what the user can do; `retry` asks for the screen again after `Unavailable` and introduces the app again after `Refused`.
- [ ] T086 [P] [US4] Compatibility matrix contract test in `contract-tests/src/androidHostTest/kotlin/dev/tonholo/ssrcompose/contracttests/CompatibilityMatrixTest.kt`: for every pairing of sample catalog version (1, 2, 3) and sample screen, the consumer ends with a correct screen, an unavailable screen or a refusal, and never something it cannot draw (SC-002).
- [ ] T087 [P] [US4] TestKit test in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/VariantCatalogsTest.kt`: a flavor that sets its own catalog version is checked against its own saved bundle, and two variants that share a version but differ in marked composables fail the check.

### Implementation for User Story 4

- [ ] T088 [US4] Implement the introduction validator and the frame limit in `<producer>/internal/IntroductionValidator.kt`, configurable through `ProducerLimits`.
- [ ] T089 [US4] Pick the session's screens by its catalog version, refuse a version with no registered screens, and implement the refusal paths and one codec per supported contract version, in `<producer>/internal/Session.kt`, `<producer>/internal/ScreenRegistry.kt` and `<producer>/internal/CodecRegistry.kt` (depends on T088).
- [ ] T090 [US4] Implement the guard in `<producer>/internal/ScreenGuard.kt`, run before each `Screen` is sent, and stop the screen on a mismatch or when its code throws, in `<producer>/internal/Session.kt`.
- [ ] T091 [US4] Handle `Refused`, `ScreenUnavailable`, undrawable content and undecodable messages in `<consumer>/internal/ConsumerSession.kt`, with the default messages and the `retry` action in `<consumer>/SsrScreen.kt`.
- [ ] T092 [US4] Add per-variant catalog versions to `<gradle>/SsrComposeExtension.kt` and `<gradle>/tasks/`, as T027 found.
- [ ] T093 [US4] Extend the samples: confirm that the `catalogV1` flavor, which has only the composables in `src/main`, still produces a bundle equal to `samples/catalogs/1/`; make the sample server, which holds catalogs 1, 2 and 3 with a bridge each, take the catalog versions to register from a command line argument in `samples/server/.../Main.kt`; and add a `MixedScreen.kt` registered under version `"1"` that places a composable from the version 3 bridge, to show the guard.
- [ ] T094 [US4] Run quickstart.md step 5 with both app flavors against one server, then with version 1 not registered, look at the screenshots, and report their paths and the server's event output.

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
- [ ] T099 [US5] Add an `AdaptiveScreen.kt` in `samples/server/.../`, registered under the `current` flavor's catalog version, that picks its text by theme and orientation and holds a counter with `remember`, and a way to open it in `samples/app/.../MainActivity.kt`.
- [ ] T100 [US5] Run quickstart.md step 6 on an emulator (dark theme with `adb -s <serial> shell cmd uimode night yes`, then rotate), look at the screenshots, and report their paths and the time from each change to the updated screen from the session log (SC-007).

**Checkpoint**: All five user stories work on a device.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: The second build tool, the measured bounds, the remaining unchecked facts, and docs that are true.

- [ ] T101 Implement the Kotlin toolchain plugin in `toolchain-plugin/` (`module.yaml`, `plugin.yaml` with the tasks `ssrCatalogPart`, `ssrCatalogBundle`, `ssrCatalogCheck`, `ssrCatalogDump`, `ssrCatalogSync`, `ssrCatalogIndex` and `ssrCatalogVerify`, and `toolchain-plugin/src/dev/tonholo/ssrcompose/toolchain/` calling `tooling`), following what T030 found. It depends on `tooling` and `catalog-model`, reads the compiler plugin id constant and the option names from `catalog-model` with no literal compiler plugin id in any yaml file, and adds `core` to each compilation at the plugin's own version, as the Gradle plugin does. The Gradle samples use the root build as an included build; the Kotlin toolchain cannot, so the toolchain plugin and `samples/toolchain-server/` read the artifacts published to the local Maven repository with `./gradlew publishToMavenLocal`, the way T030 found. Add `samples/toolchain-server/` as the sample server set up for the Kotlin toolchain. Read the open risk first: the toolchain may not let this plugin add the compiler plugin, which conflicts with the no literal id rule above, and the owner decides at the start of this task ([research.md R5](research.md#open-risk-for-t101)).
- [ ] T102 Run quickstart.md step 9, confirm the toolchain run passes the same compiler plugin id as the `ssrcompose.compiler.plugin.id` property, and record in research.md R5 whether the Kotlin toolchain path is now checked, for the server and, if the toolchain can build one, for an Android library.
- [ ] T103 Add a `loadTest` source set and task to `producer/build.gradle.kts`, and write `producer/src/loadTest/kotlin/dev/tonholo/ssrcompose/producer/SessionLoadTest.kt`: hold 10,000 sessions over 10 served catalog versions, measure the time to accept an app and send its first screen, and check that 1,000 healthy sessions see no error and get their screens no more than 100 milliseconds later while one session sends invalid and oversized introductions and callback arguments.
- [ ] T104 Run `./gradlew :producer:loadTest`, write the measured numbers into research.md R14, and report any success criterion (SC-010, SC-011) that is not met. The accept time measured here is in memory on the JVM and is not the device number of SC-005.
- [ ] T115 Measure the finished build with the synthetic catalog of T031a, 20,000 components over about 50 parts, in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/CatalogScaleBuildTest.kt`: run the scenarios of T114 at that size, and the clean compile of a server module that calls 20 of its composables with and without the catalog. Write the numbers, the machine and the commands into research.md R16, and report each of SC-013 to SC-016 that is not met. This is a gate: it confirms on the finished build the figures T031a measured, and reports any that changed.
- [ ] T106 [P] Write `docs/editor-setup.md` with the IDE registry setting `kotlin.k2.only.bundled.compiler.plugins.enabled`, and a short list for the owner to check by hand in an IDE, since an agent cannot: the bridge functions and enums resolve, a catalog edit is picked up after a sync, and the enum workaround of research.md R3 holds. Record the owner's result in research.md R13 when it comes.
- [ ] T107 [P] Write the ADRs listed in research.md R15 as `docs/adr/0002-compiler-created-bridge-per-catalog.md`, `docs/adr/0003-catalog-bundle-with-parts-and-manifest.md`, `docs/adr/0004-build-tool-plugins-over-shared-tooling.md`, `docs/adr/0005-catalogs-fetched-at-sync-and-pinned.md`, `docs/adr/0006-module-layout-and-targets.md` (including the build-time `catalog-model` library) and `docs/adr/0007-screens-per-catalog-version.md`, and add them to the index in `docs/adr/README.md`.
- [ ] T108 [P] Add short API documentation to every public type named in the one list of public `core` types in `data-model.md` ("What is public in `core`") and under "What is public" in `contracts/producer-api.md` and `contracts/consumer-api.md`, and confirm with `grep` that nothing else in `core`, `consumer` and `producer` is public: `core` against the data model's list, `core-testing` against `InMemoryConnection` and its `pair()` only, and each library against its own contract's list.
- [ ] T109 Update the agent files with the real modules and commands: `.ai/guidelines.md` (module map with `catalog-model`, commands, QA targets), `.ai/registry.md`, `.ai/rules/architecture.md` (the catalog is produced, is build time only and lives in `catalog-model`, two catalogs, consumer and producer naming), the test table in `.ai/rules/testing.md` and `.ai/skills/build-and-verify/SKILL.md`, then run `python3 .ai/scripts/check-links.py` and `python3 .ai/scripts/sync-agents.py --check`.
- [ ] T110 [P] Update `README.md` (status and a short usage example), `CONTRIBUTING.md` (setup commands for the root build), `CHANGELOG.md`, and write `docs/getting-started.md` that takes a new developer from marking one composable to seeing it on a device, including the rule that the producer is released before or together with the app (SC-001, FR-064). The getting started page also says plainly that screens are written per catalog version, so screens for two versions repeat their layout while state and logic can be shared as plain Kotlin; that a catalog shows component names to anyone who can read its location; and which Kotlin version the release supports.
- [ ] T111 Check the whole diff against `.ai/rules/security.md`, rule by rule, and FR-059: no reason or message that reaches the user carries a stack trace, a class name or another app's values. Fix what breaks one.
- [ ] T112 Run every step of `specs/001-consumer-catalog-bridge/quickstart.md` from a clean checkout, including step 7 with no network and step 10, then `./gradlew build detekt`, and report each step's result, including any that could not run.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies. T005 is a gate.
- **Foundational (Phase 2)**: needs Setup. Blocks every user story. T113 comes first. T025, T026, T027, T029 and T031a are gates: a result that changes the design stops the work and is reported.
- **User Story 1 (Phase 3)**: needs Foundational. T116 and T117 need the index format from T031a. Its last task, T059, is an early load probe.
- **User Story 2 (Phase 4)**: needs User Story 1.
- **User Story 3 (Phase 5)**: needs User Story 1. It does not need User Story 2.
- **User Story 4 (Phase 6)**: needs User Stories 1, 2 and 3, since the compatibility matrix uses all three sample catalogs.
- **User Story 5 (Phase 7)**: needs User Story 1 only.
- **Polish (Phase 8)**: needs all five stories. T115 is the last build measurement gate. T101, T106, T107 and T110 can start earlier.

### User Story Dependencies

```text
Setup -> Foundational -> US1 -> US2 --\
                           |-> US3 ---+-> US4
                           \-> US5
```

US2, US3 and US5 can be built side by side once US1 is done, as long as changes to `<producer>/internal/Session.kt`, `<consumer>/internal/ConsumerSession.kt`, `<consumer>/SsrScreen.kt` and the files under `<compiler>/` are made one at a time.

### Within Each User Story

- Tests first, and they must fail before the implementation starts.
- `core` and `catalog-model` first, then the compiler plugin and tooling, then the libraries, then the samples, and the device check last.
- A story is done when its device check task is done and reported.

### Parallel Opportunities

- Phase 1: T006 and T007 after T005.
- Phase 2: after T113, the tests T008 to T012 together; then T014, T017, T018 and T020 together; T030 and T031 next to the other experiments.
- Each story: all of its test tasks together. In US1 also T045, T046, T052 and T053.
- Phase 8: T106, T107, T108 and T110 together.

---

## Parallel Example: User Story 1

```text
Write the tests together:
  T032 consumer checker       T037 live composition      T041 contract test
  T033 part, bundle, check    T038 component and session T042 Compose UI test
  T034 sync                   T118 screen registration   T043 Gradle tasks
  T035 verify                 T040 consumer side         T114 build cache
  T116 catalog index          T036 bridges

Then in parallel, since they touch different modules:
  T045 part, bundle, check, dump   T052 ComponentBindings
  T046 sync, lock, verify          T053 device values
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1, with the build as the gate.
2. Phase 2: the build-time catalog module, the contract, the skeletons, and the experiments. Each gate has a written result before the next phase.
3. Phase 3, User Story 1.
4. Stop and check on a device (T058). Show it to the owner.

### Incremental Delivery

1. Setup and Foundational: the contract is tested and the risky facts are settled, including the scale gate.
2. User Story 1: an app composable placed by a separate server build, on a device, with the catalog cached per module. MVP.
3. User Story 2: taps run server lambdas.
4. User Story 3: server content inside app composables.
5. User Story 4: old apps keep working through screens per catalog version, and unserved ones are refused clearly.
6. User Story 5: screens follow the device.
7. Polish: the second build tool, the measured bounds, and true docs.

Each story ends with a device check, so the feature is visible on a device from the first story on.

---

## Notes

- [P] tasks touch different files and have no unfinished dependency.
- Every task is one small Conventional Commit, made as the owner's commit rules and crew rules say.
- Anything in research.md marked "not checked" stays that way until the task that settles it has run.
- The samples of this feature use components with values, callbacks and plain slots. The owner's `PaymentSection` with a title slot and `CameraScanner` need the later remote composables feature.
- Task ids are never renumbered, since they are also issue titles. T105 moved from Phase 8 to Phase 3 in the design revision of 2026-11-15, because collecting parts from several modules is now part of the base design.

---

## Removed tasks

Removed in the design revision of 2026-11-15. Their ids are not reused.

- T013: tested the rules that adapted a placed component to an older catalog. Screens are now written per catalog version, so nothing is adapted.
- T021: implemented those rules in `core`. Removed for the same reason.
- T039: tested the set of catalogs the producer was given at start. The running producer holds no catalog now. Screen registration replaces it and is tested by T118.
