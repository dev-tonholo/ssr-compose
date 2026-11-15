# Data Model: Consumer Catalog and Bridge

Entities from the spec with their fields and rules. Kotlin names are proposals. Each section says which module an entity lives in:

- `core`: what the app and the producer use at run time.
- `catalog-model`: the catalog, used only at build time by `tooling` and `compiler-plugin`. `core`, `consumer` and `producer` never depend on it.
- `producer`: what only the producer library has.

## Catalog

### CatalogVersion (`core`)

A label the app developer sets. At most 64 characters, no control characters. No order: only equal or not equal matters. It lives in `core` so the handshake can carry it. `catalog-model` uses this same type.

### ValueKind (`core`)

The one definition of the kinds of values: text, whole number, decimal number, yes or no (`flag` in the file), choice (names a choice list by its `listName`), list of one value kind, and bytes. A list holds a text, a whole number, a decimal number, a flag or a choice, never bytes or another list. It lives in `core`, next to `DataValue`, because both build-time and run-time code need it: `catalog-model` uses it for parameters and callback arguments, and the producer uses it to declare and check callback arguments. Neither defines a list of kinds of its own. Where bytes may appear (only as a callback argument) is a catalog rule, checked in `catalog-model`.

### ChoiceList (`catalog-model`)

| Field | Rule |
|---|---|
| `name` | a valid identifier, unique in its part |
| `entries` | at least one, valid identifiers, unique, in declaration order |

### Parameter (`catalog-model`)

| Field | Meaning |
|---|---|
| `name` | a valid identifier, unique in its component |
| `kind` | a value kind, a callback, or a slot |
| `required` | false when the composable gives the parameter a default. The default itself is not in the catalog. |
| `nullable` | true when a value or choice parameter's type is nullable |

A callback parameter lists the kinds of the arguments it carries, none to four. A slot parameter says what it accepts (`any`, `consumer`, `producer`) and may list the value kinds it passes to its content.

### ComponentDescriptor (`catalog-model`)

| Field | Rule |
|---|---|
| `name` | a valid identifier, unique across the whole catalog |
| `parameters` | in declaration order. No hard limit on how many. |

### PartName (`catalog-model`)

Lowercase letters, digits and hyphens, 1 to 64 characters. By default it comes from the module's path in the build: lowercased, every run of characters other than ASCII letters and digits (`a` to `z`, `0` to `9` once lowercased) turned into one hyphen, hyphens at both ends dropped. `:feature:home` and `:feature_home` give `feature-home`, `:featureHome` gives `featurehome`, and the root module, whose path is `:`, gives `root`. The app developer can set another one. A derived name over 64 characters fails the build and asks for one to be set. Two modules whose parts get the same name fail the bundle step and both are named.

### CatalogPart (`catalog-model`)

The components of one app module.

| Field | Rule |
|---|---|
| `format` | the part format version |
| `part` | its `PartName` |
| `choices` | every choice list its components use, sorted by name |
| `components` | sorted by name |

A part stands on its own: it lists every choice list its components use, even when another part lists the same one.

### PartEntry (`catalog-model`)

One line of the manifest.

| Field | Rule |
|---|---|
| `name` | a `PartName`, unique in the manifest |
| `file` | always `<name>.part.json`, unique in the manifest, never empty, a dot or `manifest.json`. The part is found next to the manifest under this name. |
| `fingerprint` | lowercase hexadecimal SHA-256 of the part's bytes |

### CatalogManifest (`catalog-model`)

| Field | Rule |
|---|---|
| `format` | the manifest format version |
| `version` | `CatalogVersion` |
| `parts` | `PartEntry` list, sorted by `name`. Empty when the app has no marked composable. |

### Catalog (`catalog-model`)

A manifest and its parts, read and checked together. Rules across parts:

- A component name is unique across all parts.
- A choice list that appears in two parts is identical in both: same name, same entries in the same order.
- Every part the manifest lists is present and matches its fingerprint, and no other part is present.

