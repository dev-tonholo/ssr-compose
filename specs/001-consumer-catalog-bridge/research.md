# Research: Consumer Catalog and Bridge

Decisions for the plan, each with its reason and what was turned down. Facts are marked as checked or not checked, as the constitution's principle VII asks.

Two experiments were run on 2026-11-15 before any design, on Kotlin 2.4.21, Compose runtime 1.12.2, Gradle 9.8.2 and JDK 25 with a JVM 17 target. Their code is in `.superpowers/spikes/k2-bridge/` and `.superpowers/spikes/k2-catalog/`, which git ignores. They are throwaway and nothing depends on them.

A third experiment, T025, ran on 2026-11-16 inside the real modules. Its results are in R1, R2, R8, R8a and R9.

The design was revised on 2026-11-15 by three owner decisions: the catalog is a bundle split per module, screens are written per catalog version, and the catalog is build time only. The experiments used a single catalog file. What they proved still holds for each part and for each bridge, and what the bundle adds is marked as not checked below.

## R1. The compiler creates each bridge from a held catalog

Decision: on the server side, the compiler plugin creates one bridge per held catalog, in the package the server developer names for that catalog. Each bridge is a set of top-level declarations: one `@Composable` function per component and one `enum class` per choice list. FIR declares them and IR gives them bodies. Each body is one call to a public function of the producer library, and passes the catalog version of its bridge as a constant. The compiler plugin reads the catalog through its index (R16), never the catalog's JSON.

Checked by experiment, from a single catalog file:

- FIR declares the enum and the composable functions from a file, and IR fills the bodies.
- The Compose compiler treats the created functions like hand-written ones. Their compiled signatures match a hand-written twin, and they compose in a real composition on a plain JVM with slot nesting and a working callback.
- Code in the same module calls them with a normal import and no extra step. Another module sees them through the first module's metadata.
- A wrong name, a missing parameter, and a `String` given where a choice is expected each fail to compile with the ordinary Kotlin error at the caller's line.
- Editing one source file recompiles only that file and causes no clash with the created declarations.

Checked by T025 on 2026-11-16, inside the real modules, on Kotlin 2.4.21 and Compose runtime 1.12.2, from a one-component index in a throwaway format of its own (the package `compiler-plugin/src/main/kotlin/dev/tonholo/ssrcompose/compiler/producer/throwaway/`; T031a lists everything that goes with it):

- The plugin creates the function from the index and gives it a body that is one call to `ConsumerComponent` with the catalog version as a constant. The bytecode of the created function holds the constant `"1"`, the component name and the parameter names, and the Compose compiler gave it the signature `Greeting(String, RemoteAction1<String>, String, Function2<Composer, Integer, Unit>, Composer, int, int)`.
- A real Gradle compile of the producer's test sources, with `mode=producer`, two `catalog` options and the order flag, composes the created function in a real composition. The 5 composition tests in `producer/src/test/kotlin/dev/tonholo/ssrcompose/producer/BridgeExperimentTest.kt` pass.
- Recomposition through a created bridge works. One of those tests changes a state that the bridge call reads, sends the snapshot apply notification and one frame, and the placed node then holds the new text (`Updated` where it held `Welcome`). When the node took its values only when it was created, that test failed.
- Two bridges with the same component name in two packages of one module compile side by side. Each has its own facade class, `<package>.__GENERATED__CALLABLES__Kt`, and each placed node carries the version of its own bridge (`"1"` and `"2"`). The two components had different parameters, and giving the first bridge the second one's parameter fails with `No parameter with name 'badge' found.`
- Each catalog needs its own generation key that carries its version. The IR step reads the key of a created function to pick the catalog and the version. When the IR step used the first catalog for every function, the two-bridge tests failed.
- A test that only compiles is not enough. A caller that leaves out an optional parameter compiled while the function would still throw at run time (see R8a). The compile tests in `compiler-plugin/src/test/kotlin/dev/tonholo/ssrcompose/compiler/producer/BridgeExperimentTest.kt` (7 tests) cover the compile errors, and the producer tests cover what runs.

