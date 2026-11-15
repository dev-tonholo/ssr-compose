# Contract: the catalog bundle

The one thing the app's build and the server's build share. It is produced by the app's build and never written by hand, but it is plain text a person can read and review.

A catalog is a bundle: one manifest, and one part for each app module that has marked composables. All of them are JSON files. Format version 1 for both. The reader, the writer and the checks live in `catalog-model`, a build-time library. Nothing that runs in the app or in the producer reads a catalog.

## Example

A catalog of version 3, from an app with two modules that have marked composables, `:app` and `:feature:payment`.

`manifest.json`:

```json
{
  "format": 1,
  "version": "3",
  "parts": [
    { "name": "app", "file": "app.part.json", "fingerprint": "5d2c..." },
    { "name": "feature-payment", "file": "feature-payment.part.json", "fingerprint": "a91f..." }
  ]
}
```

`app.part.json`:

```json
{
  "format": 1,
  "part": "app",
  "choices": [
    { "name": "Style", "entries": ["bodyMedium", "titleLarge"] }
  ],
  "components": [
    {
      "name": "Greeting",
      "parameters": [
        { "name": "title", "kind": "text", "required": true },
        { "name": "style", "kind": "choice", "choice": "Style", "required": false }
      ]
    }
  ]
}
```

`feature-payment.part.json`:

```json
{
  "format": 1,
  "part": "feature-payment",
  "choices": [],
  "components": [
    {
      "name": "CameraScanner",
      "parameters": [
        { "name": "cancel", "kind": "slot", "arguments": ["text"], "accepts": "any", "required": true },
        { "name": "onCancelClick", "kind": "callback", "required": true },
        { "name": "onScan", "kind": "callback", "arguments": ["bytes"], "required": true }
      ]
    },
    {
      "name": "PaymentSection",
      "parameters": [
        { "name": "title", "kind": "text", "required": true },
        { "name": "onPayClick", "kind": "callback", "required": true },
        { "name": "content", "kind": "slot", "accepts": "consumer", "required": false }
      ]
    }
  ]
}
```

## Manifest fields

| Field | Meaning | Rule |
|---|---|---|
| `format` | the version of the manifest format | a whole number. A reader refuses a format it does not know. |
| `version` | the catalog version the app developer set | at most 64 characters, no control characters. A label with no order. |
| `parts` | one entry per part | sorted by `name`. Empty when the app has no marked composable. |
| `parts[].name` | the part's name | lowercase letters, digits and hyphens, 1 to 64 characters. Unique in the manifest. |
| `parts[].file` | the part's file name | always `<name>.part.json`, built from the part's `name`. Unique in the manifest. A reader rejects the whole manifest otherwise. See "Part file names" below. |
| `parts[].fingerprint` | the fingerprint of the part's bytes | lowercase hexadecimal SHA-256. |

## Part fields

| Field | Meaning | Rule |
|---|---|---|
| `format` | the version of the part format | a whole number. A reader refuses a format it does not know. |
| `part` | the part's name | equals its `name` in the manifest. |
| `choices` | every fixed list of choices this part's components use | sorted by `name`. Each has at least one entry, in declaration order. |
| `components` | every marked composable of the module | sorted by `name`. No hard limit on how many. |
| `parameters` | a component's inputs | in declaration order. No hard limit on how many. Names are unique in a component. |

## Rules across parts

- A part stands on its own: it lists every choice list its components use.
- The same choice list in two parts must be identical: same name, same entries in the same order.
- A component name must be unique across the whole bundle.
- The app build fails when one of these breaks, and names both parts. A reader rejects such a bundle.

## Part names

A part's name comes from the module's path in the build. The path is made lowercase, every run of characters other than `a` to `z` and `0` to `9` becomes one hyphen, and hyphens at the start and the end are dropped:

| Module path | Part name |
|---|---|
| `:feature:home` | `feature-home` |
| `:feature_home` | `feature-home` |
| `:featureHome` | `featurehome` |
| `:app` | `app` |
| `:` (the root module of the build) | `root` |