Its fingerprint is the SHA-256 of the manifest's bytes. A reader refuses a manifest or a part larger than its size cap, 16 MiB each by default, before parsing it. See [contracts/catalog-bundle.md](contracts/catalog-bundle.md).

### CatalogIndex (`catalog-model`, internal)

What the compiler plugin reads to create one bridge, produced from one saved catalog by a cacheable build step. Internal, never shared between builds and never committed. Its format is settled by the scale gate, T031a, and is not fixed here.

### SavedCatalog and lock file (server project)

One folder per held catalog version inside the server project, `ssr/catalogs/<version>/`, with `manifest.json` and one `<name>.part.json` file per part. One lock file lists, for each held version: the version, the location of its manifest, and the manifest's `fingerprint`. See [contracts/build-tooling.md](contracts/build-tooling.md).

## Registration (`producer`)

What the producer is given at start: for each served catalog version, its screens by name, and the contract versions it supports.

| Field | Rule |
|---|---|
| catalog version | a `CatalogVersion`, registered once |
| screens | screen name to the screen's content, each name once per version |
| `supportedContractVersions` | the contract versions the producer accepts. By default, every version the library has a codec for. A server developer stops a contract version by leaving it out. An app whose version is not in the set is refused as "too old" when its version is below the highest in the set, gaps included, and as "too new" when it is above the highest. |

Rules: a version or a screen name inside one version registered twice, or a `supportedContractVersions` that is empty or holds a version the library has no codec for, makes `Producer` fail at once. The producer serves exactly the catalog versions that have at least one screen, and accepts exactly the contract versions in `supportedContractVersions`. It holds no catalog.

## Handshake (`core`)

### ContractVersion

A whole number. Versions are in order.

### DeviceContext

Can change during a session.

| Field | Rule |
|---|---|
| `density` | above 0, at most 10 |
| `orientation` | portrait or landscape, derived from the size |
| `screenWidthPx`, `screenHeightPx` | 1 to 100,000 |
| `theme` | light or dark |
| `language` | a language tag, at most 35 characters |

### AppIdentity

Fixed for a session. Self-declared and not proven. `version`, `variant`, `flavor`: each at most 64 characters, `flavor` may be empty.

### Introduction

`contractVersion`, `catalogVersion`, `app`, `device`. It never carries a catalog.

### Answer

A sealed type with two cases: `Accepted` with the contract version, or `Refused` with a `RefusalKind` and a reason for the user.

`RefusalKind`: contract version too old, contract version too new, catalog version not known or no longer supported, introduction not valid.

## Messages and frames (`core`)

### ConsumerMessage and ProducerMessage

The messages of [contracts/messages.md](contracts/messages.md). In code, the producer message that carries an answer, `ProducerMessage.Answered`, wraps an `Answer`. On the wire, the JSON codec shows `Accepted` and `Refused` as messages of their own, at the same level as `Screen` and `ScreenUnavailable`.

### First frame

The consumer's first frame, the one that carries `Introduce`, is a 4-byte big-endian contract version followed by the codec's payload. No other frame has a prefix. The receiver knows which frame is the first from its position in the connection, never from its bytes. The prefix must equal the contract version inside `Introduce`. `MessageFrame` holds this layout.

### MessageCodec

Turns messages into frames and back for one contract version, with its own pair of functions for the first frame. Every decoding failure is one error type, `MessageDecodingException`. The public way to get the JSON codec is `MessageCodec.json()`.

## Screen

### DataValue (`core`)

The neutral model for values on the wire: text, whole number, decimal number, flag, choice entry (by name), list, bytes, nothing.

### PlacedComponent (`core`, on the wire)

A parameter the screen did not give is absent from all three maps, and the app's composable uses its own default for it.

| Field | Meaning |
|---|---|
| `name` | the component's name |
| `values` | value parameter name to `DataValue` |
| `callbacks` | callback parameter name to `ActionId` |
| `slots` | slot parameter name to an ordered list of `PlacedComponent` |

