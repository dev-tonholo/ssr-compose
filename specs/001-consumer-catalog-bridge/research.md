# Research: Consumer Catalog and Bridge

Decisions for the plan, each with its reason and what was turned down. Facts are marked as checked or not checked, as the constitution's principle VII asks.

Two experiments were run on 2026-11-15 before any design, on Kotlin 2.4.21, Compose runtime 1.12.2, Gradle 9.8.2 and JDK 25 with a JVM 17 target. Their code is in `.superpowers/spikes/k2-bridge/` and `.superpowers/spikes/k2-catalog/`, which git ignores. They are throwaway and nothing depends on them.

The design was revised on 2026-11-15 by three owner decisions: the catalog is a bundle split per module, screens are written per catalog version, and the catalog is build time only. The experiments used a single catalog file. What they proved still holds for each part and for each bridge, and what the bundle adds is marked as not checked below.

## R1. The compiler creates each bridge from a held catalog

Decision: on the server side, the compiler plugin creates one bridge per held catalog, in the package the server developer names for that catalog. Each bridge is a set of top-level declarations: one `@Composable` function per component and one `enum class` per choice list. FIR declares them and IR gives them bodies. Each body is one call to a public function of the producer library, and passes the catalog version of its bridge as a constant. The compiler plugin reads the catalog through its index (R16), never the catalog's JSON.

Checked by experiment, from a single catalog file:

- FIR declares the enum and the composable functions from a file, and IR fills the bodies.
- The Compose compiler treats the created functions like hand-written ones. Their compiled signatures match a hand-written twin, and they compose in a real composition on a plain JVM with slot nesting and a working callback.
- Code in the same module calls them with a normal import and no extra step. Another module sees them through the first module's metadata.
- A wrong name, a missing parameter, and a `String` given where a choice is expected each fail to compile with the ordinary Kotlin error at the caller's line.
- Editing one source file recompiles only that file and causes no clash with the created declarations.

Not checked: two bridges with the same function and enum names in two packages of one module (T025 settles it); creating declarations from the index instead of a catalog file, and creating a declaration only when code names it (T031a settles both).

Alternatives considered: a source generator that writes Kotlin files. Turned down by the owner: the compiler creates the definitions.

## R2. The order of the two compiler plugins is a correctness matter

Decision: the bridge plugin's IR step must run before the Compose compiler's. The build-tool plugins pass `-Xcompiler-plugin-order=<bridge>><compose>` themselves, and the bridge plugin keeps a guard that stops the build with a clear message if it runs second.

Checked by experiment:

- With the right order it works. With the wrong order and no guard, the build passes and the program fails at run time with `NoClassDefFoundError: androidx/compose/runtime/internal/ComposableFunction0`. That is a silent miscompile.
- The order flag controls this in Gradle, both ways.
- The guard turns the wrong order into a compile error.
- The compile-testing library hides every plugin behind one ID, so the flag cannot be tested there. Only list order counts in those tests.

Not checked: that a Gradle compiler plugin integration can add the flag cleanly, that the Android Gradle Plugin keeps it, and how the Kotlin toolchain orders plugins.

## R3. A real enum for a choice list needs a workaround

Decision: a choice list becomes a real `enum class`, as the owner's example shows. The typed FIR API cannot create enum entries, so the plugin adds them through compiler internals. The result has the same bytecode shape as a hand-written enum, and `entries`, `valueOf` and an exhaustive `when` all work.

Checked: all of the above, in the command line compiler and Gradle.

Risk: the workaround uses an unchecked cast and writes into the class's declaration list. It is the part most likely to break in the IDE or in a new Kotlin version. Not checked: any IDE behaviour.

Fallback if it breaks: a class with one constant per choice, created with the typed API. It loses the exhaustive `when` on the server, which a server screen rarely needs, since it gives choices and does not receive them.

## R4. Fragments per source file, one part per module, one manifest per catalog

