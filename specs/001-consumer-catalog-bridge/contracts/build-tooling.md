# Contract: what the build does

What a developer sets up and runs. The same steps exist for Gradle and for the Kotlin toolchain, with the same names and behaviour. Sketches of the shape, not final syntax.

## In the app's project (the consumer)

Setup, Gradle form, in the module that sets the catalog version (usually the application module):

```kotlin
plugins { id("dev.tonholo.ssrcompose") }

ssrCompose {
    consumer {
        catalogVersion = "3"
        catalogDir = layout.projectDirectory.dir("ssr/catalogs")
    }
}
```

In any other module that has marked composables:

```kotlin
plugins { id("dev.tonholo.ssrcompose") }

ssrCompose {
    consumer {
        partName = "payment" // optional; by default it comes from the module's path
    }
}
```

| Step | Where | What it does |
|---|---|---|
| compile | every module with the plugin | The compiler plugin checks every `@ConsumerComposable` and writes one fragment per source file. An unsupported parameter, or a marked function that is not `@Composable`, is a compile error at that line. |
| `ssrCatalogPart` | every module with the plugin | Merges the module's fragments into the module's part, `<name>.part.json`, and writes the module's renderer entries (name to renderer). Fails when a name is used twice in the module, or when two fragments disagree about a choice list. Cacheable: its only inputs are the module's fragments and its part name. Where the renderer entries are written is settled by T055 and not checked yet. |
| `ssrCatalogBundle` | the module that sets `catalogVersion` | Collects its own part and the parts of the modules it depends on, checks across parts (a component name used in two parts, a choice list that differs between two parts, two modules with the same part name, each naming both places), and writes the manifest. It also writes the app's renderer index as a resource, from the renderer entries of every part. Cacheable: its inputs are the parts, the renderer entries and the catalog version. Where the renderer index is written is settled by T055 and not checked yet. |
| `ssrCatalogCheck` | the module that sets `catalogVersion` | Compares the bundle with the saved one in `ssr/catalogs/<version>/`, byte for byte, and names each file that differs. Fails when any differs, and when no saved bundle exists yet for that version, with a message that says to run `ssrCatalogDump`. It never writes the saved bundle itself. It is part of the normal check and build. |
| `ssrCatalogDump` | the module that sets `catalogVersion` | Writes the bundle to `ssr/catalogs/<version>/`: `manifest.json` and one `<name>.part.json` file per part, and removes any part file the manifest does not list. Run on purpose, after changing a marked composable and raising the version. The folder is committed. |

The saved folders are what the app team publishes for server projects, at any location both can reach.

Which compilation a part comes from:

- A part belongs to one compilation of its module: one Android variant, or one target's compilation in a Kotlin Multiplatform module. The compiler plugin writes its fragments in a folder per compilation.
- Every variant has a catalog version. By default it is the project's `catalogVersion`. A variant or flavor whose marked composables differ sets its own, for example `variant("catalogV1") { catalogVersion = "1" }`.
- `ssrCatalogCheck` runs for the variant being built and compares that variant's bundle with the saved bundle of its version. So two variants that share a version must have the same marked composables, or the check fails.
- The app sends, at the handshake, the catalog version of the variant it was built as.
- A marked `expect` and `actual` pair counts as one component.

## In the server's project (the producer)

Setup, Gradle form:

```kotlin
plugins { id("dev.tonholo.ssrcompose") }

ssrCompose {
    producer {
        catalog(version = "2") {
            location = "https://example.com/app/catalogs/2/manifest.json"
            bridgePackage = "com.example.app.bridge.v2"
        }
        catalog(version = "3") {
            location = "https://example.com/app/catalogs/3/manifest.json"
            bridgePackage = "com.example.app.bridge.v3"
        }
    }
}
```

Each `catalog` block declares one held catalog: its version, the address of its manifest, and the package its bridge is created in. Versions and packages are each unique. Which versions the running producer serves is not set here: it serves the versions it has screens registered for. See [producer-api.md](producer-api.md).

