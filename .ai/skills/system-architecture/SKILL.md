---
name: system-architecture
description: The module boundaries, shared contracts, invariants and platform limits of ssr-compose. Load it when you design a feature, validate a delivery against a design, or settle an architecture question.
---

# System architecture

Knowledge for the system-architect and anyone designing across modules in ssr-compose. Roles say how to work; this skill says what is true in this project.

## What this covers

- Deliverables and module dependencies.
- Rules that hold across the whole system.
- Decisions of the first feature that shape later designs.
- Limits of the JVM, the Kotlin compiler and the IDE that a design must respect.
- Where decisions and contracts are recorded.

## Facts

### Modules

- The root build includes `core`, `core-testing`, `catalog-model`, `consumer`, `producer`, `compiler-plugin`, `tooling`, `gradle-plugin` and `contract-tests` (`settings.gradle.kts`).
- `core` depends on no other module of this repo (`core/build.gradle.kts`).
- `core-testing`, `catalog-model`, `consumer` and `producer` each expose `core` as an `api` dependency (their `build.gradle.kts`).
- `compiler-plugin` depends on `catalog-model` (`compiler-plugin/build.gradle.kts`). `tooling` exposes `catalog-model` (`tooling/build.gradle.kts`). `gradle-plugin` depends on `tooling` and `catalog-model` (`gradle-plugin/build.gradle.kts`).
- `contract-tests` joins `core`, `consumer` and `producer` (`contract-tests/build.gradle.kts`).
- The project targets JVM 17 and uses Kotlin 2.4.21 and Compose Multiplatform 1.12.2 (`gradle/libs.versions.toml`).

### System rules

The rules that hold across the system are in [the architecture rules](../../rules/architecture.md), and the principles behind them are in [the constitution](../../../.specify/memory/constitution.md). Read rules 2 to 6, 8, 9 and 13 to 15 before you design across modules.

Rules 14 and 15 hold the principle I invariants.

### First feature decisions

- The server creates one bridge per held catalog (`specs/001-consumer-catalog-bridge/research.md`, R9).
- A catalog of 20,000 components may add at most 2 seconds to a clean compile of a server module that calls 20 of them (`specs/001-consumer-catalog-bridge/spec.md`, SC-016).
- Bridge functions are spread over 256 classes per catalog (`specs/001-consumer-catalog-bridge/research.md`, R16).
- A bridge has at most 118 parameters, a class at most 150 functions, a choice enum at most 2,000 entries and a bridge package at most 255 bytes (`compiler-plugin/src/main/kotlin/dev/tonholo/ssrcompose/compiler/producer/BridgeLimits.kt`, `specs/001-consumer-catalog-bridge/research.md` R18).
- Per-module choice enums live as top-level `internal enum class` declarations in `<bridgePackage>.<moduleSegment>` (`docs/adr/0010-per-module-choice-enums-in-bridge-subpackage.md`).
- The file formats and messages are defined in `specs/001-consumer-catalog-bridge/contracts/`.
- The producer composes every screen with Compose UI foundation in an `ImageComposeScene`, reads the result through the public semantics and inspector API, and sends a positioned document; the consumer arranges nothing (`specs/001-consumer-catalog-bridge/research.md` R20, `docs/adr/0015-producer-renders-with-compose-ui-foundation.md`).
- App components are measured on the device and reported with `ComponentMeasured` (`docs/adr/0016-app-components-measured-on-the-device.md`).
- Every scene of a producer process runs on one scene thread, `ssr-compose-scene`, with a watchdog that reports a screen holding it longer than `sceneTaskLimit`; a composition that blocks stops every screen of its process, and scale comes from more processes (`docs/adr/0021-producer-runs-every-scene-on-one-scene-thread.md`, proposed).
- Generated resources reach each Android variant as Java resources through the Android Gradle plugin's variant API, compiled against `gradle-api` with `compileOnly`, written only by the module that sets `catalogVersion` (`docs/adr/0022-android-variant-api-for-generated-resources.md`, proposed).
- The app's fonts travel in a font bundle with its own version, separate from the catalog, and the default family is Roboto carried on both sides, with a server text naming a family and no family refused (`specs/001-consumer-catalog-bridge/contracts/font-bundle.md`, `docs/adr/0017-app-fonts-travel-in-a-pinned-font-bundle.md`, `docs/adr/0020-bundle-roboto-as-the-default-font.md`).

### Platform limits