Decision: on the app side, the compiler plugin writes one small fragment file per source file that has marked composables. A cacheable step in each module, `ssrCatalogPart`, merges that module's fragments into the module's part. A step in the module that sets the catalog version, `ssrCatalogBundle`, collects the parts of the modules it depends on, checks across parts that every component name is unique and that a choice list used by two parts is the same in both, and writes the manifest with each part's fingerprint. Duplicate names inside a module are caught when the part is made, and across modules when the bundle is made, naming both places.

A part's name comes from the module's path (`:feature:home` gives `feature-home`, and the root module gives `root`; the full rule is in [contracts/catalog-bundle.md](contracts/catalog-bundle.md)) and can be set by the developer. Two modules whose parts would get the same name fail the bundle step.

Checked by experiment, for one module:

- A FIR checker finds `@ConsumerComposable` functions and reads every parameter kind the spec lists, including slots with arguments and callbacks with data. With the Compose compiler present, FIR shows a slot as a composable function type, and a plain lambda as a function type.
- An unsupported parameter fails the build at the parameter's line and column, with the composable, the parameter and the reason. A marked function that is not `@Composable` also fails.
- A modifier parameter is left out.
- Under incremental compilation the plugin sees only the recompiled files, and one build can run several rounds. A catalog written directly by the plugin loses components.
- With fragments plus a merge, the merged result after incremental builds is byte for byte the same as after a clean build. Removing the annotation deletes the fragment. A renamed or deleted file leaves an old fragment that the merge ignores because its source is gone.
- Two clean builds give identical bytes.
- The compiler catches a duplicate name only on a full build. The merge catches it always and names both.

Not checked: a Kotlin Multiplatform module with an Android target (the plugin runs once per compilation, so fragments need a folder per compilation, and a marked `expect` and `actual` pair would look like a duplicate); one module collecting the parts of the modules it depends on (T105 settles it); the build cache and the configuration cache for the part and bundle steps (T114 settles them); and the state of fragments after a failed incremental build.

Alternatives considered: the compiler writes the whole catalog. Turned down by the experiment. One catalog file for the whole app, merged in one step: turned down by the owner, because one change in any module would produce the whole file again and no module's result could be cached on its own.

## R5. Build-tool plugins on top of the compiler plugin

Decision: the compiler plugin cannot do everything, so two thin build-tool plugins wrap it, one for Gradle and one for the Kotlin toolchain. Their shared logic lives in one plain library, `tooling`, so neither build tool gets a feature the other lacks. They do these things:

- Apply the compiler plugin with its options and the plugin order.
- Make each module's part, collect the parts into a bundle, and check the bundle against the saved one of the same version.
- Fetch, validate, save and pin catalogs for a server project.
- Turn each saved catalog into an index, and declare each index as a compile input. Checked, for a single catalog file: without a declared input, editing the catalog leaves the build stale.
- Declare every input and output with relative paths and no timestamps, so the build cache works across machines.

How the build is set up, as the owner asked on the root build pull request and as the owner's thunderbird repository does for its baseline profile plugins and its pii-safe compiler plugin:

