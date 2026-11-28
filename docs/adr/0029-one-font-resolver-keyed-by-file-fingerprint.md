# 0029. One font resolver for every scene, with each font named by its file's fingerprint

- Status: Accepted on 2026-11-27 by the owner
- Date: 2026-11-27
- Modules: `producer`

## Context

Compose UI desktop 1.12.2 makes a font resolver per scene root (`RootNodeOwner.skiko.kt` line 547), and each resolver that loads a font keeps its own Skia typeface and font collection. On the reference laptop that copy costs about 720 KB per live session, a quarter of the 2.8 MB budget of [research.md R20](../../specs/001-consumer-catalog-bridge/research.md#first-slices-of-l2b). One resolver for all scenes measured 2,547 to 2,643 KB for a bare layout-only scene, against 3,280 to 3,431 KB with one per scene (#227 C1 memory report).

The risk: Compose caches a font loaded from bytes by its class, `identity`, weight and style, never by its bytes (`DesktopFont.desktop.kt` line 37, `FontCache.load`), and `LoadedFont.equals` ignores the bytes too. Once a key is registered in a font collection it is never replaced. App fonts come in font bundles with versions ([ADR 0017](0017-app-fonts-travel-in-a-pinned-font-bundle.md)), and a new version exists exactly because a file changed, often under the same family, weight and style. Probe P46: with one resolver and the same identity, a second font file measured a line at 1,075 px, the first file's width, where alone it measures 1,093 px. The same happens inside one scene today: a session that switches bundles with `FontVersionChanged` and keeps the identity would go on measuring with the old file.

## Options

1. One resolver per scene, as today. Correct only if identities never repeat across versions, and costs about 720 KB per session.
2. One resolver for every scene, with each font's identity the SHA-256 of its file. A key then names exactly one file: two files never share one, and the same file in two bundles shares one typeface, which is correct. This holds inside one version too: two families of one bundle with different files under the same weight and style get different identities, and two that use the same file share its typeface. P46: 1,075 and 1,093 px, each file its own width.
3. One resolver per font bundle version. It separates versions, but identities must still be distinct inside each version, since two families of one bundle can hold different files under the same identity, weight and style. A session that switches version changes resolver, and memory grows with the versions in use; it adds a moving part for nothing option 2 lacks.

## Decision

Option 2, accepted by the owner on 2026-11-27. The producer holds one `FontFamily.Resolver` for the process and provides it as `LocalFontFamilyResolver` around every screen in `ScreenScene`. It is used only on the scene thread, since Compose's font cache has no lock ([ADR 0021](0021-producer-runs-every-scene-on-one-scene-thread.md)). Every font the producer creates from bytes, the bundled Roboto included, takes as its identity `sha256:` and the file's fingerprint: from `fonts.json` for a bundle font, which the producer checks again at start ([contracts/font-bundle.md](../../specs/001-consumer-catalog-bridge/contracts/font-bundle.md)), and computed once at load for Roboto.

`FontVersionChanged` evicts nothing. The session switches to the families of the bundle it names, whose fonts already carry their own fingerprints, the screen recomposes and lays out with them, and the old typefaces stay registered. That is bounded: the producer loads only the versions it holds at start, so the cache holds at most the distinct font files of those versions plus the four bundled Roboto files, which any session may use with or without a bundle.

## Consequences

- Good: about 720 KB less per live session on the laptop (P43 on a Mac: 4,261 to 3,543 KB with the new wake rule).
- Good: the identity rule also fixes the stale font after `FontVersionChanged` inside one scene, which exists with or without sharing.
- Bad: every typeface any session loaded stays in memory for the life of the process, bounded by the bundles loaded at start and the bundled Roboto.
- Bad: registering a new typeface clears the shared paragraph cache of every session (`FontCache.ensureRegistered`); this happens once per font file.
- Bad: unproven on Linux: whether a glyph missing from one session's font is measured with a font another session registered. On macOS it was not (P46), but the fallback goes through the shared collection. A test in the fonts slice settles it on Linux CI.
- Follow-up: slice S5 in R20, "Waking and fonts for live sessions"; T128 builds bundle fonts with this identity.