Not checked: two modules creating the same package; creating declarations from the final index, and creating a declaration only when code names it (T031a settles both); a readable file name for the created functions (`createTopLevelFunction` takes one, T048 passes it).

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
- The guard reports the problem through the compiler's diagnostic reporter (`pluginContext.diagnosticReporter`) with a diagnostic that the plugin owns, so it is a normal compile error and not an internal error with a stack trace. The message did not change. The plugin id comes from the generated constant. The message is: `The bridge plugin 'dev.tonholo.ssrcompose' must run before the Compose compiler plugin. Pass -Xcompiler-plugin-order=dev.tonholo.ssrcompose>androidx.compose.compiler.plugins.kotlin`
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
- The checker and the fragment writer handle text parameters only. A marked composable with another parameter type fails the compile with a plain message that names the composable and the parameter, and writes no fragment for that file. Still missing, for T044: errors at the line and column of the parameter, a marked function that is not `@Composable`, modifier parameters left out, nullable text, a type alias of text, choice lists in a fragment, a function name that is not a plain Kotlin identifier, and every other kind (callbacks T064, slots T075).
- The writer leaves out every marked `expect`, so a marked `expect` whose `actual` is not marked would give no component. No test runs this case. T044 should make it an error.
- The same-source rule above holds inside one compilation only. In an incremental compile that rechecks the `expect` file and not the `actual` file, the writer removes the fragment the `actual` file wrote earlier, and the part loses the component. `FragmentWriterTest` shows it with two writers. A commonMain edit forces a full recompile today, which hides it, and no Gradle test runs it. Only the part step can decide this, because it sees every source file. It is part of #152.
- Two different files with the same package and the same name that both hold marked composables write one fragment, and the last one wins without a message. A clash of the class both files would make on the JVM makes this rare on Android. Two file names that differ only by case, `Home.kt` and `home.kt` in one package, also share a fragment file on a file system that ignores case.
- A fragment's source and file name follow rules (`data-model.md`). A file with no marked composable never needs a fragment, so a file name that breaks the rules, such as `Odd..Name.kt`, builds. A marked file with such a name fails the compile with a message that names the file. Both are tested.

Not checked: one module collecting the parts of the modules it depends on (T105 settles it); the build cache and the configuration cache for the part and bundle steps (T114 settles them); and the state of fragments after a failed incremental build.

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
- The compile used the Android SDK that was already on the machine. The `-classpath` of the compile starts with `/Users/rtonholo/Library/Android/sdk/platforms/android-37.0/android.jar`. The toolchain finds the SDK from `ANDROID_HOME`, then `ANDROID_SDK_ROOT`, then `~/Library/Android/sdk`. No file in the top two levels of that SDK folder changed during the probe.
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

Not checked: all of it. The experiments covered reading composables (R4) and creating declarations (R1), not this combination. A task builds the smallest version first. If creating the renderer in the compiler proves fragile, the hand-written bindings still work, and the renderer becomes follow-up work.

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
- The experiment's `ConsumerComponent` takes the actions as an array of `RemoteAction1<String>`. That is a stand-in. T050 and T065 replace it with `Callback`. `PlacedNode` and `ScreenApplier` in `producer/src/main/kotlin/dev/tonholo/ssrcompose/producer/internal/` are stand-ins too, for T049 and T077.

An optional callback parameter is not supported yet. A probe made while the throwaway index still accepted `action-of-text optional` showed that a caller that left the optional action out placed the actions `{onSelect=..., onCancel=null}`: the bridge passes the `null` on, and `ConsumerComponent` took a non-null array, so `null` did not mean "not given" as R8a says. The throwaway reader now refuses `optional` on an `action-of-text`, with a test. T065 and T066 reopen it.

Not checked: `RemoteAction0` and `RemoteAction2` to `RemoteAction4`, `Callback` and its conversion (T060, T065 and T066 settle them).

## R8a. Optional and nullable parameters

Decision: the catalog says if a parameter can be left out, never what its default is, because only the app knows. In the bridge, a parameter that can be left out is nullable with `null` as its default, and `null` means "not given": nothing is sent and the app's own default applies. A value or choice parameter whose type is nullable in the app is marked `nullable` in the catalog and stays nullable in the bridge.

Checked by T025 on 2026-11-16, for a nullable optional text and a nullable optional slot, both with `null` as the default:

- Both are created, callers can leave them out, and the result compiles and composes. A node placed with them left out holds only the required value and has no slot node.
- The default value of a created parameter is not `null` by itself. FIR gives it a placeholder, and a caller that leaves the parameter out gets `java.lang.Throwable: Stub for declaration generated by Plugin[BridgeKey(catalogVersion=1)]` at run time. The compile still succeeds. All 5 composition tests failed with that message until the IR step set each optional parameter's default to `null`.
- Because of that, a compile-only test cannot guard defaults. A test for them has to run the created function.
- The nullable slot type is `ComposableFunction0<Unit>?` with the `@Composable` type attribute. Removing the attribute changed no test result, so the experiment does not show whether it is needed, and T048 decides. A slot created as a plain `() -> Unit` with the attribute also passes all the tests. A plain `() -> Unit` without the attribute fails the caller that puts a bridge call in the trailing lambda: `GreetingAllParametersCaller.fixture.kt:14:9 @Composable invocations can only happen from the context of a @Composable function`.

Not checked: a required parameter whose type is nullable in the app, sent as an explicit nothing; nullable types in the catalog (the catalog experiment rejected them, by its own choice).

## R8b. Slots that pass arguments

Decision: such a slot is described in the catalog and cannot be filled until remote composables exist. An optional one gets no bridge parameter. A component with a required one cannot be placed: its bridge function is created so the name resolves, and any call to it is a compile error that names the later feature.

