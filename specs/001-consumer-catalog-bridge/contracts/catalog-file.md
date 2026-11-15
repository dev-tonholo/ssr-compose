# Contract: the catalog file

The one thing the app's build and the server's build share. It is produced by the app's build and never written by hand, but it is plain text a person can read and review.

Format version 1. The reader and writer live in `core`.

## Example

```json
{
  "format": 1,
  "version": "3",
  "choices": [
    { "name": "Style", "entries": ["bodyMedium", "titleLarge"] }
  ],
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
      "name": "Greeting",
      "parameters": [
        { "name": "title", "kind": "text", "required": true },
        { "name": "style", "kind": "choice", "choice": "Style", "required": false }
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

## Fields

| Field | Meaning | Rule |
|---|---|---|
| `format` | the version of this file format | a whole number. A reader refuses a format it does not know. |
| `version` | the catalog version the app developer set | at most 64 characters, no control characters. A label with no order. |
| `choices` | every fixed list of choices a parameter uses | sorted by `name`. Each has at least one entry, in declaration order. |
| `components` | every marked composable | sorted by `name`, at most 1,000. Names are unique. |
| `parameters` | a component's inputs | in declaration order, at most 50. Names are unique in a component. |

## Parameter kinds

| `kind` | Extra fields | Comes from |
|---|---|---|
| `text` | | `String` |
| `whole` | | `Int`, `Long` |
| `decimal` | | `Float`, `Double` |
| `flag` | | `Boolean` |
| `choice` | `choice`: the name of an entry in `choices` | an `enum class` |
| `list` | `of`: one of the value kinds above | `List<T>` |
| `callback` | `arguments` (optional): up to four value kinds, or `bytes` | `() -> Unit`, `(A) -> Unit`, `(A, B) -> Unit` and so on |
| `slot` | `accepts`: `any`, `consumer` or `producer`. `arguments` (optional): value kinds the component passes to the content | `@Composable () -> Unit`, `@Composable (A, B) -> Unit` |

- `required` is false when the parameter has a default value in the composable. The catalog does not hold the default itself: only the app knows it.
- A value or choice parameter may also have `"nullable": true`, when its type in the composable is nullable. The field is left out when false.
- `bytes` is allowed only as a callback argument.
- A callback and a slot use the same field, `arguments`, for what they pass along.
- A callback has no result. A callback with more than four arguments is a build error in the app.
- A slot's `accepts` comes from `@ConsumerComposable` or `@ProducerComposable` on the slot's type. With neither, it is `any`. It is recorded in this feature and not checked yet.
- A slot with `arguments` can be described here. Filling it needs the later remote composables feature. Until then, an optional one is simply not given, and a component with a required one cannot be placed by the server at all. `CameraScanner` above is such a component.
- A modifier parameter is left out of the file.

## Names

Every `name` and every choice entry must be a valid Kotlin identifier that is not a keyword, at most 64 characters. A reader rejects the whole file otherwise, and creates nothing from it.

## Stability

The same marked composables always give the same bytes: fixed key order, sorted components and choices, two-space indent, one trailing newline, no timestamps and no paths. A change to the catalog is then a small reviewable difference.

## Fingerprint

The fingerprint of a catalog file is the SHA-256 of its bytes, written in lowercase hexadecimal.

## Where the file lives

- In the app's repository: one saved file per released catalog version, committed. The app build compares what it produces with the saved file of the same version and fails when they differ.
- In the server's repository: one saved file per catalog version it holds, with its fingerprint in a lock file. See [build-tooling.md](build-tooling.md).