- Convention plugins in `build-logic` are Plugin classes registered in the `gradlePlugin` block, with ids `dev.tonholo.ssrcompose.conventions.<concern>`. Each id is listed in the version catalog with no version and applied with `alias(libs.plugins...)`. No precompiled script plugin, and no plugin id written as a string in a module build file.
- The compiler plugin id lives in one place, the `ssrcompose.compiler.plugin.id` property of the root `gradle.properties`, set to `dev.tonholo.ssrcompose`. The buildconfig Gradle plugin generates one constant from it, in `catalog-model`, next to the option names, as pii-safe generates its constant from `tfa.piisafe.compiler.plugin.id`. `compiler-plugin`, `gradle-plugin` and `toolchain-plugin` all read that one constant. It is not a constant in `core`, and no literal compiler plugin id appears in the toolchain plugin's yaml files or anywhere else. pii-safe generates its constant in the compiler plugin and reads the property again in its Gradle plugin; one constant in `catalog-model` keeps one home for the id even for the toolchain plugin, which is not built by Gradle and cannot run buildconfig itself.
- The option names are written once, in `catalog-model`, which `compiler-plugin` and both build-tool plugins can see without `core`. T113 creates them and the generated id. pii-safe keeps them in its annotations module; here the annotations are in `core`, which must not hold them, so `catalog-model` takes that place.
- The Gradle plugin is one `KotlinCompilerPluginSupportPlugin` class. In `applyToCompilation` it adds `core` (the annotations) to the compile classpath of each compilation, at the plugin's own version, as pii-safe adds its annotations module, so a developer applies one plugin and nothing else.
- Compile tests use kotlin-compile-testing with sources in `*.fixture.kt` files under `src/test/resources/fixtures`. The folder is a test source folder so the IDE reads the fixtures, and the fixtures are excluded from test compilation.
- The samples apply the Gradle plugin with `alias(libs.plugins.ssrcompose)`, from T056 and T057 on; T007 creates the sample builds before the plugin exists.

Rationale: R2 and R4 show the compiler alone is not enough, and the constitution says nothing may work only through Gradle. The owner's ktgd project already has this shape: a compiler plugin, a Gradle plugin and a toolchain plugin with its tasks in a `plugin.yaml`.

This adds two artifacts and two libraries (`tooling`, and `catalog-model` from R17) to the constitution's four deliverables. It is recorded in the plan's Complexity Tracking.

Not checked: the toolchain plugin itself, how the Kotlin toolchain builds an Android library, and how the toolchain names its modules. T030 records that naming here and maps it to the part name rule of [contracts/catalog-bundle.md](contracts/catalog-bundle.md), so both build tools give the same part names.

## R6. Fetching at sync

Decision: an `ssrCatalogSync` step works on each catalog location, which is the address of a manifest (a web address or a file path):

1. Fetch the manifest, refuse it before reading if it is over the size cap, and validate it. Every part's `file` must be its `name` followed by `.part.json`, the `file` values must be unique, and none may be empty, a dot or `manifest.json`. That keeps every file a plain file name, with no path separator, no `..` and no scheme.
2. Fetch only the parts whose fingerprint is not already saved for that version, each from the manifest's location with its `file` name, each refused before reading if it is over the size cap.
3. Check each part against its fingerprint in the manifest, validate it, and check across parts.
4. Save the manifest and the parts in `ssr/catalogs/<version>/`, and write the location and the manifest's fingerprint to the lock file.

If any check fails, or the manifest loads but one part cannot be fetched, nothing of that catalog is saved and the saved copy, if any, is kept. Since a catalog is saved whole and a changed manifest waits for `--update`, fetching only the missing parts happens on `--update` and after a saved part file was removed by hand. Builds read only the saved copies. When the manifest at a location no longer matches the lock file, the step reports it and changes nothing unless asked.

The step is wired so the IDE's sync triggers it, and it can also be run on its own. A build never fetches.

Not checked: how to make a Gradle task run on IDE sync. A search found only forum answers, and some say sync does not run tasks. A task settles it. If sync cannot trigger it reliably, the step stays a command the developer runs, and a build fails with a clear message when a saved catalog is missing.

Safety: only `https` addresses and file paths are accepted by default. Part file names are checked before any part is fetched. Names in a catalog are validated before anything is created from them. The size cap is 16 MiB per file by default, and configurable.

## R7. Consumer side: one renderer per marked composable

Decision: on the app side, the compiler plugin creates, for each marked composable, a small renderer that takes a placed component and calls the real composable with decoded values, callbacks that send a trigger, and slots that render their content. Because it is created per composable, incremental compilation is safe. The part step writes each module's entries of name to renderer, and the bundle step writes the app's renderer index as a resource, which the consumer library loads once. The renderer index is not the catalog: it maps names to the app's own code and holds no descriptors. The renderer outputs are not part of the catalog, and they follow the same per-module rule as the parts: a change in one module produces again only that module's renderer entries, and the app's renderer index is written again with the manifest. Where they are written is settled by T055 and not checked yet.