Consequence for the first samples: the owner's `PaymentSection` and `CameraScanner` examples both have a required slot with arguments, so the samples of this feature use components with values, callbacks and slots without arguments.

Not checked: how that compile error is produced.

## R8c. Where a part comes from in a project with variants

Decision: a part belongs to one compilation. Each Android variant has a catalog version, the project's by default, and the check compares the variant's bundle with the saved bundle of its version. A flavor with different marked composables sets its own version. The compiler plugin writes fragments in a folder per compilation. A marked `expect` and `actual` pair is one component.

Checked by experiment (T027), in a Kotlin Multiplatform module with an Android target and two product flavors, `free` and `paid`, on AGP 9.5.1 and Kotlin 2.4.21:

- Product flavors next to the Kotlin Multiplatform plugin only work with `android.builtInKotlin=false` and `android.newDsl=false` in `gradle.properties`. Without them AGP stops at configuration: "The 'com.android.library' (or 'com.android.application') plugin is not compatible with the 'org.jetbrains.kotlin.multiplatform' plugin since AGP 9.0", and it names these two flags as a way to "temporarily bypass this issue". With the flags AGP and Kotlin both print deprecation warnings, and AGP says the flags go away in version 10. The test fixture sets them. A real project that wants variants in a Kotlin Multiplatform module has to do the same until that changes.
- The fragments folders seen, one per variant compilation: `build/ssr/fragments/android/freeDebug` and `build/ssr/fragments/android/paidDebug`. A scratch probe, not a test, also saw `android/freeDebugUnitTest`, so test compilations get their own folders. The part step must read only the folder of the variant's own compilation. That rule is not built yet. `KmpAndroidFragmentsTest` asserts the two full paths, target segment included.
- The free folder holds `Home`, `Shared`, `Panel` and `FreeBanner`, and the paid folder holds `Home`, `Shared`, `Panel` and `PaidBanner`. A composable in `commonMain` appears in every variant's folder, as expected, because every variant compiles it.
- A marked `expect` and `actual` pair is one component in the part (R4).
- An edit to one file leaves the part equal to a clean build's part (R4).

Probed and not covered by a test:

- The AGP plugin this repository uses, `com.android.kotlin.multiplatform.library`, has one variant only: target `android`, compilations `main` and `hostTest`, folder `ssr/fragments/android/main`. It cannot have flavors. A marked `expect` and `actual` pair compiled with it, next to a JVM target.
- A plain `com.android.library` module with product flavors and no Kotlin Multiplatform plugin runs the plugin for each variant too, with compilations `freeDebug` and `paidDebug`. Its target name is empty, so the folder has no target segment: `ssr/fragments/freeDebug`.

Not checked: the per-variant catalog version in the build setup (T092), the check of a variant's bundle against the saved bundle of its version, and what the build does when two variants share a version but have different marked composables.

## R9. One bridge per held catalog

Decision: the server project holds one saved bundle per catalog version, in `ssr/catalogs/<version>/`. Every held catalog gets its own bridge, in a package the server developer names for it in the build setup. There is no single catalog that the only bridge is created from. A composable removed in a newer catalog still exists in the bridges of the older catalogs that have it.

Rationale: versions have no order, so "newest" cannot be worked out, and the owner chose screens per catalog version (R10), which needs every version's composables to be callable. Two bridges with the same names cannot share a package, so each gets its own. The bridges are created in one module of the server, and other modules depend on it.

Checked by T025 on 2026-11-16: two packages with the same component name in one module, created from two held catalogs (versions `1` and `2`), compile side by side and compose. Each bridge passes its own version, and a call that mixes the parameters of the two fails to compile. See R1 for the details.

Not checked: the compile time and memory of 10 bridges of up to 20,000 components each (T031a), and what happens when two modules create the same package.

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

Decision: there is no hard limit on components or parameters. The library is tested at 20,000 components with about 80,000 parameters, with the mix real apps have (most components with 2 to 4 parameters, a few with 15 or more). The owner measured three real apps with 621, 751 and 1,085 composables, the last one an MVP. The reader and writer are protected by a size cap on the manifest and on each part, 16 MiB each by default ([`SizeCap`](../../catalog-model/src/main/kotlin/dev/tonholo/ssrcompose/catalog/bundle/SizeCap.kt)), checked before parsing or past the cap on stream read. Measured on JetBrains Runtime 25.0.4 with its default garbage collector, a 16 MiB worst-shape part read passes at 288 MiB of heap and fails at 272 MiB (the verification task gives it 320 MiB), a 16 MiB worst-shape manifest read passes at 144 MiB, a 16 MiB part write passes at 72 MiB, and a 16 MiB stream read through `SizeCap.read` passes at 40 MiB and fails at 32 MiB. A caller with less heap sets a smaller cap.

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
