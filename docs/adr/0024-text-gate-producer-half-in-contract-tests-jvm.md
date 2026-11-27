# 0024. The text gate's producer half runs in a JVM target of contract-tests

- Status: Proposed
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
5. Apply the real Gradle plugin through an included build. Cost: the root build would include itself or move `gradle-plugin` out, a change to the build layout ([ADR 0001](0001-keep-the-poc-in-a-folder.md)) far larger than the gate.

## Decision

Option 1. It is the only option that keeps one case list and one file format in one module, compiles real source files with the same plugin id, mode and order flag a server build gets, and touches no production code. Probe P22 built it in a copy of this branch: every rewritten text carried the mark, the reversed order failed the compile with `PLUGIN_ORDER`, and `:contract-tests:testAndroidHostTest`, `:contract-tests:assembleAndroidDeviceTest`, `:producer:test` and `:producer:verifyMavenPublications` passed, also with `--configuration-cache`.

The wiring, in `contract-tests/build.gradle.kts`:

- The plugin id comes from the Gradle property `ssrcompose.compiler.plugin.id`, the same source as `CompilerPluginBuildConfig.COMPILER_PLUGIN_ID`.
- The order flag is `-Xcompiler-plugin-order=<id>>androidx.compose.compiler.plugins.kotlin`, the text of `PluginOrder.FLAG` in `gradle-plugin`, which the root build cannot import. A wrong Compose id cannot pass silently: the rewrite's own call-based check fails the compile with `PLUGIN_ORDER` when the SSR plugin does not run first (P22).
- No catalog option, so the rewrite runs as it does in a server module with screens and no catalog (ADR 0023, T130 item 3).

## Consequences

- Good: the gate measures what a server build produces, with the rewrite, the plugin order and the mode a server module uses, and one module owns the whole gate.
- Good: `./gradlew build` runs the producer half, so CI's Linux job runs it on every pull request.
- Bad: `contract-tests` is now Android plus JVM, and the module map in `.ai/guidelines.md` ("Test-only module (Android host tests)") and the testing rules' table need a line for the JVM tests. The engineering-manager brings that to the owner.
- Bad: the order flag's Compose id is written twice, here and in `PluginOrder.FLAG`. The rewrite's own order check is what guards it.
- Bad: `producer` gains a test fixture that knows its internals. It is not published, and it holds measuring code only.
- Follow-up: when T128 loads app fonts from a font bundle in the producer, the gate's Open Sans goes through that loader instead of `Font(identity, data)`.