A developer can do the same by hand with `ComponentBindings`.

Not checked: all of it. The experiments covered reading composables (R4) and creating declarations (R1), not this combination. A task builds the smallest version first. If creating the renderer in the compiler proves fragile, the hand-written bindings still work, and the renderer becomes follow-up work.

## R8. Callbacks on the server follow the function type pattern

Decision: a callback parameter of a bridge function has one of the types `RemoteAction0`, `RemoteAction1<A>`, `RemoteAction2<A, B>`, `RemoteAction3<A, B, C>` or `RemoteAction4<A, B, C, D>`, in the producer library. The number matches the arguments of the app's callback, as Kotlin's own `Function0`, `Function1` and so on do. Each is a function interface, so a server developer passes a lambda.

This is the owner's proposal. Two points were agreed with it:

- There is no result type. The app's callback is an ordinary function that returns at once, so it cannot wait for the server. A result from the server, if ever needed, is a different thing and gets its own type then.
- The pattern stops at four arguments. A marked composable with a longer callback is a build error in the app. Raising the limit later breaks nobody.

In the catalog a callback lists its `arguments`, the same field a slot uses.

The bridge also gives the producer, for each callback, a `Callback` with two things: the kinds of its arguments as core's `ValueKind`, the same type the catalog uses, and a conversion from the received values to the lambda's typed arguments (a choice entry to its enum, a whole number to `Int` or `Long`, a decimal to `Float` or `Double`, a list to a typed list). The producer checks the number and kinds first, then runs the conversion. A failure in either step, such as an unknown choice entry or a number that does not fit, is a refused trigger reported through `onTriggerRefused`, never a failed action. The running producer holds no catalog, so this declaration is the only thing it checks against. Only the bridge knows a choice list's entries, since the enum is generated from the catalog, so the entry check belongs in the conversion.

Not checked: creating a parameter of these types in FIR (the experiment used plain function types, which worked), and how the Compose compiler treats a lambda converted to a function interface compared with a plain function type. The early experiment that covers created types settles both.

## R8a. Optional and nullable parameters

Decision: the catalog says if a parameter can be left out, never what its default is, because only the app knows. In the bridge, a parameter that can be left out is nullable with `null` as its default, and `null` means "not given": nothing is sent and the app's own default applies. A value or choice parameter whose type is nullable in the app is marked `nullable` in the catalog and stays nullable in the bridge.

Not checked: defaults in created functions (the bridge experiment created none), and nullable types in the catalog (the catalog experiment rejected them, by its own choice).

## R8b. Slots that pass arguments

Decision: such a slot is described in the catalog and cannot be filled until remote composables exist. An optional one gets no bridge parameter. A component with a required one cannot be placed: its bridge function is created so the name resolves, and any call to it is a compile error that names the later feature.

Consequence for the first samples: the owner's `PaymentSection` and `CameraScanner` examples both have a required slot with arguments, so the samples of this feature use components with values, callbacks and slots without arguments.

Not checked: how that compile error is produced.

## R8c. Where a part comes from in a project with variants

Decision: a part belongs to one compilation. Each Android variant has a catalog version, the project's by default, and the check compares the variant's bundle with the saved bundle of its version. A flavor with different marked composables sets its own version. The compiler plugin writes fragments in a folder per compilation. A marked `expect` and `actual` pair is one component.

Not checked: all of it, since the catalog experiment used a plain JVM module.

## R9. One bridge per held catalog

Decision: the server project holds one saved bundle per catalog version, in `ssr/catalogs/<version>/`. Every held catalog gets its own bridge, in a package the server developer names for it in the build setup. There is no single catalog that the only bridge is created from. A composable removed in a newer catalog still exists in the bridges of the older catalogs that have it.

