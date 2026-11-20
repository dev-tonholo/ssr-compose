# Contract: messages between consumer and producer

Contract version 1. The one definition both sides share. It lives in `core`.

Messages are described by meaning, not by bytes, except for the first frame's prefix. This feature ships one codec, JSON, so tests are readable. The later connection feature may add others without changing these messages.

## Consumer to producer

| Message | Fields | When |
|---|---|---|
| `Introduce` | `contractVersion`, `catalogVersion`, `fontVersion` (absent when the app declares no font), `app`, `device` (with `fontScale` and `layoutDirection`) | first, once per session, in the first frame |
| `OpenScreen` | `screen` (a name), `viewport` (the width and height in dp the app gives the screen; an unbounded side is sent as the window's size) | after `Accepted`. Asking again, or for another name, replaces the open screen. |
| `DeviceChanged` | `device` | when a device value changes, after `Accepted` |
| `FontVersionChanged` | `fontVersion` (absent when the app now declares no font) | when the font version of `SsrScreen`'s fonts changes during a session (rule 3e) |
| `ViewportChanged` | `viewport` | when the space the app gives the open screen changes, at most 20 times per second, keeping the latest viewport |
| `ComponentMeasured` | `revision`, `components` (each with `id`, `width`, `height` and, per slot, `maxWidth` and `maxHeight`, null when unbounded) | when the size or slot space of an app component of the shown or pending document changed, at most 20 times per second: changes between two sends are merged, keeping the latest size of each component (rule 5a) |
| `Trigger` | `action` (an action ID), `arguments` (a list of values, empty when the callback has none) | when the user triggers a callback of an app component, or taps a node with a remote click |
| `Switched` | `switcher` (a node id), `variant` | when the user taps a node with a local click, after the device already shows that variant (see "Screen content") |

## Producer to consumer

| Message | Fields | When |
|---|---|---|
| `Accepted` | `contractVersion` | the introduction is accepted |
| `Refused` | `kind`, `reason` | the introduction is refused, or a message arrives before acceptance |
| `Screen` | `screen`, `revision`, `pending`, `fontVersion` (the font version it was laid out with, absent for none), `content` | a screen was composed and laid out, and again each time it changes. `revision` rises by one with each `Screen` of a session. `pending` is true while an app component has no reported size or a slot has no reported space. |
| `ScreenUnavailable` | `screen`, `reason` | no screen is registered under that name for the session's catalog version, the screen placed a component from another catalog version's bridge, the screen holds content a document cannot express or is over the document limit, or the screen's code failed |

In code, `Accepted` and `Refused` are the two cases of the sealed `Answer` in `core`'s handshake package, and the producer message that carries an answer, `ProducerMessage.Answered`, wraps it. On the wire, the JSON codec shows `Accepted` and `Refused` as messages of their own, at the same level as `Screen` and `ScreenUnavailable`:

```json
{"type":"Accepted","contractVersion":1}
{"type":"Refused","kind":"IntroductionNotValid","reason":"Not valid"}
```

## The first frame

The consumer's first frame, the one that carries `Introduce`, is a 4-byte big-endian contract version followed by the codec's payload. No other frame, in either direction, has a prefix.

- The receiver knows which frame is the first from its position in the connection, never from its bytes.
- The prefix must equal the contract version inside `Introduce`. A first frame where they differ is not valid.
- The prefix lets the producer pick a codec, or refuse, before it decodes the payload.
- `MessageFrame` in `core` holds this layout and reads the version from a first frame.

## Screen content

`content` is a screen document: a tree of nodes that the producer composed, measured and placed with the device's density, font scale, layout direction and the viewport. Its root is the `Box` the producer wraps around every screen. The consumer places each node at the box it was given and decides nothing about order, size or position. The design, the node fields and full examples are in [research.md R20](../research.md#r20-server-rendered-content-194), and the types in [data-model.md](../data-model.md#screen).

Every node has an `id`, unique in the document, and `x`, `y`, `width` and `height` in dp from the top-left corner of its parent. `x` and `y` can be negative, where Compose placed a node past its parent's top or left edge, for example with an offset or a centered child larger than its parent; the consumer places it there too. `width` and `height` are never negative. The cases:

| Node | Holds |
|---|---|
| `Box` | an optional `decoration` (background, border, shape), `click`, `semantics` and `children` |
| `Text` | `text`, `style` (`family`, `size` in sp, `weight`, `italic`, `color`, `lineHeight`, `align`), `maxLines`, `overflow`, `click`, `semantics` |
| `Scroll` | `orientation`, `contentWidth`, `contentHeight`, `children`; scrolled on the device with no message |
| `Switcher` | `selected`, `largestWidth`, `largestHeight` and 2 to 16 `variants`, each a list of nodes; its box is the size of the `selected` variant |
| `Component` | an app component: `name`, `values`, `callbacks`, `slots`, `maxWidth`, `maxHeight`. Its `width` and `height` are the size the device last reported, or 0 before that. |

A slot of a `Component` holds a `SlotContent`: `width`, `height` and `nodes`, positioned from the slot's top-left corner and laid out by the producer in the space the device reported for that slot. Slot content can hold app components again.

A `click` is `Remote` (`action`, `label`, `role`), which sends `Trigger` with no arguments, or `Local` (`switcher`, `variant`), which shows that variant on the device at once and sends `Switched`. Colors are `"#AARRGGBB"`.

A `Switcher` node's box is the size of its `selected` variant, the one the producer laid the screen out with, and `largestWidth` and `largestHeight` give the size of its largest variant. A tap on a `Local` click shows the chosen variant at once, in a fixed box of the largest variant's size anchored at the switcher's top-left corner and drawn above the nodes after it, and sends `Switched(switcher, variant)`. The producer makes that variant the selected one, lays the screen out again and sends a new revision in which the switcher's box and the content around it are placed for that variant. Until that revision arrives, a larger variant can cover the content below it and a smaller one leaves a gap: the device moves nothing on its own. The device keeps its choice by switcher id across updates, also when a revision still names the older selected variant.

A screen with no app component:

```json
{"type":"Screen","screen":"home","revision":1,"pending":false,"content":{"root":{"type":"Box","id":1,"x":0.0,"y":0.0,"width":411.4,"height":843.4,"children":[{"type":"Text","id":5,"x":16.0,"y":16.0,"width":379.4,"height":40.0,"text":"Served by the producer","style":{"size":14.0,"weight":400,"color":"#DE000000"}}]}}}
```

An app component, after the device reported its size, and its slot holding server content:

```json
{"type":"ComponentMeasured","revision":4,"components":[{"id":9,"width":379.4,"height":160.0,"slots":{"content":{"maxWidth":347.4,"maxHeight":null}}}]}
{"type":"Component","id":9,"x":16.0,"y":16.0,"width":379.4,"height":160.0,"maxWidth":379.4,"maxHeight":null,"name":"Section","values":{"title":{"type":"Text","value":"Today"}},"slots":{"content":{"width":347.4,"height":20.0,"nodes":[{"type":"Text","id":11,"x":0.0,"y":0.0,"width":347.4,"height":20.0,"text":"Rendered by the server, inside the app's Section","style":{"size":14.0,"weight":400,"color":"#FF000000"}}]}}}
```

An app component on the wire carries no catalog version. The producer keeps the version of the bridge that placed each component, for the guard, and never sends it. An absent `values`, `callbacks` or `slots` is empty, and a parameter the screen did not give is absent.

An action ID is a short text the producer assigns. It is unique inside a session and means nothing outside it.

## Rules

1. `Introduce` is the first message, and only the first frame may carry it. Anything else before acceptance gets `Refused`, kind "introduction not valid".
2. No `Screen` is sent before `Accepted`.
3. Every app component in a `Screen`, at any depth, inside other nodes and inside slots, with its parameters, callbacks and slots, comes from the session's catalog version: it was placed by the bridge of that version, and the producer checks it before sending, and sends `ScreenUnavailable` instead when a component came from another version's bridge. The producer also sends `ScreenUnavailable` for a screen it cannot express as a document, such as one with an image, a brush, a lazy list, a font family not in the session's font bundle, a weight or italic its font family has no file for, or more than 10,000 app components, or one larger than its document limit (8 MiB by default).
3a. The consumer checks a document while decoding it. A negative or non-finite size, a non-finite position or one beyond 100,000 dp either way, a font size or line height not above 0 or above 1,000, `maxLines` below 1, a weight outside 1 to 1,000, a repeated `id`, a `Local` click to a switcher or variant that is not in the document, a `selected` variant the switcher does not have, a largest size smaller than a variant's box, or a switcher with fewer than 2 or more than 16 variants makes the message unreadable (rule 10).
3b. The consumer never shows a `pending` document. It measures it without showing it and sends `ComponentMeasured` for its revision; meanwhile it keeps showing the last document that was not pending, or the loading state before the first one. The producer ignores a `ComponentMeasured` for an older revision. A document stays pending until every app component in it has a reported size and every slot a reported space. An app component inside a slot can only be measured after its parent's slot was laid out, so this takes one round per level of nesting, and the depth limit of the frame bounds it at 31 levels. Once everything is measured, sizes that keep changing, such as a component whose size depends on its slot content and the other way round, stop after 4 more rounds for one change (a limit the server can set), and the producer sends the latest reported sizes, not pending. So a screen is never shown with an app component that has no reported size.
3c. A text whose style names a `family` the app did not give `SsrScreen`, like a component with no binding, makes the screen a mismatch, and `ScreenProblem.Mismatch.component` holds that family name. A text with no `family` is drawn with the library's own Roboto, the font the producer measured it with. The producer writes no `family` for a text in its bundled Roboto family, and refuses a server text that names no family at all ([ADR 0020](../../../docs/adr/0020-bundle-roboto-as-the-default-font.md)).
3d. An `Introduce` with a `fontVersion` the producer does not hold is refused with the kind "font version not supported" ([font-bundle.md](font-bundle.md)).
3e. When `SsrScreen`'s fonts change to another font version during a session, the consumer sends `FontVersionChanged` with the new version (absent when the app now declares no font). The producer checks that it holds that bundle. If it does, it switches the session's fonts, lays the screen out again with its state kept, and sends a new revision; each `Screen` names the `fontVersion` it was laid out with, and the consumer keeps drawing a document with the fonts of the version it names, so it never draws with fonts the producer did not measure. If the producer does not hold that version, it answers `Refused` with the kind "font version not supported" and the session is over (rule 11). `FontVersionChanged` is untrusted and counts against the trigger limit of rule 7b; over it, the producer keeps the latest version and applies it when the next second starts, never dropping it.
4. A `Trigger` for an action ID the open screen no longer has is ignored and reported. It never ends the session.
5. Triggers from one session run in the order they arrive.
4a. A `Switched` for a switcher the open screen no longer has is ignored and reported; one with a variant the switcher does not have is refused and reported. `Switched` counts against the trigger limit of rule 7b, but it is never dropped: over the limit the producer keeps the latest variant of each switcher and applies it when the next second starts, so the producer always ends on the variant the device shows. Neither ends the session.
5a. `ComponentMeasured` and `ViewportChanged` are untrusted. A size or space that is negative, not finite or above 100,000 dp, more than one measurement for one component, more than 10,000 measurements in one message, more than 118 slots in one measurement (the most parameters a bridge can have), or a slot name longer than 64 characters, is refused and reported without laying anything out. Until the frame limit of T088 exists, these lists are checked only after the whole message is decoded. A measurement for an id that is not an app component of the latest revision is ignored and reported. One message holds every changed component, so a first report is one message; a screen with more than 10,000 app components is not sent (rule 3). These two messages have their own limit, 30 per second per session by default, apart from the trigger limit of rule 7b, so they never crowd out taps. Over that limit the producer drops nothing: it merges what arrives, keeping the latest size of each component and the latest viewport, and applies the merged values when the next second starts. None of these ends the session.
6. `Trigger` arguments larger than the limit (1 megabyte in total by default) are refused without being decoded. Arguments whose number or kinds do not match what the bridge declared for that callback, or that the bridge's conversion cannot turn into the lambda's types, are refused and reported. Nothing runs for a refused trigger.
7. `reason` texts are for the user and carry no internal details. The producer writes each `reason` of `Refused` and `ScreenUnavailable` in the language the app sent in `DeviceContext.language`, or in the latest one after a `DeviceChanged`. When that language is missing or not valid, as in a refusal of an introduction the producer cannot read, or when the producer has no text in it, the reason is in English. The consumer shows a reason as given.
7a. A trigger whose lambda fails changes nothing: the screen stays as it was and no message is sent for it.
7b. Triggers over the session's limit (100 per second by default) are dropped and reported. A `Switched` over the same limit is merged, not dropped (rule 4a).
8. A frame larger than its limit is refused without being decoded.
9. The first frame starts with the contract version, as described above, so the producer can pick a codec, or refuse, before decoding the rest.
10. A consumer that cannot decode a producer message treats it like a refusal: the conversation is over. The app shows `ScreenProblem.AnswerUnreadable` ([consumer-api.md](consumer-api.md#problems-the-user-can-be-shown)).
11. After `ScreenUnavailable` the session stays accepted. After `Refused` the session is over.
12. The connection is expected to deliver frames in order and without loss, or to end.

## Seams in `core`

```kotlin
interface Connection {
    val incoming: Flow<ByteArray>
    suspend fun send(frame: ByteArray)
    suspend fun close()
}

interface MessageCodec {
    val contractVersion: ContractVersion
    fun encodeFirstFrame(introduce: ConsumerMessage.Introduce): ByteArray
    fun decodeFirstFrame(frame: ByteArray): ConsumerMessage.Introduce
    fun encode(message: ConsumerMessage): ByteArray
    fun encode(message: ProducerMessage): ByteArray
    fun decodeConsumerMessage(frame: ByteArray): ConsumerMessage
    fun decodeProducerMessage(frame: ByteArray): ProducerMessage

    companion object {
        fun json(): MessageCodec
    }
}

class MessageDecodingException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

Sketches of the shape, matching the code already written for T011 and T019, before R20 adds `ViewportChanged`, `ComponentMeasured` and the document. Every decoding failure, of a first frame or any other, throws `MessageDecodingException` and nothing else. `decodeConsumerMessage` rejects an `Introduce`, since only the first frame may carry one. `MessageCodec.json()` is the public way to get the JSON codec for contract version 1. No type from a transport or from one encoding appears in these seams. The `core-testing` module has a public `InMemoryConnection` pair for tests. Which `core` types are public is listed once, in [data-model.md](../data-model.md#what-is-public-in-core).
