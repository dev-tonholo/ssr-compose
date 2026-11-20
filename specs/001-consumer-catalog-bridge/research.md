# Research: Consumer Catalog and Bridge

Decisions for the plan, each with its reason and what was turned down. Facts are marked as checked or not checked, as the constitution's principle VII asks.

Two experiments were run on 2026-11-15 before any design, on Kotlin 2.4.21, Compose runtime 1.12.2, Gradle 9.8.2 and JDK 25 with a JVM 17 target. Their code is in `.superpowers/spikes/k2-bridge/` and `.superpowers/spikes/k2-catalog/`, which git ignores. They are throwaway and nothing depends on them.

A third experiment, T025, ran on 2026-11-16 inside the real modules. Its results are in R1, R2, R8, R8a and R9. A fourth, T026, ran the same day in the same modules. Its result is in R8b. A fifth, T029, ran on 2026-11-17 in the same modules. Its result is in R7.

The design was revised on 2026-11-15 by three owner decisions: the catalog is a bundle split per module, screens are written per catalog version, and the catalog is build time only. The experiments used a single catalog file. What they proved still holds for each part and for each bridge, and what the bundle adds is marked as not checked below.

## R1. The compiler creates each bridge from a held catalog

Decision: on the server side, the compiler plugin creates one bridge per held catalog, in the package the server developer names for that catalog. Each bridge is a set of top-level declarations: one `@Composable` function for each component the module names (R16) and one `enum class` per choice list. FIR declares them and IR gives them bodies. Each body is one call to a public function of the producer library, and passes the catalog version of its bridge as a constant. The compiler plugin reads the catalog through its index (R16), never the catalog's JSON.

Checked by experiment, from a single catalog file:

- FIR declares the enum and the composable functions from a file, and IR fills the bodies.
- The Compose compiler treats the created functions like hand-written ones. Their compiled signatures match a hand-written twin, and they compose in a real composition on a plain JVM with slot nesting and a working callback.
- Code in the same module calls them with a normal import and no extra step. Another module sees them through the first module's metadata.
- A wrong name, a missing parameter, and a `String` given where a choice is expected each fail to compile with the ordinary Kotlin error at the caller's line.
- Editing one source file recompiles only that file and causes no clash with the created declarations.

Checked by T025 on 2026-11-16, inside the real modules, on Kotlin 2.4.21 and Compose runtime 1.12.2, from a one-component index in a text format of its own. The second pull request of T031a replaced that format with the binary index of R16:

- The plugin creates the function from the index and gives it a body that is one call to `ConsumerComponent` with the catalog version as a constant. The bytecode of the created function holds the constant `"1"`, the component name and the parameter names, and the Compose compiler gave it the signature `Greeting(String, RemoteAction1<String>, String, Function2<Composer, Integer, Unit>, Composer, int, int)`.
- A compile with `mode=producer` and two `catalog` options composes the created function in a real composition. The 5 composition tests in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/producer/BridgeCompositionTest.kt` pass. They ran in the producer module's tests until the binary index replaced the text format.
- Recomposition through a created bridge works. One of those tests changes a state that the bridge call reads, sends the snapshot apply notification and one frame, and the placed node then holds the new text (`Updated` where it held `Welcome`). When the node took its values only when it was created, that test failed.
- Two bridges with the same component name in two packages of one module compile side by side. Each has its own facade class, `<package>.__GENERATED__CALLABLES__Kt`, and each placed node carries the version of its own bridge (`"1"` and `"2"`). The two components had different parameters, and giving the first bridge the second one's parameter fails with `No parameter with name 'badge' found.`
- Each catalog needs its own generation key that carries its version. The IR step reads the key of a created function to pick the catalog and the version. When the IR step used the first catalog for every function, the two-bridge tests failed.
- A test that only compiles is not enough. A caller that leaves out an optional parameter compiled while the function would still throw at run time (see R8a). The compile tests in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/producer/BridgeExperimentTest.kt` (7 tests) cover the compile errors, and `BridgeCompositionTest.kt` covers what runs.

The real plugin creates its declarations from the binary index since the second pull request of T031a, and each function's file name comes from the class rule in [R16](#r16-scale-and-build-performance).

