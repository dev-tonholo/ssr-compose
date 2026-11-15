# Data Model: Consumer Catalog and Bridge

Entities from the spec with their fields and rules. They live in `core` unless noted. Kotlin names are proposals.

## Catalog

### CatalogVersion

A label the app developer sets. At most 64 characters, no control characters. No order: only equal or not equal matters.

### ValueKind

text, whole number, decimal number, yes or no (`flag` in the file), choice (names a `ChoiceList`), list of one value kind. Bytes exists only as a callback argument.

### ChoiceList

| Field | Rule |
|---|---|
| `name` | a valid identifier, unique in the catalog |
| `entries` | at least one, valid identifiers, unique, in declaration order |

### Parameter

| Field | Meaning |
|---|---|
| `name` | a valid identifier, unique in its component |
| `kind` | a value kind, a callback, or a slot |
| `required` | false when the composable gives the parameter a default. The default itself is not in the catalog. |
| `nullable` | true when a value or choice parameter's type is nullable |

A callback parameter lists the kinds of the arguments it carries, none to four. A slot parameter says what it accepts (`any`, `consumer`, `producer`) and may list the value kinds it passes to its content.

### ComponentDescriptor

| Field | Rule |
|---|---|
| `name` | a valid identifier, unique in the catalog |
| `parameters` | in declaration order, "at most 50" |

### Catalog

| Field | Rule |
|---|---|
| `format` | the file format version |
| `version` | `CatalogVersion` |
| `choices` | sorted by name |
| `components` | sorted by name, "at most 1,000" |

A catalog can be read from and written to a catalog file, and has a fingerprint (SHA-256 of the file's bytes). See [contracts/catalog-file.md](contracts/catalog-file.md).

### CatalogSet (producer)

What the producer is given at start: every catalog it holds, the set of versions it supports (by default, all held), and the one version the bridge was created from. Rules: versions are unique; two held catalogs with the same version must have the same fingerprint; the bridge version is one of the held ones.

### SavedCatalog and lock file (server project)

One catalog file per held version inside the server project, and one lock file that lists, for each: the version, the location it came from, and its fingerprint. See [contracts/build-tooling.md](contracts/build-tooling.md).

## Handshake

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

`Accepted` with the contract version, or `Refused` with a `RefusalKind` and a reason for the user.

`RefusalKind`: contract version too old, contract version too new, catalog version not known or no longer supported, introduction not valid.

## Screen

### DataValue

The neutral model for values on the wire: text, whole number, decimal number, flag, choice entry (by name), list, bytes, nothing.

### PlacedComponent

A parameter the screen did not give is absent from all three maps, and the app's composable uses its own default for it.


| Field | Meaning |
|---|---|
| `name` | the component's name |
| `values` | value parameter name to `DataValue` |
| `callbacks` | callback parameter name to `ActionId` |
| `slots` | slot parameter name to an ordered list of `PlacedComponent` |

### ActionId

A short text the producer assigns to one callback of one placed component. Unique inside a session.

### ScreenContent

An ordered list of `PlacedComponent`.

## Session (producer)

One accepted connection between one app and the producer.

```text
Connected -> Accepted -> Open(screen) -> Closed
         \-> Refused -> Closed
```

| From | Event | To | What happens |
|---|---|---|---|
| Connected | a valid introduction | Accepted | `Accepted` is sent |
| Connected | an invalid or unsupported introduction, or any other message | Refused | `Refused` is sent and the session closes |
| Accepted or Open | the app opens a screen the producer has | Open | the open screen, if any, is disposed; the new one is composed and sent |
| Accepted or Open | the app opens a name the producer does not have | Accepted | `ScreenUnavailable` is sent and reported |
| Open | a device change | Open | the context changes, the screen recomposes, state is kept |
| Open | a trigger for an action the screen has | Open | the lambda runs; if state changed, the screen recomposes and is sent |
| Open | a trigger for an action the screen does not have | Open | ignored and reported |
| Open | a trigger whose lambda fails | Open | the changes it made are thrown away, the screen stays as it was, and the failure is reported |
| Open | triggers arrive over the limit, "100 per second by default" | Open | the extra ones are dropped and reported |
| Open | a component is missing with no replacement, or the screen's code fails | Accepted | `ScreenUnavailable` is sent and reported |
| any | the connection ends | Closed | the composition is disposed |

A session holds: its catalog, its contract version, the app identity, the current device context, the live composition of the open screen, and that screen's actions. Nothing in it is shared with another session.

## Fitting rules

Given what a screen gives a component, and that component's descriptor in the session's catalog:

1. The component is not in the catalog: missing.
2. A parameter the screen gives that the descriptor does not have is left out, and reported. This holds for values, callbacks and slots.
3. A required parameter of the descriptor that the screen did not give: missing.
4. A parameter whose kind differs from the descriptor's: missing. Kinds are compared exactly.
5. A choice value whose entry the descriptor's choice list does not have: missing.
6. Slot content is fitted the same way, component by component. A missing component inside a slot makes the whole screen follow the replacement rule.

"Missing" then follows the spec: the replacement is shown if one was defined, otherwise the whole screen is unavailable.
