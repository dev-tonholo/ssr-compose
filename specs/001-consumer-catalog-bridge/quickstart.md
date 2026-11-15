# Quickstart: checking that the feature works

How to prove the feature end to end once it is built. Types are in [data-model.md](data-model.md) and the [contracts](contracts/).

Module, task and file names below are the planned ones. They do not exist until the tasks are done.

## Before you start

- JDK 17 or newer, and the Android SDK.
- An emulator or a phone. Find its serial with `adb devices` and pass it to every device command. Ask the owner before using a physical phone.
- The `android` CLI for screenshots and taps.
- In the IDE registry, `kotlin.k2.only.bundled.compiler.plugins.enabled` turned off, to see what the compiler creates.

## 1. The libraries build and their tests pass

```sh
./gradlew build detekt
./gradlew publishToMavenLocal
```

Expected: both pass. The first includes the compiler plugin's compile tests, the unit tests and the contract tests.

## 2. Story 1: a server screen places an app composable

The two samples are separate builds. They share one folder of catalogs, `samples/catalogs/`, with one subfolder per catalog version, and nothing else.

```sh
./gradlew -p samples/app ssrCatalogDump
cp -R samples/app/ssr/catalogs/1 samples/catalogs/1
./gradlew -p samples/server ssrCatalogSync
./gradlew -p samples/server run
adb -s <serial> reverse tcp:8788 tcp:8788
./gradlew -p samples/app assembleDebug
android run --device=<serial> --apks=samples/app/build/outputs/apk/debug/app-debug.apk --activity=<the sample activity>
android screen capture --device=<serial> -o story-1.png
```

Expected:

- `samples/app/ssr/catalogs/1/` holds `manifest.json` and one part per sample app module with marked composables, together describing every marked sample composable, and running the dump twice gives the same bytes.
- `samples/server` builds with no dependency on `samples/app`. Its `ssr/catalogs/1/` matches the app's folder, and its `ssr/catalogs.lock` holds the fingerprint of that `manifest.json`.
- The screenshot shows the sample composable, drawn by the app's own code, with the text the server gave.
- The server's event output shows one accepted introduction with the device values, the app identity, the catalog version and the contract version, and no catalog in it.

Then break each of these in the sample server, one at a time, and build: a component name that does not exist, a missing required parameter, a text where a choice is expected. Expected: each fails to compile at that line.

## 3. Story 2: a tap runs a lambda on the server

```sh
android input --device=<serial> tap 'text=Add one'
android screen capture --device=<serial> -o story-2.png
```

Expected: the count on screen goes up, and the server's output shows the lambda ran in that session. Record the time from the tap to the updated screen from the session log.

## 4. Story 3: server content inside an app composable

Open the sample screen that fills a container's slot with two other composables.

Expected: both appear inside the container, where the app places its slot, in the order the server wrote them.

Then try to fill a slot that passes arguments in the sample server. Expected: a compile error that names the component and the slot, and says that slots with arguments are not supported yet.

## 5. Story 4: apps that did not update keep working

Build the sample app's `catalogV1` flavor, which is built from an older set of marked composables and has catalog version 1, and the `current` flavor, which has catalog version 3. Run both against one server that holds the catalogs of versions 1, 2 and 3, each with its own bridge, and registers a `home` screen for version 1 and for version 3.

Expected: each app shows the `home` screen written for its own catalog version, drawn only from what its catalog has.

On the older app, open the sample screen registered for version 1 that, by mistake, places a composable from the version 3 bridge. Expected: "This screen cannot be shown", no partial screen, and a server event that names the composable, the screen, catalog version 1 and catalog version 3.

On the older app, open a screen name that is registered only for version 3. Expected: "This screen cannot be shown", and the app can still open `home`.

Restart the server without the version 1 registration and reopen the older app. Expected: a refusal with a reason and what to do.

## 6. Story 5: a screen follows the device

With the adaptive sample screen open and its counter above zero:

```sh
adb -s <serial> shell cmd uimode night yes
android screen capture --device=<serial> -o dark.png
```

Then rotate the device and capture again. Expected: the screen matches the theme and the orientation within a second, and the counter keeps its value.

## 7. Same result with no network

Disconnect the machine from the network and run `./gradlew -p samples/server build` on a clean checkout. Expected: it builds from the saved catalogs, and the output is the same as with a network.

## 8. Limits and load

```sh
./gradlew :producer:loadTest
```

Expected: a report with 10,000 sessions held at once over 10 served catalog versions, the time to accept an app and send its first screen, and that 1,000 healthy sessions saw no error and no more than 100 milliseconds of extra delay while another session sent invalid and oversized introductions and callback data. Until this runs, those numbers are targets and not facts.

## 9. The Kotlin toolchain

In `samples/toolchain-server`, a copy of the sample server set up for the Kotlin toolchain:

```sh
kotlin do ssrCatalogSync
kotlin build
```

Expected: the same saved catalogs and the same bridge as the Gradle sample. Until this runs, the Kotlin toolchain path is not verified.

## 10. Build performance

```sh
./gradlew :gradle-plugin:functionalTest --tests '*CatalogBuildCacheTest*'
./gradlew -p samples/app assembleCurrentDebug
./gradlew -p samples/app assembleCurrentDebug
```

Expected: the functional tests pass: a build with nothing changed runs no catalog step, a change in one module runs only that module's part step and the bundle step, and a second checkout in another folder takes every catalog output from the build cache. The second sample build reports every catalog step as up to date. The numbers for a catalog of 20,000 components come from T031a and T115, and are written in research.md R16 with the machine and the commands. Until those run, the 2 second figure is a target and not a fact.
