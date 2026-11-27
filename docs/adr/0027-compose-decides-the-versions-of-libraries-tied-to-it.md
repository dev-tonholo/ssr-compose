# 0027. Let Compose decide the versions of the libraries tied to it

- Status: Accepted
- Date: 2026-11-27
- Modules: `producer`, `compiler-plugin`, `consumer`, `contract-tests`

## Context

Some tests need libraries that only work at the version Compose Multiplatform asks for.

- The tests of `producer` and `compiler-plugin` need a Skiko runtime for the OS they run on.
- The device tests of `consumer` and `contract-tests` need Compose test libraries. `SsrScreen` must never show a stand-in value for a read that failed. That promise depends on what is placed and drawn in a real frame, so a test without real layout cannot prove it.

This record replaces [ADR 0009](0009-androidx-compose-test-libraries-for-device-tests.md). It states the choice of device test libraries made there as a decision of its own, and changes how their version is chosen.

When each of these libraries had its own version in `gradle/libs.versions.toml`, Dependabot could move it to its latest release without Compose, even inside a group. When it moved Skiko alone, the producer tests failed with `NoSuchMethodError`. When `androidx-compose-test` moves alone, Gradle raises AndroidX Compose UI on the device test classpaths above the version Compose Multiplatform asks for.

`skiko-awt` and AndroidX Compose UI both publish version constraints for these libraries in their Gradle module files.

## Options

### Device test libraries

1. Use `androidx.compose.ui:ui-test-junit4` and `androidx.compose.ui:ui-test-manifest` with `androidx.test:runner`. They match the AndroidX Compose runtime that Compose Multiplatform resolves to on Android. The cost is a second family of Compose libraries whose version must follow `compose-multiplatform`.
2. Use the Compose Multiplatform test libraries, so every Compose library has one version. `org.jetbrains.compose.ui:ui-test-manifest:1.12.2` is not on Maven Central or Google Maven (checked on 2026-11-27), so the device test build fails.
3. Run the tests on the host with Robolectric. No device is needed, but the tests would not run on real Android rendering, and it adds another test framework.

### Versions of the tied libraries

1. Keep a version for each, and add a build check that fails on a mismatch. Dependabot needs an ignore rule, and a person changes the versions by hand.
2. For Skiko, use the per-OS artifacts of Compose Desktop, `org.jetbrains.compose.desktop:desktop-jvm-<os>`. In 1.12.2 they bring the matching Skiko runtime, but in 1.13.0-rc01 the POM files for macOS arm64 and Linux x64 no longer list it. They also bring all of Compose Desktop, Material included. It does nothing for the AndroidX test libraries.
3. Declare the libraries in the catalog with no version, so the published constraints supply it. No build code and no Dependabot rule. It depends on Skiko and AndroidX to keep publishing the constraints. A classpath without Compose cannot resolve them, and the build fails.

## Decision

Device test libraries: option 1. The device tests use `androidx.compose.ui:ui-test-junit4`, `androidx.compose.ui:ui-test-manifest` and `androidx.test:runner`. Only the `androidDeviceTest` source sets of `consumer` and `contract-tests` use them, and no consumer of a library receives them.

Versions: option 3. The Skiko runtimes, `ui-test-junit4` and `ui-test-manifest` have no version in the catalog. `androidx.test:runner` is not tied to Compose and keeps its version.

## Consequences

- Good: device tests can prove what is placed and drawn on a real Android device or emulator.
- Good: a change of `compose-multiplatform` moves the tied libraries with it, up or down, and Dependabot has no version of theirs to move.
- Bad: the catalog no longer shows which versions are in use. These commands show them:
  - `./gradlew :producer:dependencies --configuration testRuntimeClasspath`
  - `./gradlew :consumer:dependencies --configuration androidDeviceTestRuntimeClasspath`
- Bad: nothing stops a person from adding a version back. A version older than the one Compose asks for loses: Gradle resolves Compose's version and says nothing. A newer one wins.
  - A newer Skiko can turn the producer tests red. With Skiko 0.156.1 and Compose 1.12.2, 136 of 169 fail.
  - A newer AndroidX Compose test library is silent in the pull request build. With 1.13.0, AndroidX Compose UI on the device test classpaths becomes 1.13.0, and `./gradlew build detekt` passes. Building the device test APKs does fail with 1.13.0, but only because that release needs compile SDK 37.1; the check does not look at the tie.
- Bad: another dependency that asks for a newer Skiko or AndroidX Compose raises it too, because Gradle takes the highest version asked for.
- Follow-up: use the Compose Multiplatform test libraries if a matching manifest library is published.
- Follow-up: before the upgrade to Compose 1.13, settle what it does to [ADR 0015](0015-producer-renders-with-compose-ui-foundation.md), where a server picks the Skiko runtime of its OS. With 1.13.0-rc01, `org.jetbrains.compose.ui:ui-skiko-desktop` puts `org.jetbrains.skiko:skiko-awt-runtime-all` on the `runtimeClasspath` of `producer`. Its POM calls it a "Composition of all Skiko AWT Runtimes", and its jar is 44.9 MB.
