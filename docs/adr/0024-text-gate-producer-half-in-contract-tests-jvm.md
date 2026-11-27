# 0024. The text gate's producer half runs in a JVM target of contract-tests

- Status: Rejected on 2026-11-26, never accepted: the owner chose that the device measures server text ([ADR 0025](0025-device-measures-server-text.md)), so the text gate this record places is not built. Kept as the record of probes P21 and P22.
- Date: 2026-11-26
- Modules: `contract-tests`, `producer`, `compiler-plugin`

## Context

The text gate (T123, [research.md R20](../../specs/001-consumer-catalog-bridge/research.md#text-gate-t123)) compares what the device drew with what the producer measured for the same text. The device half is in `contract-tests` (PR #212): one case list and one file format in `contract-tests/src/textGate`, compiled into the Android device tests and the Android host tests. The producer half is not built. It has four needs:

1. Compose desktop and Skiko, since the producer measures in an `ImageComposeScene`.
2. The same case list and file format as the device half, from one definition.
3. The producer's internals (`ScreenScene`, `ScaledTextScene`, the text mark, the bundled Roboto), since no public API renders a screen yet.
4. Server text written as `BasicText` and compiled with the SSR compiler plugin in `producer` mode, before the Compose compiler ([ADR 0023](0023-producer-converts-text-sizes-with-the-font-scale-curve.md)). Only the gate's server text: `producer`'s own `ScaledBasicText` calls `BasicText`, and the producer's tests rely on a raw `BasicText` being refused.

Probed on 2026-11-26 (R20 probes P21 and P22):

- The Android host tests cannot measure. `./gradlew :contract-tests:dependencyInsight --configuration androidHostTestRuntimeClasspath --dependency org.jetbrains.compose.ui:ui` picks the variant `androidRuntimeElements-published` for `:producer`'s Compose UI too, and the same command for `org.jetbrains.skiko` finds nothing. `ImageComposeScene` is desktop only.
- The Gradle plugin `dev.tonholo.ssrcompose` cannot be applied inside the root build: it is a module of that build, not an included build, and nothing in the root build applies it.

## Options

1. A `jvm()` target on `contract-tests`. Its `jvmTest` compilation adds `src/textGate/kotlin` like the two Android test source sets, puts `:compiler-plugin` on `kotlinCompilerPluginClasspathJvmTest`, and passes `mode=producer` and the plugin order to `compileTestKotlinJvm`. The producer's internals reach it through a test fixture in `producer` (`java-test-fixtures`, which the Kotlin Gradle plugin associates with `main`), not published, as `catalog-model` does. Cost: `contract-tests` becomes Android plus JVM, the plugin is wired by hand in one build file, and `producer` gains a test fixture.
2. A new test compilation in `producer` associated with `main`, with the plugin on it. Internals come free. Cost: the case list and file format live in `contract-tests`, so `producer` would read another module's source folder or a copy of it, against the one-definition rule.
3. Compile the gate's server text at test run time with kotlin-compile-testing, as `compiler-plugin`'s `TextRewriteRunner` does, with the producer's classes as a friend path. Proven in `compiler-plugin`. Cost: plugin order comes from the order of a registrar list, not from `-Xcompiler-plugin-order` as a real server build passes it, and the server text is a string compiled inside a test, not a source file.
4. Measure in the Android host tests. Ruled out by the probe above.
5. Apply the real Gradle plugin from a separate build that includes the root build, as `samples/server` does with `includeBuild("../..")`. The gate would then be a third sample build with its own settings, run by its own command and its own CI step like the `Samples` job, outside `./gradlew build`. Cost: the gate leaves `contract-tests`, so the case list and file format move to a module both builds can reach or are read across builds, and the root build's `check` no longer runs it.

## Decision

Option 1. It is the only option that keeps one case list and one file format in one module, compiles real source files with the same plugin id, mode and order flag a server build gets, and touches no production code. Probe P22 built it in a copy of this branch: every rewritten text carried the mark, the reversed order failed the compile with `PLUGIN_ORDER`, and `:contract-tests:testAndroidHostTest`, `:contract-tests:assembleAndroidDeviceTest`, `:producer:test` and `:producer:verifyMavenPublications` passed, also with `--configuration-cache`.

The wiring, in `contract-tests/build.gradle.kts`, as P22 built it:

- `kotlin { jvm() }` beside the Android target. No `commonMain` code is added, so the JVM main compilation stays empty.
- The `jvmTest` source set adds `src/textGate/kotlin` with `kotlin.srcDir`, and `src/androidHostTest/resources` with `resources.srcDir`, so the JVM tests read the same recorded files as `RecordedTextGateFileTest`. Its dependencies: `projects.producer`, `project.dependencies.testFixtures(projects.producer)`, `libs.compose.foundation`, `libs.compose.ui`, `libs.kotlinx.serialization.json`, `libs.open.sans.fonts` with `isTransitive = false`, and `runtimeOnly` of `libs.skiko.runtime.macos.arm64` and `libs.skiko.runtime.linux.x64`, the two runtimes `producer`'s tests use. kotlin.test comes from the testing convention.
- `"kotlinCompilerPluginClasspathJvmTest"(projects.compilerPlugin)` in `dependencies { }`.
- On `tasks.named<KotlinJvmCompile>("compileTestKotlinJvm")`, `compilerOptions.freeCompilerArgs` adds `-P`, `plugin:<id>:mode=producer` and `-Xcompiler-plugin-order=<id>>androidx.compose.compiler.plugins.kotlin`.
- The plugin id `<id>` comes from the Gradle property `ssrcompose.compiler.plugin.id`, the same source as `CompilerPluginBuildConfig.COMPILER_PLUGIN_ID`.
- The order flag is the text of `PluginOrder.FLAG` in `gradle-plugin`, which the root build cannot import. P22 found that a misspelled Compose id, and no order flag at all, both compile with no error and the rewrite still runs, since the default order on this build puts the SSR plugin first. So the flag is not what keeps the order today. What keeps a wrong order from passing silently is the rewrite's own call-based check, which fails the compile with `PLUGIN_ORDER` when the SSR plugin runs after the Compose compiler (P22). The flag stays, so the order does not depend on that default.
- No catalog option, so the rewrite runs as it does in a server module with screens and no catalog (ADR 0023, T130 item 3).

In `producer/build.gradle.kts`: the `java-test-fixtures` plugin, `testFixturesImplementation` of `libs.compose.ui` and `libs.compose.foundation`, and the two fixture variants left out of the Java component with `withVariantsFromConfiguration(...) { skip() }`, the block `catalog-model/build.gradle.kts` has.

## Consequences

- Good: the gate measures what a server build produces, with the rewrite, the plugin order and the mode a server module uses, and one module owns the whole gate.
- Good: `./gradlew build` runs the producer half, so CI's Linux job runs it on every pull request.
- Bad: `contract-tests` is now Android plus JVM, and the module map in `.ai/guidelines.md` ("Test-only module (Android host tests)") and the testing rules' table need a line for the JVM tests. The engineering-manager brings that to the owner.
- Bad: the order flag's Compose id is written twice, here and in `PluginOrder.FLAG`, and a typo in it is ignored without an error. The rewrite's own order check is what guards the order.
- Bad: `producer` gains a test fixture that knows its internals. It is not published, and it holds measuring code only.
- Follow-up: when T128 loads app fonts from a font bundle in the producer, the gate's Open Sans goes through that loader instead of `Font(identity, data)`.