The wire form has no catalog version: the session's version is known to both sides from the introduction.

### Placed node (`producer`, internal)

The producer's own node for a placed component. It holds what `PlacedComponent` holds, plus the catalog version of the bridge that placed it, taken from the constant in the bridge function, and the `Callback` of each callback parameter. The guard reads the version. The actions use the `Callback`.

### Callback (`producer`)

What a bridge function gives `ConsumerComponent` for one callback parameter. A sketch of the shape, not final signatures:

| Field | Meaning |
|---|---|
| `arguments` | the kinds of the callback's arguments, as core's `ValueKind`. Empty when the callback has none. |
| `convert` | a conversion from the received `DataValue`s to the lambda's typed arguments, which returns the call to run: a choice entry to its enum, a whole number to `Int` or `Long`, a decimal to `Float` or `Double`, a list to a typed list. It wraps the `RemoteAction` the server developer wrote. |

The producer holds no catalog, so for each trigger it first checks the number and the kinds of the values against `arguments`, then runs `convert`, then runs the returned call. A failure in the check or in the conversion, such as an unknown choice entry, a whole number out of range for `Int`, or a list with one wrong element, is a refused trigger: nothing runs, the session carries on, and `onTriggerRefused` is reported. Only a call that fails while it runs is a failed action, reported as `onActionFailed`. See [contracts/producer-api.md](contracts/producer-api.md).

### ActionId (`core`)

A short text the producer assigns to one callback of one placed component. Unique inside a session.

### ScreenContent (`core`)

An ordered list of `PlacedComponent`.

## Session (`producer`)

One accepted connection between one app and the producer.

```text
Connected -> Accepted -> Open(screen) -> Closed
         \-> Refused -> Closed
```

| From | Event | To | What happens |
|---|---|---|---|
| Connected | a valid introduction for a served catalog version and a contract version in `supportedContractVersions` | Accepted | `Accepted` is sent |
| Connected | an invalid introduction, an unserved catalog version, a contract version not in `supportedContractVersions` (too old when below its highest version, gaps included; too new when above it), or any other message | Refused | `Refused` is sent and the session closes |
| Accepted or Open | the app opens a name registered for its catalog version | Open | the open screen, if any, is disposed; the new one is composed and sent |
| Accepted or Open | the app opens a name with no screen for its catalog version | Accepted | `ScreenUnavailable` is sent and reported |
| Open | a device change | Open | the context changes, the screen recomposes, state is kept |
| Open | a trigger for an action the screen has | Open | the arguments are checked against the `Callback`'s kinds and converted; the returned call runs; if state changed, the screen recomposes and is sent |
| Open | a trigger for an action the screen does not have | Open | ignored and reported |
| Open | a trigger whose arguments go over the size limit, do not match the `Callback`'s number or kinds, or cannot be converted | Open | refused and reported through `onTriggerRefused`; nothing runs |
| Open | a trigger whose call fails while it runs | Open | the changes it made are thrown away, the screen stays as it was, and the failure is reported through `onActionFailed` |
| Open | triggers arrive over the limit, "100 per second by default" | Open | the extra ones are dropped and reported |
| Open | the screen places a component from another catalog version's bridge, or the screen's code fails | Accepted | the screen is not sent, `ScreenUnavailable` is sent and reported |
| any | the connection ends | Closed | the composition is disposed |

A session holds: its catalog version, the screens registered for it, its contract version, the app identity, the current device context, the live composition of the open screen, and that screen's actions. Nothing in it is shared with another session.

## The guard

Each time a screen is about to be sent, the producer compares the catalog version on every placed node, including those inside slots, with the session's catalog version.

1. All equal: the screen is sent.
2. Any different: the screen is not sent, the app gets `ScreenUnavailable`, the session stays accepted, and the observer is told the component, the screen, the session's version and the version of the bridge that placed it.

A screen registered for one version and written with that version's bridge never trips the guard. It catches a screen that mixes bridges by mistake.
