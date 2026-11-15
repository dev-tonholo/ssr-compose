# Contract: what the build does

What a developer sets up and runs. The same steps exist for Gradle and for the Kotlin toolchain, with the same names and behaviour. Sketches of the shape, not final syntax.

## In the app's project (the consumer)

Setup, Gradle form:

```kotlin
plugins { id("dev.tonholo.ssrcompose") }

ssrCompose {
    consumer {
        catalogVersion = "3"
        catalogDir = layout.projectDirectory.dir("ssr/catalogs")
    }
}
```

| Step | What it does |
|---|---|
| compile | The compiler plugin checks every `@ConsumerComposable` and writes one fragment per source file. An unsupported parameter, or a marked function that is not `@Composable`, is a compile error at that line. |
| `ssrCatalogCheck` | Merges the fragments into a catalog and compares it with the saved file `ssr/catalogs/<version>.json`. Fails when a name is used twice, when the result differs from the saved file, and when no saved file exists yet for that version, with a message that says to run `ssrCatalogDump`. It never writes the saved file itself. It is part of the normal check and build. |
| `ssrCatalogDump` | Writes the merged catalog to `ssr/catalogs/<version>.json`. Run on purpose, after changing a marked composable and raising the version. The file is committed. |

The saved files are what the app team publishes for server projects, at any location both can reach.

Which compilation the catalog comes from:

- A catalog belongs to one compilation of the app: one Android variant, or one target's compilation in a Kotlin Multiplatform module. The compiler plugin writes its fragments in a folder per compilation.
- Every variant has a catalog version. By default it is the project's `catalogVersion`. A variant or flavor whose marked composables differ sets its own, for example `variant("catalogV1") { catalogVersion = "1" }`.
- `ssrCatalogCheck` runs for the variant being built and compares that variant's catalog with the saved file of its version. So two variants that share a version must have the same marked composables, or the check fails.
- The app sends, at the handshake, the catalog version of the variant it was built as.
- A marked `expect` and `actual` pair counts as one component.

## In the server's project (the producer)

Setup, Gradle form:

```kotlin
plugins { id("dev.tonholo.ssrcompose") }

ssrCompose {
    producer {
        catalogs {
            from("https://example.com/app/catalogs/2.json")
            from("https://example.com/app/catalogs/3.json")
        }
        bridgeCatalog = "3"
        supportedCatalogs = setOf("2", "3")
        bridgePackage = "com.example.app.bridge"
    }
}
```

| Step | What it does |
|---|---|
| `ssrCatalogSync` | Fetches each catalog, validates it, saves it as `ssr/catalogs/<version>.json` and records its location and fingerprint in `ssr/catalogs.lock`. The IDE's sync triggers it where the build tool allows, and a developer can run it directly. |
| compile | The compiler plugin reads the saved bridge catalog and creates one `@Composable` function per component and one `enum class` per choice list, in `bridgePackage`. It never uses the network. The bridge is created in one module. Other modules of the server depend on that module and do not apply the producer setup themselves. |
| `ssrCatalogVerify` | Fails when a saved catalog does not match its fingerprint in the lock file, when the bridge catalog or a supported catalog is not saved, or when two saved catalogs share a version. It is part of the normal check and build. |

Rules for `ssrCatalogSync`:

1. Only `https` addresses and file paths are accepted, unless the developer opts in to something else.
2. A catalog that is not valid, or that holds a name that cannot be a Kotlin name, is rejected and nothing is saved.
3. When a location's content no longer matches the lock file, the step says so and keeps the saved copy. `--update` replaces it.
4. When a location cannot be reached and a saved copy exists, the saved copy is used and the developer is told. With no saved copy, the step fails.
5. `ssr/catalogs/` and `ssr/catalogs.lock` are committed, so the same commit always builds the same way, with or without a network.

## The lock file

```json
{
  "catalogs": [
    { "version": "2", "location": "https://example.com/app/catalogs/2.json", "sha256": "..." },
    { "version": "3", "location": "https://example.com/app/catalogs/3.json", "sha256": "..." }
  ]
}
```

## What the build-tool plugins take care of

- They apply the compiler plugin with its options.
- They put the bridge plugin before the Compose compiler. The bridge plugin also stops the build with a clear message if it finds it ran second.
- They declare each saved catalog as a build input, so an edited catalog causes a rebuild.

## The editor

To see what the compiler creates, turn off `kotlin.k2.only.bundled.compiler.plugins.enabled` in the IDE registry. This is a one-time setup step and is documented for users.
