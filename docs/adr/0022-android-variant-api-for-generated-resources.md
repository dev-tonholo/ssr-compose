# 0022. Add generated resources to Android variants through the Android Gradle plugin's variant API

- Status: Proposed
- Date: 2026-11-21
- Modules: `gradle-plugin`, `core`, `consumer`

## Context

The app's font version must reach the consumer at run time, in a small resource the build generates for each Android variant (T128 part A, [contracts/font-bundle.md](../../specs/001-consumer-catalog-bridge/contracts/font-bundle.md)). T047 needs the same for the catalog version, and the renderer index is written as a resource too ([contracts/build-tooling.md](../../specs/001-consumer-catalog-bridge/contracts/build-tooling.md)). A catalog version can differ per variant ([research.md R8c](../../specs/001-consumer-catalog-bridge/research.md#r8c-where-a-part-comes-from-in-a-project-with-variants)).

`gradle-plugin` has no Android API on its classpath today. `build-logic` already compiles against the Android Gradle plugin (`implementation(plugin(libs.plugins.android.kmp.library))`), but that is the repo's own build, not what apps apply.

A probe on 2026-11-21 settled the facts (research.md R20, probe P20). It used a throwaway build with AGP 9.5.1 and Kotlin 2.4.21, and an included-build plugin compiled against `com.android.tools.build:gradle-api:9.5.1` with `compileOnly`:

- On `com.android.application` and `com.android.library`, `variant.sources.res`, `resources` and `assets` are all present. On `com.android.kotlin.multiplatform.library` (variant `androidMain`) only `resources` is present; `res` and `assets` are null.
- A task output added with `sources.resources?.addGeneratedSourceDirectory(task, Task::outputDir)` reached the APK at its path, an app host test read it from the class loader (one copy), and the multiplatform library put it in its AAR's `classes.jar`.
- When a library and the app both wrote the same path, AGP 9.5.1 kept the app's file and warned: "Future versions of the Android Gradle Plugin will throw an error in this case."
- In a build with no AGP, a plain JVM module applied the plugin with no error, since the Android code ran only from `pluginManager.withPlugin` callbacks.
- `KotlinMultiplatformAndroidComponentsExtension` extends `AndroidComponentsExtension`, as do the application and library extensions, in `gradle-api` 9.5.1.

## Options

1. `compileOnly` on `com.android.tools.build:gradle-api` at the catalog's `agp` version, reacting to each Android plugin id with `pluginManager.withPlugin` and adding a generated folder to `variant.sources.resources` through `onVariants`. The public, typed variant API that AGP documents for generated sources: per variant, with the task dependency wired by AGP, configuration cache safe. Costs: the plugin is compiled against one AGP API version, and its classes must be in the same class loader scope as AGP, so an app applies it in the same `plugins {}` block as AGP or declares both in the root build.
2. The Kotlin Gradle plugin only: `kotlinCompilation.defaultSourceSet.resources.srcDir(task)`. No Android API at all. Rejected: a Kotlin source set is not a variant, so a flavor with its own catalog version (R8c) cannot get its own file, and AGP 9's built-in Kotlin for `com.android.application` is not wired through Kotlin source sets the way the multiplatform plugin is (not probed).
3. Carry the version through the compiler plugin as a constant in generated code, since T055 already generates `ConsumerFonts`. No Android API, and a duplicate becomes a duplicate class error. Rejected: a compiler option change makes the module compile again from scratch, the consumer would need to find a generated class by name, and the renderer index is a resource in any case, so the Android wiring is needed anyway.
4. Reach AGP by reflection, with `extensions.findByName("androidComponents")` and no Android type at compile time. Rejected: no type safety, and every AGP change fails at run time in the app's build instead of at compile time here.
5. Write into a fixed source folder such as `src/main/resources`. Rejected: it writes into the app's sources, is not per variant, and is not a task output, so caching and up-to-date checks break.

## Decision

Option 1:

- `gradle-plugin` adds `compileOnly` on `com.android.tools.build:gradle-api` at the `agp` version (9.5.1), as a new catalog entry the worker adds with the add-dependency skill. It never becomes an `implementation` or `api` dependency, so an app gets the AGP it applies itself.
- The plugin reacts to `com.android.application`, `com.android.library` and `com.android.kotlin.multiplatform.library` with `pluginManager.withPlugin`, and gets the typed `ApplicationAndroidComponentsExtension`, `LibraryAndroidComponentsExtension` or `KotlinMultiplatformAndroidComponentsExtension`. The typed extensions avoid an unchecked cast. All AGP types sit in one internal file that only those callbacks reach, so a build without AGP never loads them.
- Each variant gets its generated folder through `variant.sources.resources?.addGeneratedSourceDirectory`. Java resources are the one kind every supported plugin has, and the consumer reads them from its class loader on the device and in host tests with no `Context`.
- Only the module that sets the version writes the resource (the one that sets `catalogVersion` or declares the fonts), so an app never holds two copies of one path.
- The resource path is defined once, in `core`, which `consumer` depends on and `gradle-plugin` reaches through `tooling` and `catalog-model`. The path stays out of `META-INF`, where AGP leaves out some files by default.

Supported app module types, in the order T128 builds them:

1. `com.android.application`, first.
2. `com.android.library`, the same code with its own typed extension. It also covers a Kotlin Multiplatform module with `androidTarget()` next to `com.android.library`, as R8c's fixture uses.
3. `com.android.kotlin.multiplatform.library`, with Java resources only. It has one variant, `androidMain`.
4. Plain JVM modules are servers: the consumer is Android only, so they get no consumer resource. Their font bundle goes into the JVM `main` resources through Gradle's source set API (`ssrFontResources`), with no AGP.

Not supported in this feature: `com.android.dynamic-feature` and `com.android.test` modules.

## Consequences

- Good: per-variant resources through AGP's public, documented, typed API, with task wiring, caching and the configuration cache handled by AGP.
- Good: no AGP class is loaded in a build without Android, and the plugin ships no AGP.
- Bad: the plugin is compiled against AGP 9.5.1's API. The lowest AGP version it supports is 9.5.1 until a functional test runs an older one.
- Bad: an app applies the plugin in the same class loader scope as AGP. A plugin applied from another scope, such as `buildSrc` compiled without AGP, was not probed.
- Follow-up: T128 part A adds the catalog entry and the font version resource, with one functional test per supported module type; T047 reuses the same wiring for the catalog version and the renderer index. Not checked here: reading the resource on a device (the APK held it), dynamic feature modules, and AGP versions below 9.5.1.