| Step | What it does |
|---|---|
| `ssrCatalogSync` | For each held catalog, fetches the manifest, checks it, fetches only the parts not already saved for that version, checks each part against its fingerprint in the manifest, and saves them in `ssr/catalogs/<version>/`. Records the location and the manifest's fingerprint in `ssr/catalogs.lock`. The IDE's sync triggers it where the build tool allows, and a developer can run it directly. |
| `ssrCatalogIndex` | One per held catalog. Turns the saved bundle into an index the compiler plugin reads, in the build folder. Cacheable, internal, never committed and never shared between builds. Its format is settled by the scale gate, T031a. It runs before compile and is not meant to be run by hand. |
| compile | The compiler plugin reads each index and creates one bridge per held catalog in its `bridgePackage`: one `@Composable` function per component and one `enum class` per choice list, each body carrying the catalog version as a constant. It never uses the network and never parses a catalog's JSON. The bridges are created in one module. Other modules of the server depend on that module and do not apply the producer setup themselves. |
| `ssrCatalogVerify` | Fails when a saved manifest does not match its fingerprint in the lock file, when a saved part does not match its fingerprint in the manifest, when a declared catalog has no saved bundle, when a saved manifest's version differs from its declared version, or when the saved folder holds a part file the manifest does not list. It is part of the normal check and build. |

Rules for `ssrCatalogSync`:

1. Only `https` addresses and file paths are accepted, unless the developer opts in to something else.
2. A manifest or a part larger than the size cap is refused before it is read. The cap is 16 MiB each by default, set with `catalogFileSizeLimit` in `ssrCompose { }`.
3. Each part's `file` must be its `name` followed by `.part.json`, and the `file` values must be unique and never empty, a dot or `manifest.json`, as [catalog-bundle.md](catalog-bundle.md) says. A manifest that breaks this is rejected before any part is fetched. Parts are fetched from the manifest's location with that file name.
4. A manifest whose version differs from the declared version, a part that does not match its fingerprint, a catalog that is not valid, or one that holds a name that cannot be a Kotlin name, is rejected, and nothing of that catalog is saved. The same holds when the manifest loads but one of its parts cannot be fetched: nothing of that catalog is saved, and the saved copy, if there is one, is kept as it was.
5. When the manifest at a location no longer matches the lock file, the step says so and keeps the saved copy. `--update` replaces it.
6. When a location cannot be reached and a saved copy exists, the saved copy is used and the developer is told. With no saved copy, the step fails.
7. Since a catalog is saved whole or not at all, and a changed manifest is kept out until `--update`, fetching only the missing parts happens in two cases: a sync with `--update`, where parts whose fingerprint did not change are kept, and a sync after a saved part file was removed by hand. A first sync of a version fetches every part. In every case each part is checked against the manifest before anything is saved.
8. `ssr/catalogs/` and `ssr/catalogs.lock` are committed, so the same commit always builds the same way, with or without a network.

## The lock file

```json
{
  "catalogs": [
    { "version": "2", "location": "https://example.com/app/catalogs/2/manifest.json", "fingerprint": "..." },
    { "version": "3", "location": "https://example.com/app/catalogs/3/manifest.json", "fingerprint": "..." }
  ]
}
```

`fingerprint` is the manifest's fingerprint, the lowercase hexadecimal SHA-256 of its bytes, which covers every part. The field has the same name, and the same kind of value, as each part's `fingerprint` in the manifest.

## Build performance and the build cache

- Every catalog step (`ssrCatalogPart`, `ssrCatalogBundle`, `ssrCatalogIndex`) declares its inputs and outputs, and can be taken from the build cache.
- Inputs are tracked by relative path, and outputs hold no absolute path and no timestamp, so a cache entry made on one machine is reused on another and in another checkout folder.
- A build with nothing changed runs no catalog step. A change in one app module runs that module's `ssrCatalogPart` and the `ssrCatalogBundle`, and nothing else.
- The compile step of a server module has the indexes as inputs, not the catalogs' JSON.

Not checked: all of the above. T114 tests it with Gradle TestKit, and T031a and T115 measure it with a catalog of 20,000 components.

## What the build-tool plugins take care of

- They apply the compiler plugin with its options.
- They put the bridge plugin before the Compose compiler. The bridge plugin also stops the build with a clear message if it finds it ran second.
- They declare each index as a compile input, so a changed saved catalog causes a rebuild.

## The editor

To see what the compiler creates, turn off `kotlin.k2.only.bundled.compiler.plugins.enabled` in the IDE registry. This is a one-time setup step and is documented for users.