Rationale: versions have no order, so "newest" cannot be worked out, and the owner chose screens per catalog version (R10), which needs every version's composables to be callable. Two bridges with the same names cannot share a package, so each gets its own. The bridges are created in one module of the server, and other modules depend on it.

Not checked: two packages with the same names in one module (T025), the compile time and memory of 10 bridges of up to 20,000 components each (T031a), and what happens when two modules create the same package.

Alternatives considered: one bridge from one chosen catalog, with screens adapted automatically to older catalogs. Turned down by the owner on 2026-11-15, in favour of versioned screens.

## R10. Screens per catalog version, and the guard

Decision: a server developer registers screens per catalog version:

```kotlin
val producer = Producer {
    catalog(version = "1") { screen(name = "home") { HomeScreenV1() } }
    catalog(version = "2") { screen(name = "home") { HomeScreenV2() } }
}
```

The producer serves a catalog version while screens are registered for it. An app of any other catalog version is refused at the introduction with the kind "catalog version not known or no longer supported". An app that asks for a name with no screen for its version is told the screen cannot be shown, and its session stays accepted. A server developer stops serving a version by removing its registration. `Producer` fails at once when a version or a screen name in one version is registered twice.

The guard keeps the promise that an app never receives a component its catalog does not have. Every placed component carries the catalog version its bridge was created with, as a constant in the bridge function's body. Before a screen is sent, the producer compares that version with the session's. On a mismatch it does not send the screen, tells the app the screen cannot be shown, and calls the observer with the composable, the screen and both versions. The version stays inside the producer and is not part of the message on the wire.

Consequences:

- Screens for two versions repeat their layout, because the two bridges are different functions. State and logic can be shared as plain Kotlin. This is a known cost, stated in the spec and in the user documentation.
- `LocalConsumer.current` keeps `device`, `app` and `catalogVersion`. There is nothing to ask about a composable at run time, since a screen only uses the bridge of its own version.
- The running producer holds no catalog. Callback arguments are checked against the kinds in the bridge's `Callback` and turned into the lambda's types by its conversion (R8).

Not checked: the implementation.

Alternatives considered: one bridge, automatic adaptation of a screen to an older catalog, replacements for missing composables and a run-time `has` check. Turned down by the owner on 2026-11-15: "the server would choose when to stop serving each version".

## R10a. A callback whose lambda fails

Decision: the producer runs a callback's lambda so that what it writes to the screen's state is kept only when it finishes. If it throws, those writes are thrown away, the screen is not recomposed, the session carries on, and the observer is told. The Compose runtime's snapshots allow this: the lambda runs in a snapshot that is applied on success and dropped on failure.

Not checked: that a dropped snapshot leaves the live composition untouched in every case, for example when the lambda also changed something outside snapshot state. A test in the callbacks story settles the first part. The second is a documented limit: only snapshot state is protected.

## R10b. Callbacks that arrive too often

Decision: a limit per session, 100 per second by default and configurable through the producer's limits. Callbacks over the limit are dropped and reported, and the session carries on.

## R11. Modules and targets

| Module | Kind | Targets | Depends on |
|---|---|---|---|
| `core` | Kotlin Multiplatform library | `jvm`, `android` | nothing in this repo |
| `catalog-model` | Kotlin JVM library, build time only | JVM | `core` (`CatalogVersion` and `ValueKind` only) |
| `consumer` | Kotlin Multiplatform library | `android` | `core` |
| `producer` | Kotlin JVM library | JVM | `core` |
| `compiler-plugin` | Kotlin JVM | JVM | `catalog-model` |
| `tooling` | Kotlin JVM library | JVM | `catalog-model` |
| `gradle-plugin` | Gradle plugin | JVM | `tooling`, `catalog-model` (plugin id and option names) |
| `toolchain-plugin` | Kotlin toolchain plugin | JVM | `tooling`, `catalog-model` (plugin id and option names) |
| `contract-tests` | test-only | `android` host tests | `core`, `consumer`, `producer` |
| `samples/app` | its own build | Android | `consumer` and the Gradle plugin, from the root build included as a build |
| `samples/server` | its own build | JVM | `producer` and the Gradle plugin, from the root build included as a build |

