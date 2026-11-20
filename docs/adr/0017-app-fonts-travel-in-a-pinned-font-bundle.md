# 0017. The app's fonts travel in a pinned font bundle

- Status: Accepted
- Date: 2026-11-20
- Modules: `core`, `tooling`, `gradle-plugin`, `producer`, `consumer`

## Context

With [ADR 0015](0015-producer-renders-with-compose-ui-foundation.md) the producer measures text, and the device draws it. Measured with another font, the same text gets other widths, line heights and line breaks: a probe on 2026-11-20 measured one text at 147, 148 and 144 px of height with three fonts at the same size. The owner asked on 2026-11-20 for the app's fonts to reach the producer in this item.

The catalog is build time only (FR-069), and the producer needs fonts while it runs. A probe showed that Compose desktop loads a font from bytes (`androidx.compose.ui.text.platform.Font(identity, data, weight)`), that bytes that are not a font fall back to the default font without any error, and that Skia's `FontMgr.makeFromData` returns null for them.

## Options

1. The font files travel inside the catalog bundle, and FR-069 gains an exception for them. One channel and one version, but the catalog is then needed at run time on the server, against an earlier owner answer.
2. A font bundle of its own, with its own version: built from the app's declared fonts, saved and published like a catalog, fetched and pinned by the server build with a lock file, put in the server's resources, and named by the app in its introduction. The catalog stays build time only. Cost: a second versioned artifact the two builds share, and a second set of build steps.
3. The server developer configures font files on the producer by hand. Nothing ties them to the app's fonts, so they drift without anyone noticing.
4. The consumer sends its fonts when it connects. Megabytes on every connection, from an untrusted peer.

## Decision

Option 2, chosen by the owner on 2026-11-20. FR-069 stays as it was. The format is defined once, in `core`, since the build tooling and the running producer both read it ([research.md R20](../../specs/001-consumer-catalog-bridge/research.md#fonts) and `contracts/font-bundle.md`). Everything the plugin does stays possible by hand: a server developer can give a font version's files to the producer builder, and an app developer can give the consumer its font families and version.

## Consequences

- Good: the producer measures with the same font files the device draws with.
- Good: the catalog stays build time only, and fonts change on their own version without a new catalog version.
- Good: a session names its font version, so the producer refuses an app whose fonts it does not hold instead of measuring with the wrong ones.
- Good: a font version change during a session reaches the producer, chosen by the owner on 2026-11-20 over fixing the version for the session: the consumer sends `FontVersionChanged`, and the producer switches the session's fonts and lays the screen out again with its state kept, or refuses the session when it does not hold that version. Each `Screen` names the font version it was laid out with ([contracts/messages.md](../../specs/001-consumer-catalog-bridge/contracts/messages.md#rules), rule 3e). A new message was chosen over a field in `DeviceChanged`: fonts are the app's, not device values, and `DeviceChanged` has no refusal path, so a field there would add a refusal to every device change and repeat the introduction's font version.
- Bad: the catalog is no longer the only thing the two builds share, which principle IV asks for. The plan records it as a deviation.
- Bad: a missing or broken font stops the producer at start, since it cannot measure correctly without it.
- Follow-up: the font steps in the tooling, the Gradle plugin and the Kotlin toolchain plugin, the producer's loading and the consumer's `ConsumerFonts` (R20 slice 8).
