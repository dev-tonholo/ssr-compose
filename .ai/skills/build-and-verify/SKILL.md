---
name: build-and-verify
description: Build, test and run this repository, and read the common failures. Use before reporting any change as done, when a build or test fails, or when you need to run the proof of concept server or app.
---

# Build and verify

How to build and check this repository. The rules are in [testing](../../rules/testing.md) and [ui-compose](../../rules/ui-compose.md).

The root is a Gradle build with the modules `:core`, `:core-testing`, `:consumer`, `:producer`, `:compiler-plugin`, `:tooling`, `:gradle-plugin` and `:contract-tests`. The proof of concept in `poc/` is a separate Gradle project with the modules `:protocol`, `:server` and `:app`. Run every command from the repo root.

## Steps

1. Fast loop, one module: `./poc/gradlew -p poc :server:test`, `./poc/gradlew -p poc :protocol:test` or `./poc/gradlew -p poc :app:testDebugUnitTest`.
2. Full check: `./poc/gradlew -p poc build detekt`.
3. Run the server: `./poc/gradlew -p poc :server:run`. It listens on port 8787. Port 8080 on the owner's machine belongs to another project. Stop the server with `kill $(lsof -ti tcp:8787 -sTCP:LISTEN)`.
4. Find the device serial with `adb devices`. Pass the serial to every device command. Ask the owner before you touch a physical phone.
5. Forward the server port to the device: `adb -s <serial> reverse tcp:8787 tcp:8787`.
6. Build the app: `./poc/gradlew -p poc :app:assembleDebug`.
7. Install and launch it: `android run --device=<serial> --apks=poc/app/build/outputs/apk/debug/app-debug.apk --activity=dev.tonholo.ssrcompose.app.MainActivity`. Not verified: not run with the `android` CLI when this file was written. The Gradle form `ANDROID_SERIAL=<serial> ./poc/gradlew -p poc :app:installDebug` was run and works.
8. Look at the result: `android screen capture --device=<serial> -o <file>.png`.
9. See the session traffic: `adb -s <serial> logcat -d -s SsrSession:D`.
10. UI tests: `ANDROID_SERIAL=<serial> ./poc/gradlew -p poc :app:connectedDebugAndroidTest`. It needs a device.

## Common failures

- The app shows "Failed to connect": the server is not running, or the port forward is gone. Run steps 3 and 5 again.
- The app shows "The server closed the connection": the server stopped. Start it and tap Retry.
- detekt fails on a long test name: shorten the name.
- A Kotlin version error from kotlinx.rpc: the PoC needs Kotlin between 2.2.0 and 2.4.20.

## Verify

```sh
./poc/gradlew -p poc build detekt
python3 .ai/scripts/check-links.py
python3 .ai/scripts/sync-agents.py --check
```

## Report

Say which commands you ran and their result, which device you used, and which steps you could not run and why.