The two samples are separate builds on purpose. The spec's success criterion says the sample server must build with none of the sample app's code available, sharing only the catalog.

The owner confirmed earlier that `core` has a JVM target and an Android target, and `minSdk` 23.

Checked by building (T005, 2026-11-15, Gradle 9.8.2, JDK 25 with a Java 17 target): the module shape above builds as planned. `./gradlew clean build --no-build-cache --rerun-tasks` and `./gradlew build` both exit 0 for `build-logic` and all seven modules. What the build showed:

- `core` is a Kotlin Multiplatform library with `jvm` and `android` targets, using the `com.android.kotlin.multiplatform.library` plugin. It compiles for both.
- `consumer` has only the `android` target, and Compose (runtime, foundation and UI 1.12.2) compiles in `commonMain`. A trivial `@Composable` that calls `BasicText` is in it to prove this.
- `producer`, `compiler-plugin`, `tooling` and `gradle-plugin` are plain Kotlin JVM modules. `compiler-plugin` has `kotlin-compiler-embeddable` as `compileOnly`. `gradle-plugin` passes `validatePlugins`.
- `contract-tests` is an Android-only Kotlin Multiplatform module with no main code. Its host tests live in the `androidHostTest` source set and run with `./gradlew :contract-tests:testAndroidHostTest`, which `check` and `build` also run. It depends on `consumer` (Android library) and `producer` (plain JVM library) without trouble. The test result file shows `tests="1" skipped="0" failures="0" errors="0"`.
- In AGP 9.5.1, the Android block inside `kotlin { }` is `android { }`. The older `androidLibrary { }` block still works but is deprecated. `withHostTest { }` inside it is what creates the `androidHostTest` source set. It is enabled in the `kmp-library` convention, so `consumer` and `core` also get host test tasks. Without it, Kotlin warns that `commonTest` is not connected to any compilation in `consumer`.
- The Java 17 target holds on this JDK 25 machine. `file` on `ProducerModule.class`, `ConsumerModule.class` (Android output), and `CoreModule.class` (both the `jvm` and `android` outputs) printed "version 61.0" each.
- A throwaway test in `tooling/src/test` and one in `core/src/jvmTest`, deleted afterwards, each ran under `./gradlew :tooling:test :core:jvmTest` with `tests="1" failures="0"`. So the JUnit Platform branch and the Kotlin Multiplatform `jvm` target of the `testing` convention both run tests.
- Kotlin `internal` is not visible across modules, so T003's "one trivial internal class" and T004's "a class from `consumer` and a class from `producer`" cannot both hold. `ConsumerModule` and `ProducerModule` are public. The other four trivial classes are internal.
- `CompilerPluginRegistrar` and `CompilerConfiguration.Internals` need an opt-in in `kotlin-compiler-embeddable` 2.4.21, so the trivial class in `compiler-plugin` uses `FqName` instead.
- `detekt` is in the `check` chain with its default rules and no config file (T006 adds that). It runs and passes on the four JVM modules. It reports `NO-SOURCE` for `core`, `consumer` and `contract-tests`, although `core` and `consumer` have Kotlin files in `commonMain`. The cause was not looked into. T006 has to make detekt read the Kotlin Multiplatform source folders.
- Type-safe project accessors (`projects.core`) print "Type-safe project accessors is an incubating feature." The build log also shows one Gradle deprecation, `Configuration.setVisible(boolean)`, while Gradle configures `:compiler-plugin`. What triggers it was not traced.

## R12. Versions