The real plugin creates a declaration only when the code of the module names it. [R16](#named-bridges-in-the-real-plugin) has how and what proves it.

Alternatives considered: a source generator that writes Kotlin files. Turned down by the owner: the compiler creates the definitions.

## R2. The order of the two compiler plugins is a correctness matter

Decision: the bridge plugin's IR step must run before the Compose compiler's. The build-tool plugins pass `-Xcompiler-plugin-order=<bridge>><compose>` themselves, and the bridge plugin keeps a guard that stops the build with a clear message if it runs second.

Checked by experiment:

- With the right order it works. With the wrong order and no guard, the build passes and the program fails at run time with `NoClassDefFoundError: androidx/compose/runtime/internal/ComposableFunction0`. That is a silent miscompile.
- The order flag controls this in Gradle, both ways.
- The guard turns the wrong order into a compile error.
- The compile-testing library hides every plugin behind one ID, so the flag cannot be tested there. Only list order counts in those tests.

Checked in the Kotlin toolchain probe (T030, run on 2026-11-16 with Kotlin toolchain 0.14.0 and Kotlin 2.4.21; the setup and `<probe>` are explained in [R5](#the-kotlin-toolchain-probe-t030)):

- The toolchain has no setting for plugin order. Its documentation page on compiler plugins and the output of `<probe> show settings` list none.
- The toolchain builds the compiler arguments itself. For the `app` module (`mode=producer`), the run's `build/logs/<run>/debug.log` has a `Kotlin compiler args:` line. This one is from the run `amper_2026-11-16_18-51-37_90046-53686_build`. These are the plugin parts of it, with long paths shortened:

```
-Xplugin=<cache>/kotlin-compose-compiler-plugin-embeddable-2.4.21.jar,~/.m2/repository/dev/tonholo/ssrcompose/compiler-plugin/unspecified/compiler-plugin-unspecified.jar,~/.m2/repository/dev/tonholo/ssrcompose/catalog-model/unspecified/catalog-model-unspecified.jar,<stdlib, annotations and serialization-json jars>,~/.m2/repository/dev/tonholo/ssrcompose/core-jvm/unspecified/core-jvm-unspecified.jar,<serialization-core and coroutines jars>
-P plugin:androidx.compose.compiler.plugins.kotlin:generateFunctionKeyMetaAnnotations=true,plugin:androidx.compose.compiler.plugins.kotlin:sourceInformation=true,plugin:dev.tonholo.ssrcompose:mode=producer
-Xcompiler-plugin-order=dev.tonholo.ssrcompose>androidx.compose.compiler.plugins.kotlin
-verbose
```

- In that one `-Xplugin=` argument the Compose compiler is first and this plugin is second. The module's `freeCompilerArgs` come after the plugin arguments, so a flag written there reaches the compiler as written. The module file of `app` in [R5](#the-kotlin-toolchain-probe-t030) passes the order flag that way.
- The compiler reads the flag and knows both plugin ids. With the flag twice, once each way, the build fails:

```
$ <probe> build
ERROR: Compiler plugin 'androidx.compose.compiler.plugins.kotlin' is part of an constraint cycle: PluginOrderConstraint(before=dev.tonholo.ssrcompose, after=androidx.compose.compiler.plugins.kotlin, rawArgument=dev.tonholo.ssrcompose>androidx.compose.compiler.plugins.kotlin), PluginOrderConstraint(before=androidx.compose.compiler.plugins.kotlin, after=dev.tonholo.ssrcompose, rawArgument=androidx.compose.compiler.plugins.kotlin>dev.tonholo.ssrcompose)

ERROR: Task ':app:compileJvm' failed: Kotlin compilation failed with 1 errors (see above)
```

- The same flag with an id that no plugin has, `no.such.plugin>androidx.compose.compiler.plugins.kotlin`, builds without a message. A misspelled plugin id in the flag is not caught, so the flag text needs its own check.
- The Android library module passes the same flag with `mode: consumer`, and the toolchain gives it the same kind of `-Xplugin=`, `-P` and order arguments.

Checked by T025 on 2026-11-16:

- With the Compose registrar listed before the bridge registrar and no guard, the compile test failed because the build exited with `OK`. That is the silent miscompile again.
- The guard reports the problem through the compiler's diagnostic reporter (`pluginContext.diagnosticReporter`) with a diagnostic that the plugin owns, so it is a normal compile error and not an internal error with a stack trace. The plugin id comes from the generated constant. T029 reworded the opening from `The bridge plugin` so the shared guard covers consumer renderers too: `The SSR Compose compiler plugin 'dev.tonholo.ssrcompose' must run before the Compose compiler plugin. Pass -Xcompiler-plugin-order=dev.tonholo.ssrcompose>androidx.compose.compiler.plugins.kotlin`
- The guard fires when a created function already has a `$composer` parameter, which only happens when the Compose compiler ran first.
- In a real Gradle compile of the producer's test sources, `-Xcompiler-plugin-order=<bridge>><compose>` passes and `-Xcompiler-plugin-order=<compose>><bridge>` fails with the message above, so the flag is honored both ways. With no flag the same compile also passed, as in the spike. That is the order of the plugin classpath, so it is not proof.
- The flag was added with `compilerOptions.freeCompilerArgs` in the producer's `build.gradle.kts`.

Not checked: adding the flag from a `KotlinCompilerPluginSupportPlugin` (T025 passed it from a plain `build.gradle.kts`), and that the Android Gradle Plugin keeps it. Also not checked in the toolchain: the effect of the flag (the plugin in that probe had no IR step yet, so no run could show the order), and the order the compiler uses when the flag is absent (only the order of the jars in the argument was seen). Both wait for a probe with the IR step. Also not checked: that the toolchain plugin of T101 can add the flag itself (see [Open risk for T101](#open-risk-for-t101) in R5).

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

Checked by experiment (T027, `KmpAndroidFragmentsTest`), for a Kotlin Multiplatform module with an Android target and two product flavors:

- The plugin runs once for each Android variant compilation, so each variant gets its own folder of fragments (R8c lists the paths). The free variant's folder holds the free composable and not the paid one, and the other way round.
- The checker sees both the `expect` file and the `actual` file of a marked pair in an Android compilation. The fragment writer leaves out a marked `expect` and writes the marked `actual`, so the pair counts once and the merge rule stays as it is: a name used twice fails. Without that rule the part held `Shared` twice and the merge failed with "A part must not repeat a component name".
- For a file that only one flavor has, the second compile after an edit was incremental. It wrote again only that file's fragment, and the module's part equalled the part of a clean build, byte for byte. A test compares the bytes after the edit with the bytes of a clean build in another folder, and checks that the part changed and still holds the untouched files' components. The edits in the Gradle tests only add a parameter. Removing the marker or deleting a file is covered only by the compile tests, which are not incremental, and by the gap about deleted source files below (#152).
- For an edit to a file in `commonMain`, every fragment of the variant was written again, so the compilation was not incremental. The Kotlin log of an incremental compile shows `enableUnsafeIncrementalCompilationForMultiplatform=false`, which is probably the reason, but that is not confirmed. The part still equals the part of a clean build.
- The `expect` file and the `actual` file can have the same name in the same package, `Panel.kt` in `commonMain` and in `androidMain`, which is a common layout. Both then have the same fragment source and share one fragment file. The part held that pair once, and it would have held it once even without the rule that leaves out the `expect`. The pair with two file names, `Shared.kt` and `SharedActual.kt`, held the component twice without that rule, so the fixture has both pairs. Inside one compilation the result does not depend on which of the two files the compiler checks first: the writer does not let the `expect` file, which has no component, remove the fragment the `actual` file wrote in the same compilation. `FragmentWriterTest` covers that order.
- `compileKotlinMetadata` and `compileCommonMainKotlinMetadata` were skipped in every project tried, an Android-only module and an Android plus JVM module, so the plugin never saw them and no `metadata` folder appeared. With a native or JavaScript target they would run, and that is not checked.

Found by the experiment and left for later tasks:

- The fragments folder is not an output of the compile task. Deleting it and building again left the compile up to date and wrote no fragments. `ssrCatalogPart` must tell a missing folder from a module with no marked composables, and a compile taken from the build cache has the same gap. This is filed as #151: T047 registers the folder as an output of the compile task, and T114 proves it.
- A fragment names its source by the folders of its package and its file name, such as `fixture/kmp/Home.kt`, because a path of the machine would break the relative path rule for the inputs of `ssrCatalogPart` and the cache. The cost is that the part step cannot see from the name alone that a source file is gone. It needs the module's source file list, or the compiler plugin needs one more option. This is filed as #152, with T045 as the owner, T047 wiring it and T033 testing it.
- Settled by T044: the checker and the fragment writer describe text, whole number, decimal number, yes or no, choice and list parameters. A default makes `required` false, and a nullable type sets `nullable`, also through a type alias. An enum from another module is read through its entries, and a fragment holds the choice lists its components use. A modifier parameter is left out. Each of these fails the compile at the line and column of the parameter, with the composable, the parameter and the reason: a type the catalog cannot describe, an enum with no entries, an enum with an entry that is not a plain Kotlin identifier, a list inside a list, a list with nullable elements, a parameter name the catalog cannot hold, and two different enums with the same name in one file. A function name the catalog cannot hold and a marked function that is not `@Composable`, a member, an extension or generic function fail at the function.
- T044 moved the checks for a marked function that is not `@Composable`, a member, an extension, a function with context parameters, a generic function and a private function from `RendererComposableChecker` to the consumer checker (`MarkableFunction` and `ConsumerErrors`), so they are one rule, found in every consumer compilation. `RendererComposableChecker` keeps only the overload clash. The generic message now ends `Only top-level composables without type parameters can be marked.`, and the extension message ends `Only top-level composables without an extension receiver or type parameters can be marked.`
- Still open after T044: callbacks (T064) and slots (T075). Two enums with the same name in two files of one module are each fine in their own fragment, so the part step (T045) must refuse the pair when their entries differ.
- The writer leaves out every marked `expect`. A marked `expect` whose `actual` has no marker fails the compile at the `actual` (T044). A marked pair and an unmarked pair both compile. A default written on the `expect` makes the parameter of the pair optional, because an `actual` cannot declare one.
- The same-source rule above holds inside one compilation only. In an incremental compile that rechecks the `expect` file and not the `actual` file, the writer removes the fragment the `actual` file wrote earlier, and the part loses the component. `FragmentWriterTest` shows it with two writers. A commonMain edit forces a full recompile today, which hides it, and no Gradle test runs it. Only the part step can decide this, because it sees every source file. It is part of #152.
- Two different files with the same package and the same name that both hold marked composables write one fragment, and the last one wins without a message. A clash of the class both files would make on the JVM makes this rare on Android. Two file names that differ only by case, `Home.kt` and `home.kt` in one package, also share a fragment file on a file system that ignores case.
- A fragment's source and file name follow rules (`data-model.md`). A file with no marked composable never needs a fragment, so a file name that breaks the rules, such as `Odd..Name.kt`, builds. A marked file with such a name fails the compile with a message that names the file. Both are tested.

Not checked: one module collecting the parts of the modules it depends on (T105 settles it); the build cache and the configuration cache for the part and bundle steps (T114 settles them); and the state of fragments after a failed incremental build.

Checked by experiment (T028, `SlotAnnotationTest`), for reading slot type annotations in the consumer checker:

- `SlotParameterMapper` detects `@Composable () -> Unit` parameters using `FirValueParameter.returnTypeRef.coneType`, checking `isSomeFunctionType(session)`, a single `Unit` type argument, and `Composable` in the type's annotation class IDs.
- Annotation class IDs are read by walking the typealias chain in a loop: starting from the slot's `coneType`, each step reads `customAnnotations` (via `toAnnotationClassId(session)`), then follows `attributes.abbreviatedType` to its `FirTypeAliasSymbol`, reads `resolvedAnnotationsWithClassIds`, and continues with `resolvedExpandedTypeRef.coneType` until no typealias remains.
- Direct slot types with `@ConsumerComposable`, `@ProducerComposable`, or neither produced `consumer`, `producer`, and `any` in the written fragment's `accepts` field.
- Typealias slot types worked for all three placements (annotation on the typealias right-hand side, annotation on the `typealias` declaration itself, and annotation at the parameter use site) and across nested typealias chains, including conflicts split across chain levels.
- A slot type carrying both `@ConsumerComposable` and `@ProducerComposable` fails compilation with `ConflictingSlotAnnotations`, naming the composable and the slot and stating that a slot accepts consumer composables, producer composables or either, not both. This is the owner's decision because each annotation means only that side.
- Nullable slot types (`(@Composable () -> Unit)?`) are rejected by `isSlot` and fail the build with `UnsupportedParameter` until T075.
- Marked composables or parameters whose name is not a plain Kotlin identifier of at most 64 characters (such as backtick-escaped names) fail compilation with `UnsupportedName` before any model descriptor is constructed, instead of throwing from the catalog model.
- Not checked: slots with parameters (`@Composable (A) -> Unit`). T044 now reports `UnsupportedParameter` and `ConflictingSlotAnnotations` at the slot parameter, and `SlotAnnotationTest` pins the location.

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

Found by T027, for T024 (not fixed here): in a plain `com.android.library` module, with AGP 9.5.1's built-in Kotlin and no Kotlin Gradle plugin applied, the compile failed in the compiler plugin:

```
e: java.lang.NoSuchFieldError: Class org.jetbrains.kotlin.config.CompilerConfigurationKey does not have member field 'org.jetbrains.kotlin.config.CompilerConfigurationKey$Companion Companion'
	at dev.tonholo.ssrcompose.compiler.SsrConfigurationKeys.<clinit>(SsrConfigurationKeys.kt:10)
	at dev.tonholo.ssrcompose.compiler.SsrCommandLineProcessor.processOption(SsrCommandLineProcessor.kt:45)
```

It went away when `org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21` was added to the build script classpath of the module. The compiler plugin is built against Kotlin 2.4.21, so the likely cause is that the built-in Kotlin ran an older compiler. Which version it used is not checked.

This adds two artifacts and two libraries (`tooling`, and `catalog-model` from R17) to the constitution's four deliverables. It is recorded in the plan's Complexity Tracking.

### The Kotlin toolchain probe (T030)

The probe is `samples/toolchain-probe/`. It was run on 2026-11-16 with Kotlin toolchain 0.14.0 on macOS, JDK from Android Studio, and the Android SDK that is already on the machine. The probe is a throwaway experiment and nothing depends on it.

How the toolchain was obtained:

- `samples/toolchain-probe/kotlin` is the toolchain's own wrapper script for version 0.14.0. It was downloaded from `https://packages.jetbrains.team/maven/p/amper/amper/org/jetbrains/kotlin/kotlin-cli/0.14.0/kotlin-cli-0.14.0-wrapper`. Its SHA-256 is `855436c4b4222f57c561e7bfe2f7f223bd72710d0f52a101883434ecbf05b089`, the same as the published `kotlin-cli-0.14.0-wrapper.sha256`.
- The wrapper is a shell script, so the probe runs on macOS and Linux only. The toolchain's documentation also names a Windows script, `kotlin.bat`. It is not in the probe and was not run.
- On the first run the wrapper downloads the distribution (`kotlin-cli-0.14.0`) and a Zulu JRE 25 into `KOTLIN_CLI_BOOTSTRAP_CACHE_DIR`. By default that is `~/Library/Caches/JetBrains/Kotlin/cli`.
- The probe keeps both caches inside its own git-ignored `build/` folder. In the commands below, `<probe>` stands for this line:

```
env KOTLIN_CLI_BOOTSTRAP_CACHE_DIR="$PWD/samples/toolchain-probe/build/kotlin-cli" samples/toolchain-probe/kotlin --shared-cache-dir="$PWD/samples/toolchain-probe/build/shared-cache"
```

- Each command also ends with `--project-dir=samples/toolchain-probe`, which is left out of the examples to keep them short.

How the toolchain finds the artifacts after `./gradlew publishToMavenLocal`:

- The command does not publish everything. It published `core`, `core-jvm`, `core-android`, `core-testing` and its two variants, `consumer` and `consumer-android`, and the Gradle plugin with its marker. It did not publish `compiler-plugin`, `catalog-model`, `producer` or `tooling`. Those modules apply `maven-publish` but declare no publication, so the task has nothing to write. Every artifact is published at the version `unspecified`.
- The probe works around this with an init script, `samples/toolchain-probe/publish-probe.init.gradle.kts`, that adds one `java` publication to `:compiler-plugin` and `:catalog-model`. No build file of the repository changed. A real fix needs a publication and a real version in those modules (T101 and T113 depend on it):

```
$ ./gradlew --init-script samples/toolchain-probe/publish-probe.init.gradle.kts :compiler-plugin:publishToMavenLocal :catalog-model:publishToMavenLocal
> Task :catalog-model:publishProbePublicationToMavenLocal
> Task :catalog-model:publishToMavenLocal
> Task :compiler-plugin:publishProbePublicationToMavenLocal
> Task :compiler-plugin:publishToMavenLocal
BUILD SUCCESSFUL in 5s
```

- All of it lands in `~/.m2/repository/dev/tonholo/ssrcompose/` (1040 KB once both publications had run).
- The toolchain does not look in `~/.m2` unless the module asks. Without a `repositories` entry the build fails:

```
$ <probe> build
ERROR: Unable to resolve dependency dev.tonholo.ssrcompose:compiler-plugin:unspecified
         Unable to download checksums of file compiler-plugin-unspecified.pom
         Unable to download checksums of file compiler-plugin-unspecified.module
       Repositories used for resolution:
         - https://repo1.maven.org/maven2
         - https://maven.google.com

ERROR: Task ':app:downloadCompilerPluginsMain' failed: Unable to resolve dependencies for dev.tonholo.ssrcompose:compiler-plugin:unspecified, see the errors above.
```

- With `repositories: [mavenLocal]` in the module's `module.yaml` the build passes. The plugin is given as `dependency: dev.tonholo.ssrcompose:compiler-plugin:unspecified` in `settings.kotlin.compilerPlugins`, and the toolchain uses the jars straight from `~/.m2`. This is the module file of `app`:

```yaml
product: jvm/app

repositories:
  - mavenLocal

dependencies:
  - dev.tonholo.ssrcompose:core:unspecified

settings:
  kotlin:
    version: 2.4.21
    freeCompilerArgs:
      - -Xcompiler-plugin-order=dev.tonholo.ssrcompose>androidx.compose.compiler.plugins.kotlin
    compilerPlugins:
      - id: dev.tonholo.ssrcompose
        dependency: dev.tonholo.ssrcompose:compiler-plugin:unspecified
        options:
          mode: producer
  compose:
    enabled: true
```

- The toolchain follows the plugin's own dependencies. The compiler plugin's classpath held `catalog-model` and, through it, `core-jvm` (see the `-Xplugin=` line in R2). It reads the Gradle module metadata that `publishToMavenLocal` wrote, so `core` as a normal dependency gives `core-jvm` to the JVM module and `core-android` (an AAR) to the Android library.
- The `repositories` entry is written in each module file in the probe. Putting it in one shared place was not tried.

Where the compiler plugin runs, and the proof:

- Build command and result, with the Compose compiler on in the same module:

```
$ <probe> build
✓ Compilation successful for 'app' [jvm]
✓ Compilation successful for 'android-lib' [android]
Build successful
```

- The compiler plugin's own message is written to the run log, not to the console. The toolchain only prints the compiler's warnings and errors. This is the line in `samples/toolchain-probe/build/logs/<run>/info.log`:

```
00:04.643 INFO  :app:compileJvm           KotlinBuildToolsApiKt     SSR Compose compiler plugin received SsrPluginOptions(mode=Producer, fragmentsDir=null, catalogs=[])
00:18.074 INFO  :android-lib:compileAndroidDebug KotlinBuildToolsApiKt     SSR Compose compiler plugin received SsrPluginOptions(mode=Consumer, fragmentsDir=null, catalogs=[])
```

- A control run shows the options reach the plugin. With `mode: bogus` in `app/module.yaml` the plugin refuses it with its own text:

```
$ <probe> build
ERROR: Unknown mode 'bogus'. Use one of: consumer, producer

ERROR: Task ':app:compileJvm' failed: Kotlin compilation failed with 1 errors (see above)
```

Defaults of the toolchain that differ from this repository:

- Kotlin: modules that do not set `settings.kotlin.version` use 2.4.20, and the probe pins 2.4.21 as the repository does.
- Java: `jvm.release` is 25, from the JDK it finds. This repository targets 17.
- Compose: 1.12.1, and this repository uses 1.12.2.

Whether the toolchain can build an Android library:

- Yes, as a compile, with both plugins. The probe module is `android-lib`. There is no `android/lib` product type; the documented product types are listed on `https://kotlin-toolchain.org/latest/user-guide/product-types/overview/`. The library is a Kotlin Multiplatform library with one platform:

```yaml
product:
  type: kmp/lib
  platforms: [android]
```

- The module also sets `settings.android.compileSdk: 37` and `minSdk: 23`.
- `<probe> build` made `build/tasks/_android-lib_aarAndroidDebug/android-lib-jvm.aar`, which holds `AndroidManifest.xml` and `libs/android-lib-jvm.jar`. The jar holds `GreetingKt.class`. Only the debug variant was built.
- The compile used the Android SDK that was already on the machine. The `-classpath` of the compile starts with `<android-sdk>/platforms/android-37.0/android.jar`. The toolchain finds the SDK from `ANDROID_HOME`, then `ANDROID_SDK_ROOT`, then `~/Library/Android/sdk`. No file in the top two levels of that SDK folder changed during the probe.
- The plugin ran in the Android compile (the second line of the log above), and `-Xcompiler-plugin-order` reached it as in R2.
- Not checked: the release variant and `kotlin package`, the Gradle distribution that the toolchain installs for Android packaging (`installGradleDistribution`), publishing the AAR, and a Kotlin Multiplatform library with the Android target next to other targets.
- The Complexity Tracking row of the plan that expects the consumer side to work from Gradle only depends on this answer. It stays, narrowed to the items above that are not checked.

How the toolchain names its modules, and the part name rule:

- The module name is the name of the folder that holds `module.yaml`, and nothing else. The module path in `project.yaml` is not part of it, and the root module takes the name of the project folder. `<probe> show modules` on the probe:

```
╭────────────────────────┬──────────────────┬───────────────────╮
│ Name                   │ Type             │ Short description │
├────────────────────────┼──────────────────┼───────────────────┤
│ android-lib            │ kmp/lib          │                   │
├────────────────────────┼──────────────────┼───────────────────┤
│ app                    │ jvm/app          │                   │
├────────────────────────┼──────────────────┼───────────────────┤
│ featureHome            │ jvm/lib          │                   │
├────────────────────────┼──────────────────┼───────────────────┤
│ feature_cart           │ jvm/lib          │                   │
├────────────────────────┼──────────────────┼───────────────────┤
│ home                   │ jvm/lib          │                   │
├────────────────────────┼──────────────────┼───────────────────┤
│ module-location-plugin │ jvm/amper-plugin │                   │
├────────────────────────┼──────────────────┼───────────────────┤
│ toolchain-probe        │ jvm/lib          │                   │
╰────────────────────────┴──────────────────┴───────────────────╯
```

- The module in `feature/home` is named `home`, and the root module is named `toolchain-probe`, after the folder `samples/toolchain-probe`. A part name made from the module name would be `home` and `toolchain-probe`, which is not what the rule gives for `:feature:home` and `:`.
- Folder names must be unique in the project. Two modules in `feature/home` and `settings/home` fail the build:

```
$ <probe> show modules
ERROR: Module name 'home' is not unique, it's declared in:
.../samples/toolchain-probe/feature/home/module.yaml
.../samples/toolchain-probe/settings/home/module.yaml
```

- A toolchain plugin can read the path. The probe's `module-location-plugin` task gets `${module.name}`, `${module.rootDir}` (the absolute path of the module folder) and `${project.rootDir}` (the absolute path of the project folder) from its `plugin.yaml`, and prints the module folder relative to the project folder:

```
$ <probe> do printLocation
[home][printLocation@module-location-plugin] moduleName=home relativePath='feature/home'
[feature_cart][printLocation@module-location-plugin] moduleName=feature_cart relativePath='feature_cart'
[toolchain-probe][printLocation@module-location-plugin] moduleName=toolchain-probe relativePath=''
[featureHome][printLocation@module-location-plugin] moduleName=featureHome relativePath='featureHome'
printLocation successful
```

- The part name rule can be applied to that relative path, with an empty path meaning the root module. Three of the probe's modules give the same results as rows of the contract's table, and `feature_cart` is an extra example that has no row there:

| Module folder in the probe, relative to the project folder | Part name by the rule | Row of the contract's table |
|---|---|---|
| `feature/home` | `feature-home` | `:feature:home` gives `feature-home` |
| `featureHome` | `featurehome` | `:featureHome` gives `featurehome` |
| empty (the root module) | `root` | `:` gives `root` |
| `feature_cart` | `feature-cart` | none, an extra example of an underscore in a folder name |

- A Windows path separator is a backslash. Under the rule it is one more run of characters other than `a` to `z` and `0` to `9`, so it would give a hyphen like `/` does. That was not run.

- So the toolchain plugin must build the part name from the relative folder path, never from `module.name`. One rule on a path gives both build tools the same name when the Gradle project folders follow the Gradle paths.
- Two cases make the names differ. A Gradle project whose folder is not its path gives the Gradle name from the path and the toolchain name from the folder. And a project that uses both build tools cannot have two folders with the same name, because the toolchain refuses it and Gradle does not.

Not checked: the toolchain plugin itself, the build cache for the toolchain's tasks, and the toolchain's IDE support.

### Open risk for T101

The toolchain plugin of T101 may not be able to add the compiler plugin to a module. If it cannot, this conflicts with [plan.md](plan.md), which says no literal compiler plugin id appears in any yaml file, the toolchain plugin's included. The probe only proved that a module can name the plugin itself, with its literal id, in `settings.kotlin.compilerPlugins`.

What the documentation says, checked on 2026-11-16:

- The page about compiler plugins says: "At the moment, they can't publish plugins because the Kotlin Toolchain doesn't support plugin publication yet, so this low-level API is the only way you, as an end user, can configure compiler plugins."
- The plugin quick start says: "It is not yet possible to use references (${...}) in module.yaml files or access the module configuration tree from plugin.yaml." The references page lists `module.settings.**` as readable by a plugin, so the two pages do not agree.
- Plugins documented so far contribute tasks, commands, checks and generated sources and resources. No page says a plugin can add a compiler plugin, its options or the order flag to a module.
- The page on publishing libraries (`https://kotlin-toolchain.org/latest/user-guide/publishing/libraries/`) was checked too. It covers publishing libraries, including to the local Maven repository with `kotlin publish mavenLocal`, and says the feature is in preview. It says nothing about publishing toolchain plugins, or about a plugin adding a compiler plugin to a module. Our compiler plugin is already used as a plain Maven dependency, as the probe proved, so this page does not block T030.

The three ways out, none of them tried:

- Write the literal id, the dependency, the options and the order flag in each module's `module.yaml`, as the probe does. This breaks the plan's rule, and every module carries the same block.
- Put that block in one module template that each module applies with `apply:`. The literal id then sits in one yaml file instead of many, which still breaks the plan's rule, and the template must then be kept in step with the id in `catalog-model` by hand or by a check.
- Wait until the toolchain lets a plugin add a compiler plugin to a module, or lets a plugin be published.

The owner decides which one when T101 starts.

## R6. Fetching at sync

Decision: an `ssrCatalogSync` step works on each catalog location, which is the address of a manifest (a web address or a file path):

1. Fetch the manifest, refuse it before reading if it is over the size cap, and validate it. Every part's `file` must be its `name` followed by `.part.json`, the `file` values must be unique, and none may be empty, a dot or `manifest.json`. That keeps every file a plain file name, with no path separator, no `..` and no scheme.
2. Fetch only the parts whose fingerprint is not already saved for that version, each from the manifest's location with its `file` name, each refused before reading if it is over the size cap.
3. Check each part against its fingerprint in the manifest, validate it, and check across parts.
4. Save the manifest and the parts in `ssr/catalogs/<version>/`, and write the location and the manifest's fingerprint to the lock file.

If any check fails, or the manifest loads but one part cannot be fetched, nothing of that catalog is saved and the saved copy, if any, is kept. Since a catalog is saved whole and a changed manifest waits for `--update`, fetching only the missing parts happens on `--update` and after a saved part file was removed by hand. Builds read only the saved copies. When the manifest at a location no longer matches the lock file, the step reports it and changes nothing unless asked.

Decision: `gradle-plugin` depends on JetBrains `org.jetbrains.gradle.plugin.idea-ext` (`1.4.1`, from the Gradle Plugin Portal) and applies it to the root project when `producer { }` is configured, wiring `ssrCatalogSync` into `idea.project.settings.taskTriggers.afterSync` with no extra user plugin step. A developer can also run `ssrCatalogSync` directly.

Known limits of this wiring:

- `gradle-idea-ext` is published only on the Gradle Plugin Portal and not on Maven Central, so the server's plugin repositories (`pluginManagement { repositories { ... } }`) must include `gradlePluginPortal()`. A plugin's own dependencies resolve from `pluginManagement`, not from `dependencyResolutionManagement`.
- `idea-ext` project settings live on the root project, so a subproject with `producer { }` reaches into `project.rootProject`, which Gradle's Isolated Projects mode forbids (unverified against this repo's `build-logic`; noted for T047).
- A user who applies a different `idea-ext` version next to ours is not checked. The plugin applies its own `1.4.1` on the root project and warns when it cannot find the trigger API; a version clash is left to T047.
- Android Studio running `idea-ext` task triggers is unverified: the `idea-ext` wiki (https://github.com/JetBrains/gradle-idea-ext-plugin/wiki) names only IntelliJ IDEA, and AGP's sync-time source generation is for Android projects rather than a plain Kotlin JVM server.
- Gradle 9 has no built-in public IDE sync hook (checked against Gradle 9 documentation and the Gradle forum thread https://discuss.gradle.org/t/how-to-trigger-a-task-during-idea-sync/48253, not an official Gradle 9 sync API page).

Checked by `SyncHookTest` in `gradle-plugin/src/functionalTest/kotlin/dev/tonholo/ssrcompose/gradle/SyncHookTest.kt` on Gradle 9.9.1:

- A single-project server with `producer { }` and no `idea-ext` in its build script exports `taskPath` equal to `ssrCatalogSync` in `TaskTriggersConfig.toMap()["afterSync"]`, and running `:ssrCatalogSync` succeeds.
- A multi-module server where only `:server` sets `producer { }` exports `taskPath` equal to `ssrCatalogSync` with `projectPath` pointing to `:server` on the root project's `afterSync` triggers, and running `:server:ssrCatalogSync` succeeds.
- A consumer project (`consumer { }`) leaves `afterSync` empty.
- Running `:inspectSyncTriggers` and `:ssrCatalogSync` twice with `--configuration-cache` succeeds on both runs, confirming configuration-cache compatibility for `ssrCatalogSync` and reading the trigger map.

Alternatives checked and rejected or kept as manual fallback:

- `prepareKotlinBuildScriptModel` (exposed in Gradle's Tooling API javadoc as `KotlinDslModelsParameters.PREPARATION_TASK_NAME`): not documented as a user sync hook (https://docs.gradle.org/current/javadoc/org/gradle/tooling/model/kotlin/dsl/KotlinDslModelsParameters.html); the Gradle forum thread https://discuss.gradle.org/t/how-to-trigger-a-task-during-idea-sync/48253 calls the Kotlin DSL sync task an implementation detail of the IDE's Gradle support that can change at any time.
- `idea.sync.active` system property set by IntelliJ-based IDEs during sync: undocumented, shown only as a workaround on the Gradle forum (https://discuss.gradle.org/t/how-to-trigger-a-task-during-idea-sync/48253), and acts as a sync detector rather than a task hook.
- Per-user "Execute After Sync" in the Gradle tool window (Tasks Activation): needs no dependency and works as a zero-dependency manual fallback, but is stored in `.idea/workspace.xml` and is not shared with the team.

Still needs a person in IntelliJ IDEA and in Android Studio separately (TestKit does not launch an IDE's Gradle importer):

1. Open a Kotlin JVM server project (single-module or multi-module where `:server` applies `alias(libs.plugins.ssrcompose)` and `ssrCompose { producer { catalog(...) { ... } } }`, with `gradlePluginPortal()` in `pluginManagement` repositories and no `idea-ext` in the build script).
2. Run Gradle sync in IntelliJ IDEA (click "Reload All Gradle Projects" in the Gradle tool window) and separately in Android Studio (click "Sync Project with Gradle Files"), noting the exact IDE name and version.
3. Right-click `:ssrCatalogSync` (or `:server:ssrCatalogSync`) in the Gradle tool window and select "Tasks Activation" (or inspect `.idea/workspace.xml` under `<component name="ExternalProjectsManager">` for `AFTER_SYNC`) to confirm `ssrCatalogSync` appears under "After Sync".
4. Confirm in the Sync / Build output window that `ssrCatalogSync stand-in executed` prints automatically when sync finishes.
5. Remove the `producer { }` block and re-sync, then separately remove the trigger in Tasks Activation and re-sync, and check whether `.idea/workspace.xml` keeps a stale activation entry (reported on the Gradle forum at https://discuss.gradle.org/t/how-to-trigger-a-task-during-idea-sync/48253).
6. Record the IDE name, version, and pass/fail result for both IntelliJ IDEA and Android Studio here in R6.

Safety: only `https` addresses and file paths are accepted by default. Part file names are checked before any part is fetched. Names in a catalog are validated before anything is created from them. The size cap is 16 MiB per file by default, and configurable.

## R7. Consumer side: one renderer per marked composable

Decision: on the app side, the compiler plugin creates, for each marked composable, a small renderer that takes a placed component and calls the real composable with decoded values, callbacks that send a trigger, and slots that render their content. Because it is created per composable, incremental compilation is safe. The part step writes each module's entries of name to renderer, and the bundle step writes the app's renderer index as a resource, which the consumer library loads once. The renderer index is not the catalog: it maps names to the app's own code and holds no descriptors. The renderer outputs are not part of the catalog, and they follow the same per-module rule as the parts: a change in one module produces again only that module's renderer entries, and the app's renderer index is written again with the manifest. Where they are written is settled by T055 and not checked yet.

A developer can do the same by hand with `ComponentBindings`.

### Consumer-side renderers (T029)

Decision: works for top-level marked composables with no extension receiver and no context parameters whose regular parameters are all `String`, with one marked composable per name in a package. Creating one renderer per marked composable in the compiler plugin is kept, and the gate passes. Other value kinds, defaults and the renderer index wait for T055, callbacks for T064, and slots for T075.

Checked on 2026-11-17 in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/consumer/RendererExperimentTest.kt` (11 tests), on Kotlin 2.4.21 and Compose runtime 1.12.2:

- Proof 1 (it compiles): compiling two marked composables with a text parameter each, `Greeting(title: String)` and `Banner(headline: String)`, together with a caller under `mode=consumer` creates `GreetingRenderer(placed: PlacedComponentScope)` and `BannerRenderer(placed: PlacedComponentScope)` in the composables' package, the Compose compiler transforms both and their calls, and the compilation exits with `OK`. Returning an empty set from `RendererDeclarationGenerator.getTopLevelCallableIds` makes 6 of the 11 tests fail; the compile and composition tests fail on the exit-code assertion (`expected: <OK> but was: <COMPILATION_ERROR>`) with `Unresolved reference 'GreetingRenderer'` and `Unresolved reference 'BannerRenderer'` in the compiler messages.
- Proof 2 (calling it calls the real composable with the decoded value): composing `GreetingRenderer` in a real `Composition` with a `PlacedComponentScope` over `DataValue.Text("Welcome aboard")` under `"title"` calls `Greeting` and records `"greeting:Welcome aboard"`. Leaving the generated body empty in `RendererBodyGenerator` still compiles and makes the 3 composition tests fail on the output assertion (`expected: <greeting:Welcome aboard> but was: <null>` and `expected: <updated-banner:Spring Sale> but was: <null>`).
- Proof 3 (editing one file does not break another file's renderer): `Greeting` and `Banner` are compiled together, then an edited `Banner` is compiled alone with the first compilation's output folder on the classpath, and the caller is compiled and run over the edited output followed by the first output. `BannerRenderer` runs the edited `Banner` (`"updated-banner:Spring Sale"`) and `GreetingRenderer` from the first output still resolves and runs `Greeting` (`"greeting:Welcome aboard"`). Giving each created function its own `containingFileName` (`__SsrCompose_GreetingRenderer__.kt` and `__SsrCompose_BannerRenderer__.kt`) puts each renderer in its own facade class (`__SsrCompose_GreetingRenderer__Kt.class` and `__SsrCompose_BannerRenderer__Kt.class`). Without `containingFileName`, both share `__GENERATED__CALLABLES__Kt.class`, the second compile overwrites that class with only `BannerRenderer`, and this test fails on the caller's exit-code assertion (`expected: <OK> but was: <COMPILATION_ERROR>`) with `Unresolved reference 'GreetingRenderer'`.
- Target pinning, overloads, extension receivers and context parameters: a marked `Greeting(title: String)` next to an unmarked `Greeting(count: Int)` in the same package compiles and composes `Greeting(title: String)` (`"greeting:Welcome aboard"`), because `RendererKey` carries the regular parameter names and `ClassId` types and `RendererBodyGenerator` resolves only `@ConsumerComposable` functions with regular parameters only matching the full `RendererKey`. `RendererDeclarationGenerator` stays side-effect free and skips overloaded marked names, extension functions, functions with context parameters, generic functions and private functions, while `RendererComposableChecker` (`compiler/consumer/renderer/fir/checker/`) reports `RendererDiagnostics.INVALID_MARKED_COMPOSABLE` at each overloaded declaration's `source`. T044 later moved the extension, context parameter, generic and private checks to the consumer checker, which reports them whether or not `fragmentsDir` is set (see R4). Two marked `Greeting` overloads in the same package fail at both declarations (`6:1` and `11:1`) with `The marked composable 'Greeting' is declared more than once in package 'fixtures.consumer.renderer'. Marked composables in the same package cannot be overloaded.` A marked extension composable `fun String.Tag(title: String)` and a marked composable with context parameters `context(prefix: String) fun Tag(title: String)` each fail at their declaration (`6:1`) naming `Tag` and stating the rule for marked composables (the wording changed in T044, see R4), instead of crashing Compose IR lowering with `Unexpected null argument for composable call`. A marked composable with type parameters, such as `fun <T> Tag(title: String)`, gets no renderer and fails at its declaration (`6:1`) with `The marked composable 'Tag' has type parameters. Only top-level composables without type parameters can be marked.` (the T029 wording was `Only non-generic composables can be marked.`), because the generated call supplies no type arguments. A `private` marked composable gets no renderer and fails at its declaration (`6:1`) with `The marked composable 'Tag' is private. Only internal or public composables can be marked.`; before this check it compiled to an internal compiler error in IR lowering (`SyntheticAccessorTransformer`), because the renderer lives in its own file and cannot call a private function.
- Parameter filtering: `RendererDeclarationGenerator` creates a renderer only when every regular parameter of the marked composable is `kotlin.String`, whether or not `fragmentsDir` is set. Marked composables with an `Int` parameter (`Counter(count: Int)`) or a `@Composable () -> Unit` slot (`Section(content: @Composable () -> Unit)`) get no renderer, and a caller that names `CounterRenderer` or `SectionRenderer` fails to compile with `Unresolved reference 'CounterRenderer'` or `Unresolved reference 'SectionRenderer'`.
- Synthetic file name vs user file name: compiling a user source file named `GreetingRenderer.kt` in the same package as the marked `Greeting` composable succeeds because the created renderer sits in `__SsrCompose_GreetingRenderer__.kt`. Using `GreetingRenderer.kt` as `containingFileName` makes that test fail with `Duplicate JVM class name 'fixtures/consumer/renderer/GreetingRendererKt'`.
- Plugin order guard: when `ComposePluginRegistrar` runs before `SsrCompilerPluginRegistrar` in consumer mode, `PluginOrderGuard` in `compiler/ir/` detects the `$composer` parameter on the created renderer (`RendererKey`) and reports `PluginOrderDiagnostics.PLUGIN_ORDER`. Bypassing `PluginOrderGuard.reportIfViolated` in `RendererIrGenerationExtension` makes the compilation crash with `IrGenerationExtensionException: The marked composable fixtures/consumer/renderer/Greeting with parameters [RendererParameter(name=title, type=kotlin/String)] was not found in the module` at `RendererBodyGenerator.targetComposable`, because the Compose compiler has already added `$composer` and `$changed` to `Greeting`.

FIR and IR APIs used:

- FIR (`compiler/consumer/renderer/fir/RendererDeclarationGenerator.kt`, `fir/checker/RendererCheckersExtension.kt`, `fir/checker/RendererComposableChecker.kt` and `diagnostic/RendererDiagnostics.kt`): `FirDeclarationGenerationExtension` (side-effect free, cached once per `FirSession` in a `lazy` map and not overriding `hasPackage`, which FIR calls during `IMPORTS` before annotation predicates resolve), `FirDeclarationPredicateRegistrar` with `LookupPredicate.create { annotated(SsrNames.consumerComposable.asSingleFqName()) }` and `session.predicateBasedProvider.getSymbolsByPredicate`, `session.getRegularClassSymbolByClassId` to check that `PlacedComponentScope` is on the compilation classpath (so consumer compilations that only have `core`, such as the fragment tests, skip renderer creation), `receiverParameterSymbol`, `contextParameterSymbols` and `valueParameterSymbols` with `resolvedReturnType.isString` and `resolvedReturnType.classId` to filter eligible composables and pin parameter types, `createTopLevelFunction` with `RendererKey(composable, parameters)` and `containingFileName = "__SsrCompose_<Composable>Renderer__.kt"`, `buildComposableAnnotation()` (`SsrNames.composable`) shared with the producer bridge generator in `compiler/fir/ComposableAnnotation.kt`, and `FirAdditionalCheckersExtension` with `FirDeclarationChecker<FirNamedFunction>` (`MppCheckerKind.Platform`) using `session.firProvider.symbolProvider.getTopLevelFunctionSymbols` (rather than `predicateBasedProvider`, which in a `Platform` checker records a predicate lookup on `androidMain` `actual` files without `commonMain` `expect` files and breaks incremental flavor recompilation with `ACTUAL_WITHOUT_EXPECT` in `KmpAndroidFragmentsTest`) to report `RendererDiagnostics.INVALID_MARKED_COMPOSABLE` at `declaration.source`.
- IR (`compiler/consumer/renderer/ir/RendererIrGenerationExtension.kt` and `RendererBodyGenerator.kt`): `IrGenerationExtension`, `PluginOrderGuard.reportIfViolated` and `PluginOrderDiagnostics` in `compiler/ir/` shared with the producer IR extension, `IrElementTransformerVoid`, `hasAnnotation(classId = SsrNames.consumerComposable)` and `classOrNull` to index only `@ConsumerComposable` functions with regular parameters only by their full `RendererKey`, `IrPluginContext.finderForBuiltins().findFunctions` to look up `PlacedComponentScope.text`, and `DeclarationIrBuilder` (`irBlockBody`, `irCall`, `irGet`, `irString`) to call the target `IrSimpleFunctionSymbol` of the current `IrModuleFragment` with `placed.text(parameterName)` for each regular value parameter (`IrParameterKind.Regular`).

Not checked:

- True Gradle incremental compilation of a consumer module that calls or packages created renderers (settled by T055 in `ConsumerRendererIncrementalTest`). `kotlin-compile-testing` compiles all given sources in one shot, so the third test simulates an incremental step by compiling only the edited file in a second `KotlinCompilation` with the first compilation's output folder on the classpath. It proves that per-composable facade classes do not overwrite each other and that `predicateBasedProvider` only creates renderers for the sources of the current compilation, not for compiled classes on the classpath. It does not check Gradle's incremental compiler state or stale class removal when a marked composable is renamed or deleted.
- Running a created renderer against the real `:consumer` Android library (settled by T055 in `contract-tests`, `ConsumerRendererContractTest`). `PlacedComponentScope` in `:consumer` has an `internal` constructor and `:consumer` is an Android-only Kotlin Multiplatform module, so the JVM compile test in `:compiler-plugin` uses a small stand-in `PlacedComponentScope` in `PlacedComponentScopeStandIn.fixture.kt`. The stand-in throws `IllegalStateException` from `checkNotNull` and does not model strict-read failures (`PlacedValueException`), which are left to the contract test in T055.
- A user function with the same signature as a created renderer, `fun <Composable>Renderer(placed: PlacedComponentScope)`, in the composable's package (settled by T055). The synthetic file name `__SsrCompose_<Composable>Renderer__.kt` avoids a JVM facade class clash with a user file named `<Composable>Renderer.kt`, while a user function with the same name and parameter type still fails with `Conflicting overloads` until T055 settles the renderer callable naming and index.
- The duplicate check sees only the current compilation's module sources, so a marked name split between `commonMain` and `androidMain` is likely not caught (not run).
- Two modules that each mark `Greeting` in the same package `p` both create `p.__SsrCompose_GreetingRenderer__Kt`, so they clash on an app classpath and the first one wins (settled by T055 with the renderer naming and index).
- Optional parameters that fall back to the composable's own default when absent, nullable parameters, whole numbers, decimal numbers, flags, choices, lists, modifier parameters, callbacks, slots, and the renderer index resource (T055, T064 and T075).

### Hand-written bindings and device values (T052, T053)

Checked on 2026-11-17.

- A strict read fails for an absent value, a `None` value, a value of another kind and an unknown choice entry. All four failures are a `PlacedValueException`. The contract did not name a type for the first, second and fourth, so they use the same one as the third.
- `PlacedComponentScope` is `@Immutable` and equal when its placed components are equal. A test recomposes a screen with a new but equal scope and sees the binding skipped, and sees it run again when the component changes.
- `@Immutable` is a promise the types do not enforce: `PlacedComponent` holds `Map` and `List` interfaces and `DataValue.Bytes` wraps a `ByteArray`. A scope over a map that is changed in place would be skipped with stale values, so the scope documents that the placed component must not change after it is made. Decoded messages are new objects, so this holds.
- The equality walks the whole placed component, slots included, for each equal but new scope on every recomposition of its parent. The codec refuses JSON nesting deeper than 128 levels and a slot takes 3, so slots nest about 40 deep at most. Nothing caps the components on a screen. The owner's three apps have 621 to 1,085 composables in the whole app, so a screen is expected to have hundreds. The cost is about components times depth field comparisons, 40,000 for 1,000 components at depth 40. T054 measured a whole screen, see "Failed reads during composition". If it proves too slow, a later task can compare each decoded component once when a `Screen` arrives and reuse the old scope for an equal one, so later comparisons stop at the identity check.
- Every device value is read in common code, so nothing was put in `consumer/src/androidMain/`. Density comes from `LocalDensity`, the window size from `LocalWindowInfo.containerSize`, the theme from `isSystemInDarkTheme()` and the language from `Locale.current`.
- The values are published to `DeviceValues.current` in a `SideEffect`, so a reader outside composition, such as T098, sees them after the screen is committed.
- Tested: each value, and a change of density, window size and theme followed by a recomposition. The theme and language tests live in `consumer/src/androidHostTest/`, because `isSystemInDarkTheme()` reads Android's `LocalConfiguration` on Android and the test must provide one. The host tests return default values for Android stubs, set in the kmp-library convention plugin for every KMP module, because Compose's runtime calls them.
- Not tested: a change of language during a session. `Locale.current` is not state, so it is read again only when something else recomposes the function. A locale change should reach the screen because Android sends a configuration change and `isSystemInDarkTheme()` reads the configuration, but that was not run on a device.
- Not tested: the window size on a real device before the first layout, which may be zero. The producer accepts 1 to 100,000, so the session waits for a positive size before it introduces the app.

### Failed reads during composition (T054)

Decision: inside `SsrScreen`, a failed read reports the component and returns a stand-in. The screen is replaced by `ScreenProblem.Mismatch` for the topmost failed component. A layout gate keeps content with a reported problem from being drawn in the screen's window in the frame that composed it.

What the runtime does, tested on `androidx.compose.runtime` 1.12.1, the version Compose Multiplatform 1.12.2 resolves to on Android:

- `try/catch` around a composable call does not compile: "Try catch is not supported around composable function invocations."
- A failure in a first composition can be caught around `setContent`, but `Recomposer.processCompositionError` records it and `deriveStateLocked` moves the recomposer to `Inactive`. Other compositions on that recomposer stop updating, and a later failure resurfaces as the first one.
- `SubcomposeLayout` reaches the same code through the parent's `composeInitial`. This path was read in the source and not run.
- This runtime has no public error boundary.

Rejected options:

- Catching around `setContent` or `subcompose` keeps the app from crashing but freezes it.
- A private recomposer for each `SsrScreen` isolates the failure in a test with a no-op applier, and app composition locals can be forwarded. Hosting real UI on it needs `ComposeView` inside `AndroidView`, so Android-only code and a second Compose owner. Reads during measure, such as in a lazy list, would still escape, and a failing `LaunchedEffect` shuts the private recomposer down.

Where the gate holds, tested on the `medium_phone` emulator in 3 of 3 runs each, with counts of placements and draws of the stand-in:

- A read that fails on a later recomposition, and a failed read in a lazy list item: 0 placements and 0 draws.
- A failed read in a lazy list item scrolled into view after the first frame: 1 placement and 0 draws. The item is composed and placed during the list's layout. With the gate turned off it is still 0 draws in 3 of 3 runs, because the screen is replaced by the problem before the item is first drawn. When the problem is shown 500 ms later, the stand-in is drawn once without the gate and never with it, in one run each. A readable item scrolled into view is drawn. Device tests assert both.
- A failed read in a `Dialog`: 2 placements and 2 draws. In a `Popup`: 1 placement and 1 draw. Both are drawn in their own window, which the gate in the screen's window does not reach. So content in a `Dialog` or `Popup` of a failing binding can show the stand-in briefly, until the problem replaces the screen (2 draws measured in a `Dialog`, 1 in a `Popup`). #189 will remove it by composing a new screen paused and applying it only when no read failed. Device tests assert the measured counts, so a change in either direction fails them.

Measured on the `medium_phone` emulator in two debug runs each. A plain `Column` composes every component.

- A binding that captures nothing: 5,000 placed components first showed in 1.75 s and 2.32 s, and a new `Screen` with one changed component showed in 302 ms and 394 ms.
- A binding that captures a value: the same screen first showed in 1.97 s and 2.12 s. Three updates of one component showed in 502, 403 and 387 ms, then 585, 593 and 425 ms. The binding body ran 5,000 times, then once more per update.
- A host test counts 10,001 binding compositions for 10,000 components and one change.

In the host test harness, a capturing binding runs its body once more on the first update after the first composition, and never again. The extra run comes from the host test harness, not from ssr-compose: a plain named composable that reads `currentRecomposeScope` shows it too, with no ssr-compose code, while a lambda that captures nothing does not. It did not happen on the emulator.

Fixed bug: the session went on running on the decoding thread when the composition's effect dispatcher does not dispatch, and then changed state that composition reads from a background thread. It showed as a device test crash, `ArrayIndexOutOfBoundsException` in `SlotWriter.moveSlotGapTo` while the test activity's composition was disposed. `createComposeRule` runs composition effects on an `UnconfinedTestDispatcher`, so an app's own UI tests of `SsrScreen` would hit it too. In a plain `ComposeView`, whose effects run on the Android main dispatcher, the session took 0 of 41 frames off the main thread without the fix, in 3 of 3 runs. `SsrScreen` now runs the session on `Dispatchers.Main.immediate` in every effect context.

- Before the fix: `SsrScreenProblemTest` crashed in 12 of 300 runs, and a plain Compose test class of the same size in 0 of 300.
- After the fix: 0 of 300.
- A device test checks that the session takes every frame on the main thread. Before the fix it failed in 3 of 3 runs, with 8 to 10 of 21 frames taken off the main thread.
- The exact runtime path from the background change to the slot table was not traced.

## R8. Callbacks on the server follow the function type pattern

Decision: a callback parameter of a bridge function has one of the types `RemoteAction0`, `RemoteAction1<A>`, `RemoteAction2<A, B>`, `RemoteAction3<A, B, C>` or `RemoteAction4<A, B, C, D>`, in the producer library. The number matches the arguments of the app's callback, as Kotlin's own `Function0`, `Function1` and so on do. Each is a function interface, so a server developer passes a lambda.

This is the owner's proposal. Two points were agreed with it:

- There is no result type. The app's callback is an ordinary function that returns at once, so it cannot wait for the server. A result from the server, if ever needed, is a different thing and gets its own type then.
- The pattern stops at four arguments. A marked composable with a longer callback is a build error in the app. Raising the limit later breaks nobody.

In the catalog a callback lists its `arguments`, the same field a slot uses.

The bridge also gives the producer, for each callback, a `Callback` with two things: the kinds of its arguments as core's `ValueKind`, the same type the catalog uses, and a conversion from the received values to the lambda's typed arguments (a choice entry to its enum, a whole number to `Int` or `Long`, a decimal to `Float` or `Double`, a list to a typed list). The producer checks the number and kinds first, then runs the conversion. A failure in either step, such as an unknown choice entry or a number that does not fit, is a refused trigger reported through `onTriggerRefused`, never a failed action. The running producer holds no catalog, so this declaration is the only thing it checks against. Only the bridge knows a choice list's entries, since the enum is generated from the catalog, so the entry check belongs in the conversion.

Checked by T025 on 2026-11-16, for `RemoteAction1<String>` only:

- FIR can create a parameter of this type. The caller passes a lambda, `onSelect = { selected -> ... }`, it converts to the function interface, and the type of `selected` is inferred as `String`.
- The Compose compiler keeps the type: the compiled signature holds `RemoteAction1` as a plain parameter type, and a required one gets the usual `checkNotNullParameter`.
- The placed node holds the action, and running it with `"clicked"` runs the caller's lambda with that text.
- The experiment's `ConsumerComponent` took the actions as an array of `RemoteAction1<String>`. That was a stand-in. T048 replaced the T025 `ConsumerComponent` signature with the map-based entry point of [R18](#r18-server-bridge-kinds-choice-enums-and-consumercomponent-t048-design), and T065 replaces its transitional `actions` map with `Callback`. `PlacedNode` in `producer/src/main/kotlin/dev/tonholo/ssrcompose/producer/internal/` is a stand-in too, for T077. T049 and T050 kept it and built `ScreenApplier` and the live composition on it.

An optional callback parameter is not supported yet. A probe made while the T025 text index still accepted an optional action showed that a caller that left the optional action out placed the actions `{onSelect=..., onCancel=null}`: the bridge passes the `null` on, and `ConsumerComponent` took a non-null array, so `null` did not mean "not given" as R8a says. The plugin refuses an optional callback with a compile error that names the component and the parameter (`BridgeCatalogRefusalTest`). T065 and T066 reopen it.

Not checked: `RemoteAction0` and `RemoteAction2` to `RemoteAction4`, `Callback` and its conversion (T060, T065 and T066 settle them).

## R8a. Optional and nullable parameters

Decision: the catalog says if a parameter can be left out, never what its default is, because only the app knows. In the bridge, a parameter that can be left out is nullable with `null` as its default, and `null` means "not given": nothing is sent and the app's own default applies. A value or choice parameter whose type is nullable in the app is marked `nullable` in the catalog and stays nullable in the bridge.

Checked by T025 on 2026-11-16, for a nullable optional text and a nullable optional slot, both with `null` as the default:

- Both are created, callers can leave them out, and the result compiles and composes. A node placed with them left out holds only the required value and has no slot node.
- A text that has a default in the app and a type that is not nullable, such as `title: String = "Welcome"`, takes the same shape in the bridge. `BridgeCompositionTest` proves that a call that leaves it out sends no value. The app does not use its own default yet: a created renderer calls `placed.text` for every parameter, so it throws when the value is absent. T055 changes that.
- The default value of a created parameter is not `null` by itself. FIR gives it a placeholder, and a caller that leaves the parameter out gets `java.lang.Throwable: Stub for declaration generated by Plugin[BridgeKey(catalogVersion=1)]` at run time. The compile still succeeds. All 5 composition tests failed with that message until the IR step set each optional parameter's default to `null`.
- Because of that, a compile-only test cannot guard defaults. A test for them has to run the created function.
- The nullable slot type is `ComposableFunction0<Unit>?` with the `@Composable` type attribute. Removing the attribute changed no test result, so the experiment does not show whether it is needed, and T048 decides. A slot created as a plain `() -> Unit` with the attribute also passes all the tests. A plain `() -> Unit` without the attribute fails the caller that puts a bridge call in the trailing lambda: `GreetingAllParametersCaller.fixture.kt:14:9 @Composable invocations can only happen from the context of a @Composable function`.

Not checked: a required parameter whose type is nullable in the app, sent as an explicit nothing; nullable types in the catalog (the catalog experiment rejected them, by its own choice).

## R8b. Slots that pass arguments

Decision: such a slot is described in the catalog and cannot be filled until remote composables exist. An optional one gets no bridge parameter. A component with a required one cannot be placed: its bridge function is created so the name resolves, every call and every reference to it is a compile error that names the composable and the slot and says that slots with arguments are not supported yet, and the function throws the same message if it is ever composed.

Consequence for the first samples: the owner's `PaymentSection` and `CameraScanner` examples both have a required slot with arguments, so the samples of this feature use components with values, callbacks and slots without arguments.

Checked by T026 on 2026-11-17, in the compile tests of `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/producer/UnplaceableComponentTest.kt` and the run test of `UnplaceableBridgeRunTimeTest.kt`, on Kotlin 2.4.21:

- The error is reported by a FIR checker of this plugin, `fir/UnplaceableBridgeCheckers.kt`, registered with `FirAdditionalCheckersExtension`. It checks function calls and callable references, and reports `BridgeDiagnostics.UNPLACEABLE_COMPONENT`, a diagnostic of this plugin in `diagnostic/BridgeDiagnostics.kt`. The message is only the chosen text: `The composable 'PaymentSection' cannot be placed from the server: its required slot 'methodPicker' receives arguments, and slots with arguments are not supported yet.` The tests assert that an error line ends with that whole text, and a run with the message cut to the last words fails 5 of the 10 tests in `UnplaceableComponentTest` and the run time test.
- The compiler does not use `@Deprecated` or any compiler-owned error for this, so no compiler words come before the text.
- The bridge function carries `@UnplaceableBridge(message)`, an annotation of the producer library with binary retention, put there by `fir/UnplaceableBridgeMarker.kt`. The checker reads the message from that annotation. It reads it the same way from the function created in the same module and from the compiled bridge of another module, so the checker has no list of components of its own.
- In the module that creates the bridge, these fail with the message: a call with a trailing lambda, a reference `::PaymentSection`, and a call with wrong arguments. The bridge name still resolves, with no unresolved reference, and a placeable component in the same catalog is not affected.
- A module that depends on the bridge module gets the same error for a call and for a reference when the plugin runs there with `mode=check`. A call to a placeable bridge from that module compiles.
- `mode=check` is a third value of `CompilerPluginMode`, next to `consumer` and `producer`, chosen by the owner. In it the plugin registers only the checker and creates no bridge. The command line gives it the same parsing and the same error text as the other two, and an unknown mode lists `check`. In `mode=producer`, the checker runs in every compilation, with or without a catalog, and only bridge creation needs a catalog: a caller compiled with `mode=producer` and no catalog still gets the error when calling an unplaceable bridge from another module, and a caller compiled with `mode=producer` and its own catalog gets the error for the other module's unplaceable bridge while its own bridges still resolve. The Gradle plugin never picks `check` yet: its `ssrCompose` block resolves only consumer or producer, and T047 adds the choice for modules that create no bridge.
- A module that runs no plugin is not stopped: the same caller, compiled with only the Compose compiler against the bridge module's output, compiles. That is the limit of a compile time check.
- The run time backstop: the body of an unplaceable bridge function is `error(message)` with the same text, made in `ir/BridgeBodyGenerator.kt`. A caller compiled without this plugin was run through a composition, and the exception it threw had that message as its root cause. The test calls the caller by reflection from a class loader over the two outputs.

Limits and not checked:

- `FirAdditionalCheckersExtension`, the checker base class, `getAnnotationWithResolvedArgumentsByClassId` and the diagnostic factories are public and compile without a warning, but the Kotlin documentation for FIR plugins (`docs/fir/fir-plugins.md`) only describes the extension point, and compiler plugins have no stability promise. Check them at each Kotlin upgrade.
- Whether `@Suppress` turns this error off: not checked. The owner chose not to try it.
- Not checked: whether the IDE shows the error; any Kotlin version other than 2.4.21; the removal of `mode=check`, which T047 decides. [R16](#named-bridges-in-the-real-plugin) found that created bridges no longer need it. The compile time the checker adds is measured in R16.
- The plugin refuses an optional slot with arguments with a compile error, so such a slot, which gets no bridge parameter, is not made yet. A component with two required slots with arguments names only the first. T076 does both.
- The plugin's `SlotOfText` kind is a required slot whose content receives one text, created as `ComposableFunction1<String, Unit>`. T076 builds the type from the arguments in the catalog.

## R8c. Where a part comes from in a project with variants

Decision: a part belongs to one compilation. Each Android variant has a catalog version, the project's by default, and the check compares the variant's bundle with the saved bundle of its version. A flavor with different marked composables sets its own version. The compiler plugin writes fragments in a folder per compilation. A marked `expect` and `actual` pair is one component.

Checked by experiment (T027), in a Kotlin Multiplatform module with an Android target and two product flavors, `free` and `paid`, on AGP 9.5.1 and Kotlin 2.4.21:

- Product flavors next to the Kotlin Multiplatform plugin only work with `android.builtInKotlin=false` and `android.newDsl=false` in `gradle.properties`. Without them AGP stops at configuration: "The 'com.android.library' (or 'com.android.application') plugin is not compatible with the 'org.jetbrains.kotlin.multiplatform' plugin since AGP 9.0", and it names these two flags as a way to "temporarily bypass this issue". With the flags AGP and Kotlin both print deprecation warnings, and AGP says the flags go away in version 10. The test fixture sets them. A real project that wants variants in a Kotlin Multiplatform module has to do the same until that changes.
- The fragments folders seen, one per variant compilation: `build/ssr/fragments/android/freeDebug` and `build/ssr/fragments/android/paidDebug`. A scratch probe, not a test, also saw `android/freeDebugUnitTest`, so test compilations get their own folders. The part step must read only the folder of the variant's own compilation. That rule is not built yet. `KmpAndroidFragmentsTest` asserts the two full paths, target segment included.
- The free folder holds `Home`, `Shared`, `Panel` and `FreeBanner`, and the paid folder holds `Home`, `Shared`, `Panel` and `PaidBanner`. A composable in `commonMain` appears in every variant's folder, as expected, because every variant compiles it.
- A marked `expect` and `actual` pair is one component in the part (R4). The `actual` must be marked too, or the compile fails (T044).
- An edit to one file leaves the part equal to a clean build's part (R4).

Probed and not covered by a test:

- The AGP plugin this repository uses, `com.android.kotlin.multiplatform.library`, has one variant only: target `android`, compilations `main` and `hostTest`, folder `ssr/fragments/android/main`. It cannot have flavors. A marked `expect` and `actual` pair compiled with it, next to a JVM target.
- A plain `com.android.library` module with product flavors and no Kotlin Multiplatform plugin runs the plugin for each variant too, with compilations `freeDebug` and `paidDebug`. Its target name is empty, so the folder has no target segment: `ssr/fragments/freeDebug`.

Not checked: the per-variant catalog version in the build setup (T092), the check of a variant's bundle against the saved bundle of its version, and what the build does when two variants share a version but have different marked composables.

## R9. One bridge per held catalog

Decision: the server project holds one saved bundle per catalog version, in `ssr/catalogs/<version>/`. Every held catalog gets its own bridge, in a package the server developer names for it in the build setup. There is no single catalog that the only bridge is created from. A composable removed in a newer catalog still exists in the bridges of the older catalogs that have it.

Rationale: versions have no order, so "newest" cannot be worked out, and the owner chose screens per catalog version (R10), which needs every version's composables to be callable. Two bridges with the same names cannot share a package, so each gets its own.

The owner decided on 2026-11-18 that there is no separate bridge module. Each server module that calls bridges holds the catalogs it needs and creates the bridges it names. A full rebuild of that module when one of its catalogs is updated is accepted. This replaces the earlier rule that bridges are created in one module and other modules depend on it.

The owner also decided that created bridge functions are internal to the module that creates them, in classes named after that module, and that the plugin finds the names a module uses by scanning the module's source folders. [R16](#r16-scale-and-build-performance) has the evidence and what is not checked yet.

Checked by T025 on 2026-11-16: two packages with the same component name in one module, created from two held catalogs (versions `1` and `2`), compile side by side and compose. Each bridge passes its own version, and a call that mixes the parameters of the two fails to compile. See R1 for the details.

The compile time and memory of 10 held catalogs of 20,000 components each are measured on the real plugin in R16. Two modules with different module names that create the same bridge compile, load on one classpath, and each runs its own bridge (`TwoModuleBridgeTest`).

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

Checked for T022 on 2026-11-16, with Gradle 9.9.1 and JDK 25. `./gradlew :core:build :catalog-model:build detekt --rerun-tasks` exits 0 with 54 tasks executed, so the build and detekt pass for every target of `core` (`jvm` and `android`) and for `catalog-model`. The dependency reports come from `./gradlew :core:dependencies :consumer:dependencies :producer:dependencies`, which exits 0. The runtime classpath of `core` for its Android target (`androidRuntimeClasspath`) and for its JVM target (`jvmRuntimeClasspath`) holds only `kotlinx-coroutines-core` 1.11.0, `kotlin-stdlib` 2.4.21 and `kotlinx-serialization-json` 1.13.0 with its core module. It has no Android, Ktor, Netty, WebSocket or OkHttp artifact. `consumer` and `producer` reach `core` as `project ':core'`, and no line of any of the three reports names `catalog-model`, Ktor, Netty, WebSocket, OkHttp or the plugin's own group. `producer` also brings in Compose runtime 1.12.2, as T003 planned. That is not a failure, because the rule covers `core` and the `catalog-model` link only.

## R12. Versions

Checked against Maven metadata on 2026-11-15: Kotlin 2.4.21 (2.5.0 is a release candidate), Android Gradle Plugin 9.5.1, Compose Multiplatform 1.12.2, kotlinx.serialization 1.13.0, kotlinx.coroutines 1.11.0. Checked by experiment: `dev.zacsweers.kctfork:core:0.14.0` works with Kotlin 2.4.21 and the Compose compiler. The Kotlin toolchain on the owner's machine is 0.13.0.

Checked again for T001 on 2026-11-15, by downloading each `maven-metadata.xml` and searching it for the exact version. The commands were `curl -s https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-compiler-embeddable/maven-metadata.xml` and the same for `org/jetbrains/compose/compose-gradle-plugin`, `dev/zacsweers/kctfork/core`, `org/jetbrains/kotlinx/kotlinx-serialization-json`, `org/jetbrains/kotlinx/kotlinx-coroutines-core`, `app/cash/turbine/turbine`, `io/ktor/ktor-server-core` and `dev/detekt/detekt-gradle-plugin`. The Android Gradle Plugin came from `curl -s https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml`, followed by `grep -c "<version>9.5.1</version>"`, which printed 1. Every pinned version is in its metadata file: Kotlin 2.4.21 (`kotlin-compiler-embeddable`, latest tag is 2.5.0-RC), Android Gradle Plugin 9.5.1, `compose-gradle-plugin` 1.12.2, `kotlinx-serialization-json` 1.13.0, `kotlinx-coroutines-core` 1.11.0, `dev.zacsweers.kctfork:core` 0.14.0, Turbine 1.2.1 (latest), Ktor 3.6.1 (latest), detekt Gradle plugin 2.0.0-alpha.6 (latest). No version had to change. The buildconfig plugin `com.github.gmazzo.buildconfig` was checked with `curl -s https://plugins.gradle.org/m2/com/github/gmazzo/buildconfig/com.github.gmazzo.buildconfig.gradle.plugin/maven-metadata.xml`: 6.1.2 is the latest release, and it is the version in the catalog.

The metadata check does not prove every artifact exists, so `./gradlew build` resolved what the modules use: Compose runtime, foundation and UI 1.12.2 (`consumer`), `kotlinx-serialization-json` 1.13.0 (`core`), `kotlin-compiler-embeddable` 2.4.21 (`compiler-plugin`), `kotlin-test`, `kotlin-test-junit`, `kotlinx-coroutines-test` and Turbine (the host test classpath of `contract-tests`), and the detekt plugin. The kctfork and Ktor entries are in the catalog but no module uses them yet, so for those only the metadata check was done.

All versions, including the JVM target 17, `minSdk` 23 and `compileSdk` 37, are in `gradle/libs.versions.toml`. The convention plugins are Plugin classes that read them through the generated catalog accessors, and the modules apply them with `alias(libs.plugins.ssrcompose.conventions.<concern>)`, so a `grep -rnE` for the six version numbers over `build-logic`, the seven modules, `gradle/`, and the root build files finds them only in `gradle/libs.versions.toml`. The Gradle wrapper version (9.9.1) is in `gradle/wrapper/gradle-wrapper.properties`, as Gradle requires.

Rechecked on 2026-11-15 for the latest stable release of every entry in the catalog, not only that it exists. Stable means no alpha, beta, release candidate or snapshot. The sources are the `maven-metadata.xml` files of Maven Central, Google Maven and the Gradle plugin portal, the release pages of the projects, and `https://services.gradle.org/versions/current` for Gradle.

| Entry | In the catalog | Latest stable | Source | Note |
|---|---|---|---|---|
| Gradle wrapper | 9.9.1 (was 9.8.2) | 9.9.1 | https://services.gradle.org/versions/current | updated |
| Kotlin: the plugins, `kotlin-compiler-embeddable`, `kotlin-test`, `kotlin-test-junit` | 2.4.21 | 2.4.21 | Maven Central, https://github.com/JetBrains/kotlin/releases | 2.5.0 is only a release candidate. The Compose compiler plugin follows Kotlin. |
| Android Gradle Plugin | 9.5.1 | 9.5.1 | https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml | 9.6.0 is only an alpha. 9.5 needs Gradle 9.6.0 or newer, JDK 17 and supports API level 37 (https://developer.android.com/build/releases/agp-9-5-0-release-notes), which fits Gradle 9.9.1 and `compileSdk` 37. |
| Compose Multiplatform (runtime, foundation, UI) | 1.12.2 | 1.12.2 | Maven Central, https://github.com/JetBrains/compose-multiplatform/releases | 1.13.0 is only a beta. |
| kotlinx.serialization | 1.13.0 | 1.13.0 | Maven Central | |
| kotlinx.coroutines (core, test) | 1.11.0 | 1.11.0 | Maven Central | |
| kctfork (`kotlin-compile-testing`) | 0.14.0 | 0.14.0 | Maven Central, https://github.com/ZacSweers/kotlin-compile-testing/releases | Its notes say it was built for Kotlin 2.4.20. It works with 2.4.21, as the experiment above found. |
| Turbine | 1.2.1 | 1.2.1 | Maven Central | |
| detekt Gradle plugin | 2.0.0-alpha.6 | none: no stable 2.x exists | Maven Central, https://github.com/detekt/detekt/releases | alpha.6 is the newest release of the 2.0 line. Every release of `dev.detekt` so far is an alpha. |
| gradle-idea-ext (`org.jetbrains.gradle.plugin.idea-ext`) | 1.4.1 | 1.4.1 | https://plugins.gradle.org/m2/gradle/plugin/org/jetbrains/gradle/plugin/idea-ext/gradle-idea-ext/maven-metadata.xml | Published only on the Gradle Plugin Portal. |
| buildconfig plugin | 6.1.2 | 6.1.2 | https://plugins.gradle.org/m2/com/github/gmazzo/buildconfig/com.github.gmazzo.buildconfig.gradle.plugin/maven-metadata.xml | |
| Ktor (six artifacts, samples only) | 3.6.1 | 3.6.1 | Maven Central | |

Nothing but the Gradle wrapper had to move, so the set stays compatible and no entry held another back. `./gradlew build`, `./gradlew detekt` and `./gradlew :contract-tests:check` pass on Gradle 9.9.1, and the dependency reports of `core`, `consumer` and `producer` are identical to the ones on 9.8.2. `./gradlew build --warning-mode all` shows no new warning: the same two as before, the incubating type-safe project accessors and the `Configuration.setVisible(boolean)` deprecation while Gradle configures `:compiler-plugin`.

Compiler plugin APIs for top-level generation are marked experimental in Kotlin, so each new Kotlin version needs the plugin's tests to pass before it is supported.

Checked: the version of the buildconfig Gradle plugin (`com.github.gmazzo.buildconfig`) is 6.1.2, the latest release on the Gradle plugin portal and the version the owner's thunderbird repository uses. T001 added it to the catalog.

## R13. The editor

Developers must turn off `kotlin.k2.only.bundled.compiler.plugins.enabled` in the IDE registry to see created declarations, as the owner said. It is a documented setup step.

The owner tried named bridges in IntelliJ IDEA 2026.3, and [R16](#named-bridges-in-the-real-plugin) has the results. Not checked in the IDE: the enum workaround (R3) and a catalog change while the plugin is loaded. Known risks from reading the code: the index is read once per plugin load, so a catalog change is not seen until reload, and created declarations have no source to navigate to.

## R14. What carries over from the earlier draft

Still valid, and designed the same way: the handshake values and their validation limits, a neutral data model so no wire encoding is baked into the contract, a replaceable codec and connection with no transport in the libraries, one live composition per session with one global write observer shared by the process (the Compose runtime still keeps one apply observer per session, see below), an observer for reporting, and a load test for the bounds.

The cost of a state write grows with the number of live sessions. Each session has its own Recomposer, and the Compose runtime registers one apply observer in each. A write to state that a composition read is added to every Recomposer, and each one wakes. Measured on the first version of this change, before the snapshot waiter moved to its own dispatcher, so the numbers describe that code with one writer and every other screen idle, one write awaited until its new content arrived: 0.6 ms at 100 screens, 3.9 ms at 1,000, 5.2 ms at 3,000 and 5.9 ms at 6,000. No CPU was measured and 10,000 screens were not run. Inferred, not measured: this is fine at a low write rate. Inferred, not measured: the ceiling of 100 callbacks per second per session in R10b allows up to 1,000,000 writes per second at 10,000 sessions, which this design cannot serve. No realistic write rate is stated yet, so the design is not proven for 10,000 sessions. T059 and T103 now drive writes at a stated rate and report latency and CPU, so the load tests decide. The fallback, if they show trouble, is a few shared Recomposers, one per dispatcher thread, with the failure of one screen caught per screen so it does not stop its neighbours.

Validation limits, all configurable: introduction frame at most 8 KB; density above 0 and at most 10; screen width and height 1 to 100,000 pixels; language tag at most 35 characters; app version, variant and flavor at most 64 characters each with no control characters, and catalog version the folder label rule of [contracts/catalog-bundle.md](contracts/catalog-bundle.md#manifest-fields); callback arguments at most 1 megabyte in total; callbacks at most 100 per second per session. On the build side: a manifest and each part at most 16 MiB, checked before reading; part names at most 64 characters, lowercase letters, digits and hyphens, and never a Windows device name.

## R15. Decisions that become ADRs

- The compiler creates one bridge per held catalog, with the plugin order rule (R1, R2, R9).
- The catalog is a bundle: fragments per source file, a part per module and a manifest (R4).
- Build-tool plugins over one shared tooling library (R5).
- Catalogs are fetched at sync and pinned (R6).
- Module layout and targets, with the build-time `catalog-model` library (R11, R17).
- Screens per catalog version, and the guard (R10).
- Text the user reads (R19), in [ADR 0011](../../docs/adr/0011-replaceable-text-through-a-string-manager.md), and the look of the problem message (R19), in [ADR 0012](../../docs/adr/0012-problem-message-style-parameters.md).

## R16. Scale and build performance

Decision: there is no hard limit on components or parameters. The library is tested at 20,000 components with about 80,000 parameters, with the mix real apps have (most components with 2 to 4 parameters, a few with 15 or more). The owner measured three real apps with 621, 751 and 1,085 composables, the last one an MVP. The reader and writer are protected by a size cap on the manifest and on each part, 16 MiB each by default ([`SizeCap`](../../catalog-model/src/main/kotlin/dev/tonholo/ssrcompose/catalog/bundle/SizeCap.kt)), checked before parsing or past the cap on stream read. Measured on JetBrains Runtime 25.0.4 with its default garbage collector, a 16 MiB worst-shape part read passes at 288 MiB of heap and fails at 272 MiB (the verification task gives it 320 MiB), a 16 MiB worst-shape manifest read passes at 144 MiB, a 16 MiB part write passes at 72 MiB, and a 16 MiB stream read through `SizeCap.read` passes at 40 MiB and fails at 32 MiB. A caller with less heap sets a smaller cap.

The compiler never parses the catalog's JSON. A cacheable build step, `ssrCatalogIndex`, turns each saved catalog into an index that the compiler plugin reads. The index is internal and is never shared between builds or committed. Its format is binary, chosen by the owner on 2026-11-18 from the measurements below. The compiler plugin reads an index of at most 256 MiB. The index step uses the same cap, `CatalogIndexReader.DEFAULT_SIZE_CAP`, and fails with a clear error instead of writing an index the compiler would refuse.

Build performance targets, with the 20,000 component catalog:

- A build with nothing changed does no catalog work.
- A change in one app module produces again only that module's part and the manifest.
- Build outputs hold no absolute path and no timestamp, so the build cache works across machines.
- The catalog adds at most 2 seconds to a clean compile of a server module that calls 20 of its composables. The owner kept this figure on 2026-11-18.

### What the scale gate decided

T031a ran on 2026-11-17 and 2026-11-18. The owner decided these points on 2026-11-18:

- Bridges are created only for the components a server module consumes. SC-016 stays at 2 seconds.
- A full rebuild of the module that holds a catalog, when that catalog is updated, is accepted. There is no separate bridge module (R9).
- The index format is binary.
- Bridge functions are spread over several classes, and each component has a stable class. The rule is a fixed 256 classes per catalog. A component's class comes from the SHA-256 of its name encoded as UTF-8: read the first 4 bytes of the hash as a big endian signed 32-bit integer, then take `Math.floorMod(value, 256)`, which is always 0 to 255. Adding or removing a component moves no other component. A class is created only for a bucket that holds a named component. One class holds at most 150 functions, and a bridge over that limit is a clear compile error. The owner first set 500. Measurement lowered it to 150, because 500 functions at the largest parameter count do not fit in one class ([Class file limits of a created bridge](#class-file-limits-of-a-created-bridge)).
- The catalog has no limit on parameters, but the JVM allows at most 255 parameter units per method, and the Compose compiler's extra parameters use some of them. A created bridge has at most 118 parameters. A component with more is a clear compile error that names the component, and tests cover the largest count that compiles and one parameter more.
- The plugin finds the names a module uses by scanning the module's source folders on every compile. The build passes the folders as an internal compiler option. A name list written by a build step was turned down, because it would be a compile input and a change to a compile input rebuilds the whole module. Scanning only the files the compiler receives was turned down, because each edit then recompiles every other file that calls a bridge.
- Created bridge functions are internal to the module that creates them, in classes named after that module. Two modules with different module names that create the same bridge function then do not clash, and a module cannot expose a created bridge function to another module. This decision covers functions only. Enum classes are open, as the next section says.

### Enum classes of one catalog in two modules

Each bridge will also have one enum class per choice list. An enum's JVM name is its package and its name, so internal visibility and class names per module do not separate two copies.

The owner chose on 2026-11-18: each module gets its own enums, in a package or an outer class named after the module. The real plugin creates no enums yet. T048 builds this option and proves it on created enums. The full T048 design, including the subpackage choice, test reuse, import shape, module name rules, value kind mapping, `ConsumerComponent` signature and JVM limit probes, is in [R18](#r18-server-bridge-kinds-choice-enums-and-consumercomponent-t048-design) and [ADR 0010](../../docs/adr/0010-per-module-choice-enums-in-bridge-subpackage.md).

The evidence comes from hand-written enums in a throwaway multi-project Gradle build, Kotlin 2.4.21 and Gradle 9.9.1, on 2026-11-18. It is not committed:

- Identical entries do not give identical bytes. `Tone.class` differs between two modules only by the module name in its Kotlin metadata. With the same module name forced on both, the SHA-256 of the two files is the same.
- With identical entries, a classpath that holds both modules runs, and a module that depends on the other and declares its own copy compiles and runs.
- A merged jar depends on its duplicate strategy. `DuplicatesStrategy.FAIL` stops the build. `EXCLUDE` and `INCLUDE` build a jar that runs.
- With different entries, every compile passes. The first copy on the classpath wins, and the other module fails at run time with `NoSuchFieldError: Class app.bridge.Tone does not have member field 'app.bridge.Tone Third'`.

Per-module enums are the only option that cannot fail silently at run time, and they keep the decision that there is no separate bridge module. The cost is that the import depends on the module name. Keeping one enum per choice list was turned down because of the run time failure above. Creating the enums in one shared place was turned down because it partly reverses the decision that there is no separate bridge module.

### Who delivers what

T031a is delivered in three pull requests, and T031a is ticked with the third.

1. The first holds the experiments and this record. It changes no production code.
2. The second delivers T116 and T117 (the binary index reader and writer, and the index step), deletes the T025 format and adds the class rule of T048 to the real plugin.
3. The third delivered the rest of T048 that T031a needs: creating only the named bridges, the source folder scan, internal bridge functions and class names per module. The limits and the unsupported parameter errors now apply only to components the module names. The real plugin creates no enums yet. The owner chose the enum option from a test of hand-written enums. This pull request also measured the real plugin again and ticked T031a. T048 stays open for the value, choice and list parameters it still owes, and its work on choice parameters builds the chosen enum option and proves it on created enums.

The first pull request added temporary pieces. The second removed the T025 index text and the candidate index formats. The third removed the pieces that were left, as this table says:

| Temporary piece | Pull request | What happens to it |
|---|---|---|
| `ExperimentBridgeRegistrar.kt` and `SourceScanBridgePlugin.kt` | Third | Deleted, the real plugin replaces them |
| `compiler-plugin/src/namedOnlyProbe/`, the `namedOnlyProbeJar` task and the `namedOnlyProbe` configuration in `compiler-plugin/build.gradle.kts` | Third | Deleted |
| The `namedOnlyProbe` and `namedOnlyProbeClasspath` configurations and the probe jar wiring of `functionalScaleTest` in `gradle-plugin/build.gradle.kts` | Third | Deleted |
| `NamedOnlyProbeFixture.kt`, `NamedOnlyIncrementalProbeTest.kt` and `TwoModuleBridgeTest.kt` | Third | The first two are replaced by `NamedBridgeFixture.kt` and `NamedBridgeIncrementalTest.kt`, and `TwoModuleBridgeTest.kt` is rewritten. All three run the real plugin |
| `BridgeModulesFixture.kt` and the two bridge module tests in `CatalogRebuildScopeTest.kt` | Third | Deleted, because the owner did not choose a separate bridge module |
| The prototype cases in `BridgeScaleScenario.kt` and `CompileCase.kt` | Third | Moved to the real plugin, so the scale numbers measure it |

### Measurements

The numbers in the tables below measure the real plugin, unless a row says otherwise. Each compile runs in process with kotlin-compile-testing on a warm JVM and targets JVM 17, so a cold Gradle compile will differ. T115 measures the finished build.

The compile numbers use `ParameterPalette.BridgeExperiment`: a text, a callback of one text and a plain slot. They hold no number, flag, choice or list parameter, and no enum class is created, so later kinds can cost more. The index format numbers use the full mix of parameters.

Machine: Apple M3 Pro, 12 cores, 36 GiB of memory, macOS 26.6.2, JetBrains Runtime 25.0.4, Gradle 9.9.1, Kotlin 2.4.21. The test heap was 2 GiB for `tooling` and 12 GiB for `compiler-plugin`. Other work was running on the machine. During the `:compiler-plugin:scaleTest` run of 2026-11-18 that the tables below come from, the one minute load average was between 8.1 and 31.4, with a mean of 17.4 over 52 samples taken every 20 seconds.

Memory method: allocated memory is `ThreadMXBean.getThreadAllocatedBytes` on the measuring thread. Peak heap is the sum of the peak usage of each heap pool, reset after a garbage collection right before the run. Pools peak at different times, so the sum can be higher than the real peak.

Commands:

```sh
./gradlew :tooling:scaleTest --configuration-cache
./gradlew :compiler-plugin:scaleTest --configuration-cache
./gradlew :gradle-plugin:functionalScaleTest
```

`check` runs none of them.

The catalog has 20,000 components and about 80,000 parameters over 50 parts. In the `:tooling:scaleTest` run of 2026-11-17, reading and validating its bundle took a median of 176 ms (174 to 178 ms over 7 runs after 5 warmups) and allocated 211 MiB. The bundle holds 12,191,891 bytes, and its largest file holds 422,345 bytes, against the cap of 16 MiB.

Index formats, as the median of 7 runs after 5 warmups. The Lines row is history: that format was deleted, and only revision `a4d606a` can reproduce it. The Binary row is the production reader and writer, measured on 2026-11-18.

| Format | Size in bytes | Build from the bundle | Open and read 20 components | Open and read all components |
|---|---|---|---|---|
| Lines (text, parsed in full on open) | 3,121,146 | 191 ms (187 to 197), 256 MiB | 27.2 ms (26.8 to 27.4), 51 MiB | 27.8 ms (26.8 to 28.5), 51 MiB |
| Binary (string table and offsets, parameters decoded on demand) | 1,315,978 | 180.3 ms (177.5 to 187.2), 227 MiB | 3.2 ms (2.7 to 3.5), 10.4 MiB | 10.2 ms (9.7 to 11.0), 22.2 MiB |

Compiling a server module that calls 20 components. Each case was measured in rotation: 5 warmup rounds of every case, then 12 counted rounds, each starting with the next case. The time is the median of the 12 rounds. The test first checks that exactly the 20 called bridges exist.

| Case | Time | Range | Allocated | Peak heap |
|---|---|---|---|---|
| No catalog, 20 hand-written stand-ins | 107.5 ms | 87.1 to 125.8 ms | 116 MiB | 165 MiB |
| Catalog of only the 20 called components | 126.9 ms | 114.6 to 161.9 ms | 130 MiB | 171 MiB |
| One full catalog | 135.4 ms | 120.7 to 172.9 ms | 142 MiB | 162 MiB |
| Ten full catalogs | 469.2 ms | 444.9 to 571.8 ms | 462 MiB | 346 MiB |

These compiles give the compiler one file and pass no source folder, so they hold almost no scan cost. Against the stand-ins, one full catalog adds about 28 ms and 26 MiB of allocation, and ten full catalogs add about 362 ms and 346 MiB. These are differences of medians from one run, so they are rough. [What holds SC-016](#what-holds-sc-016) uses the large module below, which does scan.

A server module of 3,000 source files of 40 functions each. A clean compile gives the compiler every file. An edited file gives the compiler one file and still scans the whole source folder. The clean cases were measured in rotation, as the median of 6 rounds after 2 warmup rounds. The edited file cases were measured in rotation too, as the median of 7 rounds after 3 warmup rounds.

| Case | Time | Range | Allocated |
|---|---|---|---|
| Clean, stand-ins | 24,464 ms | 19,937 to 33,121 ms | 28,058 MiB |
| Clean, one full catalog | 25,125 ms | 20,955 to 26,863 ms | 28,304 MiB |
| Clean, ten full catalogs | 23,589 ms | 20,880 to 30,285 ms | 28,623 MiB |
| One edited file, stand-ins | 114.8 ms | 101.2 to 177.9 ms | 115 MiB |
| One edited file, one full catalog | 310.9 ms | 275.8 to 352.6 ms | 332 MiB |
| One edited file, ten full catalogs | 752.9 ms | 715.1 to 834.0 ms | 651 MiB |

Scanning the 3,000 files alone takes a median of 114.9 ms (112.1 to 116.8 ms) with the names of one catalog and 115.3 ms (112.4 to 149.2 ms) with 200,000 names, and allocates 190 MiB in both. The scan keeps nothing between compiles, so the Kotlin daemon holds no state from it.

#### Clean compile time cannot resolve the catalog cost

Clean wall clock time on this machine cannot resolve the cost of the catalog, in either direction, so it neither proves nor disproves the 2 second limit.

The first reading ran the clean cases in a fixed order and showed about 3.9 s added for one catalog (20,606 ms against 16,663 ms) and about 2.4 s for ten. Ten catalogs cannot cost less than one, so the cases moved to rotation, where no case always follows the same one. Rotation did not settle it. The independent review ran the comparison four more times, and this table adds the run of the tables above. Each cell is the time added against the stand-ins:

| Run | One full catalog | Ten full catalogs |
|---|---|---|
| Review, rotation, machine busy | +1,147 ms | -5,303 ms |
| Review, the old fixed order | +684 ms | -262 ms |
| Review, the order reversed | +2,596 ms | +12,417 ms |
| Review, rotation again | -608 ms | +83 ms |
| The tables above, rotation | +661 ms | -875 ms |

#### What holds SC-016

The plugin's own work is timed directly, inside the clean compile of the 3,000 file module. `PluginStepScaleTest` runs the production steps through a test-only registrar and adds up the time and the allocation of each step on the compiling thread. Production code holds no timing code, and no test asserts on a time. Work nested in a step counts in that step too, so a total can be too high. The harness does not install the refusal checks, which run only when a module names a component that cannot get a bridge. The measured module names none, so they would not have run here. For a module with refused components, the totals leave that work out and are a lower bound for it. Each cell is the median of 6 rounds, with the range in brackets:

| Step | One full catalog | Allocated | Ten full catalogs | Allocated |
|---|---|---|---|---|
| Read the indexes | 5.8 ms (4.0 to 12.0) | 11.3 MiB | 45.2 ms (42.3 to 94.0) | 105.3 MiB |
| Scan the sources | 484.0 ms (151.2 to 792.6) | 190.5 MiB | 450.2 ms (147.6 to 773.9) | 190.5 MiB |
| Decode the named components | 0.5 ms (0.4 to 4.3) | 0.0 MiB | 3.5 ms (2.0 to 5.6) | 0.5 MiB |
| Declare the bridges | 0.3 ms (0.2 to 0.4) | 0.2 MiB | 0.6 ms (0.5 to 1.0) | 1.5 MiB |
| Create the bridge bodies | 82.5 ms (33.6 to 276.8) | 8.2 MiB | 45.1 ms (28.9 to 120.9) | 9.0 MiB |
| Check calls and references | 45.6 ms (40.3 to 56.5) | 14.9 MiB | 44.0 ms (43.0 to 44.7) | 14.9 MiB |
| Total of each round | 596.8 ms (342.6 to 1,138.9) | 225.0 MiB | 608.3 ms (291.1 to 1,039.5) | 321.6 MiB |

The steps do not hold the work the compiler does afterwards on the created functions, such as the Compose compiler and code generation. The edited file rows do, because that compile holds little else. They add 196 ms for one catalog and 638 ms for ten in the tables above, and 173 ms and 521 ms in the run of the independent review. Allocation does not depend on the load of the machine. A clean compile allocates 246 MiB more with one catalog and 566 MiB more with ten, on about 28,058 MiB. The review measured 251 MiB and 572 MiB on about 27,800 MiB.

SC-016 holds on these numbers. Adding the slowest round of the steps to the edited file row gives about 1,335 ms for one catalog and 1,678 ms for ten, and that sum counts the scan twice. The numbers come from a warm JVM in process on a busy machine. The scan took longer inside the compile than alone, and the reason was not looked into. A cold Gradle compile is not measured here, and T115 measures it.

The check of bridge calls (`mode=check`) on a module with 10,000 calls and no bridge took a median of 473.1 ms (453.8 to 524.5 ms) against 468.1 ms (435.4 to 630.7 ms) without the plugin, and allocated 592 MiB against 591 MiB. The ranges overlap.

The next two rows are history. A test prototype created every function of the catalog on 2026-11-17, with JVM target 1.8 and the T025 index. The real plugin cannot create every function, so nothing can reproduce them. They are the reason the owner chose named-only creation.

| Case | Time | Range | Allocated | Peak heap |
|---|---|---|---|---|
| One full catalog, every function created (prototype) | 20,519 ms | 20,304 to 21,778 ms | 16,172 MiB | 2,173 MiB |
| Ten full catalogs, every function created (prototype) | 258,427 ms | 242,133 to 442,571 ms | 160,781 MiB | 14,936 MiB (above the 12 GiB heap limit, so an overestimate) |

### Named bridges in the real plugin

The build passes the source folders of a compilation to the compiler plugin as the internal option `sourceFolder`. They come from `KotlinCompilation.allKotlinSourceSets` and the `kotlin.srcDirs` of each source set. The option is not a compile input, so it adds no absolute path to the build cache key.

A compile task can leave out files of those folders, for example with `kotlin.exclude`. Right before the compile runs, the build lists those files, and the scan skips them, so a bridge never depends on a file the compile task does not track. The internal option `excludedSources` holds the path of that list, and the list is not a compile input. A file the compiler receives always counts, whatever the list says, so an old list cannot hide it.

The plugin scans those folders and the files the compiler receives on every compile, and keeps only the catalog components whose names appear as whole identifiers. Only those components are decoded and checked against the limits. The scan reads readable regular `.kt` and `.kts` files and follows folder links. A path it cannot read counts as holding no name, and it reads a file the compiler receives inside a source folder only once. It can create too many bridges, but it does not miss a name written in a scanned file. `NamedBridgeCreationTest` covers aliased imports, star imports, fully qualified calls, callable references, backticked names and two held catalogs.

A name can match by accident, in a comment, a string or an unrelated class. If that component cannot be created because of an unsupported parameter, the parameter limit or the class limit, the plugin declares nothing for it. Code that only mentions the name compiles, and a user class or function with the same name does not clash, even in the bridge package. An import of the component, and a call or callable reference that Kotlin cannot resolve, fail with the error that names the component. Kotlin prints its own `Unresolved reference` error next to it.

A mention counts toward the class limit, because the scan cannot tell a mention from a call. When more than 150 named components fall in one class, none of them gets a bridge, and a real use of any of them fails the build with the class limit error, even if some of the names are only mentions. A build that only mentions them passes. The owner accepted this limit on 2026-11-18. It is rare: the fullest class of the 20,000 component catalog holds 96 components, so a module of that catalog stays under the limit even if it names every component.

Created functions are internal. A public inline function cannot call one, and Kotlin reports `Public-API inline function cannot access non-public-API function`. An internal inline function can. A test compilation creates its own bridges, in its own classes, next to those of main.

A bridge class is named `Bridge<bucket>_<module>_<hash>Kt`. `<module>` is the module name with every character outside `A-Z`, `a-z`, `0-9` and `_` replaced by `_`, cut to 64 characters. `<hash>` is the first 8 bytes of the SHA-256 of the module name in hexadecimal, so `my-app` and `my_app` differ. The longest class file name is 99 bytes, within the 255 bytes file systems allow. The name holds no path. Gradle builds the module name from project names, so it is the same on every machine that uses the same root project name.

An incremental build keeps every class file equal to a clean build when a first call is added or a last call is removed, by adding or deleting a file and by editing an existing file, when one of two callers is removed, and when an unrelated file is edited (`NamedBridgeIncrementalTest`). After the last call to a component is removed, `META-INF/<module>.kotlin_module` still lists the deleted bridge class until the next clean build. The owner accepted this on 2026-11-18. Only a test compilation was tried as a reader of that file, and it passed.

Generated sources are scanned. A task-generated folder and the output of a real KSP processor (KSP 2.3.14, a test dependency only) both create the bridges they call, and the bridge stays when another file is edited. In Kotlin Multiplatform, a jvm compilation scans `commonMain` and `jvmMain`, and a bridge that both call is declared once. A shared metadata compilation with a `js` target passes when common code mentions a component. Gradle prints `KMP Dependencies Resolution Failure` for that target, because `core` publishes no `js` variant, and the compile still succeeds.

`mode=check` is no longer needed for bridges created by this plugin, which is the question T047 asks. Another module cannot reach an internal bridge, because Kotlin refuses the call (`TwoModuleBridgeTest`), and a test compilation of a server module runs `mode=producer` itself. The mode stays until T047 decides.

The owner tested the IDE on IntelliJ IDEA 2026.3 (IU-263.7307.200) on 2026-11-18. Command line builds are not affected by any of these results:

- A component named for the first time stays unresolved until a Gradle sync. Reopening the file does not help. The IDE keeps the plugin's registered extensions until the Kotlin facet changes (`KtCompilerPluginsCache`), and the scan runs at registration.
- After a sync, a bridge removed from the code still resolves until the next sync.
- A name that is not in the catalog stays red, which is correct.
- Completion offers a declared bridge under a star import of the bridge package. It does not offer it without that import, and auto-import does not offer it. JetBrains tracks these as KTIJ-27574 and KTIJ-29912.
- Quick Documentation is empty for a bridge, and the IDE symbol search does not find it.

A probe on the same IDE and date tried a possible follow-up, with a throwaway copy of the plugin that is not part of this pull request. The copy declared every catalog component when the compiler configuration had no module name, which is how IntelliJ's `KtCompilerPluginsCache` configures a plugin. The catalog held 20,003 components:

- Components resolved with no Gradle sync, under a star import and under explicit imports.
- A name not in the catalog stayed red, and a wrong argument gave the normal parameter errors.
- Completion offered bridges under a star import of the bridge package, with no noticeable lag according to the owner. The lag was not measured.
- Loading the catalog in the IDE took 28 to 156 ms.
- A command line compile of the same code still created only the 5 named bridges.
- Hover and auto-import were not changed by this.
- The missing module name is an observed behaviour of IntelliJ, not a documented contract.

This pull request does not include that behaviour. T119 and #182 track it as a follow-up.

### What the experiments showed

- In the prototype, 20,000 components could not compile with every function in one class. The compile failed with `Class too large`. The real plugin spreads the functions it creates over 256 classes by the class rule.
- Kotlin 2.4.21 creates every top-level function a plugin declares (`FrontendFilesForPluginsGenerationPipelinePhase`), so the only way to create fewer is to declare fewer names.
- With 256 classes and the calculation above, the fullest class holds 96 of the 20,000 components.
- A change to the index alone rebuilds the whole module that holds it. Gradle prints `The input changes require a full rebuild for incremental task`. A source edit in the same module stays incremental: Gradle prints no such line, and the class of a file that was not edited is not written again (`CatalogRebuildScopeTest`).
- In the prototype, with a separate bridge module, a change to a component nobody calls recompiled no class of the calling module. The owner did not choose this layout, and its tests are deleted.
- When a change does not touch a component the caller uses, the caller's class files stay byte for byte the same (`BridgeScaleTest`).
- Two modules with different module names that create one bridge write classes with different names, and a module without its own bridge cannot call the bridge of another (`TwoModuleBridgeTest`).

### Class file limits of a created bridge

The second pull request measured every limit of the class file format in [chapter 4 of the JVM specification, Java SE 17](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.11), the project's JVM target. Kotlin 2.4.21 and the Compose compiler of that version wrote the classes, for JVM target 17, and `BridgeLimitsTest` checks that every class file has version 61. The compile tests used JVM target 1.8, class file version 52, until the review of this pull request. Moving to 17 changed no number in the "Real plugin" column, no function or parameter boundary and no method size below. The limits live in `BridgeLimits.kt`.

Where the numbers come from:

- The "Real plugin" column comes from the committed test `given the widest class at the function limit` in `BridgeLimitsTest`, which prints one `SCALE` line per shape. Run it with `./gradlew :compiler-plugin:scaleTest --tests "*widest class*"`.
- `BridgeLimitsTest` also proves 118 parameters load, 119 optional `Long` parameters fail to load, 150 functions compile in one class and 151 are a clear compile error.
- These numbers come from probes that are not committed, and no committed test repeats them: the whole "Hand-written twin" column, the function boundaries of 178 and 177, the stops at 120 required wide and 222 one-slot parameters, the 255 character name runs, and the table of method bytes per parameter shape. All of them were measured on JVM target 17.

The real plugin was measured at its largest classes: 150 functions of 118 parameters with a different 64 character name each, in seven shapes, each with `sourceInformation` off and on. A catalog cannot hold a longer name. The widest constant pool comes from required slots with a text argument and `sourceInformation=true`. The longest method comes from required actions and `sourceInformation=true`. The plugin cannot create `Long` or `Double` yet, so a hand-written `@Composable` twin of the worst shape was measured too: 150 functions of 118 optional `Long` parameters, and the same with `Double`, with different 255 character function and parameter names. Each function calls one composable per parameter. `Long` and `Double` gave the same numbers, on JVM target 1.8 and on 17. An earlier probe of the twin, whose exact source was not kept, reported 55,266 constant pool entries and 11,569 code bytes. Those two numbers could not be reproduced, so the table holds the numbers of the source described here.

| Limit | JVM maximum | Real plugin | Hand-written twin | How it stays safe |
|---|---|---|---|---|
| Constant pool entries per class | 65,534, because `constant_pool_count` is at most 65,535 and counts one extra slot ([section 4.1](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.1)) | 55,509 | 37,544 | At most 150 functions per class. The widest shape uses 85% of the pool |
| Methods per class, with synthetic methods | 65,535 | 300 | 302 | Each function adds one restart lambda. No `$default` method is created |
| Fields per class | 65,535 | 0 | 0 | The plugin creates no field |
| Direct superinterfaces | 65,535 | 0 | 0 | The plugin adds none |
| `max_locals` | 65,535 | 148 | 265 | Follows the parameter limit |
| `max_stack` | 65,535 | 135 | 253 | Follows the parameter limit |
| Parameter units per method | 255 | 136 | 254 | At most 118 parameters. The widest method is the synthetic restart lambda |
| `code_length` per method | 65,535, with a ceiling of 32,767 here | 9,772 | 10,519 | The ceiling is half the limit, because tools can add code later |
| Bytes of one `CONSTANT_Utf8_info` | 65,535 | 65,535 | 65,534 | The longest entry is a chunk of Kotlin metadata, which the compiler splits. Names are capped below |
| Array dimensions | 255 | 0 | 0 | No created signature has an array type |

A metadata entry of exactly 65,535 bytes is safe. The length field of `CONSTANT_Utf8_info` is two bytes, so [section 4.4.7 of the JVM specification](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.4.7) allows 65,535 bytes. More metadata cannot make one entry longer: `bytesToStrings` in `utfEncoding.kt` of Kotlin 2.4.21 starts a new string once the current one reaches 65,534 bytes, and one more character adds at most 2 bytes. For all seven shapes, the committed test prints the longest entry of the class next to the longest chunk of its `@Metadata` `d1` value, checks that the JVM loads the class, and checks that a second compile resolves a created function from that metadata.

The parameter limit comes from the worst shape. All optional `Long` or `Double` parameters compile and load at 118. At 119 the compile succeeds and the class fails to load with `ClassFormatError: Too many arguments in method signature`, so the limit has to be a compile error. Required wide parameters stop at 120, and one-slot kinds, which are all the plugin creates today, stop at 222.

The function limit comes from the constant pool. At 118 optional text parameters with a different 64 character name in every function and `sourceInformation` off, 178 functions compile and load, and 179 fail with `Class too large`. The widest shape, required slots with a text argument and `sourceInformation=true`, uses 55,509 of the 65,534 entries at 150 functions. It compiles and loads at 177 functions and fails with `Class too large` at 178. Shared parameter names reach 500, but a catalog can hold different names, so 500 is not safe. The limit is 150, which leaves room for the extra constants of the kinds T048 adds. 150 functions also pass with 255 character names on the wide twin.

Largest method of one created bridge, in bytes after the Compose compiler has run:

| Parameter shape | 1 parameter | 118 parameters | Bytes per parameter |
|---|---|---|---|
| Optional text | 204 | 9,581 | 80.1 |
| Required text | 179 | 7,078 | 59.0 |
| Required callback of one text | 196 | 9,530 | 79.8 |
| Optional slot | 204 | 9,582 | 80.1 |
| Required slot | 179 | 7,080 | 59.0 |
| Required slot with one text argument, which only throws | 99 | 1,260 | 9.9 |
| A mix of the first four | 204 | 8,831 | 73.7 |

Names and text stay inside the limits by design:

- A component, parameter or choice list name has at most 64 characters, so at most 192 bytes. A longer name in an index is a clear compile error, tested at 65,535 and 65,536 bytes of multibyte text.
- A catalog version has at most 64 characters. A longer version option is a clear compile error.
- A bridge package has at most 255 bytes, which also keeps each of its folders within the 255 bytes most file systems allow for one file name. A longer package is a clear compile error.
- A bridge package is made of Kotlin identifiers separated by dots. A segment may be a hard keyword, as in `in.acme.bridge`, and a caller imports it with backticks. Anything else is a clear compile error that names the catalog version.
- A bridge package cannot be `java` or sit inside it, because the JVM refuses to define those classes. No segment can be a Windows device name (`CON`, `PRN`, `AUX`, `NUL`, `COM1` to `COM9`, `LPT1` to `LPT9`) in any case, because Windows cannot create that folder. Each is a clear compile error.
- Each held catalog needs its own bridge package, because the function limit is counted per class. Packages are compared without regard to case, because `app.bridge` and `app.Bridge` write the same class files on a case-insensitive disk. The comparison folds through upper case and then lower case, so `app.σ` and `app.ς` also clash. Two catalogs in one package are a clear compile error that names both versions and each spelling of the package.
- The version inside an index must match the version of its catalog option. A difference is a clear compile error that names both.
- A created class file name is at most 99 bytes, whatever the component or the module is called ([Named bridges in the real plugin](#named-bridges-in-the-real-plugin)).
- The longest renderer class file, `__SsrCompose_<Name>Renderer__Kt.class`, has 223 bytes for a name of 64 three-byte characters.

T048 must measure these again when it adds a kind, and before it adds enums:

- Both limits, because a new kind can add slots and constants.
- The method size per parameter of each new kind, against the ceiling of 32,767 bytes.
- Array dimensions, for list parameters.
- For enums: a choice list becomes an enum class, and its static initializer is one method, so a long choice list can reach the `code_length` limit and the constant pool limit.

Not checked:

- Enum classes created by the plugin. The option is chosen from hand-written enums, and T048 proves it on created enums.
- A created bridge with a `Long` or `Double` parameter. The plugin cannot create one yet, so the wide numbers above come from a hand-written twin, and T048 measures the real bridge.
- Any IDE other than IntelliJ IDEA 2026.3 (IU-263.7307.200).
- A file that stopped being excluded in the IDE. Only a Gradle compile rewrites the list of excluded files, so the IDE can keep skipping that file until the next compile. This comes from reading the code and was not tried in the IDE.
- Two modules with the same module name on one classpath. They write the same bridge class name for a component, so one class hides the other.
- Any reader of `META-INF/<module>.kotlin_module` other than a test compilation, while the file still lists a deleted bridge class.
- Source files added directly to a compile task and not to a source set. A clean compile sees them, because the compiler receives them, but an incremental compile that does not recompile them does not scan them.
- A server module with an Android target. The bridge body needs the producer library, which runs on a plain JVM only.
- A cold Gradle compile, a shared remote build cache and the finished build, which T114 and T115 measure.

## R17. The catalog is build time only

Decision: the owner said "We don't need the catalog at the runtime, as we will use it only during the build time to generate the types." So the catalog model, its validation, the manifest and part reader and writer, the fingerprint and the index live in a JVM library, `catalog-model`, that `tooling` and `compiler-plugin` depend on. `gradle-plugin` and `toolchain-plugin` depend on it too, only for the compiler plugin's id and option names. `core`, `consumer` and `producer` never depend on it. `core` keeps `CatalogVersion` (a label), `ValueKind` (the kinds of values), the two annotations, the handshake, the messages, the connection and `DataValue`. `catalog-model` depends on `core` for `CatalogVersion` and `ValueKind` only, so each has one definition. `ValueKind` is in `core` because the producer needs it at run time to declare and check callback arguments, and `catalog-model` needs it for parameters; one list of kinds serves both, and neither module defines its own.

Rationale: principle II asks for one definition of everything both sides agree on. The catalog format still has one definition, in `catalog-model`. Keeping it in `core` would put a JSON reader, hashing and validation in every app and every server at run time, where nothing uses them. The plan records this in Complexity Tracking.

Consequences: the running producer cannot look anything up in a catalog. It checks the session's catalog version against the versions it has screens for (R10), and callback arguments against the kinds and the conversion in the bridge's `Callback` (R8).

Catalog code written in `core` before this decision is moved into `catalog-model` by T113, except `ValueKind`, which stays in `core` and moves from `core/catalog/` to `core/data/`.

## R18. Server bridge kinds, choice enums and ConsumerComponent (T048 design)

Complete design for T048 (issue #52), settled on 2026-11-19 and approved by the owner on 2026-11-19 with the `@InternalSsrComposeApi` opt-in annotation for generated-code entry points. It covers value, choice and list bridge parameters, per-module choice enums, the final `ConsumerComponent` contract, the opt-in boundary, and JVM 17 class file limits. The architecture decision for enum placement lives in [ADR 0010](../../docs/adr/0010-per-module-choice-enums-in-bridge-subpackage.md) (Accepted).

### 1. Boundaries

- `core`: unchanged. `DataValue`, `ValueKind`, `CatalogVersion` and `PlacedComponent` already provide the exact runtime model.
- `consumer` and `compiler-plugin/.../compiler/consumer/`: unchanged. T048 does not touch either path, so it stays isolated from open PR #190 and parallel consumer-checker work.
- `catalog-model`: adds one compiler plugin option constant, `BRIDGE_MODULE = "bridgeModule"`, in `options/CompilerPluginOptions.kt`. Catalog bundle format v1 and binary index format v1 do not change.
- `producer`: owns `ConsumerComponent`, its internal `PlacedNode.Component` and `PlacedNodeContent` mapping, five small public list-conversion helpers (`textListValue`, `wholeListValue`, `decimalListValue`, `flagListValue`, `choiceListValue`), and the `@InternalSsrComposeApi` opt-in annotation marking compiler-bridge entry points. `producer` knows nothing about `catalog-model` or generated enum classes (`choiceListValue` accepts `List<Enum<*>>`). T050 owns `LocalConsumer` and keeps T025's array `ConsumerComponent` unchanged on purpose; T048 Slice 1 replaces that stand-in entry point cleanly after T050 merges, without touching `LocalConsumer.kt`.
- `compiler-plugin/.../compiler/producer/`: owns `bridgeModule` option parsing, `BridgeEnumPackageRule`, `BridgeLimits` (including `MAX_CHOICE_ENTRIES = 2_000` and enum package overlap checks), on-demand choice list selection, FIR `internal enum class` generation and friend reuse, FIR/IR mapping for all value, choice and list kinds, and `RefusedBridgeCheckers` for refused choice enums. It does not depend on `producer` in `main` (only in `test`, as today) and emits references only to `core`, `producer` and the Kotlin standard library.
- `gradle-plugin`: adds optional `moduleName: Property<String>` to `ProducerSettings` and passes `bridgeModule` as a normal `SubpluginOption` in `SsrSubpluginOptions` for every producer compilation (`main` and `test` receive the same value). T101 later passes the same `bridgeModule` option from the Kotlin toolchain plugin using the same relative-path rule.

### 2. Interfaces and contracts

#### ConsumerComponent and PlacedNode.Component (`producer`)

The final public signature in [contracts/producer-api.md](contracts/producer-api.md#what-a-bridge-function-is) is:

```kotlin
@Composable
public fun ConsumerComponent(
    catalogVersion: CatalogVersion,
    name: String,
    values: Map<String, DataValue?> = emptyMap(),
    callbacks: Map<String, Callback?> = emptyMap(),
    slots: Map<String, (@Composable () -> Unit)?> = emptyMap(),
)
```

Because `Callback` does not exist until T065 (US2), T048 Slice 1 deletes the T025 eight-parameter parallel-array stand-in (`valueNames`/`values: Array<String?>`, `actionNames`/`actions: Array<RemoteAction1<String>>`, `slotNames`/`slots`) with no deprecated overload kept, and introduces the map-based entry point with a transitional `actions` map parameter:

```kotlin
@Composable
public fun ConsumerComponent(
    catalogVersion: CatalogVersion,
    name: String,
    values: Map<String, DataValue?> = emptyMap(),
    actions: Map<String, RemoteAction1<String>?> = emptyMap(),
    slots: Map<String, (@Composable () -> Unit)?> = emptyMap(),
)
```

When T065 and T066 land in US2, `actions: Map<String, RemoteAction1<String>?> = emptyMap()` is replaced in place by `callbacks: Map<String, Callback?> = emptyMap()`. Every requirement from T038 holds immediately in US1:

- `catalogVersion` uses `CatalogVersion` (`@JvmInline value class` in `core`), giving type safety and `CatalogVersion` validation with zero runtime allocation overhead.
- `values` keeps insertion order and drops entries whose value is `null` (an optional parameter not given is absent from `PlacedComponent.values`, FR-062). Entries whose value is `DataValue.None` are kept (a required nullable parameter explicitly given `null` is sent as `DataValue.None`).
- `actions` (and later `callbacks`) drops entries whose value is `null`, so an optional callback not given is absent (FR-062).
- `slots` drops entries whose value is `null`, so an optional slot not given emits no `PlacedNode.Slot` child and is absent from `PlacedComponent.slots` (FR-062).

Matching internal `PlacedNode.Component` in `producer/src/main/kotlin/dev/tonholo/ssrcompose/producer/internal/PlacedNode.kt`:

- `val name: String`
- `val catalogVersion: CatalogVersion`
- `var values: Map<String, DataValue> = emptyMap()`
- `var actions: Map<String, RemoteAction1<String>> = emptyMap()` (replaced by `callbacks: Map<String, Callback>` in T065/T067)

`PlacedNodeContent.kt` passes `values = values` directly to `PlacedComponent`, removing the T025 `DataValue.Text` wrapper.

#### List conversion helpers (`producer`)

Probe C below proved that inlining `list.map { DataValue.Text(it) }` in bridge bodies crashes compilation (`Class too large`, constant pool overflow above 65,535) at 150 functions of 118 list parameters and reaches 29,565 bytes per method. Five public top-level helpers in `producer/src/main/kotlin/dev/tonholo/ssrcompose/producer/ListValues.kt` keep both hand-written and generated bridges small and readable:

```kotlin
public fun textListValue(items: List<String>): DataValue.ListValue
public fun wholeListValue(items: List<Long>): DataValue.ListValue
public fun decimalListValue(items: List<Double>): DataValue.ListValue
public fun flagListValue(items: List<Boolean>): DataValue.ListValue
public fun choiceListValue(items: List<Enum<*>>): DataValue.ListValue
```

Distinct names avoid JVM erasure clashes on `List<*>` without `@JvmName`, need no `GETSTATIC` receiver in bytecode, and give unambiguous single-symbol IR lookups.

#### Opt-in annotation for generated-code entry points (`producer`)

Public `producer` declarations that exist only for compiler-generated and hand-written bridges carry `@InternalSsrComposeApi`, declared in `producer/src/main/kotlin/dev/tonholo/ssrcompose/producer/InternalSsrComposeApi.kt`:

```kotlin
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Intended for compiler-generated and hand-written SSR Compose bridges. " +
        "Opt in explicitly with @OptIn(InternalSsrComposeApi::class) if calling by hand.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class InternalSsrComposeApi
```

- Level: `RequiresOptIn.Level.WARNING`. Hand calls without opt-in emit a clear compiler warning (and fail under `-Werror`), keeping hand-written bridges usable (FR-024) while flagging direct use, matching the owner's stated preference.
- What carries `@InternalSsrComposeApi`: `ConsumerComponent`, `textListValue`, `wholeListValue`, `decimalListValue`, `flagListValue`, `choiceListValue`, `UnplaceableBridge` (which `contracts/producer-api.md` already forbids developers from writing), and `Callback` with its value readers when T065 introduces them in US2.
- What does NOT carry `@InternalSsrComposeApi`: `Producer` and its builder, `ProducerLimits`, `ProducerObserver`, `LocalConsumer` and `Consumer`, `RemoteAction0` to `RemoteAction4` (server developers pass lambdas and may name these types in screen helpers), and `InternalSsrComposeApi` itself.
- How the compiler plugin adds the opt-in in FIR and IR: in `BridgeDeclarationGenerator.generateFunctions`, when `session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(ProducerNames.internalSsrComposeApi) != null` (skipping cleanly on compilations without `producer`, such as KMP shared metadata compilations), attach `@OptIn(InternalSsrComposeApi::class)` to each generated bridge `FirNamedFunction` using `buildAnnotation` targeting `ClassId(FqName("kotlin"), Name.identifier("OptIn"))` with `markerClass = [InternalSsrComposeApi::class]` (`buildCollectionLiteral` over `buildGetClassCall` and `buildResolvedQualifier`). Never attach `@InternalSsrComposeApi` itself to a generated bridge, or opt-in would propagate to server callers. `BridgeBodyGenerator` in IR needs no extra annotation because `kotlin.OptIn` has `AnnotationRetention.SOURCE` and is not emitted into `.class` files (zero constant pool slots, zero code bytes).
- How hand-written bridges and tests opt in: hand-written bridges and tests that call `ConsumerComponent`, the list helpers, or `@UnplaceableBridge` directly opt in locally with `@OptIn(InternalSsrComposeApi::class)` on the function or `@file:OptIn(InternalSsrComposeApi::class)` on the test file. Never set a module-wide `compilerOptions.optIn` for `InternalSsrComposeApi` in `compiler-plugin` test compilations or server builds, so tests prove that server callers of generated bridges compile cleanly under `-Werror` with no opt-in of their own.

#### Parameter kind mapping

Each `Parameter.Value(name, kind, required, nullable)` maps to a Kotlin bridge parameter type and a `DataValue?` expression passed inside `values = mapOf(...)` (or `emptyMap()` when a component has zero value parameters):

| Catalog kind | `ValueKind` | Required, not nullable | Required, nullable | Optional (`required = false`) |
|---|---|---|---|---|
| `text` | `Text` | `String` -> `DataValue.Text(p)` | `String?` -> `if (p != null) DataValue.Text(p) else DataValue.None` | `String? = null` -> `if (p != null) DataValue.Text(p) else null` |
| `whole` | `Whole` | `Long` -> `DataValue.WholeNumber(p)` | `Long?` -> `if (p != null) DataValue.WholeNumber(p) else DataValue.None` | `Long? = null` -> `if (p != null) DataValue.WholeNumber(p) else null` |
| `decimal` | `Decimal` | `Double` -> `DataValue.DecimalNumber(p)` | `Double?` -> `if (p != null) DataValue.DecimalNumber(p) else DataValue.None` | `Double? = null` -> `if (p != null) DataValue.DecimalNumber(p) else null` |
| `flag` | `Flag` | `Boolean` -> `DataValue.Flag(p)` | `Boolean?` -> `if (p != null) DataValue.Flag(p) else DataValue.None` | `Boolean? = null` -> `if (p != null) DataValue.Flag(p) else null` |
| `choice` | `Choice(listName)` | `<Enum>` -> `DataValue.Choice(p.name)` | `<Enum>?` -> `if (p != null) DataValue.Choice(p.name) else DataValue.None` | `<Enum>? = null` -> `if (p != null) DataValue.Choice(p.name) else null` |
| `list` of `text` | `ListOf(Text)` | `List<String>` -> `textListValue(p)` | `List<String>?` -> `if (p != null) textListValue(p) else DataValue.None` | `List<String>? = null` -> `if (p != null) textListValue(p) else null` |
| `list` of `whole` | `ListOf(Whole)` | `List<Long>` -> `wholeListValue(p)` | `List<Long>?` -> `if (p != null) wholeListValue(p) else DataValue.None` | `List<Long>? = null` -> `if (p != null) wholeListValue(p) else null` |
| `list` of `decimal` | `ListOf(Decimal)` | `List<Double>` -> `decimalListValue(p)` | `List<Double>?` -> `if (p != null) decimalListValue(p) else DataValue.None` | `List<Double>? = null` -> `if (p != null) decimalListValue(p) else null` |
| `list` of `flag` | `ListOf(Flag)` | `List<Boolean>` -> `flagListValue(p)` | `List<Boolean>?` -> `if (p != null) flagListValue(p) else DataValue.None` | `List<Boolean>? = null` -> `if (p != null) flagListValue(p) else null` |
| `list` of `choice` | `ListOf(Choice(listName))` | `List<<Enum>>` -> `choiceListValue(p)` | `List<<Enum>>?` -> `if (p != null) choiceListValue(p) else DataValue.None` | `List<<Enum>>? = null` -> `if (p != null) choiceListValue(p) else null` |

Notes on this mapping:

- Catalog format v1 collapses `Int` and `Long` into `whole`, and `Float` and `Double` into `decimal`, with no bit-width field. Bridges therefore use `Long` and `Double`, matching `DataValue.WholeNumber`, `DataValue.DecimalNumber`, `PlacedComponentScope.whole` and `PlacedComponentScope.decimal`. Callers with `Int` or `Float` state pass `.toLong()` or `.toDouble()`, or use `mutableLongStateOf` and `mutableDoubleStateOf`.
- List elements have no nullability flag in `ValueKind.ListOf`, so list element types in bridges are always non-null.
- For a parameter that is both optional and nullable (`required = false, nullable = true`), `null` means omitted and the app's default applies, as [contracts/producer-api.md](contracts/producer-api.md#what-a-bridge-gives-the-producer-for-one-callback) already documents.
- In IR, when `p` is nullable (`Long?`, `Double?`, `Boolean?`, `String?`, `<Enum>?`, `List<T>?`), the non-null branch of `irIfNull` applies `IrTypeOperator.IMPLICIT_NOTNULL` to `irGet(p)` with the non-null `IrType`, so the JVM backend unboxes primitive wrappers (`Long?` -> `long`, `Double?` -> `double`, `Boolean?` -> `boolean`) cleanly. The whole `if` expression goes into a temporary before `mapOf`, for optional and required nullable values alike (see class file size in section 4).
- Existing T025 slot and action support stays as is until T066 and T076. The `@Composable` type attribute on `ComposableFunction0<Unit>?` (R8a open question) is kept so K2 Compose checkers treat generated slot parameters identically to hand-written `(@Composable () -> Unit)?`.

#### Enum design, module name rule and compiler option

- Location and visibility: each choice list `<ChoiceName>` in a held catalog becomes a top-level `internal enum class <ChoiceName>` in the subpackage `<bridgePackage>.<moduleSegment>`, with `ClassId(FqName("$bridgePackage.$moduleSegment"), Name.identifier(choiceName))`. Its entries have `Visibilities.Public` and `EffectiveVisibility.Internal`; `values()`, `valueOf(String)` and `entries` inherit `EffectiveVisibility.Internal` from `owner.resolvedStatus` via `EnumClassUtils.kt`; its primary constructor is `Visibilities.Private`.
- Import shape: a server developer imports bridge functions from `<bridgePackage>` and choice enums from `<bridgePackage>.<moduleSegment>`, identically in `main` and `test`:

```kotlin
import com.example.app.bridge.v3.Greeting
import com.example.app.bridge.v3.server.Style

@Composable
fun CheckoutScreen() {
    Greeting(title = "Order 42", style = Style.titleLarge)
}
```

- Separation across catalogs in one module: each held catalog has a distinct `bridgePackage` (case-insensitive check in `BridgeLimits.sharedPackageProblems`), so catalog `"2"` (`com.example.app.bridge.v2.server.Style`) and catalog `"3"` (`com.example.app.bridge.v3.server.Style`) live in different packages and can be imported together with Kotlin import aliases. `BridgeLimits` also rejects any configuration where one catalog's `<bridgePackage>.<moduleSegment>` equals another catalog's `bridgePackage` or `<bridgePackage>.<moduleSegment>` (case-insensitively).
- Separation across modules: two modules `:checkout` and `:billing` holding the same catalog write `.../checkout/Style.class` and `.../billing/Style.class`, with zero `.class` path overlap even when their catalogs define different entries for `Style`.
- Derivation of `<moduleSegment>` and `bridgeModule` option: `catalog-model` defines `CompilerPluginOptions.BRIDGE_MODULE = "bridgeModule"`. `gradle-plugin` adds optional `moduleName: Property<String>` to `ProducerSettings` (`ssrCompose { producer { moduleName = "..." } }`). When unset, `gradle-plugin` derives it from `project.path` (and T101 derives it from the toolchain relative module folder path), never from `project.name` or `CommonConfigurationKeys.MODULE_NAME` (whose Gradle value is `"$group:$projectName"` in Kotlin 2.4.21, `klibUtils.kt`, and adds `_test` in test compilations). Normalization rule: lowercase with `Locale.ROOT`, replace every run of characters outside ASCII `a`..`z` and `0`..`9` with one `_`, trim leading and trailing `_`, and map an empty result (`:` root) to `"root"`. When bare `kotlinc` or compile tests omit `bridgeModule`, the compiler plugin normalizes `CommonConfigurationKeys.MODULE_NAME` with the same rule. `BridgeLimits` validates the resulting segment: 1 to 64 ASCII characters (`a`..`z`, `0`..`9`, `_`), starts with `a`..`z`, not a Kotlin hard keyword, and not a Windows device name (`con`, `prn`, `aux`, `nul`, `com1`..`com9`, `lpt1`..`lpt9`). An invalid segment fails compilation with a clear message telling the developer how to set `producer { moduleName = "..." }`. `bridgeModule` is passed as a normal `SubpluginOption` (no machine path, build-cache safe, invalidates compilation when changed).
- Sharing between `main` and `test`: both `main` and `test` of one module receive the exact same `bridgeModule` value (from `project.path` or `producer { moduleName }`), while keeping different `CommonConfigurationKeys.MODULE_NAME` values (`checkout` vs `checkout_test`) so `BridgeClassRule` keeps bridge function facade classes separate. In `BridgeDeclarationGenerator`, before declaring `enumClassId`, the plugin checks `val existing = session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(enumClassId)`:
  1. `existing != null` and `existing.moduleData` is in `session.moduleData.friendDependencies` or `session.moduleData.allDependsOnDependencies`: skip declaring `enumClassId` and reuse the existing friend enum. `test` calls both `main`'s screens and `test`'s direct bridges with one `Style` type and writes no duplicate `Style.class`.
  2. `existing == null`: declare `internal enum class` in this compilation. If only `test` uses a choice list `Tone` that `main` never used, `test` declares `Tone` in the same subpackage.
  3. `existing != null` and `existing.moduleData` is NOT in `friendDependencies` or `allDependsOnDependencies`: another non-friend module on the classpath already defines `<bridgePackage>.<moduleSegment>.<ChoiceName>`. Do not shadow it; record a collision error naming the choice list, package and `producer { moduleName = "..." }`, reported by a FIR checker when referenced or when any bridge using it is called.
- Which choice lists and entries are created: a choice list is selected for creation only when a created named component uses it (in a `Choice` or `ListOf(Choice)` parameter) OR when `SourceNameScan` finds its name in the module's source files (added to the single `HashSet<String>` of scanned names with zero extra file reads). For every selected choice list, ALL entries are created in catalog declaration order. Unused choice lists are skipped, preserving SC-016.

### 3. Data flow

1. Configuration (`gradle-plugin` -> `SsrCommandLineProcessor` -> `BridgeRequest`): `SsrSubpluginOptions` resolves `bridgeModule` once per project (same for `main` and `test`), passes it as `SubpluginOption`, and keeps `sourceFolder` and `excludedSources` as `InternalSubpluginOption`. `BridgeRequest` carries `catalogs`, `sourceRoots`, `excludedSources`, `moduleName` (compilation name for `BridgeClassRule`) and `bridgeModule` (enum subpackage segment).
2. Load and validate (`BridgeCatalogLoader` -> `NamedBridgeComponents`): validate catalog options, shared `bridgePackage`s, `bridgeModule` syntax, and enum-package overlap with other catalogs' `bridgePackage`s. Any option or unreadable-index problem reports a compiler `ERROR` and registers no bridge extensions.
3. Single-pass source scan (`SourceNameScan`): collect `allComponentNames + allChoiceNames` across held indexes and scan source roots once.
4. On-demand decode and refusal (`NamedBridgeComponents`): for each held catalog, decode only named components. Collect choice lists used by those components plus choice lists named directly in source. Check `MAX_PARAMETERS` (118), `MAX_FUNCTIONS_PER_CLASS` (150), supported parameter kinds, and `MAX_CHOICE_ENTRIES` (2,000). A component that uses a choice list over 2,000 entries is refused with a clear message naming component, parameter, choice list and limit; an oversized choice list is not declared and its refusal is stored in `BridgeCatalog.enumRefusals`. Unused oversized choice lists produce no error.
5. FIR declaration (`BridgeDeclarationGenerator`): `hasPackage` matches `catalog.enumPackage.startsWith(packageFqName)` (which covers `bridgePackage` and all prefixes). `getTopLevelClassIds` returns only selected choice lists not already present in `friendDependencies`/`allDependsOnDependencies` and not colliding with non-friend classpath classes. `generateTopLevelClassLikeDeclaration` creates each `internal enum class` with all `FirEnumEntry` declarations (R3 workaround). `generateFunctions` creates each `internal` `@Composable` bridge function with `BridgeParameter.coneType()`, `hasDefaultValue = !parameter.isRequired`, and `@OptIn(InternalSsrComposeApi::class)` when `InternalSsrComposeApi` is on the classpath.
6. FIR refusal checks (`RefusedBridgeCheckers` and collision check): imports, calls, references or type qualifiers targeting a refused component or a refused/colliding choice enum report `BridgeDiagnostics.UNPLACEABLE_COMPONENT` (or a dedicated refusal diagnostic) with the refusal reason. Mentions in comments or strings stay silent.
7. IR generation (`BridgeIrGenerationExtension` -> `BridgeBodyGenerator`): `PluginOrderGuard` runs first (if Compose ran first, report `PLUGIN_ORDER` and stop). `visitConstructor` and `visitEnumEntry` populate generated enum constructors and entry initializers. `visitSimpleFunction` sets optional parameter defaults to `null` and emits either `error(unplaceableMessage)` or `ConsumerComponent(catalogVersion = CatalogVersion(key.catalogVersion), name = component.name, values = ..., actions = ..., slots = ...)`.
8. Runtime (`ConsumerComponent` -> `PlacedNode` -> `PlacedNodeContent`): `ConsumerComponent` strips `null` entries from `values`, `actions` and `slots` while preserving `DataValue.None` and insertion order, updates `PlacedNode.Component`, and `toPlacedComponent()` emits `PlacedComponent(name, values, slots)` directly.

### 4. Constraints and non-functional requirements

All numbers below were measured on 2026-11-19 on macOS, JetBrains Runtime 25.0.4, Kotlin 2.4.21, Compose compiler 2.4.21, Compose runtime 1.12.1 (from `1.12.2` Multiplatform), JVM target 17 (`majorVersion = 61`), using throwaway probes outside tracked files (`probe_limits.py`, `probe_limits2.py`, `probe_plugin.py`, `probe_150.py`, `probe_optin.py`).

#### Opt-in annotation probe (Probe D)

Probed on Kotlin 2.4.21 + Compose 2.4.21 with both `RequiresOptIn.Level.WARNING` and `RequiresOptIn.Level.ERROR` on `ConsumerComponent`, `textListValue` and `UnplaceableBridge`:

- Hand-written caller without `@OptIn`: produced 3 warnings at `Level.WARNING` (exit 0 without `-Werror`, exit 1 with `-Werror`) and 3 errors at `Level.ERROR` (exit 1 without `-Werror`), one each on `@UnplaceableBridge`, `ConsumerComponent` and `textListValue`.
- Hand-written caller with `@OptIn(InternalSsrComposeApi::class)`: compiled with exit 0 under `-Werror` at both levels.
- Server caller calling a generated bridge function under `-Werror` with no opt-in of its own: compiled with exit 0 at both levels, both when the plugin attached `@OptIn(InternalSsrComposeApi::class)` in FIR (`addOptIn = true`) and when it did not (`addOptIn = false`). On Kotlin 2.4.21, FIR opt-in checkers run on user source files and do not check plugin-generated IR bodies or synthetic plugin files; attaching `@OptIn(InternalSsrComposeApi::class)` in FIR still keeps the FIR declaration properly opted-in (including for `@UnplaceableBridge`) and guards against future compiler or IDE checkers inspecting generated FIR declarations.
- Bytecode check (`javap -v` on `Bridge0_serverKt.class`): `kotlin/OptIn` is absent from the `.class` file (`AnnotationRetention.SOURCE`), while `UnplaceableBridge` is present (`AnnotationRetention.BINARY`), confirming zero constant pool slots and zero bytecode cost from `@OptIn`.

#### Enum static initializer and constant pool limits (Probes A1, A1b and B)

Each enum entry beyond index 127 adds 16 bytes to `<clinit>`, 8 bytes to `$values` and 4 constant pool entries (measured with 64-character distinct entry names):

| Entries | Constant pool (max 65,535) | Fields (max 65,535) | `<clinit>` bytes (ceiling 32,767 / max 65,535) | `$values` bytes | Result |
|---|---|---|---|---|---|
| 100 (hand-written) | 478 | 102 | 1,513 | 702 | Compiles |
| 1,000 (hand-written) | 4,078 | 1,002 | 15,885 | 7,875 | Compiles |
| 2,000 (hand-written) | 8,078 | 2,002 | 31,885 | 15,875 | Compiles |
| 2,000 (plugin-generated, Probe B) | 8,078 | 2,002 | 31,885 | 15,875 | Compiles and loads (`entries.size == 2000`) |
| 2,055 (hand-written) | 8,298 | 2,057 | 32,765 | 16,315 | Last count within 32,767 ceiling |
| 2,056 (hand-written) | 8,302 | 2,058 | 32,781 | 16,323 | Exceeds 32,767 ceiling |
| 4,103 (hand-written) | 16,490 | 4,105 | 65,533 | 32,699 | Last count within JVM 65,535 limit |
| 4,104 (hand-written) | - | - | > 65,535 | - | Compiler crash (`Error generating class file`) |

`<clinit>` `code_length` is the binding JVM limit, not the constant pool (which is at 12.3% at 2,000 entries). Setting `BridgeLimits.MAX_CHOICE_ENTRIES = 2_000` gives 882 bytes of margin below our 32,767 ceiling and 33,650 bytes below the JVM hard limit.

#### Bridge method and 150x118 class limits with new kinds (Probes A2 and C)

Measured with `ConsumerComponent(CatalogVersion("1"), name, values = mapOf(...))`, 150 functions per class, 118 parameters per function, distinct 64-character parameter names and `sourceInformation=true`:

| Shape (150 functions x 118 parameters) | Constant pool (max 65,535) | Methods | Max `code_length` (ceiling 32,767) | Max `parameterUnits` (max 255) | `arrayDimensions` | `max_locals` | Longest UTF-8 |
|---|---|---|---|---|---|---|---|
| Required `Long` | 55,709 | 300 | 7,228 | 250 | 0 | 262 | 65,535 |
| Required nullable `Long?` (`DataValue.None`) | 55,723 | 300 | 9,119 | 132 | 0 | 144 | 65,535 |
| Optional `Long? = null` | 55,730 | 300 | 11,684 | 136 | 0 | 148 | 65,534 |
| Required `Choice` | 55,728 | 300 | 8,971 | 132 | 0 | 144 | 65,535 |
| Optional `Choice? = null` | 55,736 | 300 | 13,930 | 136 | 0 | 148 | 65,534 |
| Required `List<Choice>` (helper) | 55,732 | 300 | 9,887 | 132 | 0 | 144 | 65,535 |
| Optional `List<Choice>? = null` (helper) | 55,734 | 300 | 13,242 | 136 | 0 | 148 | 65,534 |
| Optional `List<String>? = null` (helper) | 55,733 | 300 | 13,242 | 136 | 0 | 148 | 65,534 |
| Optional `List<String>? = null` (inline `.map`) | > 65,535 | - | 29,565 (1 fn) | 136 | 0 | 282 | - (crashes at 150 fns) |

Probe A2 also compared `mapOf(...)` against parallel `arrayOf(names), arrayOf(values)` at 118 parameters: `mapOf` was 217 to 460 bytes smaller per method across every shape (`req_Long` 7,134 vs 7,594; `opt_Long` 11,596 vs 11,813; `opt_ListText_helper` 13,153 vs 13,373). `Double` matched `Long` identically (`req_Double` 250 units, `opt_Double` 136 units), and `Boolean` used 132/136 units. These byte counts were measured with inline branches. With temporaries, T048 Slice 1 measured the whole class of one 118-parameter bridge at 94 KB with `mapOf`, against 89 KB for the T025 arrays.

- Both `MAX_PARAMETERS = 118` and `MAX_FUNCTIONS_PER_CLASS = 150` hold for the constant pool and `code_length` of every new kind when list helpers are used. The optional and required nullable rows above were measured with inline branches. Class file size for the new kinds is unproven until recheck item 2.
- Class file size: every value whose expression branches, optional or required nullable, of every kind, goes into a temporary before the `mapOf(...)` call. A branch evaluated inside the `mapOf` arguments, with the vararg array on the operand stack, makes the JVM write a full stack map frame that lists every local, once per branch. Measured by T048 Slice 1 on 2026-11-19 with `javap` on one bridge of 118 optional text parameters: 187 KB with the branches inline (356 full frames), 94 KB with temporaries (122 full frames), and 89 KB for the T025 array body on `80d61eed`. At 150 functions the inline form wrote a 26.7 MB class (27.9 MB with source information), which the compiler cannot read back: IntelliJ's file loader refuses files over about 20 MB with `FileTooBigException`. Probes A2 and C did not measure the class file size.
- `arrayDimensions` is 0 for all `List<T>` parameters because `List<T>` erases to `java.util.List`.
- SC-016 (2 seconds on 20,000 components): adding choice list names to the single `SourceNameScan` pass adds no file I/O, and decoding only used choice lists creates only the enums the module references. Trade-off: full re-verification on the real plugin runs in `:compiler-plugin:scaleTest`.
- Build cache relocatability: `bridgeModule` holds only a normalized identifier (no paths or timestamps).

#### Recheck plan T048 must execute on the real plugin

1. Extend `BridgeLimitsTest.SHAPES` (unit tests, 118 parameters) with required, optional and required-nullable `Whole`, `Decimal`, `Flag`, `Choice`, `ListOf(Text)` and `ListOf(Choice)`, asserting JVM major version 61, class load, `codeLength <= 32_767`, `parameterUnits <= 255` and `arrayDimensions == 0`.
2. Extend `BridgeLimitsTest.WIDEST_SHAPES` (`scaleTest`, 150 functions x 118 parameters, `sourceInformation` false and true) with required `Whole`, required nullable `Whole`, optional `Choice`, optional `ListOf(Choice)` and mixed new kinds, asserting all JVM limits and `assertMetadataReadsBack`, and record the real plugin numbers in R16. Measure the class file size next to the constant pool and `code_length`: the read-back compile fails above about 20 MB.
3. Add enum limit tests in `BridgeLimitsTest`: 2,000 entries with 64-character names compiles, loads and stays within `<clinit> <= 32_767` and `constantPoolCount <= 65_535`; 2,001 entries fails cleanly when used and passes when unused or only in comments; 64 three-byte character choice name fits in 255 byte file name; invalid `bridgeModule` values and enum-package overlaps fail cleanly.
4. Re-run `:compiler-plugin:scaleTest` and update R16's real plugin numbers and "Not checked" list.

### 5. Invariants

- Every bridge function and choice enum created for a server module is `internal`; another module without `friendPaths` cannot call the bridge or reference the enum (`TwoModuleBridgeTest`).
- Two server modules with distinct `bridgeModule` values holding the same catalog and `bridgePackage`, even with different entries for one choice list, write disjoint `.class` paths and run together on one classpath without `NoSuchFieldError` (`TwoModuleBridgeTest`).
- `main` and `test` of one module share one `Class<*>` for every choice enum compiled in `main`, and `test` writes no duplicate `.class` file for it (`NamedBridgeCreationTest` and Gradle TestKit test).
- Every created choice enum contains all entries of its choice list in exact catalog declaration order (`BridgeGenerationTest`).
- A required nullable parameter passed `null` produces `DataValue.None` in `PlacedComponent.values`; an omitted or `null` optional parameter (value, choice, list, action/callback, slot) is absent from `PlacedComponent` (`ConsumerComponentTest`, `BridgeCompositionTest`).
- Created bridge code references only `core`, `producer` and Kotlin stdlib types, never `catalog-model` or `consumer` (`BridgeGenerationTest` and Gradle dependency check).
- Server screens calling generated bridges compile cleanly under `-Werror` with no opt-in, while hand calls to `ConsumerComponent`, list helpers or `@UnplaceableBridge` emit an opt-in warning unless annotated with `@OptIn(InternalSsrComposeApi::class)` (`InternalSsrComposeApiTest`).
- Every generated class file targets JVM 17 (version 61) and stays within `constant_pool_count <= 65_535`, `code_length <= 32_767`, `parameterUnits <= 255`, `arrayDimensions == 0`, a class file size below 10 MB (half the read-back limit of about 20 MB), and 255 byte path segments (`BridgeLimitsTest` and `scaleTest`).
- No change in `core`, `consumer`, or `compiler-plugin/.../compiler/consumer/` (`git diff --stat`).

### 6. Risks and edge cases

- Gradle incremental compilation when `main` begins using an enum previously generated only by `test`: Probe B proved fresh `K2JVMCompiler` compilations reuse `main`'s enum and emit no `Style.class` in `test`, but Gradle's incremental compiler deleting a previously written `test/classes/.../Style.class` is unproven. T048 must add a Gradle TestKit incremental test covering that transition.
- Two independent modules whose paths normalize to the same `<moduleSegment>` (`:my-app` and `:my_app`): if one depends on the other, the non-friend classpath collision check catches it at compile time; if neither depends on the other and both land on one runtime classpath, they share the same enum `.class` path (same known limit as R16 line 972 for bridge functions). Setting `producer { moduleName = "..." }` resolves it.
- Public screen signatures referencing generated enums: a `public` screen `@Composable fun HomeScreen()` can use `Style` in its body, while a function exposing `Style` in its signature (parameter or return type) must be `internal`, enforced by Kotlin. This is desirable because per-module enums must not leak across module boundaries.
- IDE behavior with generated enums (R3 workaround): unproven in IntelliJ IDEA 2026.3. T106 tracks manual IDE verification; R3 documents the typed-API class-with-constants fallback if a future Kotlin release or IDE breaks `FirEnumEntry` generation.
- `ConsumerComponent` binary compatibility with T050: T050 intentionally keeps T025's array `ConsumerComponent`. T048 Slice 1 must land after T050 (or rebase on T050) and update `ConsumerComponentTest`, `BridgeCompositionTest` and `TwoModuleBridgeTest` in the same slice so every commit builds cleanly.
- Choice list name matching a component name in the same catalog: safe because bridge functions live in `<bridgePackage>` and choice enums live in `<bridgePackage>.<moduleSegment>`.
- `Long` and `Double` narrowing on the consumer side: catalog format v1 does not record `Int` vs `Long` or `Float` vs `Double`. Consumer renderers (T055) and hand-written bindings read `Long`/`Double` from `PlacedComponentScope` and check range when converting to `Int`/`Float`.

### 7. Rejected alternatives

- Nested enums in a module-named outer class (`<bridgePackage>.<ModuleOuter>.<ChoiceName>`): turned down in [ADR 0010](../../docs/adr/0010-per-module-choice-enums-in-bridge-subpackage.md) due to `main`/`test` `NestHost` conflict, 255 byte `<Outer>$<Choice>.class` contention, outer class constant pool limit, PascalCase component name collisions, and untested nested FIR enum generation.
- Top-level `internal typealias <ChoiceName>` in `<bridgePackage>`: turned down in [ADR 0010](../../docs/adr/0010-per-module-choice-enums-in-bridge-subpackage.md) after reading Kotlin 2.4.21 `FrontendFilesForPluginsGenerationPipelinePhase.kt`, which emits `<bridgePackage>/<ChoiceName>Kt.class` for top-level typealiases and reintroduces the cross-module `.class` collision.
- Deriving the enum package from `CommonConfigurationKeys.MODULE_NAME`: turned down in [ADR 0010](../../docs/adr/0010-per-module-choice-enums-in-bridge-subpackage.md) after reading Kotlin Gradle plugin `klibUtils.kt`, which sets `-module-name` to `"$group:$projectName"` and appends `_test` in tests.
- Creating only the enum entries a module mentions in source: turned down because it breaks `main`/`test` sharing when `test` uses another entry, breaks `entries`/`values()`/`valueOf`/exhaustive `when` and ordinals, causes ABI churn on every new entry use, and saves negligible compile time.
- Keeping T025's parallel arrays (`valueNames`/`values`) on `ConsumerComponent`: turned down because Probe A2 measured `mapOf(...)` 217 to 460 bytes smaller per 118-parameter bridge method with inline branches (with temporaries the whole class is 5% larger than with the arrays, see section 4), avoids parallel-array misalignment bugs in hand-written bridges, and matches [contracts/producer-api.md](contracts/producer-api.md).
- Inlining `list.map { DataValue.<Kind>(it) }` in bridge bodies: turned down because Probe C proved it overflows the 65,535 constant pool limit at 150 functions x 118 parameters (`Error generating class file`) and consumes 29,565 / 32,767 bytes per method.
- Generating `Int`/`Long` or `Float`/`Double` overloads per whole/decimal parameter: turned down because $k$ parameters produce $2^k$ overloads, immediately breaking the 150-function class limit.
- `RequiresOptIn.Level.ERROR` on `InternalSsrComposeApi`: turned down in favour of `Level.WARNING` to match the owner's stated warning behavior (`1ccz50n1`) and keep hand-written bridges gentle without `-Werror`; tightening to `ERROR` later is a one-line change if needed.
- Global `compilerOptions.optIn` in `compiler-plugin` tests or server modules: turned down because it would mask accidental opt-in propagation from generated bridges to server callers.

### 8. Slices and delivery plan

Each slice delivers one mechanism, is test-first, buildable, and reviewable alone:

1. Producer `ConsumerComponent` map API, `PlacedNode` with `DataValue` and `CatalogVersion`, `ListValues` helpers, and `@InternalSsrComposeApi` opt-in annotation on `ConsumerComponent`, the five list helpers and `UnplaceableBridge`, plus `@OptIn(InternalSsrComposeApi::class)` on generated bridge functions in FIR (`producer` + update existing `BridgeDeclarationGenerator`, `BridgeBodyGenerator` and `BridgeCompositionTest` so the build stays green). Covers required-nullable `DataValue.None` vs absent `null` for values, actions and slots (T038, FR-062).
2. Scalar value kinds (`Whole` -> `Long`, `Decimal` -> `Double`, `Flag` -> `Boolean`) plus required-nullable support for all scalar kinds (`Text`, `Whole`, `Decimal`, `Flag`) in `BridgeParameterMapping`, `BridgeDeclarationGenerator` and `BridgeBodyGenerator`, with compile, composition and refusal tests.
3. List value kinds (`ListOf` of `Text`, `Whole`, `Decimal`, `Flag`, including required-nullable and optional) wired through `ListValues` helpers, with compile and composition tests.
4. `bridgeModule` compiler option (`catalog-model`, `gradle-plugin` `ProducerSettings.moduleName` + `SsrSubpluginOptions`, `SsrCommandLineProcessor`, `BridgeEnumPackageRule`, `BridgeLimits` validation and overlap check), with unit and Gradle TestKit tests.
5. Per-module `internal enum class` generation in `<bridgePackage>.<moduleSegment>` for `Choice` and `ListOf(Choice)` parameters (all entries in declaration order, on-demand selection via `SourceNameScan` + used components, `MAX_CHOICE_ENTRIES = 2_000` refusal + `RefusedBridgeCheckers`, IR constructor/entry/value conversion), with compile, composition, exhaustive `when`, `entries`/`valueOf` and two-module isolation tests.
6. `main` and `test` friend enum reuse (`session.dependenciesSymbolProvider` against `friendDependencies` / `allDependsOnDependencies`) and non-friend classpath collision refusal, with compile tests and Gradle TestKit incremental tests (including the `test`-only -> `main`-also transition).
7. JVM 17 limit recheck and scale verification (`BridgeLimitsTest`, `scaleTest`, `BridgeGenerationTest` of T036), recording final real-plugin numbers in R16. It adds the class file size check to `BridgeLimitsTest`. Until then only the `scaleTest` read-back compile guards the size, and it fails only above about 20 MB.

Approved by the owner on 2026-11-19 to ship as four pull requests in this order:

1. PR 1: `ConsumerComponent` map API, list helpers and `@InternalSsrComposeApi` opt-in annotation (Slice 1). Starts after T050 merges.
2. PR 2: scalar and list kinds (Slices 2 and 3).
3. PR 3: `bridgeModule` option, per-module choice enums, and `main` and `test` reuse (Slices 4, 5 and 6).
4. PR 4: JVM limit recheck and scale numbers (Slice 7).

### 9. Validation checklist

Numbered points checked in validate mode against the T048 delivery:

1. `ConsumerComponent` in `producer` has the map-based signature (`catalogVersion: CatalogVersion`, `name: String`, `values: Map<String, DataValue?> = emptyMap()`, `actions: Map<String, RemoteAction1<String>?> = emptyMap()`, `slots: Map<String, (@Composable () -> Unit)?> = emptyMap()`); T025's parallel-array entry point is deleted.
2. `PlacedNode.Component` holds `catalogVersion: CatalogVersion` and `values: Map<String, DataValue>`, and `PlacedNodeContent.toPlacedComponent()` passes `values` directly without wrapping in `DataValue.Text`.
3. Passing `null` to a required nullable parameter puts `DataValue.None` into `PlacedComponent.values`; omitting or passing `null` to an optional parameter (value, choice, list, action, slot) leaves it absent from `PlacedComponent` (FR-062, T038).
4. `producer` provides public `textListValue`, `wholeListValue`, `decimalListValue`, `flagListValue` and `choiceListValue`, and `BridgeBodyGenerator` calls them instead of inlining `list.map`.
5. `producer` defines `@InternalSsrComposeApi` with `@RequiresOptIn(level = RequiresOptIn.Level.WARNING)` and `AnnotationRetention.BINARY`, applied to `ConsumerComponent`, all five list helpers and `UnplaceableBridge` (and NOT to `Producer`, `ProducerLimits`, `ProducerObserver`, `LocalConsumer`, `Consumer` or `RemoteAction*`).
6. `BridgeDeclarationGenerator` attaches `@OptIn(InternalSsrComposeApi::class)` (never `@InternalSsrComposeApi` itself) to generated bridge functions when `InternalSsrComposeApi` is on the classpath; server callers of generated bridges compile cleanly under `-Werror` with no opt-in, while hand calls to `ConsumerComponent`, a list helper or `@UnplaceableBridge` warn without `@OptIn` and compile cleanly under `-Werror` with `@OptIn(InternalSsrComposeApi::class)`. Hand-written tests opt in locally rather than via global `compilerOptions.optIn`.
7. Every `Parameter.Value` kind (`Text`, `Whole`, `Decimal`, `Flag`, `Choice`, `ListOf` of each) maps to the exact Kotlin type and `DataValue` expression in section 2, including required, required-nullable, optional and optional-nullable, and rejects wrong types, wrong names and missing required parameters at compile time (SC-004).
8. `CompilerPluginOptions.BRIDGE_MODULE` exists in `catalog-model`; `gradle-plugin` exposes optional `ProducerSettings.moduleName`, derives the default from `project.path` (`:` -> `"root"`, lowercase non-alnum runs to `_`, trimmed), and passes the same `bridgeModule` `SubpluginOption` to `main` and `test` with no machine paths.
9. `BridgeLimits` validates `bridgeModule` (1 to 64 ASCII `a-z0-9_`, starts with `a-z`, not a Kotlin hard keyword, not a Windows device name) and rejects any catalog enum package overlapping another catalog's `bridgePackage` or enum package (case-insensitively).
10. Choice enums are created as top-level `internal enum class <ChoiceName>` in `<bridgePackage>.<moduleSegment>` with all entries in catalog declaration order; `entries`, `values()`, `valueOf` and exhaustive `when` work; external non-friend modules fail to access them.
11. Choice lists are created on demand (only when used by a created named component or named in `SourceNameScan`); unused choice lists, including ones over 2,000 entries, do not fail the build.
12. Two modules holding the same catalog and `bridgePackage` with different entries for one choice list produce disjoint `.class` paths and run together on one classpath without `NoSuchFieldError`.
13. Two catalogs in one module with different `bridgePackage`s keep same-named choice enums apart and compile side by side.
14. A `test` compilation with `main` in `friendPaths` reuses `main`'s compiled enum (same `Class<*>`, no duplicate `.class` in `test`), still creates extra enums that only `test` uses in the same subpackage, and a Gradle TestKit test covers the incremental transition when `main` starts using an enum that only `test` used; a non-friend classpath collision reports a clear compile error naming `producer { moduleName }`.
15. `BridgeLimits.MAX_CHOICE_ENTRIES = 2_000` is enforced: 2,000 entries with 64-character names compiles, loads and stays within `<clinit> <= 32_767`; 2,001 entries fails cleanly when used and passes when unused or only mentioned in comments.
16. `BridgeLimitsTest` and `scaleTest` prove 118 parameters, 150 functions per class (`constant_pool_count <= 65_535`), `code_length <= 32_767`, `parameterUnits <= 255`, `arrayDimensions == 0`, a class file size below 10 MB and JVM version 61 on the real plugin for the new kinds, and R16 is updated with the measured numbers.
17. Created code references no class outside `core`, `producer` and Kotlin stdlib; `git diff` touches nothing in `core`, `consumer`, or `compiler-plugin/.../compiler/consumer/`.

## R19. Text the user reads and the look of the problem message (#196)

Status: approved by the owner on 2026-11-19, with two changes: the problem message takes Material's shape of several style parameters instead of one style object, and the producer writes its reasons in the language the app sent, with English as the fallback. Both changes are written into this section. Decision records: [ADR 0011](../../docs/adr/0011-replaceable-text-through-a-string-manager.md) for the text and [ADR 0012](../../docs/adr/0012-problem-message-style-parameters.md) for the look. Contract: [consumer-api.md](contracts/consumer-api.md#text-the-user-reads).

Decision: every text the library writes for the user has a key in a public enum, `SsrComposeStringResource`. Composition reads the text from a `SsrComposeStringResourceManager` held in a `CompositionLocal`, whose default is the library's English manager. The session never writes user text: `ScreenProblem` carries what happened, and the text is chosen in composition. The default problem message becomes the public `ScreenProblemMessage` composable. It takes several style parameters, each defaulting to a value of `ScreenProblemMessageDefaults`, the way Material components and their `Defaults` objects work. The two colours are grouped in `ScreenProblemMessageColors`. The producer writes the `reason` of `Refused` and `ScreenUnavailable` in the language the app sent in the handshake, with English as the fallback.

The owner's words: "For the text, we must have something that can be changed by the consumer app. The reasoning is because the consumer app may provide localization for other languages that we don't provide." And: "For the styling, we should follow the same approach the material components follows: style parameters in the composable, Defaults object with our defaults. We don't need to follow that exact rule, and we can have a better one that fits best with our library, but we must not hardcode these things."

The owner's review on 2026-11-19 approved the two new `ScreenProblem` cases, the four keys, the manager in a `CompositionLocal` with the English default and partial replacement with fallback, and plurals waiting for the first plural text. It changed the style shape to Material's, and settled that the producer writes reasons in the app's language.

This changes code from PR #190, which was merged on 2026-11-19. No version of `consumer` has been released, so no app breaks.

### What the user reads today

Every string literal in `consumer/src/commonMain` was read on 2026-11-19 (`consumer/src/androidMain` does not exist). Four are written for the user:

| Text | Where | When the user sees it | Key |
|---|---|---|---|
| "Try again" | `internal/screen/DefaultScreenProblem.kt` | the retry button of every problem | `ScreenProblemRetry` |
| "This screen cannot be shown right now." | `internal/screen/DefaultScreenProblem.kt` | `ScreenProblem.Mismatch` | `ScreenProblemMismatch` |
| "The connection to the server ended. Please try again." | `internal/session/ConsumerSession.kt` | the connection fails or ends | `ScreenProblemConnectionEnded` |
| "The app could not read the answer from the server." | `internal/session/ConsumerSession.kt` | a frame cannot be decoded, a `Screen` or `ScreenUnavailable` arrives before `Accepted`, or `Accepted` names another contract version | `ScreenProblemAnswerUnreadable` |

None of the four needs a plural or an argument. FR-059 forbids internal details in user text, and FR-037 forbids a developer placeholder, so no key may take the component name of a `Mismatch` or any kind name as an argument.

The other literals stay as they are, because no user reads them:

- `PlacedValueException` messages in `internal/read/PlacedValueProblems.kt` and the kind names in `internal/read/KindName.kt` are for the developer. Inside `SsrScreen` a failed read reports instead of throwing (R7).
- `SEND_CANCELLED` in `ConsumerSession.kt` is the message of an internal exception. The session turns it into the end of the conversation, and the user sees `ScreenProblemConnectionEnded`.
- The `require` message in `ComponentBindings.kt` is for the developer who binds a name twice.
- `TEXT_STAND_IN` in `PlacedComponentScope.kt` is an empty stand-in that is never drawn (R7).

The `reason` of `ScreenProblem.Refused` and `ScreenProblem.Unavailable` is written by the producer, in the language the app sent in the handshake (`DeviceContext.language`, principle IV), and in the latest language after a `DeviceChanged`. When the language is missing, not valid, or the producer has no text in it, the reason is in English. This is [messages.md](contracts/messages.md) rule 7 and [producer-api.md](contracts/producer-api.md#setting-up), and T051 builds it. This design changes no producer code. The app shows reasons as given; the manager does not translate them. An app that wants its own words for a refusal can still map `Refused.kind` in its own `unavailable` composable. `Unavailable` has no kind, so an app can only replace its reason with one text of its own for every case.

### 1. Boundaries

- `consumer` owns the keys, the manager interface, the English text, the `CompositionLocal`, `ScreenProblemMessage` and its style. All of it lives in `consumer/src/commonMain`. Nothing needs Android.
- `core` is not touched. `producer` is not touched by this design: the rule that it writes reasons in the app's language is in the contracts, and T051 builds it.
- The session (`internal/session/`) owns what happened and must not know any user text or any key. Composition owns which words and which look.
- `ScreenProblemMessage` draws one message and one retry button inside the space its `modifier` gives it. It arranges nothing of the server's screen, so the library still owns no screen arrangement in its public API.
- No Material dependency. `consumer` depends on Compose runtime, foundation and ui only (`consumer/build.gradle.kts`), and the style uses their types: `Color`, `TextStyle`, `Dp` and `PaddingValues`.

### 2. Interfaces and contracts

The signatures are in [consumer-api.md](contracts/consumer-api.md#text-the-user-reads). In short:

- `ScreenProblem` gains two cases, `ConnectionEnded` and `AnswerUnreadable`, both `data object`. `Refused` and `Unavailable` now carry only the producer's reasons. An unreadable answer is no longer a `Refused` with the kind `IntroductionNotValid`, because the app did not send an invalid introduction and the producer did not refuse.
- `SsrComposeStringResource` is an enum with the four keys above.
- `SsrComposeStringResourceManager` has one `@Composable` lookup, `stringResource(resource, vararg arguments: Any): String`. Its shape matches `androidx.compose.ui.res.stringResource(id, vararg formatArgs)`, so an Android app can forward a key to its own `R.string` with one line per key.
- `LocalSsrComposeStringResourceManager` is a `staticCompositionLocalOf` whose default is the internal English manager. An app that never provides one sees the English text of today.
- An app that replaces only some keys reads the current manager before it provides its own, and falls back to it. The probe below runs exactly this.
- `ScreenProblemMessage(problem, retry, modifier, colors, textStyle, contentPadding, spacing, maxContentWidth, retryContentPadding, retryBorderWidth)` is public. Each style parameter defaults to a value of `ScreenProblemMessageDefaults`. `SsrScreen`'s default `unavailable` calls it with `Modifier.fillMaxSize()` and the defaults.
- The model is Material 3's `Button`, read in `material3-android-1.4.0-sources.jar`: `colors: ButtonColors = ButtonDefaults.buttonColors()` and `contentPadding: PaddingValues = ButtonDefaults.ContentPadding`. `ButtonColors` is a plain class, not a `data class`, with a public constructor and a `copy` in which `Color.Unspecified` keeps the old value. `buttonColors()` is `@Composable` because it reads the theme, and `ContentPadding` is a plain value.
- Grouped: the background and the content colour, in `ScreenProblemMessageColors(containerColor, contentColor)`. They change together with the theme and must be chosen as a readable pair, and their default reads `isSystemInDarkTheme()`, so it is a `@Composable` function, `ScreenProblemMessageDefaults.colors(containerColor, contentColor)`, whose parameters default to `Color.Unspecified`, meaning the theme's default. The class follows `ButtonColors`: `@Immutable`, public constructor, `copy` with the same `Color.Unspecified` rule, and hand-written `equals` and `hashCode`.
- Not grouped: the text style, the padding, the gap, the maximum width, and the retry button's padding and border width. None of them depends on the theme, so each is a plain value in `ScreenProblemMessageDefaults` (`MessageTextStyle`, `ContentPadding`, `Spacing`, `MaxContentWidth`, `RetryContentPadding`, `RetryBorderWidth`), like `ButtonDefaults.ContentPadding`. The retry border takes its colour from `contentColor`, so it is a width and not a `BorderStroke`, which would hold a second colour.
- `contentColor` colours the text and the retry border and replaces the colour of `textStyle`, as Material's content colour does.
- Files and packages, one public type per file and one package per component, as the owner's entry wds2m4d5 ("Compose UI: previews, ImageVector icons, own theme object, package per component") asks: `SsrComposeStringResource` and `SsrComposeStringResourceManager` (with `LocalSsrComposeStringResourceManager` in the same file) in `dev.tonholo.ssrcompose.consumer.text`; the English manager, `EnglishStringResourceManager`, in `dev.tonholo.ssrcompose.consumer.internal.text`; `ScreenProblemMessage`, `ScreenProblemMessageColors` and `ScreenProblemMessageDefaults` in `dev.tonholo.ssrcompose.consumer.problem`. `ScreenProblem` stays in `dev.tonholo.ssrcompose.consumer`, where `SsrScreen` names it.
- An app's own `unavailable` composable must handle the two new cases. For them it can show its own text, or the library's through `LocalSsrComposeStringResourceManager.current`. The contract has the example.

Plurals: no text needs one today. The contract fixes the signature for when the first one appears: an enum `SsrComposePluralResource` and `@Composable fun pluralResource(resource: SsrComposePluralResource, quantity: Int, vararg arguments: Any): String`. `quantity` picks the plural form and is not added to `arguments` by itself, which matches `pluralStringResource(id, count, vararg formatArgs)` on Android. Shipping it now would mean an enum with no entries. A `when` over such an enum does not compile without an `else` branch (probe below), so every app's manager would need dead code. The owner chose on 2026-11-19 to wait for the first plural text and to add the lookup then, with a default body.

Compatibility: adding a key to an enum is a source break for an app whose manager has an exhaustive `when` with no `else`. That is on purpose: the build tells the app that a new text needs a translation. An app that does not want this writes `else -> fallback.stringResource(...)`. Adding the plural lookup later as a new interface member with a default body that uses the English manager should keep existing managers compiling. Whether it also keeps them binary compatible on the JVM was not checked.

### 3. Data flow

1. The session ends a conversation or hears from the producer, and publishes `SessionState.Problem(problem)` with a `ScreenProblem` that holds no library text.
2. `SsrScreen` calls `unavailable(problem, retry)` in composition.
3. The default `unavailable` calls `ScreenProblemMessage`, which reads `LocalSsrComposeStringResourceManager.current`.
4. For `ConnectionEnded`, `AnswerUnreadable` and `Mismatch` it asks the manager for the matching key. For `Refused` and `Unavailable` it shows the producer's `reason` as it is. It always asks for `ScreenProblemRetry`.
5. It draws with its style parameters.

The retry rules do not change. A retry after `Refused`, `ConnectionEnded` or `AnswerUnreadable` introduces the app again, because the conversation has ended. A retry after `Unavailable` or `Mismatch` asks for the screen again.

Failure: a manager that throws, during composition, stops the app's recomposer (R7), and a `try/catch` cannot wrap a composable call. The library cannot guard against it. The manager's documentation says it must return a text for every key and must not throw.

A language change: a manager that reads Android resources through `stringResource` reads the configuration, so it gives the new text on the next composition. The English manager does not change. A producer `reason` stays in the language it was written in until the producer sends another message. Not run on a device.

### 4. Constraints and non-functional requirements

- Cost: `ScreenProblemMessage` does two lookups per composition, and only while a problem shows. There is one message per `SsrScreen`. No lookup runs while a screen is shown, so a screen of thousands of components (R7) pays nothing.
- Reliability: the default works with no setup. An app that does not localize keeps today's English text and look.
- Maintainability: each English text has one definition, in the internal English manager. `ConsumerSession` holds no user text.
- Trade-off: a key enum and a `vararg` lookup give up type-checked arguments. No key has arguments, and the shape matches Android, so an Android app writes the least code. Typed arguments were turned down below.

### 5. Invariants

1. With no manager provided, every key shows its English text of today, word for word. A common host test reads each key through the default manager and compares it with a fixed table.
2. Every key is used by `ScreenProblemMessage`. A device test shows every library-written problem with a manager that returns a marked text for each key, and checks that `SsrComposeStringResource.entries` were all shown.
3. `ConsumerSession` holds no user text. Check: `grep -n 'const val' consumer/src/commonMain/kotlin/dev/tonholo/ssrcompose/consumer/internal/session/ConsumerSession.kt` lists only `MIN_WINDOW_PX` and `SEND_CANCELLED`.
4. The only English user text in `consumer/src/commonMain` is in `internal/text/EnglishStringResourceManager.kt`. Check: `grep -rn '"' consumer/src/commonMain` finds no text for the user in any other file. The reviewer sorts the hits with the list above.
5. No Material dependency. Check: `grep -n material consumer/build.gradle.kts` finds nothing.
6. All of it is in common code. Check: `grep -rn 'android\.' --include='*.kt' consumer/src/commonMain` finds nothing (ui-compose rule 1).
7. The default colours are readable in light and in dark theme. The device test that measures the luminance range inside the text and the retry label runs for both themes.
8. The default look is unchanged. The device test that checks the insets and the gap of today's message still passes against `ScreenProblemMessage` with its defaults.

### 6. Risks and edge cases

- An app's manager that misses a key fails its own build when it has no `else`, and shows the fallback when it has one. No runtime path shows a key name.
- A manager that throws crashes composition. Handled by documentation only (see Data flow).
- `isSystemInDarkTheme()` reads the system theme. An app with its own theme switch passes its own `colors`. The defaults never guess the app's theme.
- An app that changes only `containerColor` can end with unreadable text. That is the app's choice; the defaults pair both colours.
- The producer refuses an introduction it cannot read before it knows a language, and may have no text in the app's language. Both give an English reason (messages.md rule 7). The app can map `Refused.kind`. `RefusalKind` has four entries today (`core/handshake/RefusalKind.kt`).
- Input size: four keys, one message per `SsrScreen`. Nothing grows with the screen.

### 7. Rejected alternatives

- Android string resources, which an app overrides and translates by resource merging. It works on Android only, so common code would need an `expect`/`actual` for every text, and the session would need a `Context` to write its reasons. An override is matched by name with no compile check, so a typo falls back to English in silence. A probe on 2026-11-19 put `consumer/src/androidMain/res/values/strings.xml` in the module and built `:consumer:bundleAndroidMainAar`: the build passed and the AAR held only `R.txt` (empty), `AndroidManifest.xml`, `classes.jar` and the AAR metadata, so the Android library target in this build packages no resources unless the build is changed. An Android app keeps the good part of this option: its manager can return its own `R.string` texts, which it translates as usual.
- Compose Multiplatform resources. They are not in `gradle/libs.versions.toml`, they would add a dependency, and they generate a `Res` class owned by the library, with no documented way for an app to replace a library's text. The owner's need is languages the library does not ship. Read in the Compose Multiplatform documentation, not run.
- Writing the text in the session with a manager that is not composable. The session runs outside composition, so Android's `stringResource` cannot be used there, and the text would stay in the old language after a change.
- Keeping `reason: String` and filling it in `SsrScreen` from the manager before the session starts. The public API would not change, but the app could not tell a connection that ended from a producer's reason, and the text would freeze.
- A `ProblemReason` type in place of `reason: String`, with one case for producer text and one per library key. It answers the brief's "key and arguments" question in its general form, but every app that reads `reason` would have to resolve it, and two new `ScreenProblem` cases say the same with less code. No library text takes an argument.
- One interface member per text, with typed arguments, in place of a key enum. It checks arguments at compile time, but an app must implement every member, a new text breaks every app with no `else` to fall back on, and it moves away from the owner's sketch for a gain no key needs yet.
- One style object, `ScreenProblemMessageStyle`, filled by `ScreenProblemMessageDefaults.style(...)`, as the first version of this design proposed. It kept the composable at four parameters and every field in one factory. The owner chose Material's shape instead on 2026-11-19.
- A style `CompositionLocal` alone, or style parameters on `SsrScreen`. The first hides the look from the call site, and the second mixes the problem message into the screen's API.
- A slot for the retry button. Not asked for. It can come later without breaking the style parameters.
- An opt-in annotation for the new API. The types are plain Compose types and our own, none of them alpha or experimental (rule 9), and the whole library is unreleased. An opt-in would add an annotation to every app's call for no stated risk.

### 8. Slices

Each slice holds one new mechanism, is its own public API change (rule 11), and is built tests first.

1. Problems that hold no text. Add `ScreenProblem.ConnectionEnded` and `ScreenProblem.AnswerUnreadable`. The session publishes them in place of `Unavailable(CONNECTION_ENDED)` and `Refused(IntroductionNotValid, UNREADABLE_MESSAGE)`, and loses both constants. `DefaultScreenProblem` maps them to the same English text for now. Tests first: the `ConsumerSessionTest` cases that assert `Refused` for an unreadable frame, an early `Screen` or `ScreenUnavailable` and another contract version expect `AnswerUnreadable`, and those that assert `Unavailable` for a failed or ended connection expect `ConnectionEnded`. A `DefaultScreenProblemTest` case shows both with today's English text. The session still ends the conversation for an unreadable answer, as [messages.md](contracts/messages.md) rule 10 asks; only the `ScreenProblem` it shows changes. T085 in `tasks.md` says an undecodable message "is treated as a refusal" and that the default message says why; the engineering-manager updates it to name `AnswerUnreadable`.
2. Replaceable text. Add `SsrComposeStringResource`, `SsrComposeStringResourceManager`, the internal English manager and `LocalSsrComposeStringResourceManager`. `DefaultScreenProblem` reads its four texts through the local. Tests first: invariant 1 in `commonTest` with `composeOnce`; invariant 2 as a device test; and an `SsrScreen` device test in which an app's manager, provided above `SsrScreen`, shows its own text when the connection ends.
3. Styleable problem message. Move `DefaultScreenProblem` to the public `ScreenProblemMessage` in `consumer/problem/`, add `ScreenProblemMessageColors` and `ScreenProblemMessageDefaults`, and call it from `SsrScreen`'s default `unavailable`. Tests first: a device test passes `colors` with a container and a content colour that the defaults never use and finds both in the captured image, and passes a `contentPadding` and a `spacing` and measures them; invariants 7 and 8 run against the defaults; a common host test checks `ScreenProblemMessageColors` equality and that `copy` with `Color.Unspecified` keeps the old colour. `ScreenProblemMessage` gets one `@PreviewLightDark` preview in `ScreenProblemMessage.kt`, the same file as the composable. The owner's entry wds2m4d5 says: "the preview lives in the same file as the composable it previews, never in a separate previews file."
4. Plurals, only when the first plural text is specified. Add `SsrComposePluralResource` and `pluralResource` with the signature above. Tests first: a manager that records `quantity` and returns a marked text for 0, 1 and 2, and the English text of each form.

### 9. Validation checklist

1. `ScreenProblem` has exactly the cases `Refused`, `Unavailable`, `ConnectionEnded`, `AnswerUnreadable` and `Mismatch`, as in the contract.
2. `ConsumerSession` holds no user text (invariant 3), and publishes `ConnectionEnded` and `AnswerUnreadable` in the cases of slice 1.
3. `SsrComposeStringResource` has the four keys, with the English text of the table, word for word (invariant 1).
4. `SsrComposeStringResourceManager` has one `@Composable` lookup with the signature of the contract, and no plural lookup yet.
5. `LocalSsrComposeStringResourceManager` is a `staticCompositionLocalOf` whose default is the English manager, and the English manager is internal.
6. `ScreenProblemMessage` reads every text through the local, shows producer reasons as they are, and its test proves that an app's manager replaces each key (invariant 2).
7. An `SsrScreen` device test proves that a manager provided above `SsrScreen` reaches the default `unavailable`.
8. `ScreenProblemMessage` takes the parameters of the contract in its order, each style parameter defaulting to `ScreenProblemMessageDefaults`, applies `modifier` to its root, and `SsrScreen`'s default `unavailable` calls it with `Modifier.fillMaxSize()`.
9. `ScreenProblemMessageColors` is `@Immutable`, not a `data class`, with a public constructor, `copy`, `equals` and `hashCode`. `ScreenProblemMessageDefaults.colors()` and the plain values give today's look.
10. A device test proves that an app's colours, padding and gap reach the composable, and the default look is unchanged and readable in both themes (invariants 7 and 8).
11. No Material dependency and nothing Android in common code (invariants 5 and 6).
12. Public in `consumer` are only the types the contract lists under "What is public".
13. A rendering slice names the device and has screenshots in light and dark theme (ui-compose rule 7).

### Probes

Run on 2026-11-19 in this workspace with throwaway files that were deleted afterwards. `git status --short` was empty after each.

- An app's manager replaces a text. A `commonTest` file defined a key enum, a manager interface with a `@Composable` `vararg` lookup, an internal English manager, a `staticCompositionLocalOf` with it as the default, and an app manager that replaces one key and falls back to the manager it read before providing itself. Composed with `composeOnce`, it read "Try again" with no provider, then "Tentar de novo" and the English mismatch text inside the provider. `./gradlew :consumer:testAndroidHostTest --tests dev.tonholo.ssrcompose.consumer.probe.ProbeTest` exited 0 with `tests="1"` and `failures="0"`.
- An empty enum. `internal enum class ProbeEmpty` with `when (value) {}` failed `:consumer:compileAndroidMain` with `'when' expression must be exhaustive. Add an 'else' branch.` (`NO_ELSE_IN_WHEN`), and detekt reported `EmptyWhenBlock`.
- Android resources: see the first rejected alternative.

Not checked:

- Binary compatibility of an app's compiled manager after the plural lookup is added with a default body. Needs two builds of an app against two versions of `consumer`; not run.
- A language change during a session on a device.
- Why detekt's `LongParameterList` does not report `SsrScreen`'s seven parameters. A probe with a seven-parameter function and a seven-parameter composable in `commonMain` was not reported either, though the default limit is 5 (`allowedFunctionParameters: 5` in detekt 2.0.0-alpha.6's default config). A ten-parameter composable with defaults, the shape of `ScreenProblemMessage`, was not reported either: `./gradlew :consumer:detekt` exited 0 on 2026-11-19. The style choice does not rest on detekt.

## R20. Server-rendered content (#194)

Design for #194, written on 2026-11-20 in design mode. It replaces the content model of Feature 1, where a screen was an ordered list of the app's catalog components shown top to bottom. The owner turned that model down on 2026-11-20 because it is server-driven rendering: "THIS PROJECT IS NOT SERVER-DRIVEN RENDERING, NON-NEGOTIABLE." From this version on, the producer renders and arranges every screen itself, and an app component is only a hole inside server-rendered content.

The owner chose these on 2026-11-20, through the engineering-manager:

- The producer measures and places what a screen shows, with the density, size and font scale the device sent: "The server knows this information and can use it to properly measure the components."
- Server developers write normal Compose UI foundation on the producer, read through public semantics and inspector API, and the producer sends a positioned document ([ADR 0015](../../docs/adr/0015-producer-renders-with-compose-ui-foundation.md)).
- App components are measured on the device, which reports their size back ([ADR 0016](../../docs/adr/0016-app-components-measured-on-the-device.md)).
- The font scale joins the handshake. The app's fonts reach the producer in this item, in a separate pinned font bundle with its own version, and the catalog stays build time only ([ADR 0017](../../docs/adr/0017-app-fonts-travel-in-a-pinned-font-bundle.md)).
- Text in the default family is measured and drawn with Roboto, bundled in both the producer and the consumer, never with the system font; a text with no family is refused ([ADR 0020](../../docs/adr/0020-bundle-roboto-as-the-default-font.md)).
- The layout direction in the device context, the viewport in `OpenScreen` and `ViewportChanged` join contract version 1.
- A minimal local state node lands now, as the switcher designed here ([ADR 0018](../../docs/adr/0018-local-switcher-for-interactions-without-a-round-trip.md)).
- The contract stays at version 1, with the content model replaced inside it ([ADR 0019](../../docs/adr/0019-contract-version-1-carries-the-document.md)).
- The producer wraps every screen in its own `Box`.
- `LazyColumn` is not supported in this version. Server content scrolls with `verticalScroll`.
- The block on required slots that accept only producer content lifts in T076. The consumer-only restriction stays unchecked, as FR-011 says.
- One opt-in marker in `core` holds ([ADR 0014](../../docs/adr/0014-one-opt-in-marker-for-internal-api.md)).

### The principle I gate

1. Who owns what a screen shows and how it is arranged? The producer. The screen is a Compose function that runs in the producer's composition, and Compose UI foundation measures and places it with the device's values ([Probes](#probes-1), P4 and P5). The document carries the position and size of every node it holds, relative to its parent (section 2). The consumer places each node at the box it was given and decides nothing about order, size or position. The only sizes the device decides are the sizes of the app's own components, and the producer places those too, after the device reports them ([ADR 0016](../../docs/adr/0016-app-components-measured-on-the-device.md)).
2. Can a screen with no app components render, also in an app with an empty catalog? Yes. A document is made of `Box`, `Text`, `Scroll` and `Switcher` nodes with no `Component` node at all (example 1 in section 2). The consumer renders it with `ComponentBindings {}`. Slice 4 proves it with a test of a screen with no app component and empty bindings, and T055 keeps the empty renderer index test.
3. Does any line of the contract, data model, ADR or research make the app's catalog the only content, or give the consumer a layout decision? No. The `Components` list and the top-to-bottom fallback are removed (section 2, "What happens to the list of components"). [messages.md](contracts/messages.md#screen-content), [data-model.md](data-model.md#screen) and [spec.md](spec.md) FR-034 now say the producer places every node. The consumer never arranges nodes without a box from the producer, also inside slots: a slot's content is a positioned document too, laid out by the producer in the space the app's component gives that slot.
4. Does composable logic run on the producer, with the consumer showing the result without that code? Yes. State, effects, conditions and loops are ordinary Compose code in the producer's composition. Probe P4 recomposed a screen from 1 to 3 rows when a state changed and laid the rows out at 0, 33 and 66 px. The consumer gets only the document. The one piece of behaviour the consumer runs is the switcher of [ADR 0018](../../docs/adr/0018-local-switcher-for-interactions-without-a-round-trip.md), and every variant it can show was composed and measured by the producer.

### 1. Boundaries

- `core` owns the wire model: the screen document and its nodes, in a new package `dev.tonholo.ssrcompose.core.document`, the new messages, and the new handshake fields. It checks a document when it decodes one. It also owns the font bundle format, in `dev.tonholo.ssrcompose.core.font`, since the build tooling and the running producer both read it. It knows nothing about Compose.
- `producer` owns rendering: it hosts each open screen in an `ImageComposeScene` of Compose UI desktop, reads the result into a document, assigns action IDs, runs the measuring rounds of app components, loads fonts, and keeps the guard. It does not know the consumer's renderer. Its new dependencies are Compose UI, foundation and Skiko, all JVM. A server adds the Skiko runtime jar of the OS it runs on.
- `consumer` owns drawing: it places each node at its box with one internal positioned layout, draws text, decorations, scroll containers and switchers with Compose foundation, measures app components and reports them, and resolves font families by name. It never arranges nodes on its own.
- `tooling` and `gradle-plugin` own the font bundle steps: declaring the fonts, bundling, checking, saving, fetching, pinning, verifying and putting them in the server's resources ([contracts/build-tooling.md](contracts/build-tooling.md#fonts)). `catalog-model` does not change for fonts.
- `compiler-plugin` does not change for this design. Bridges still call `ConsumerComponent` with the same parameters.

### 2. Interfaces and contracts

#### The document

A `Screen` message carries a `ScreenDocument`. Its root is the `Box` the producer wraps around every screen, with the size of the viewport.

Every node has an `id`, an `x` and `y` from the top-left corner of its parent in dp, and a `width` and `height` in dp. `x` is always from the left edge, in either layout direction. `x` and `y` can be negative, where Compose placed a node past its parent's top or left edge, for example with an `offset` or a centered child larger than its parent; the producer sends what Compose laid out and the consumer places it there, so the consumer never refuses a position the producer can send. Positions are bounded to 100,000 dp either way, and sizes are never negative. Ids are unique in a document and stay the same while the node lives on the producer, since they are the producer's semantics ids.

| Node | Fields | Drawn as |
|---|---|---|
| `Box` | `decoration` (background color, border width and color, shape), `click`, `semantics`, `children` | a `Box` of that size with that decoration, holding its children |
| `Text` | `text`, `style` (`family`, `size` in sp, `weight`, `italic`, `color`, `lineHeight` in sp, `align`), `maxLines`, `overflow` (`Clip` or `Ellipsis`), `click`, `semantics` | `BasicText` inside that box |
| `Scroll` | `orientation` (`Vertical` or `Horizontal`), `contentWidth`, `contentHeight`, `children` | a box that scrolls its content on the device, with no round trip |
| `Switcher` | `selected`, `largestWidth`, `largestHeight`, `variants` (2 to 16 lists of nodes) | the chosen variant; the box is the size of `selected`; the device keeps its choice by node id |
| `Component` | `name`, `values`, `callbacks`, `slots`, `maxWidth`, `maxHeight` (null when unbounded) | the app's own composable, measured within the max size and placed at `x`, `y` |

`width` and `height` of a `Component` are the size the device last reported, or 0 before it reported one. A slot is a `SlotContent`: its own `width`, `height` and `nodes`, positioned from the slot's top-left corner, laid out by the producer in the space the device reported for that slot.

`click` is one of:

- `Remote`: `action` (an action ID), `label`, `role`. A tap sends `Trigger` with no arguments, the same message app callbacks use.
- `Local`: `switcher` (a node id) and `variant`. A tap shows that variant at once and sends `Switched`. The rule, chosen by the owner on 2026-11-20 ([ADR 0018](../../docs/adr/0018-local-switcher-for-interactions-without-a-round-trip.md)): A `Switcher` node's box is the size of its `selected` variant, the one the producer laid the screen out with, and `largestWidth` and `largestHeight` give the size of its largest variant. A tap on a `Local` click shows the chosen variant at once, in a fixed box of the largest variant's size anchored at the switcher's top-left corner and drawn above the nodes after it, and sends `Switched(switcher, variant)`. The producer makes that variant the selected one, lays the screen out again and sends a new revision in which the switcher's box and the content around it are placed for that variant. Until that revision arrives, a larger variant can cover the content below it and a smaller one leaves a gap: the device moves nothing on its own. The device keeps its choice by switcher id across updates, also when a revision still names the older selected variant.

`semantics` holds `description`, `heading` and `role`, read from the producer's semantics. Colors are written `"#AARRGGBB"`. A shape is `Rectangle` or `Rounded` with four corner radii in dp.

What is left for later: images, gradients and other brushes, shadows, custom drawing (`Canvas`, `drawBehind`), lazy lists, text with several styles (`AnnotatedString`), text fields, animations, interactions with no round trip other than the switcher and scrolling, app theme tokens, and sending only what changed. The producer refuses what it can see and cannot send: an image, a brush background, a shape other than a rectangle or rounded corners, a font family not in the session's font bundle, a weight or italic its font family has no file for (section "Fonts"), a lazy list, a text with several styles, more than 10,000 app components (rule 5a of messages.md), or an app component inside a switcher variant (a variant the device does not show is never measured, so its screen would stay pending). Each one stops the screen with `ScreenUnavailable` and a report that names the screen and the reason. Custom drawing has no semantics, so the producer cannot see it, and it is lost without a report. The server developer documentation says so.

#### Example 1: a screen with no app component

```json
{"type":"Screen","screen":"home","revision":1,"pending":false,"content":{"root":{"type":"Box","id":1,"x":0.0,"y":0.0,"width":411.4,"height":843.4,"children":[
  {"type":"Box","id":3,"x":16.0,"y":16.0,"width":379.4,"height":96.0,"decoration":{"background":"#FF6200EE","shape":{"type":"Rounded","topStart":12.0,"topEnd":12.0,"bottomEnd":12.0,"bottomStart":12.0}},"click":{"type":"Remote","action":"a1","label":"Add one","role":"Button"},"children":[
    {"type":"Text","id":4,"x":16.0,"y":16.0,"width":200.0,"height":24.0,"text":"Tapped 2 times","style":{"size":16.0,"weight":500,"color":"#FFFFFFFF"}}
  ]},
  {"type":"Text","id":5,"x":16.0,"y":128.0,"width":379.4,"height":40.0,"text":"Served by the producer","style":{"size":14.0,"weight":400,"color":"#DE000000"},"maxLines":2,"overflow":"Ellipsis"}
]}}}
```

#### Example 2: a screen with one app component

The first `Screen` is pending, since the device has not measured the `Card` yet. The consumer composes and measures it but does not show it, and answers:

```json
{"type":"ComponentMeasured","revision":1,"components":[{"id":7,"width":379.4,"height":120.0,"slots":{}}]}
```

The producer lays the screen out again with that size and sends:

```json
{"type":"Screen","screen":"home","revision":2,"pending":false,"content":{"root":{"type":"Box","id":1,"x":0.0,"y":0.0,"width":411.4,"height":843.4,"children":[
  {"type":"Component","id":7,"x":16.0,"y":16.0,"width":379.4,"height":120.0,"maxWidth":379.4,"maxHeight":null,"name":"Greeting","values":{"title":{"type":"Text","value":"Hi"}}},
  {"type":"Text","id":8,"x":16.0,"y":152.0,"width":379.4,"height":20.0,"text":"Below the app's card","style":{"size":14.0,"weight":400,"color":"#FF000000"}}
]}}}
```

#### Example 3: a slot holding server content

The device reported the `Section` at 379.4 by 160 dp and gave its `content` slot 347.4 dp of width with no height bound. The producer laid the slot's server content out in that space, and that content holds another app component:

```json
{"type":"ComponentMeasured","revision":4,"components":[{"id":9,"width":379.4,"height":160.0,"slots":{"content":{"maxWidth":347.4,"maxHeight":null}}},{"id":12,"width":120.0,"height":40.0,"slots":{}}]}
{"type":"Component","id":9,"x":16.0,"y":16.0,"width":379.4,"height":160.0,"maxWidth":379.4,"maxHeight":null,"name":"Section","values":{"title":{"type":"Text","value":"Today"}},"slots":{"content":{"width":347.4,"height":64.0,"nodes":[
  {"type":"Text","id":11,"x":0.0,"y":0.0,"width":347.4,"height":20.0,"text":"Rendered by the server, inside the app's Section","style":{"size":14.0,"weight":400,"color":"#FF000000"}},
  {"type":"Component","id":12,"x":0.0,"y":24.0,"width":120.0,"height":40.0,"maxWidth":347.4,"maxHeight":null,"name":"PayButton","callbacks":{"onClick":"a2"}}
]}}}
```

#### What happens to the list of components

`ScreenContent`, its top-to-bottom list, `PlacedComponent` as a separate wire type, and `PlacedComponent.slots` as a list of components are removed. `ProvisionalScreenContent` and the loop in `PlacedComponents.kt` are deleted. Nothing on the wire or in the consumer can show a screen as a list of app components any more: the only way content reaches the device is a positioned document. FR-034 is rewritten to match.

#### Messages

Consumer to producer:

- `Introduce`: `device` gains `fontScale` (above 0, at most 4) and `layoutDirection` (`Ltr` or `Rtl`), and the introduction gains `fontVersion`, absent when the app declares no font. The producer measures text with the font scale and the session's font bundle, and lays out with the direction. `RefusalKind` gains "font version not supported".
- `OpenScreen`: gains `viewport`, the width and height in dp the app gives `SsrScreen`. An unbounded side is sent as the window's size.
- `ViewportChanged`: `viewport`. Sent when the space the app gives `SsrScreen` changes.
- `ComponentMeasured`: `revision` and a list of `components`, each with `id`, `width`, `height` and, per slot, the `maxWidth` and `maxHeight` the app gave it (null when unbounded). Sent at most 20 times per second, with every component whose size or slot space changed, merging changes in between; at most 10,000 measurements in one message (section 4, Security).
- `DeviceChanged` and `Trigger` keep their shape. A `Trigger` for a `Remote` click has no arguments.
- `FontVersionChanged`: `fontVersion`, absent when the app now declares no font, sent when `SsrScreen`'s fonts change to another version during a session. Chosen by the owner on 2026-11-20. A message of its own, not a field of `DeviceChanged`, since fonts are the app's and `DeviceChanged` has no refusal path. It counts against the trigger limit; over it, the latest version is kept and applied the next second, never dropped.
- `Switched`: `switcher` and `variant`, sent after a local click has already shown the variant. It counts against the trigger limit; over it, the producer keeps the latest variant of each switcher and applies it when the next second starts, so a `Switched` is never lost. A switcher the screen no longer has is ignored, a variant it does not have is refused, and both are reported.

Producer to consumer:

- `Screen`: `screen`, `revision` (rises by one with each `Screen` in a session), `pending`, `fontVersion` (the font version it was laid out with) and `content`. `pending` is true while an app component has no reported size or a slot has no reported space.

#### Producer API

- Server screens are plain Compose UI foundation code: `Column`, `Row`, `Box`, `Spacer`, `BasicText`, `Modifier` and `verticalScroll`, imported from `androidx.compose`. The library defines no layout composables of its own.
- `LocalSwitcher(variants: Int, initial: Int = 0, modifier: Modifier = Modifier, content: @Composable LocalSwitcherScope.(variant: Int) -> Unit)` and `LocalSwitcherScope.switchTo(variant: Int): Modifier` in `dev.tonholo.ssrcompose.producer`.
- `Consumer` (from `LocalConsumer.current`) gains `fonts: ConsumerFonts`, with `family(name: String): FontFamily`, which fails with a clear message for a name the session's font bundle does not have.
- `ConsumerComponent` keeps its signature. Its body becomes a Compose UI `Layout` that sizes itself to the device's report and carries a semantics property with the component's name, catalog version, values and callbacks, so the reader finds it. Each slot is a child `Layout` measured in the space the device reported for it.
- `ProducerLimits` gains `measuringRounds` (4 by default), `measurementsPerSecond` (30 by default, for `ComponentMeasured` and `ViewportChanged`, apart from the trigger limit) and `documentBytes` (8 MiB by default).
- `ProducerObserver` gains `onScreenNotSendable(screen, reason)` for content the producer cannot send, and `onLayoutNotSettled(screen)` when the measuring rounds run out.
- The font bundles are given to the producer by the build (section "Fonts"), or by hand with `fonts(version) { family(name) { font(bytes, weight, italic) } }` in `Producer { }`.

#### Consumer API

- `SsrScreen` gains `fonts: ConsumerFonts`: the app's font version and families, by hand for now, with a default that holds neither. The build creates it from the declared fonts later (T055).
- `PlacedComponentScope.slot(name): @Composable () -> Unit` (T078) draws the slot's positioned content and reports the space the app gave it.
- `ComponentBindings {}` with no binding is a valid argument.

#### Fonts

The app's fonts travel in a font bundle of their own, with its own version, separate from the catalog ([ADR 0017](../../docs/adr/0017-app-fonts-travel-in-a-pinned-font-bundle.md)). The format is in [contracts/font-bundle.md](contracts/font-bundle.md), and the build steps in [contracts/build-tooling.md](contracts/build-tooling.md#fonts).

1. What is in it: `fonts.json` (format, `version`, and one entry per file with family, weight, italic, file name and SHA-256 fingerprint) and the font files, at `<family>/<weight>-<normal or italic>.<ttf or otf>`. Roboto is not in it.
2. How it is built: the app developer declares a font version and the families with their files in the module that sets `catalogVersion`. `ssrFontBundle` writes the bundle and the font version into a generated resource for the consumer. `ssrFontCheck`, part of the build, fails when the declared fonts differ from the saved bundle of that version, so a changed font needs a new font version. `ssrFontDump` saves it to `ssr/fonts/<version>/`, which is committed and published like a catalog.
3. How its version is pinned and checked on the server: the server project declares each font version it holds with its location. `ssrFontSync` fetches `fonts.json` and the files, checks every fingerprint, saves them in `ssr/fonts/<version>/` and records the fingerprint of `fonts.json` in `ssr/fonts.lock`. `ssrFontVerify`, part of the build, checks the saved files against the lock file and `fonts.json`.
4. How the server loads it: `ssrFontResources`, cacheable, copies every saved bundle into the server's resources under `ssrcompose/fonts/<version>/` with an index of the versions. `Producer { }` reads them at start, checks each file's fingerprint again and that Skia's `FontMgr.makeFromData` reads it as a font (probe P11; Compose's own `Font(identity, data)` falls back silently, probe P8), and builds one set of `FontFamily`s per version. A missing or unreadable file makes `Producer { }` fail at start and names the version, the family and the file.
5. How the two sides agree at run time: the app sends its font version in `Introduce` (`fontVersion`, absent when it declares no font). The producer refuses an app whose font version it does not hold, with the kind "font version not supported". A session's `LocalConsumer.current.fonts` holds that bundle's families and the default family, the bundled Roboto. When `SsrScreen`'s fonts change to another version during a session, the consumer sends `FontVersionChanged`; the producer switches the session's fonts and lays the screen out again with its state kept when it holds that version, and otherwise answers `Refused` with "font version not supported", which ends the session. Each `Screen` names the font version it was laid out with, and the consumer draws each document with the fonts of that version, keeping the older fonts until a `Screen` with the new version arrives, so it never draws with fonts the producer did not measure.
6. On the producer, the reader maps each text's `FontFamily` back to its family name by the identity of its loaded fonts (probe P11). A family that is not from the session's bundle, and not the default, stops the screen with `ScreenUnavailable`. A text in the bundled Roboto family writes no `family`.
7. On the consumer, `SsrScreen`'s `fonts` holds the same families, from the app's own resources. A `family` it does not have is a mismatch, like a component with no binding: `ScreenProblem.Mismatch`, whose `component` then holds the family's name.
8. The default font is Roboto, carried by both the producer and the consumer ([ADR 0020](../../docs/adr/0020-bundle-roboto-as-the-default-font.md)). A text in a document with no `family` is drawn with it, never with the device's system font. The producer cannot make `FontFamily.Default` resolve to Roboto, so server code names a family: the library's default family is the bundled Roboto, reached through `LocalConsumer.current.fonts` (T128 adds `fonts` to `Consumer`; it does not exist yet). A text with no `family`, or with `FontFamily.Default`, stops the screen with `onScreenNotSendable`, which the reader does in T122 part B. A text in the bundled Roboto family writes no `family` in the document, and the consumer draws it with its own Roboto. Slice 3 pins that the bundled Roboto named in a scene measures with the bundled files. On Compose Multiplatform 1.12.2 desktop there is no public way to remap the default: `FontFamily.Resolver` is a sealed interface, `FontFamilyResolverImpl`, `SkiaFontLoader`, `PlatformResolveInterceptor` and `FontCache` are Kotlin `internal`, and `BasicText` reads no default text style. Measured on 2026-11-20 on macOS at 16 sp, density 1, for the text "Served by the producer, measured with Roboto": 355 by 19 px with the system font (no family or `FontFamily.Default`), 330 by 19 px with the bundled Roboto named as the family, and between 330 and 331 px as Skia measures the bundled file directly.
9. Weights and italic: Skia and Android pick a missing weight, or fake bold and italic, in different ways, which would move line breaks. So the producer only sends a text whose weight and italic match a file of its family exactly. For the default Roboto those are 400, 500 and 700, and 400 italic ([ADR 0020](../../docs/adr/0020-bundle-roboto-as-the-default-font.md)). Any other weight or italic stops the screen with `onScreenNotSendable`, which names the family, the weight and the italic. The device then draws with that exact file too, so nothing is faked on either side.

### 3. Data flow

1. The consumer waits for a positive window size, then sends `Introduce` with the font scale, the layout direction and its font version, and `OpenScreen` with the viewport. An app whose font version the producer does not hold is refused.
2. The producer creates an `ImageComposeScene` with the viewport in pixels, `Density(density, fontScale)`, and the session's dispatcher as `coroutineContext`, and sets its content to `Box(Modifier.fillMaxSize()) { screen() }`. The direction is given through `LocalLayoutDirection` at the root of the content, wrapping the root `Box`, and a direction change during a session updates that provided value, not the scene. The reason: on Compose Multiplatform 1.12.2 the `ImageComposeScene` constructor and the `layoutDirection` setter that take a direction are `@ExperimentalComposeUiApi`, and a Row of two 10 dp boxes in an Rtl scene put the second box at x = 0 with the provider and no opt-in. The scene is created, driven and closed on the session dispatcher, which is also given as `coroutineContext`.
3. After each change it renders the scene, reads the semantics tree into a document (section 4), runs the guard over every `Component`, assigns action IDs to remote clicks, and sends `Screen`. A document over `documentBytes` is not sent: `ScreenUnavailable` and `onScreenNotSendable`.
4. The consumer checks the document while decoding it (section 5, invariant 3). A pending document is composed and measured but not placed; the consumer keeps showing the last document that was not pending, or the loading state before the first one. Each `Component` is measured within its max size, and each slot reports its space. The consumer sends one `ComponentMeasured` with every changed component for the revision it holds, at most 20 times per second, merging changes in between.
5. The producer ignores a `ComponentMeasured` for an older revision. Otherwise it writes the sizes into the holes' state, which lays the scene out again, and goes back to step 3. The document stays pending until every app component has a reported size and every slot a reported space. That takes one round per level of app components nested in slots, since a component inside a slot can only be measured after its parent's slot was laid out; the frame's depth limit (128 JSON levels, at least 4 per slot level) bounds it at 31 levels. Once everything has a reported size, sizes that keep changing stop after `measuringRounds` more rounds for one change: the producer sends the latest reported sizes, not pending, and calls `onLayoutNotSettled`. Every app component then has a reported size, so FR-076 holds.
6. A tap on a remote click sends `Trigger`. The producer runs the node's semantics `OnClick` action through the same path as a callback (T067), and a state change goes back to step 3. A tap on a local click shows the variant at once in a box of the largest variant's size and sends `Switched`; the producer makes it the selected variant, which lays the scene out again, and goes back to step 3, so the content around the switcher moves into place one round trip later. The device keeps its choice by switcher id meanwhile.
7. A device change, a viewport change, or a font scale change updates the scene's size or density and lays it out again. A `FontVersionChanged` for a held version switches the session's fonts and lays it out again. Remembered state is kept in each case. A `FontVersionChanged` for a version the producer does not hold ends the session with `Refused`.
8. Failures: screen code that throws, content the producer cannot send, or a guard failure gives `ScreenUnavailable` and the session stays accepted. A document the consumer cannot decode or check ends the conversation with `AnswerUnreadable` (rule 10). A missing binding or font family is `ScreenProblem.Mismatch`.

### 4. Constraints and non-functional requirements

- Reading the result: the reader walks `ImageComposeScene.semanticsOwners` and each `SemanticsNode.children` of the unmerged tree, in order. Text, its style and line count come from `SemanticsProperties.Text` and `GetTextLayoutResult`. Background, border and shape come from each `ModifierInfo.modifier` as an `InspectableValue`, with `isDebugInspectorInfoEnabled` set once for the process. Clicks come from `SemanticsActions.OnClick`, scrolling from the `verticalScroll` and `horizontalScroll` inspector values. App components, slots and switchers come from the library's own semantics properties. A node with nothing to draw, no click and no semantics is folded into its parent, and its children's boxes are moved by its offset. None of these needed an opt-in (probe P9).
- The reader takes each node's position and size from `layoutInfo.coordinates` and `layoutInfo.width` and `height`, the node's outer box. `SemanticsNode.boundsInRoot` is not used: for a box with a background and padding it gave the box inside the padding (probe P11). A text with no font size of its own reports `TextUnit.Unspecified`; the reader writes Compose's default text size for it, which slice 3 pins with a test (the value is not checked here).
- What shows in that tree is observed, not documented: `Column`, `Row`, `Spacer`, a `Box` with only padding and an empty `Box` are not there; text, a background, a border, a click, a scroll container and a lazy list are (probes P5 and P6). Pinning tests (invariant 7) fail the build when a Compose upgrade changes it.
- Cost on the producer: once warm, 22.7 ms and 297 KB of heap per composed screen of 50 text rows (P7); 5.5 ms per frame (P4); about 430 ms for the first scene of a cold JVM. Skia's native memory was not measured. At the 10,000 sessions bound the heap alone is about 3 GB. Slice 9 is a load test at 10,000 open screens that reports heap, native memory and the time to first screen, against SC-005 (under 1 second) and SC-011.
- Size: the whole document is sent on each change. P3 measured 184 KB of JSON for 1,000 rows in a layout tree shape; a positioned node adds about 40 bytes. A screen of 5,000 nodes is about 1.3 MB, under the 8 MiB `documentBytes` default. Encoding and decoding 1,000 rows took 2.6 ms and comparing two equal copies 0.2 ms (P3). Sending only what changed is later work.
- Nesting: each level of children takes 2 JSON levels and a slot takes 4 (component, `slots`, slot, `nodes`). P1 and P2 measured 2 per layout level and 3 per slot in a tree shape. The semantics tree is flatter than the layout tree, since layout-only nodes are folded. `MAX_NESTING_DEPTH` stays 128, which allows about 60 levels of nodes.
- Latency: a screen with app components needs one round trip before it shows, and one more for each round of slot measuring. SC-005 (under 1 second on a local network) covers it; slice 9 measures it.
- Text: the producer measures with the font files the device draws with ([ADR 0017](../../docs/adr/0017-app-fonts-travel-in-a-pinned-font-bundle.md), [ADR 0020](../../docs/adr/0020-bundle-roboto-as-the-default-font.md)), the device's font scale and its density. The consumer draws text in the box it was given with clipping, so a difference left in line breaking clips and does not move other nodes.
- Accessibility: text is real `BasicText`, so TalkBack reads it and the device's text engine draws it. A remote click gets `onClickLabel` and `role`, a node with a description or heading gets them in its semantics, and the order of nodes is the producer's semantics order. App components keep their own semantics.
- Theme and native look: the producer reads the theme from `LocalConsumer.current.device.theme` and writes explicit colors. App components keep the app's theme and look.
- Lazy lists: not supported. The producer lays out all of a `verticalScroll` content. A `LazyColumn` or `LazyRow` stops the screen with `onScreenNotSendable`, since only its visible items are composed (P6: 7 of 1,000).
- Security ([the security rules](../../.ai/rules/security.md)):
  - Rule 3: the consumer checks every document while decoding it (invariant 3), and a broken one ends the conversation without a crash.
  - Rules 6 and 7: `ComponentMeasured`, `ViewportChanged` and the new `DeviceContext` fields are untrusted input. The producer refuses, without laying anything out, a size or space that is negative, not finite, or above 100,000 dp, a font scale outside its range, more than one measurement for one component, and more than 10,000 measurements in one message. 10,000 is far above a real screen (the owner's largest app has 1,085 marked composables in all) and keeps one message near 600 KB at about 60 bytes per measurement; a screen with more app components is not sent, so a first report always fits in one message and FR-076 holds. A measurement for an id that is not an app component of the latest revision is ignored and reported, and never fails the session. `ComponentMeasured` and `ViewportChanged` have their own limit, 30 per second per session by default, apart from the trigger limit, so they never crowd out taps. The consumer sends at most 20 per second, merging changes in between, so a 120 Hz screen or an app component that animates its size stays under it. Over the limit the producer drops nothing: it keeps the latest size of each component and the latest viewport, bounded by the components of the latest revision, and applies them when the next second starts. A measurement that changes no size lays nothing out. With `measuringRounds`, this bounds the layouts one consumer can cause to 30 a second.
  - Fonts: a font file is untrusted input on the server. It is capped at 16 MiB, pinned by its fingerprint in `fonts.json` and by `ssr/fonts.lock`, reviewed when first saved like a catalog, checked again by the producer at start, and parsed only by Skia. On the device, fonts come from the app's own resources. A `fontVersion` in an introduction is untrusted: it is checked like a catalog version and only names a bundle the producer already holds.
  - Rule 8: the producer logs no document, measurement or handshake value at a level that ships.
- Threads: a scene is created, driven and closed only on the session's single-thread dispatcher, as `LiveScreen` does now, and that dispatcher is also given as `coroutineContext`. Passing it as `coroutineContext` alone is not enough: with a single thread executor as the dispatcher and a `LaunchedEffect` that records its thread before and after `yield()`, 29 of 30 scenes ran both on the calling thread, inside the constructor, and 1 of 30 on the executor, measured on 2026-11-20 on macOS; recomposition after a state write and `render()` also ran on the calling thread. Driven from the session thread, composition, the effect before and after `yield()`, and recomposition all ran on it. Whether recomposition runs well on that dispatcher with the shared snapshot notifier is not proven; slice 3 settles it.

### 5. Invariants

1. The consumer never arranges nodes on its own. Check: `git grep -n -E 'foundation.layout.(Column|Row)' -- consumer/src/commonMain ':!consumer/src/commonMain/kotlin/dev/tonholo/ssrcompose/consumer/problem'` finds nothing (the problem message is the app's own UI), and every node is placed by the one internal positioned layout at its `x` and `y`.
2. Nothing on the wire is a bare list of app components. Check: `ScreenDocument.root` is a `DocumentNode.Box`, and `SlotContent.nodes` is a list of `DocumentNode`, any case. A codec test decodes example 3 exactly.
3. The consumer checks a document before drawing it. A negative or non-finite size, a non-finite position or one beyond 100,000 dp either way, a font size not above 0 or above 1,000, `maxLines` below 1, a weight outside 1 to 1,000, a repeated id, a `Local` click to a switcher that is not in the document or a variant it does not have, a `selected` variant the switcher does not have, a largest size smaller than a variant's box, or a switcher with fewer than 2 or more than 16 variants fails decoding with `MessageDecodingException`. Check: one codec test per rule.
4. An app with no `@ConsumerComposable` renders server screens. Check: a consumer test with `ComponentBindings {}` and example 1, and T055's empty index test.
5. Composable logic runs on the producer only. Check: the consumer has no code that runs server code; a producer test changes a state and sees a new `Screen` with a new revision.
6. The guard covers every app component, inside layouts and slots. Check: a producer test with a component of another catalog version inside a slot inside a `Box` gets `ScreenUnavailable`.
7. What the reader sees is pinned. Check: `SemanticsVisibilityTest` in `producer` composes each foundation layout and modifier of section 2 and asserts which nodes appear, so a Compose upgrade that changes it fails.
8. A screen is never shown pending, nor with an app component that has no reported size. Check: a consumer test where a pending first document draws nothing and the settled one draws; a consumer test where a pending update keeps the last shown document on screen; a producer test with app components nested three slots deep that stays pending until all are measured, then sends `pending = false`.
9. Each marked `core` type warns when used without an opt-in, and no public signature of `consumer`, `producer` or `core-testing` names one. Check: `InternalSsrComposeApiTest` counts the warnings; and `PublicSignatureMarkerTest` reads the Kotlin metadata of every compiled class of those three modules with `kotlin-metadata-jvm` and walks each public class, constructor, function, property, supertype and type parameter bound, and every type argument inside them, failing on a marked `core` type. It reads metadata, not source lines, so a signature that spans several lines is covered, and it sees `List<ScreenDocument>`, which the Kotlin Gradle plugin's JVM ABI dump does not: probe P12 showed that dump erases type arguments (`generic (Ljava/util/List;)V`), so the ABI dump is not used for this check. The metadata also tells an `internal` declaration from a public one, so an internal use is not flagged.
10. A font the producer measures with is the font the device draws with. Check: tooling tests that a declared font's bytes reach the saved bundle and the server resources unchanged and that `ssrFontCheck` fails on a changed font with the same version; a producer test that a broken font file stops `Producer { }` at start; a producer test that an unknown font version is refused, at the introduction and in a `FontVersionChanged`; a producer test that a `FontVersionChanged` for a held version keeps remembered state and sends a revision naming the new version; a consumer test that a document is drawn with the fonts of the version it names; a producer test that text in the bundled Roboto family is measured with the bundled files, and one that a text with no family gives `onScreenNotSendable`; and a consumer test that an unknown family is a mismatch.
11. The contract version stays 1. Check: `JsonMessageCodec.contractVersion` and messages.md agree.

### 6. Risks and edge cases

- A Compose upgrade changes what the semantics tree holds, or the inspector names. Invariant 7 fails the build; the reader is changed in the same upgrade.
- Custom drawing is invisible to the reader and lost. Documented for server developers; images and brushes are refused, not lost.
- Text measured on the server can still wrap differently on a device, from another text engine version or locale data. It clips in its box. A follow-up can report overflow back, like a component size.
- A component whose size depends on its slot content, and slot content whose size depends on the component, can loop. Once every component has a reported size, `measuringRounds` bounds it at 4 more rounds for one change, and the latest reported sizes are sent.
- A late `ComponentMeasured` after the screen changed. It names an older revision and is ignored; the newer screen asks again.
- Many sessions: memory at 10,000 open screens is not measured yet. Slice 9 measures it before the feature is called done.
- Skiko's native runtime is per OS. A server without it fails at the first screen; `Producer { }` checks for it at start and fails with a message that names the missing jar.
- `isDebugInspectorInfoEnabled` is a process-wide switch. Any other Compose code in the same server process also builds inspector info, a small cost.
- The consumer's device test environment: every Compose device test fails on the owner's Android 17 phone with `NoSuchMethodException: android.hardware.input.InputManager.getInstance` from Espresso (probe P10). Device work for this design runs only on the emulator `emulator-5554`, with `ANDROID_SERIAL=emulator-5554`, never on a physical phone. The text gate and slices 4 and 5 run there.
- An older development build of either side fails at its first `Screen` with `AnswerUnreadable` ([ADR 0019](../../docs/adr/0019-contract-version-1-carries-the-document.md)).
- Text measured by Skia on the server and drawn by Android's text engine on the device, with the same font file, is not compared yet. It is the design's largest risk, so a gate after slice 3 measures it on a device before the consumer slices start.
- Every probe ran on macOS arm64. A headless Linux server with `skiko-awt-runtime-linux-x64` is not checked; slice 3 runs the producer tests on Linux in CI.
- Not settled here: how an app component keeps its state when it moves inside documents and slots (#183). The consumer keys each node by its id, which is stable while the producer's node lives.

### 7. Rejected alternatives

- The library's own layout composables with the consumer measuring, a draw-op document, or reading the internal `LayoutNode` tree: see [ADR 0015](../../docs/adr/0015-producer-renders-with-compose-ui-foundation.md).
- A fixed box for app components, or sizes declared in the catalog: see [ADR 0016](../../docs/adr/0016-app-components-measured-on-the-device.md).
- Fonts inside the catalog, configured by hand on the server, or sent by the consumer: see [ADR 0017](../../docs/adr/0017-app-fonts-travel-in-a-pinned-font-bundle.md). The system font as the default on the device, or no default: see [ADR 0020](../../docs/adr/0020-bundle-roboto-as-the-default-font.md).
- No local interaction now, or a general local state model: see [ADR 0018](../../docs/adr/0018-local-switcher-for-interactions-without-a-round-trip.md).
- Contract version 2: see [ADR 0019](../../docs/adr/0019-contract-version-1-carries-the-document.md).
- A tagged `ScreenContent` with a `Components` case, the design of the turned-down branch `feat/194/content-seam`. It keeps a screen that is only a list of app components arranged by the consumer, which is server-driven rendering.
- A scene that does not paint (`CanvasLayersComposeScene`). It is internal or experimental API; `ImageComposeScene` paints pixels nobody reads, at 5.5 ms per frame.

### Impact on T048, spec 001's tasks and the feature order

R18's `PlacedNode`, `ScreenApplier` and `PlacedNodeContent` parts describe code this design replaces; R18 stays as the record of T048.

T048's remaining pull requests (value, choice and list kinds in the compiler plugin) do not change: bridges call `ConsumerComponent` with the same parameters. Only its body changes, in slice 5 here, inside `producer`. Slice 1 moves the marker, which changes `ProducerNames` in `compiler-plugin`; it lands after the open T048 pull request merges, or the later of the two rebases.

Changes to `tasks.md` for the engineering-manager. Applied on 2026-11-20: tasks.md has T120 to T129 for slices 1 to 9 and the text gate, in this order, and every change below.

- New tasks for slices 1 to 9 and the text gate below, placed after T050 and before T054 is called done, since the MVP must show a server-rendered screen. Slice 6 is the exception: it needs the action IDs of T067, so it sits in User Story 2 after T067, and nothing in User Story 1 waits for it.
- T049: the applier and `PlacedNode` are replaced by the scene of slice 3. Mark it superseded by the slice 3 task.
- T054 and T042: drop `ProvisionalScreenContent` and FR-034's top-to-bottom wording; `SsrScreen` draws the document.
- T040 and T038: the session tests gain the viewport, the revision and `ComponentMeasured`.
- T051: `ProducerLimits` gains `measuringRounds`, `measurementsPerSecond` and `documentBytes`; the observer gains `onScreenNotSendable` and `onLayoutNotSettled`; `Producer { }` loads the font bundles and checks the Skiko runtime at start, and refuses an unknown font version.
- T061 and T067: remote clicks on server nodes use the same action IDs and `Trigger` path as callbacks.
- T072 to T074: a slot holds a `SlotContent` laid out by the producer, not a list of app components.
- T076: lift the block on required slots that accept only producer content (FR-011, FR-036).
- T077 and T078: become slice 5's `ConsumerComponent` body and `PlacedComponentScope.slot`.
- T047, T035 and T101: the font bundle steps (`ssrFontBundle`, `ssrFontCheck`, `ssrFontDump`, `ssrFontSync`, `ssrFontVerify`, `ssrFontResources`) and `ssr/fonts.lock`, for both build tools.
- T055: the build also creates `ConsumerFonts` from the declared font version and families.
- T051 and T054: the introduction carries the font version, and "font version not supported" joins `RefusalKind` and `ScreenProblem.Refused`.
- Quickstart and samples (T056, T069, T079): sample screens are written with foundation; a sample with no app component.

### 8. Slices

Each slice is tests first, one mechanism, and buildable on every commit. Each named test is written first and seen failing, and each has its own break.

1. One marker in `core` ([ADR 0014](../../docs/adr/0014-one-opt-in-marker-for-internal-api.md)). Test: `InternalSsrComposeApiTest` names each marked `core` type and counts one warning each; it fails because none is marked. Break: unmark one type and the count drops.
2. The document and messages in `core`. Tests: the codec decodes examples 1 to 3 and `ComponentMeasured` exactly and round trips them; one failing decode per rule of invariant 3; `DeviceContext` checks `fontScale` and `layoutDirection`; `Introduce` carries `fontVersion`; `FontVersionChanged` and the `fontVersion` of a `Screen` decode; the font bundle reader in `core` accepts the example of contracts/font-bundle.md and rejects a bad file name, a repeated entry and a file over its cap. The new types are added beside the old ones, unused. Break per test: remove one check, or change one serial name.
3. The producer scene and reader. Add Compose UI, foundation and the Skiko test runtime to `producer`. Tests: `SemanticsVisibilityTest` (invariant 7); the reader turns a `Box` with background and border, a text and a `verticalScroll` into example 1's nodes without its click, and a `clickable` node is sent with no `click` until slice 6 (T126); a state change sends a new revision; an image, a brush, a lazy list and an unknown shape each give `onScreenNotSendable`; the scene runs on the session dispatcher. Break per test: drop one case from the reader.
Text gate, after slice 3 and before slice 4. On the emulator `emulator-5554` only, with `ANDROID_SERIAL=emulator-5554`, never on a physical phone, draw a fixed set of strings (short, long and wrapping, Latin and one other script) at 12, 16 and 24 sp and font scales 1 and 1.3 with one font from a font bundle and with the default Roboto, and compare each one's line count and height with what the producer measured for the same font, density and width. The gate passes when every line count matches and every height is within 1 dp. If it fails, stop and report to the owner, since measuring on the producer would then need text overflow reports from the device.

4. The consumer drawing and the switch-over. Tests: example 1 draws at its boxes with `ComponentBindings {}` (invariant 4); invariant 1's grep. Then `ProducerMessage.Screen` carries `ScreenDocument`, `ScreenContent`, `ProvisionalScreenContent` and the list loop are deleted, in one commit, since the type change breaks every caller at once. Break: place one node at 0, 0 and the bounds test fails.
5. App components as holes. Tests: `ConsumerComponent` reads as a `Component` node with its values; a pending document is not shown (invariant 8); the consumer reports a measured size; the producer lays out again and sends `pending = false`; a slot reports its space and its server content comes back laid out in it (example 3); the guard inside a slot (invariant 6); nested components stay pending until all are measured (invariant 8); `measuringRounds` stops a loop only after every component is measured; a first report with 10,000 components is accepted and one with 10,001 is refused; measurements over 30 per second are merged, not dropped. Break per test.
6. Remote clicks. Tests: a click on a `Box` gets an action ID, and a `Trigger` runs it and sends a new revision. It needs the action IDs of T067, so it is T126 in User Story 2; until then the reader leaves `click` out, and slice 9's contract test runs example 1 without it.
7. The switcher. Tests: every variant is composed and laid out, and `largestWidth` and `largestHeight` are the largest variant's size; a local click shows the variant at once in the largest box, moves no other node on the device, and sends `Switched`; the producer then sends a revision where the switcher's box and the content below match the new variant; the device keeps its choice when a revision still names the old variant; a `Switched` for an unknown switcher is ignored and for an unknown variant refused; `Switched` counts against the trigger limit; a bad switcher reference or a largest size smaller than a variant fails decoding. Break per test.
8. Fonts. Tests in `tooling` and `gradle-plugin` for `ssrFontBundle`, `ssrFontCheck`, `ssrFontDump`, `ssrFontSync`, `ssrFontVerify` and `ssrFontResources`; producer tests that measure with a loaded font, fail at start on a broken one, refuse an unknown font version at the introduction and in `FontVersionChanged`, switch fonts on a held version with remembered state kept, and measure text in the bundled Roboto family; a producer test that a text with no family or `FontFamily.Default` gives `onScreenNotSendable`; consumer tests that resolve a family, draw no-family text with Roboto and report an unknown family; a producer test that a text at weight 600, or italic at weight 700, in Roboto gives `onScreenNotSendable`. Break per test.
9. Load and device checks. A load test at 10,000 open screens of 50 rows that reports heap, native memory and the time to first screen; a contract test that runs examples 1 to 3 through the real producer and consumer; a look at each on an emulator or a device whose test libraries work.

### 9. Validation checklist

1. `ScreenDocument.root` is a `Box` node, and `DocumentNode` has exactly the cases `Box`, `Text`, `Scroll`, `Switcher` and `Component`.
2. No type, function or doc names a list of app components as a screen or slot. `ScreenContent` and `ProvisionalScreenContent` do not exist.
3. The codec tests decode examples 1 to 3 and `ComponentMeasured` exactly, and fail each rule of invariant 3 with `MessageDecodingException` only.
4. `DeviceContext` has `fontScale` and `layoutDirection`, `Introduce` has `fontVersion`, `FontVersionChanged` exists, `Screen` names its `fontVersion`, `RefusalKind` has "font version not supported", `OpenScreen` has `viewport`, `ViewportChanged` exists, and `Screen` has `revision` and `pending`.
5. The producer hosts each screen in an `ImageComposeScene` with the viewport, density, font scale and direction, wrapped in a root `Box`.
6. The reader uses only public API, with no opt-in, and `SemanticsVisibilityTest` pins what it sees.
7. Unsupported content gives `ScreenUnavailable` and `onScreenNotSendable`.
8. `ConsumerComponent` keeps its signature; its node carries its catalog version; the guard walks every `Component`, in slots too.
9. The consumer places every node with one positioned layout and imports no `Column` or `Row`; a screen with no app component renders with empty bindings.
10. A pending screen is never shown, and a pending update keeps the last shown one; `ComponentMeasured` holds every changed component, is sent at most 20 times per second and merges changes in between; stale revisions are ignored; `measuringRounds` counts only rounds after every component is measured.
11. A remote click uses the `Trigger` path; a local click shows the variant at once in the largest box, moves nothing else on the device, and sends `Switched`, after which the producer lays out again.
12. Fonts go from the app's declaration to the font bundle, the saved and pinned copy on the server, the server resources and the producer, with fingerprints checked at each step; the catalog holds no font; a broken font stops the producer at start; text in the bundled Roboto family, which the library's default family is, uses the bundled Roboto on both sides.
13. The marker exists once, in `core`, on the types ADR 0014 lists, and invariant 9's `PublicSignatureMarkerTest` finds no marked type in a public signature, and catches its own fixtures.
14. The load test ran at 10,000 open screens and its numbers are in this section.
15. Principle I gate, again: the producer owns arrangement (points 1, 5 and 9); a screen with no app component renders in an app with an empty catalog (point 9); nothing makes the catalog the only content or gives the consumer a layout decision (points 2 and 9); composable logic runs on the producer only (point 5 and invariant 5).
16. The text gate passed on `emulator-5554`, with its numbers recorded here.
17. The producer refuses out-of-range measurements and viewports without laying out, ignores unknown ids, and merges, without dropping, `ComponentMeasured` and `ViewportChanged` over their own limit, apart from the trigger limit, each with a test.
18. An app component inside a switcher variant gives `onScreenNotSendable`.
19. `./gradlew build detekt --configuration-cache` and `./gradlew check --configuration-cache` pass.

### Owner decisions of the second round

Asked through the engineering-manager and answered by the owner on 2026-11-20:

1. The default font: Roboto, bundled in the producer and the consumer; text in the library's default family is measured and drawn with it, never with the system font, and a text with no family is refused ([ADR 0020](../../docs/adr/0020-bundle-roboto-as-the-default-font.md), changed on 2026-11-20 after slice 3 found no public way to remap the default).
2. Font files at run time: a separate pinned font bundle with its own version, not an exception to FR-069, which stays as it was ([ADR 0017](../../docs/adr/0017-app-fonts-travel-in-a-pinned-font-bundle.md)).
3. `layoutDirection` in the device context, the viewport in `OpenScreen` and `ViewportChanged` are accepted in contract version 1.
4. The switcher is accepted as designed ([ADR 0018](../../docs/adr/0018-local-switcher-for-interactions-without-a-round-trip.md)).
5. After the review: switcher sizing is a fixed box sized to the largest variant at once, then `Switched` to the producer, which lays out again and sends a revision where the content around has moved; the device keeps its choice by switcher id; `Switched` is rate limited like triggers (ADR 0018).
6. After the CodeRabbit review: a font version change during a session is sent with `FontVersionChanged`; the producer switches to that bundle and lays out again with state kept, or refuses the session when it does not hold it.

### Probes

Run on 2026-11-20. P1 to P3 in a throwaway worktree of c352229a with a test in `core/src/jvmTest` (`./gradlew :core:jvmTest --tests dev.tonholo.ssrcompose.core.probe.ProbeTest --configuration-cache`, exit 0), and a producer probe (`./gradlew :producer:test --tests dev.tonholo.ssrcompose.producer.probe.ProbeTest --configuration-cache`, exit 0). P4 to P9 and P11 in a throwaway Gradle project outside the repository: Kotlin 2.4.21, Compose Multiplatform 1.12.2 desktop (`ui-desktop`, `foundation-desktop`), `skiko-awt-runtime-macos-arm64` 0.150.2, JVM 17 with `-Djava.awt.headless=true`, on macOS arm64, run by `./gradlew -p <probe> test` (exit 0). Both were removed afterwards.

- P1: a sealed node tree with a nested slot encodes to `{"content":{"type":"Column","modifier":[...],"children":[{"type":"Text","text":"Hello"},{"type":"Component","name":"Card","slots":{"content":[...]}}]}}` and decodes back equal.
- P2: with `exceedsNestingDepth`, a tree of n nested layout nodes needs 2n + 2 levels and n nested slots 3n + 2.
- P3: 1,000 rows of a layout node holding a text and a component is 183,932 bytes of JSON; encoding and decoding it took 2.56 ms on average over 100 runs, and comparing two equal decoded copies 0.21 ms.
- P4: real `Column`, `Row`, `Box`, `BasicText`, `padding`, `size`, `fillMaxWidth`, `background` and `weight` laid out in `ImageComposeScene(1440, 3120, Density(3.5f, 1f))`: padding 16 dp gave 56 px, a 40 dp box 140 px, and a long text wrapped. A state change from 1 to 3 rows laid them out at 0, 33 and 66 px. The first scene took 434 ms to create and 117 ms to render in a cold JVM; 50 later frames took 5.5 ms each.
- P5: with `isDebugInspectorInfoEnabled = true`, the unmerged semantics tree held a `Box` with `size`, `background[color, shape]` and `border[width, color, shape]`, and two texts, one with `clickable[enabled, onClick, onClickLabel, role]`, each with its size and `boundsInRoot`. `Column` and `Row` did not appear.
- P6: a `Column` with a background appeared; a `Box` with only padding, a `Spacer`, an empty sized `Box` and a `Column` with only padding did not; their texts did. A `verticalScroll` column composed all 20 rows and appeared as a `scroll` node; a `LazyColumn` of 1,000 items composed 7.
- P7: 100 scenes of 50 text rows at 1080 by 2400 px took 22.7 ms each and 297 KB of heap each, once warm. Native memory was not measured.
- P8: `Font(identity, data, weight)` loaded TTF files from bytes; one text in a 200 dp box measured 147, 148 and 144 px high with the default font, Arial and Georgia. Ten bytes that are not a font loaded and rendered with the default font, with no error.
- P9: compiling the probes reported no warning, so `ImageComposeScene`, `semanticsOwners`, `SemanticsNode.layoutInfo`, `ModifierInfo` and `InspectableValue` need no opt-in. `ImageComposeScene` takes a `CoroutineContext`. Corrected on 2026-11-20 in slice 3: the constructor and the setter that take a `LayoutDirection` are `@ExperimentalComposeUiApi`, so the direction goes through `LocalLayoutDirection` at the root of the content. `LayoutInfo` has no public list of children; the full layout tree is only in the internal `LayoutNode`.
- P11: in the same project, a text inside a `Box` with a background and padding is a child of that box in the semantics tree; custom `SemanticsPropertyKey`s set on a `Layout` and on a slot `Layout` inside it are readable, with the slot under the hole and its text under the slot; running `SemanticsActions.OnClick` from the semantics ran the `clickable` lambda; `TextLayoutResult.layoutInput.style` gave `fontSize` 18 sp and a `FontFamily` whose loaded font has the identity `arial`, while a text with no size gave `Unspecified`; `FontMgr.default.makeFromData` gave a typeface for Arial and null for 10 bytes that are not a font. The background box's `boundsInRoot` started 8 px down, inside its 4 dp padding at density 2.
- P12: on 2026-11-20, in a throwaway Gradle project outside the repository (Kotlin 2.4.21 JVM, `kotlin { abiValidation { } }`, `./gradlew updateKotlinAbi`, exit 0, removed afterwards), the reference dump was written to `api/<module>.api`. A public function with a parameter of a public class `ScreenDocument`, its signature over several source lines, appeared as `direct (ILprobe/ScreenDocument;)V`, but `generic(documents: List<ScreenDocument>)` appeared as `generic (Ljava/util/List;)V`, a return type `List<ScreenDocument>` as `()Ljava/util/List;`, and a property `Map<String, ScreenDocument>` as `()Ljava/util/Map;`: the JVM dump erases type arguments. In the same project, `kotlin-metadata-jvm` 2.4.21 (`KotlinClassMetadata.readStrict`) gave `[kotlin/collections/List, probe/ScreenDocument]` for the parameter and the return, `[kotlin/collections/Map, kotlin/String, probe/ScreenDocument]` for the property, and visibility `INTERNAL` for an internal function, so a metadata walk catches every case.
- P10: a consumer device test that draws a node tree with foundation, and the existing `ScreenProblemMessageTest`, both failed on a physical Pixel 7 Pro with Android 17, where they should never have been run (device work belongs on `emulator-5554` only), with `NoSuchMethodException: android.hardware.input.InputManager.getInstance` from Espresso. Consumer drawing was not checked on a device.

### Guardrails proposed for the rules

Applied on 2026-11-20: the owner approved this wording, and rule 14 of .ai/rules/architecture.md carries it word for word. Its Column and Row check leaves out the problem message, the app's own UI: `git grep -n -E 'foundation.layout.(Column|Row)' -- consumer/src/commonMain ':!consumer/src/commonMain/kotlin/dev/tonholo/ssrcompose/consumer/problem'`.

Rule 14 of [the architecture rules](../../.ai/rules/architecture.md) and its checks still allow the top-to-bottom fallback and name `ScreenContent`. Proposed wording, for the engineering-manager to bring to the owner:

- Rule 14: "The core is server-side rendering, never server-driven rendering (principle I). The producer measures and arranges every screen and sends a positioned document. The consumer places each node where the producer put it and never arranges content on its own, in its public API or inside. No wire type, slot or screen holds only consumer catalog components."
- Rule 15: unchanged, with "renders server screens" meaning server-rendered documents.
- Checks: "`git grep -n -E 'foundation.layout.(Column|Row)' -- consumer/src/commonMain` finds nothing. `ScreenDocument.root` is a `DocumentNode.Box`. `SemanticsVisibilityTest` exists in `producer`. The consumer draws every node through one exhaustive `when` over `DocumentNode`."