The app developer can set another name with `partName`. A name longer than 64 characters fails the app build and asks for a shorter one to be set. A name equal to a Windows device name (`con`, `prn`, `aux`, `nul`, `com1` to `com9`, `lpt1` to `lpt9`) is rejected too, since its file could not be made on Windows, and the build asks for another one to be set. Two modules that would give the same name fail the app build, and both are named.

The table shows Gradle module paths. The Kotlin toolchain names its modules in its own way. T030 records how in research.md R5 and maps it to this same rule, so both build tools give the same part names. Not checked yet.

## Part file names

A part's file is always its name followed by `.part.json`: the part `feature-home` is in `feature-home.part.json`. Since a part name holds only lowercase letters, digits and hyphens, the file name never holds a path separator, `..` or a scheme, and it can never be `manifest.json`.

A reader rejects the whole manifest when any `file`:

- is not its entry's `name` followed by `.part.json`,
- is empty or a dot,
- is `manifest.json`,
- or is the same as another entry's `file`.

Nothing is fetched or read for a manifest that breaks one of these.

## Parameter kinds

| `kind` | Extra fields | Comes from |
|---|---|---|
| `text` | | `String` |
| `whole` | | `Int`, `Long` |
| `decimal` | | `Float`, `Double` |
| `flag` | | `Boolean` |
| `choice` | `choice`: the name of an entry in the part's `choices` | an `enum class` |
| `list` | `of`: one of the value kinds above | `List<T>` |
| `callback` | `arguments` (optional): up to four value kinds, or `bytes` | `() -> Unit`, `(A) -> Unit`, `(A, B) -> Unit` and so on |
| `slot` | `accepts`: `any`, `consumer` or `producer`. `arguments` (optional): value kinds the component passes to the content | `@Composable () -> Unit`, `@Composable (A, B) -> Unit` |

- In code, the value kinds (`text`, `whole`, `decimal`, `flag`, `choice`, `list` and `bytes`) are `ValueKind` in `core`. It is the one definition of them: `catalog-model` reads and writes it, and the producer uses it to declare callback arguments.
- `required` is false when the parameter has a default value in the composable. The catalog does not hold the default itself: only the app knows it.
- A value or choice parameter may also have `"nullable": true`, when its type in the composable is nullable. The field is left out when false.
- `bytes` is allowed only as a callback argument.
- A callback and a slot use the same field, `arguments`, for what they pass along.
- A callback has no result. A callback with more than four arguments is a build error in the app.
- A slot's `accepts` comes from `@ConsumerComposable` or `@ProducerComposable` on the slot's type. With neither, it is `any`. It is recorded in this feature and not checked yet.
- A slot with `arguments` can be described here. Filling it needs the later remote composables feature. Until then, an optional one is simply not given, and a component with a required one cannot be placed by the server at all. `CameraScanner` above is such a component.
- A modifier parameter is left out of the catalog.

## Names

Every component, parameter and choice list name, and every choice entry, must be a valid Kotlin identifier that is not a keyword, at most 64 characters. A reader rejects the whole catalog otherwise, and creates nothing from it.

## Size cap

A reader refuses a manifest or a part larger than its cap, before it parses it. The cap is 16 MiB for each file by default, and the build setup can change it. There is no limit on the number of components or parameters other than this.

## Stability

The same marked composables always give the same bytes in every file: fixed key order, sorted parts, components and choices, two-space indent, one trailing newline, no timestamps and no paths. A change to the catalog is then a small reviewable difference.

## Fingerprints

- A part's fingerprint is the SHA-256 of its bytes, in lowercase hexadecimal, written in the manifest.
- The catalog's fingerprint is the SHA-256 of the manifest's bytes. Since the manifest holds every part's fingerprint, it covers the whole bundle.

## Where the catalog lives

- In the app's repository: one folder per released catalog version, `ssr/catalogs/<version>/`, with `manifest.json` and one `<name>.part.json` file per part, committed. The app build compares what it produces with the saved bundle of the same version and fails when any byte differs. Moving a composable to another module changes two parts and the manifest, so it needs a new version.
- At a location both teams can reach: the app team publishes each version's folder as it is. A catalog's location is the address of its `manifest.json`. Its parts are found next to it by their `file` names.
- In the server's repository: one folder per catalog version it holds, the same layout, with the manifest's fingerprint in a lock file. See [build-tooling.md](build-tooling.md).
