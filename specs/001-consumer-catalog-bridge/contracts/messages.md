# Contract: messages between consumer and producer

Contract version 1. The one definition both sides share. It lives in `core`.

Messages are described by meaning, not by bytes. This feature ships one codec, JSON, so tests are readable. The later connection feature may add others without changing these messages.

## Consumer to producer

| Message | Fields | When |
|---|---|---|
| `Introduce` | `contractVersion`, `catalogVersion`, `app`, `device` | first, once per session |
| `OpenScreen` | `screen` (a name) | after `Accepted`. Asking again, or for another name, replaces the open screen. |
| `DeviceChanged` | `device` | when a device value changes, after `Accepted` |
| `Trigger` | `action` (an action ID), `arguments` (a list of values, empty when the callback has none) | when the user triggers a callback of a placed component |

## Producer to consumer

| Message | Fields | When |
|---|---|---|
| `Accepted` | `contractVersion` | the introduction is accepted |
| `Refused` | `kind`, `reason` | the introduction is refused, or a message arrives before acceptance |
| `Screen` | `screen`, `content` | a screen was composed, and again each time it changes |
| `ScreenUnavailable` | `screen`, `reason` | a component is missing with no replacement, the name is not known, or the screen's code failed |

## Screen content

`content` is an ordered list of placed components, shown top to bottom. This top-level arrangement is provisional. Inside it, structure comes from the app's own composables and their slots.

A placed component has:

| Field | Meaning |
|---|---|
| `name` | the component's name in the session's catalog |
| `values` | the value parameters, by name, already fitted to the session's catalog version |
| `callbacks` | callback parameter name to action ID |
| `slots` | slot parameter name to an ordered list of placed components |

An action ID is a short text the producer assigns. It is unique inside a session and means nothing outside it.

## Rules

1. `Introduce` is the first message. Anything else before acceptance gets `Refused`, kind "introduction not valid".
2. No `Screen` is sent before `Accepted`.
3. A `Screen` holds only components, parameters, callbacks and slots that are in the session's catalog.
4. A `Trigger` for an action ID the open screen no longer has is ignored and reported. It never ends the session.
5. Triggers from one session run in the order they arrive.
6. `Trigger` arguments larger than the limit (1 megabyte in total by default) are refused without being decoded. Arguments that do not match the callback's kinds in the session's catalog are refused and reported.
7. `reason` texts are for the user and carry no internal details.
7a. A trigger whose lambda fails changes nothing: the screen stays as it was and no message is sent for it.
7b. Triggers over the session's limit (100 per second by default) are dropped and reported.
8. A frame larger than its limit is refused without being decoded.
9. The first frame names the contract version in a way every codec can read, so the producer can pick a codec, or refuse, before decoding the rest.
10. A consumer that cannot decode a producer message treats it as a refusal.
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
    fun encode(message: ConsumerMessage): ByteArray
    fun encode(message: ProducerMessage): ByteArray
    fun decodeConsumerMessage(frame: ByteArray): ConsumerMessage
    fun decodeProducerMessage(frame: ByteArray): ProducerMessage
}
```

Sketches of the shape, not final signatures. No type from a transport or from one encoding appears in them. `core` also has a public `InMemoryConnection` pair for tests.