Checked against Maven metadata on 2026-11-15: Kotlin 2.4.21 (2.5.0 is a release candidate), Android Gradle Plugin 9.5.1, Compose Multiplatform 1.12.2, kotlinx.serialization 1.13.0, kotlinx.coroutines 1.11.0. Checked by experiment: `dev.zacsweers.kctfork:core:0.14.0` works with Kotlin 2.4.21 and the Compose compiler. The Kotlin toolchain on the owner's machine is 0.13.0.

Checked again for T001 on 2026-11-15, by downloading each `maven-metadata.xml` and searching it for the exact version. The commands were `curl -s https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-compiler-embeddable/maven-metadata.xml` and the same for `org/jetbrains/compose/compose-gradle-plugin`, `dev/zacsweers/kctfork/core`, `org/jetbrains/kotlinx/kotlinx-serialization-json`, `org/jetbrains/kotlinx/kotlinx-coroutines-core`, `app/cash/turbine/turbine`, `io/ktor/ktor-server-core` and `dev/detekt/detekt-gradle-plugin`. The Android Gradle Plugin came from `curl -s https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml`, followed by `grep -c "<version>9.5.1</version>"`, which printed 1. Every pinned version is in its metadata file: Kotlin 2.4.21 (`kotlin-compiler-embeddable`, latest tag is 2.5.0-RC), Android Gradle Plugin 9.5.1, `compose-gradle-plugin` 1.12.2, `kotlinx-serialization-json` 1.13.0, `kotlinx-coroutines-core` 1.11.0, `dev.zacsweers.kctfork:core` 0.14.0, Turbine 1.2.1 (latest), Ktor 3.6.1 (latest), detekt Gradle plugin 2.0.0-alpha.6 (latest). No version had to change. The buildconfig plugin `com.github.gmazzo.buildconfig` was checked with `curl -s https://plugins.gradle.org/m2/com/github/gmazzo/buildconfig/com.github.gmazzo.buildconfig.gradle.plugin/maven-metadata.xml`: 6.1.2 is the latest release, and it is the version in the catalog.

The metadata check does not prove every artifact exists, so `./gradlew build` resolved what the modules use: Compose runtime, foundation and UI 1.12.2 (`consumer`), `kotlinx-serialization-json` 1.13.0 (`core`), `kotlin-compiler-embeddable` 2.4.21 (`compiler-plugin`), `kotlin-test`, `kotlin-test-junit`, `kotlinx-coroutines-test` and Turbine (the host test classpath of `contract-tests`), and the detekt plugin. The kctfork and Ktor entries are in the catalog but no module uses them yet, so for those only the metadata check was done.

All versions, including the JVM target 17, `minSdk` 23 and `compileSdk` 37, are in `gradle/libs.versions.toml`. The convention plugins are Plugin classes that read them through the generated catalog accessors, and the modules apply them with `alias(libs.plugins.ssrcompose.conventions.<concern>)`, so a `grep -rnE` for the six version numbers over `build-logic`, the seven modules, `gradle/`, and the root build files finds them only in `gradle/libs.versions.toml`. The Gradle wrapper version (9.8.2) is in `gradle/wrapper/gradle-wrapper.properties`, as Gradle requires.

Compiler plugin APIs for top-level generation are marked experimental in Kotlin, so each new Kotlin version needs the plugin's tests to pass before it is supported.

Checked: the version of the buildconfig Gradle plugin (`com.github.gmazzo.buildconfig`) is 6.1.2, the latest release on the Gradle plugin portal and the version the owner's thunderbird repository uses. T001 added it to the catalog.

## R13. The editor

Developers must turn off `kotlin.k2.only.bundled.compiler.plugins.enabled` in the IDE registry to see created declarations, as the owner said. It is a documented setup step.

Not checked: anything in the IDE. Known risks from reading the code: the enum workaround (R3), the index being read once per plugin load so a catalog change is not seen until reload, and created declarations having no source to navigate to.

## R14. What carries over from the earlier draft

