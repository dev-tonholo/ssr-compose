# 0009. Use the AndroidX Compose test libraries for consumer device tests

- Status: Accepted
- Date: 2026-11-18
- Modules: `consumer`

## Context

`SsrScreen` must never show a stand-in value for a read that failed. That promise depends on what is placed and drawn in a real frame, so a test without real layout cannot prove it. The `consumer` module had no device tests and no test libraries for them.

## Options

1. Use `androidx.compose.ui:ui-test-junit4` and `androidx.compose.ui:ui-test-manifest` with `androidx.test:runner`. They match the AndroidX Compose runtime that Compose Multiplatform resolves to on Android. The cost is a second Compose version in the catalog, which must move with `compose-multiplatform`.
2. Use the Compose Multiplatform test libraries, so the catalog keeps one Compose version. `org.jetbrains.compose.ui:ui-test-manifest:1.12.2` could not be resolved, so the device test build fails.
3. Run the tests on the host with Robolectric. No device is needed, but the tests would not run on real Android rendering, and it adds another test framework.

## Decision

Option 1. The libraries are used only by the `androidDeviceTest` source set of `consumer`, and no consumer of the library receives them.

## Consequences

- Good: device tests can prove what is placed and drawn on a real Android device or emulator.
- Bad: `androidx-compose-test` in `gradle/libs.versions.toml` must be updated by hand when `compose-multiplatform` changes the AndroidX Compose version it resolves to.
- Follow-up: use the Compose Multiplatform test libraries if a matching manifest library is published.
