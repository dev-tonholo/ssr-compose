# 0015. The producer renders with Compose UI foundation and sends a positioned document

- Status: Accepted
- Date: 2026-11-20
- Modules: `core`, `producer`, `consumer`

## Context

Principle I of the constitution says the project is server-side rendering, never server-driven rendering. On 2026-11-20 the owner turned down a content model whose only case was app catalog components arranged by the consumer, and chose to bring the core of server-rendered documents into Feature 1. The owner then said: "We already send the device's density and screen size in the handshake. The server knows this information and can use it to properly measure the components." So the producer measures and places what a screen shows, and the consumer only draws it. Asked why the producer cannot use Compose UI foundation, the owner wanted evidence, so it was tried ([research.md R20](../../specs/001-consumer-catalog-bridge/research.md#r20-server-rendered-content-194)).

The probes ran Compose Multiplatform 1.12.2 desktop on a plain JVM 17, headless. Real `Column`, `Row`, `Box`, `BasicText` and modifiers composed and laid out in `ImageComposeScene` at the handshake's size, density and font scale, recomposed on state changes, and the result was read through public API: `ImageComposeScene.semanticsOwners`, `SemanticsNode.children` and `layoutInfo`, `ModifierInfo.modifier` as `InspectableValue` with `isDebugInspectorInfoEnabled`, and the text layout result from the semantics. No opt-in warning was reported for any of these. `Column`, `Row`, `Spacer` and a `Box` with only padding do not appear in that tree; text, backgrounds, borders, clickables and scroll containers do, each with its box.

## Options

1. The library's own layout composables (`Column`, `Row`, `Box`, `Text` and a `Modifier` of its own) that emit a layout tree, which the consumer measures with Compose foundation. Server developers learn a second set of names that look like Compose but are not. The consumer measures, which the owner did not want.
2. A draw-op document laid out on the producer, as the proof of concept did with AndroidX Remote Compose fragments. Principle III forbids that dependency, an own draw-op format is a large piece of work, and text would become glyph runs instead of real text, which loses TalkBack, selection and native font rendering.
3. Real Compose UI foundation on the producer, read through the public semantics and inspector API, sent as a positioned document: each drawn node with its box in dp relative to its parent, and its text, decoration, click or scroll data. Server developers write normal Compose. Costs: a Skiko native jar for each server OS (18.3 MB for macOS arm64, 38.5 MB of runtime classpath in total), a process-wide debug flag, about 23 ms and 297 KB of heap per composed screen of 50 rows once warm, and a layout tree that is not sent, so the consumer cannot re-flow anything.
4. Real Compose UI foundation, reading the internal `LayoutNode` tree to send a layout tree. It would keep `Column` and `Row`, but it depends on internal API that changes without notice.

## Decision

Option 3, chosen by the owner on 2026-11-20. Server developers write normal Compose, the producer measures and places with the device's values, and the document stays our own format. The owner accepted the Skiko native jars per server OS, the process-wide `isDebugInspectorInfoEnabled` flag and the memory cost, and asked for a load test at the 10,000 sessions bound and for tests that pin which nodes appear in the result, so a Compose upgrade that changes it fails the build.

## Consequences

- Good: no layout decision is left to the consumer; it places each node where the producer put it.
- Good: text stays real text on the device, drawn with the device's text engine inside the box the producer measured.
- Good: the producer's screens are plain Compose, so state, effects, conditions and loops run there as they always do.
- Bad: which nodes show in the semantics tree is observed, not documented. Pinning tests guard it on every Compose upgrade.
- Bad: the producer measures text with fonts the device must also have, so the app's fonts travel to the producer ([ADR 0017](0017-app-fonts-travel-in-a-pinned-font-bundle.md)), and both sides carry the same default font ([ADR 0020](0020-bundle-roboto-as-the-default-font.md)).
- Bad: `LazyColumn` composes only what is visible, so it is not supported in this version; server content scrolls with `verticalScroll`.
- Bad: the producer gains Compose UI, foundation and Skiko as dependencies, and a server picks the Skiko runtime of the OS it runs on.
- Follow-up: the slices of R20.