Still valid, and designed the same way: the handshake values and their validation limits, a neutral data model so no wire encoding is baked into the contract, a replaceable codec and connection with no transport in the libraries, one live composition per session with one snapshot observer shared by the process, an observer for reporting, and a load test for the bounds.

Validation limits, all configurable: introduction frame at most 8 KB; density above 0 and at most 10; screen width and height 1 to 100,000 pixels; language tag at most 35 characters; app version, variant, flavor and catalog version at most 64 characters each with no control characters; callback arguments at most 1 megabyte in total; callbacks at most 100 per second per session. On the build side: a manifest and each part at most 16 MiB, checked before reading; part names at most 64 characters, lowercase letters, digits and hyphens, and never a Windows device name.

## R15. Decisions that become ADRs

- The compiler creates one bridge per held catalog, with the plugin order rule (R1, R2, R9).
- The catalog is a bundle: fragments per source file, a part per module and a manifest (R4).
- Build-tool plugins over one shared tooling library (R5).
- Catalogs are fetched at sync and pinned (R6).
- Module layout and targets, with the build-time `catalog-model` library (R11, R17).
- Screens per catalog version, and the guard (R10).

## R16. Scale and build performance

Decision: there is no hard limit on components or parameters. The library is tested at 20,000 components with about 80,000 parameters, with the mix real apps have (most components with 2 to 4 parameters, a few with 15 or more). The owner measured three real apps with 621, 751 and 1,085 composables, the last one an MVP. The reader is protected by a size cap on the manifest and on each part, 16 MiB each by default, checked before parsing.

The compiler never parses the catalog's JSON. A cacheable build step, `ssrCatalogIndex`, turns each saved catalog into an index that the compiler plugin reads. The index is internal and is never shared between builds or committed. Its format is settled by T031a and is not fixed here.

Build performance targets, with the 20,000 component catalog:

- A build with nothing changed does no catalog work.
- A change in one app module produces again only that module's part and the manifest.
- Build outputs hold no absolute path and no timestamp, so the build cache works across machines.
- The catalog adds at most 2 seconds to a clean compile of a server module that calls 20 of its composables. This figure is provisional.

Not checked: all of it. T031a, the scale gate, measures reading and validating the bundle, building the index, compiling a server module against one and against several held catalogs of that size, whether a bridge declaration can be created only when code names it, and what is recompiled or taken from the build cache when one component in one part changes. It writes the numbers, the machine and the commands here. T115 measures the same targets again on the finished build.

## R17. The catalog is build time only

Decision: the owner said "We don't need the catalog at the runtime, as we will use it only during the build time to generate the types." So the catalog model, its validation, the manifest and part reader and writer, the fingerprint and the index live in a JVM library, `catalog-model`, that `tooling` and `compiler-plugin` depend on. `gradle-plugin` and `toolchain-plugin` depend on it too, only for the compiler plugin's id and option names. `core`, `consumer` and `producer` never depend on it. `core` keeps `CatalogVersion` (a label), `ValueKind` (the kinds of values), the two annotations, the handshake, the messages, the connection and `DataValue`. `catalog-model` depends on `core` for `CatalogVersion` and `ValueKind` only, so each has one definition. `ValueKind` is in `core` because the producer needs it at run time to declare and check callback arguments, and `catalog-model` needs it for parameters; one list of kinds serves both, and neither module defines its own.

Rationale: principle II asks for one definition of everything both sides agree on. The catalog format still has one definition, in `catalog-model`. Keeping it in `core` would put a JSON reader, hashing and validation in every app and every server at run time, where nothing uses them. The plan records this in Complexity Tracking.

Consequences: the running producer cannot look anything up in a catalog. It checks the session's catalog version against the versions it has screens for (R10), and callback arguments against the kinds and the conversion in the bridge's `Callback` (R8).

Catalog code written in `core` before this decision is moved into `catalog-model` by T113, except `ValueKind`, which stays in `core` and moves from `core/catalog/` to `core/data/`.
