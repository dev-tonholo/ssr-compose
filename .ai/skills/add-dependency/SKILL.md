---
name: add-dependency
description: Add or update a library or Gradle plugin. Use when a change needs a new dependency, a version bump, or a new compiler or Gradle plugin.
---

# Add a dependency

How to add or update a dependency. The rules are in [architecture](../../rules/architecture.md) and [code-style](../../rules/code-style.md).

## Steps

1. Check that the need is in a spec. Do not add a library for something no spec asks for.
2. Look in the version catalog first. For the library modules it is `gradle/libs.versions.toml` at the root. The proof of concept keeps its own, in `poc/gradle/libs.versions.toml`.
3. Prefer a well-established library. Check its license fits Apache 2.0.
4. Check platform support. Anything for the consumer or core library must work in Kotlin Multiplatform common code or have an Android artifact. Anything for the producer or core library must work on a plain JVM with no Android.
5. Never add AndroidX Remote Compose outside `poc/`.
6. Do not let an alpha, experimental or restricted type reach public API.
7. Add the version and the library to the catalog. Use a BOM where one exists. Never write a version in a module's build file. One exception: a library whose version Compose's published constraints supply, such as a Skiko runtime or an AndroidX Compose test library, goes in the catalog with no version ([ADR 0027](../../../docs/adr/0027-compose-decides-the-versions-of-libraries-tied-to-it.md)).
8. Declare it in the module with the catalog accessor, with the narrowest configuration that works.
9. Record a library choice as an ADR. See [the ADR blueprint](../../blueprints/adr.md).

## Verify

```sh
./poc/gradlew -p poc build detekt
./poc/gradlew -p poc :server:dependencies --configuration runtimeClasspath
```

The second command shows what really lands on the server's classpath. Nothing Android-only may be there.

## Commit

`chore(poc): add turbine to the version catalog`
