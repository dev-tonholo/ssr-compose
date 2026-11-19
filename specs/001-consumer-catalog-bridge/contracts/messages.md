# Contract: messages between consumer and producer

Contract version 1. The one definition both sides share. It lives in `core`.

Messages are described by meaning, not by bytes, except for the first frame's prefix. This feature ships one codec, JSON, so tests are readable. The later connection feature may add others without changing these messages.

## Consumer to producer

| Message | Fields | When |
|---|---|---|
| `Introduce` | `contractVersion`, `catalogVersion`, `app`, `device` | first, once per session, in the first frame |
| `OpenScreen` | `screen` (a name) | after `Accepted`. Asking again, or for another name, replaces the open screen. |
| `DeviceChanged` | `device` | when a device value changes, after `Accepted` |
| `Trigger` | `action` (an action ID), `arguments` (a list of values, empty when the callback has none) | when the user triggers a callback of a placed component |

## Producer to consumer

| Message | Fields | When |
|---|---|---|
| `Accepted` | `contractVersion` | the introduction is accepted |
| `Refused` | `kind`, `reason` | the introduction is refused, or a message arrives before acceptance |
| `Screen` | `screen`, `content` | a screen was composed, and again each time it changes |
| `ScreenUnavailable` | `screen`, `reason` | no screen is registered under that name for the session's catalog version, the screen placed a component from another catalog version's bridge, or the screen's code failed |

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

`content` is an ordered list of placed components, shown top to bottom. This top-level arrangement is provisional. Inside it, structure comes from the app's own composables and their slots.

A placed component has:

| Field | Meaning |
|---|---|
| `name` | the component's name in the session's catalog version |
| `values` | the value parameters, by name |
| `callbacks` | callback parameter name to action ID |
| `slots` | slot parameter name to an ordered list of placed components |

A placed component on the wire carries no catalog version. The producer keeps the version of the bridge that placed each component on its own node, for the guard, and never sends it.

An action ID is a short text the producer assigns. It is unique inside a session and means nothing outside it.

## Rules

1. `Introduce` is the first message, and only the first frame may carry it. Anything else before acceptance gets `Refused`, kind "introduction not valid".
2. No `Screen` is sent before `Accepted`.
3. A `Screen` holds only components, parameters, callbacks and slots of the session's catalog version. Every component of a sent screen was placed by the bridge of that version: the producer checks it before sending, and sends `ScreenUnavailable` instead when a component came from another version's bridge.
4. A `Trigger` for an action ID the open screen no longer has is ignored and reported. It never ends the session.
5. Triggers from one session run in the order they arrive.
6. `Trigger` arguments larger than the limit (1 megabyte in total by default) are refused without being decoded. Arguments whose number or kinds do not match what the bridge declared for that callback, or that the bridge's conversion cannot turn into the lambda's types, are refused and reported. Nothing runs for a refused trigger.
7. `reason` texts are for the user and carry no internal details. The producer writes each `reason` of `Refused` and `ScreenUnavailable` in the language the app sent in `DeviceContext.language`, or in the latest one after a `DeviceChanged`. When that language is missing or not valid, as in a refusal of an introduction the producer cannot read, or when the producer has no text in it, the reason is in English. The consumer shows a reason as given.
7a. A trigger whose lambda fails changes nothing: the screen stays as it was and no message is sent for it.
7b. Triggers over the session's limit (100 per second by default) are dropped and reported.
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

Sketches of the shape, matching the code already written for T011 and T019. Every decoding failure, of a first frame or any other, throws `MessageDecodingException` and nothing else. `decodeConsumerMessage` rejects an `Introduce`, since only the first frame may carry one. `MessageCodec.json()` is the public way to get the JSON codec for contract version 1. No type from a transport or from one encoding appears in these seams. The `core-testing` module has a public `InMemoryConnection` pair for tests. Which `core` types are public is listed once, in [data-model.md](../data-model.md#what-is-public-in-core).
