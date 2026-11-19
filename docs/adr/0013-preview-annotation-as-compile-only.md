# 0013. Set up Compose previews as the Kotlin Multiplatform guide shows, off what apps receive

- Status: Accepted
- Date: 2026-11-19
- Modules: `consumer`

## Context

`ScreenProblemMessage` gets a preview in light and dark theme (R19 slice 3). The Kotlin Multiplatform guide, [Compose UI previews](https://kotlinlang.org/docs/multiplatform/compose-previews.html), says what a module needs: the `@Preview` annotation for common code, and `ui-tooling` on the Android classpath. An app and a release build must not receive either one only for a preview.

`consumer` uses the `com.android.kotlin.multiplatform.library` plugin, with Android as its only target.

## Options

1. The guide's form for the annotation: `implementation` in `commonMain`. Measured on the android runtime classpath of `consumer`: the annotation artifacts reach every app. Rejected.
2. `compileOnly` in `commonMain` for `org.jetbrains.compose.ui:ui-tooling-preview`, with the previews in `commonMain`. The annotation is on the compile classpath only. Chosen. It compiles for the Android target. The common metadata compilation is skipped in a module with one target, so it was not run.
3. The deprecated annotation `org.jetbrains.compose.ui.tooling.preview.Preview` from `components-ui-tooling-preview`. Deprecated since Compose Multiplatform 1.10. Rejected.
4. No previews. Not chosen, because the owner asked for them.

For `ui-tooling` the guide has one form for this plugin: `androidRuntimeClasspath(...)` in the module's root `dependencies {}` block. `com.android.library` modules use `debugImplementation`, which this plugin does not have. `androidRuntimeClasspath` is a resolvable configuration, so it is not part of what the module publishes.

## Decision

Option 2 for the annotation, and the guide's form for `ui-tooling`:

- the annotation, `androidx.compose.ui.tooling.preview.Preview`, comes from `compose-ui-tooling-preview` (`org.jetbrains.compose.ui:ui-tooling-preview`), declared `compileOnly` in `commonMain`;
- `compose-ui-tooling` (`org.jetbrains.compose.ui:ui-tooling`) is declared with `androidRuntimeClasspath`;
- both catalog entries use the `compose-multiplatform` version;
- one private preview, marked `@PreviewLightDark`, sits in `ScreenProblemMessage.kt`, the same file as the composable it shows. A preview lives next to its composable, never in a separate previews file.

## Consequences

- Good: the pom and the Gradle module metadata of both publications (`consumer` and `consumer-android`), made with `publishToMavenLocal` into a scratch folder, have no `ui-tooling` and no preview annotation, and the pom is the same before and after. Apart from file sizes and checksums, so is the module metadata.
- Good: nothing public exposes a type of these artifacts, and the annotation is kept in the class file as invisible to reflection.
- Good: the owner rendered the light and dark preview in Android Studio on 2026-11-19.
- Bad: the `android` CLI preview render needs `com.android.library` and does not support this plugin, so previews cannot be rendered from the command line.
- Bad: the guide says IntelliJ IDEA support for the new Android library plugin was still coming when it was read. Rendering there is unchecked too.
