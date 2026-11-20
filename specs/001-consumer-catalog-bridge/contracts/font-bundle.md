# Contract: the font bundle

The app's font files, for the producer to measure server text with. It is a bundle of its own, with its own version, separate from the catalog, so the catalog stays build time only (FR-069). The design is in [research.md R20](../research.md#fonts) and [ADR 0017](../../../docs/adr/0017-app-fonts-travel-in-a-pinned-font-bundle.md). The steps that build, save, fetch and check it are in [build-tooling.md](build-tooling.md#fonts).

Its format has one definition, in `core` (`dev.tonholo.ssrcompose.core.font`), since both the build tooling and the running producer read it.

## Example

The font bundle of version 1, saved in `ssr/fonts/1/`:

```text
ssr/fonts/1/
├── fonts.json
└── brand/
    ├── 400-normal.ttf
    └── 700-normal.ttf
```

`fonts.json`:

```json
{
  "format": 1,
  "version": "1",
  "fonts": [
    {"family": "brand", "weight": 400, "italic": false, "file": "brand/400-normal.ttf", "fingerprint": "<64 lowercase hexadecimal digits>"},
    {"family": "brand", "weight": 700, "italic": false, "file": "brand/700-normal.ttf", "fingerprint": "<64 lowercase hexadecimal digits>"}
  ]
}
```

## Fields

| Field | Meaning | Rule |
|---|---|---|
| `format` | the version of this format | a whole number. A reader refuses a format it does not know. |
| `version` | the font bundle's version, `FontBundleVersion` | a label the app developer sets, 1 to 64 characters of lowercase ASCII letters, digits, dots, hyphens and underscores, that neither starts nor ends with a dot, and that is never a Windows device name (`con`, `prn`, `aux`, `nul`, `com1` to `com9`, `lpt1` to `lpt9`), alone or before a dot, since it names the folders `ssr/fonts/<version>/` and `ssrcompose/fonts/<version>/`. Lowercase only, so `A` and `a` cannot meet on a file system that ignores case; no trailing dot, so `1` and `1.` cannot meet on Windows, which drops trailing dots. A restriction is used rather than an encoding of the folder name, because it needs no encoding and decoding in every reader and keeps the folder name equal to the version. Two different labels never name the same folder. Only equal or not equal matters. |
| `fonts[].family` | the family name a server text names | a plain Kotlin identifier of at most 64 characters, as for a component name. Never `default`, which names the library's own font. |
| `fonts[].weight` | the weight | 1 to 1,000 |
| `fonts[].italic` | whether it is italic | true or false |
| `fonts[].file` | the file, next to `fonts.json` | always `<family>/<weight>-<normal or italic>.<ttf or otf>`, built from the fields above. A reader rejects the whole bundle otherwise, so a file name never holds a parent folder or a scheme. |
| `fonts[].fingerprint` | the SHA-256 of the file's bytes | 64 lowercase hexadecimal digits |

## Rules

- `fonts` is sorted by family, weight and italic. Two entries with the same family, weight and italic are rejected.
- `fonts.json` and each font file are refused, before they are read, when they are larger than their cap: 1 MiB for `fonts.json` and 16 MiB for a font file by default.
- The bundle's fingerprint is the SHA-256 of `fonts.json`, which holds every file's fingerprint, so it covers the whole bundle. A changed font file needs a new font version: the app's `ssrFontCheck` fails otherwise.
- A font file is untrusted input on the server. It is pinned by its fingerprint and by the lock file, reviewed when it is first saved like a catalog, and parsed only by the producer, with Skia, when it starts. The producer checks each file's fingerprint again and that Skia reads it as a font; a file that fails stops `Producer { }` at start and names the version, the family and the file.
- The library's own default font, Roboto, is not in a font bundle. The producer and the consumer each carry it ([ADR 0020](../../../docs/adr/0020-bundle-roboto-as-the-default-font.md)).

## How the two sides agree at run time

- The app sends its font version in `Introduce` (`fontVersion`, absent when the app declares no font). The build writes it into a generated resource, or the app gives it to `SsrScreen` by hand with its `ConsumerFonts`.
- The producer serves the font versions it loaded. An app whose font version it does not hold is refused with the kind "font version not supported". An app with no font version gets only the default font.
- Inside a session, `LocalConsumer.current.fonts` holds the families of that session's font bundle and the default font. A text in another family stops the screen with `ScreenUnavailable`.
- A font version change during a session is sent with `FontVersionChanged`: the producer switches to that bundle and lays the screen out again with its state kept, or refuses the session when it does not hold it ([messages.md](messages.md#rules), rule 3e).
- The consumer draws a text's `family` with the family of that name from its `ConsumerFonts`, built from the same declared files the bundle was made from. A family it does not have makes the screen `ScreenProblem.Mismatch`.
- The producer only sends a text whose weight and italic match a file of its family exactly, so neither side picks another weight or fakes one. Any other weight or italic stops the screen with `onScreenNotSendable` ([ADR 0020](../../../docs/adr/0020-bundle-roboto-as-the-default-font.md)).