- Generated JVM code must stay within the class file limits of Java SE 17: 65,534 usable constant pool slots (`constant_pool_count` is at most 65,535 and counts one extra slot), 65,535 fields, 65,535 methods, 255 parameter units per method, 65,535 bytes per UTF-8 entry and less than 65,536 bytes of code per method ([JVMS 17, section 4.11](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.11)).
- An enum class's `<clinit>` grows by 16 bytes per entry above index 127 on Kotlin 2.4.21 (JVM 17), crossing our 32,767 byte safe ceiling at 2,056 entries and crashing code generation above 4,103 entries (`specs/001-consumer-catalog-bridge/research.md` R18).
- A FIR plugin lists generated top-level names through `getTopLevelCallableIds`, and the compiler asks for each declaration by its id ([FirDeclarationGenerationExtension.kt](https://github.com/JetBrains/kotlin/blob/v2.4.21/compiler/fir/providers/src/org/jetbrains/kotlin/fir/extensions/FirDeclarationGenerationExtension.kt)).
- `FrontendFilesForPluginsGenerationPipelinePhase` in Kotlin 2.4.21 puts every generated top-level `FirClassLikeDeclaration` (including a `FirTypeAlias`) in `<package>/<ClassName>.kt` and ignores `fileNameForPluginGeneratedCallable`, so a generated top-level `typealias` emits `<package>/<Name>Kt.class`.
- The Kotlin 2.4.21 Gradle plugin sets JVM `-module-name` to `"$group:$projectName"` (and appends `_test` for test compilations, `klibUtils.kt` and `kotlinCompilations.kt`).
- Compose UI desktop 1.12.2 lays out on a plain JVM in `ImageComposeScene`; its semantics tree leaves out `Column`, `Row`, `Spacer` and padding-only boxes, and a `LazyColumn` composes only its visible items (`specs/001-consumer-catalog-bridge/research.md` R20, probes P4 to P6).
- Compose UI 1.12.2 keeps process-wide state with no lock: the node kind cache `classToKindSetMap` and the layer scope, matrix and positional properties shared by every layer update. Scenes built on several threads at once corrupt it or hang, and `ImageComposeScene` composes and applies inside `render()` with a recomposer of its own, so composition cannot be split from node work with public API (R20, probes P13 to P15).
- On AGP 9.5.1, a `com.android.kotlin.multiplatform.library` variant has only `sources.resources`; `res` and `assets` are null. Two modules that write the same Java resource path get a warning that future AGP versions turn into an error (R20, probe P20).
- Compose desktop's `Font(identity, data)` loads bytes that are not a font with no error and falls back to the default font (R20, probe P8).
- IntelliJ keeps a module's registered compiler plugin extensions until its Kotlin facet changes, so work done while a plugin registers runs again only after a Gradle sync ([KtCompilerPluginsCache.kt](https://github.com/JetBrains/intellij-community/blob/master/plugins/kotlin/fir/src/org/jetbrains/kotlin/idea/fir/extensions/KtCompilerPluginsCache.kt), [KtCompilerPluginsProviderIdeImpl.kt](https://github.com/JetBrains/intellij-community/blob/master/plugins/kotlin/fir/src/org/jetbrains/kotlin/idea/fir/extensions/KtCompilerPluginsProviderIdeImpl.kt)).
- IntelliJ finds top-level functions for completion from indexes of source files and from resolve extensions, so declarations from a compiler plugin are not offered for auto-import ([KtSymbolFromIndexProvider.kt](https://github.com/JetBrains/intellij-community/blob/master/plugins/kotlin/base/analysis-api/analysis-api-utils/src/org/jetbrains/kotlin/idea/base/analysis/api/utils/KtSymbolFromIndexProvider.kt), [KTIJ-27574](https://youtrack.jetbrains.com/issue/KTIJ-27574), [KTIJ-29912](https://youtrack.jetbrains.com/issue/KTIJ-29912)).

### Where decisions live

- Architecture decisions are in `docs/adr/`, one file each, listed in `docs/adr/README.md`.
- Feature research and rejected alternatives are in each feature's `research.md`.
- Public contracts are in each feature's `contracts/` folder.

## Sources

- `.specify/memory/constitution.md`
- `.ai/rules/architecture.md`
- `.ai/blueprints/adr.md`
- `settings.gradle.kts`, `gradle/libs.versions.toml` and each module's `build.gradle.kts`
- `specs/001-consumer-catalog-bridge/`
- [The Java Virtual Machine Specification, Java SE 17, chapter 4](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html)
- [Kotlin compiler source, tag v2.4.21](https://github.com/JetBrains/kotlin/tree/v2.4.21)
- [IntelliJ Kotlin plugin source](https://github.com/JetBrains/intellij-community/tree/master/plugins/kotlin)

## Open questions

- The module map in `.ai/guidelines.md` does not list `catalog-model` and says `compiler-plugin` may depend on `:core`, while the build file depends on `catalog-model`. Which one is the intended boundary?
- Which guardrails should become automated checks for module boundaries?
- How does an app component keep its state when it moves inside server-rendered documents and slots (#183)? The document's node ids are the producer's semantics ids, stable only while the producer's node lives.
- How far does running more producer processes scale on an idle machine? R20's probes P18 and P19 ran on a loaded one; T129 measures it.
- Which nodes Compose UI shows in the semantics tree is observed, not documented (research.md R20). Which upgrade check pins it best beyond `SemanticsVisibilityTest`?
