# 0020. Bundle Roboto as the default font

- Status: Accepted
- Date: 2026-11-20
- Modules: `producer`, `consumer`

## Context

The producer measures text and the device draws it ([ADR 0015](0015-producer-renders-with-compose-ui-foundation.md)). A text whose style names no app font family uses the default font. On the producer, the default is whatever Skia picks on the server's OS; on a device it is the system font, which is not Roboto on many phones, such as many Samsung phones. Measured with one and drawn with the other, line breaks differ.

## Options

1. The library bundles Roboto (Apache 2.0) in both the producer and the consumer, and measures and draws default text with it, never with the system font. Same font on both sides. Cost: four static files (weights 400, 500 and 700, and 400 italic) in every app that uses the consumer library, and a notice for the license.
2. The producer measures default text with a bundled Roboto, and the device draws it with its system font. Nothing grows in the app, but line breaks differ where the system font is not Roboto.
3. No default font: a text with no app family stops the screen, so every server text names a font from the font bundle. Exact, but every server text needs an app font, and an app with no fonts cannot show text.

## Decision

Option 1, chosen by the owner on 2026-11-20. Default text is measured and drawn with the same bundled Roboto. The family name `default` is reserved for it and never written in a document: a text with no `family` uses it.

Only the weights and styles that have a file are sent: Roboto 400, 500 and 700, and 400 italic. Skia and Android pick a missing weight, or fake bold and italic, in different ways, which would move line breaks. So a text at any other weight or style stops the screen with `onScreenNotSendable`, which names the family, the weight and the italic. The same rule holds for every family of a font bundle.

## Consequences

- Good: default text measures and draws the same on both sides, also in an app with no fonts of its own.
- Bad: the consumer library grows by four font files. Their size is not measured here; the worker records it when they are added.
- Bad: default text does not follow the device's system font.
- Bad: a server developer can only use the weights that have a file; any other weight is refused, not faked.
- Follow-up: the producer makes `FontFamily.Default` resolve to the bundled Roboto in its scene; how it does that is not proven and slice 3 settles it. The Roboto files are added through the add-dependency skill, with their license notice.
