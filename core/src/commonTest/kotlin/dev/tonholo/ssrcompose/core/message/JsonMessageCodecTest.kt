package dev.tonholo.ssrcompose.core.message

import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.handshake.Answer
import dev.tonholo.ssrcompose.core.handshake.AppIdentity
import dev.tonholo.ssrcompose.core.handshake.ContractVersion
import dev.tonholo.ssrcompose.core.handshake.Introduction
import dev.tonholo.ssrcompose.core.handshake.RefusalKind
import dev.tonholo.ssrcompose.core.handshake.TestDevices
import dev.tonholo.ssrcompose.core.handshake.Theme
import dev.tonholo.ssrcompose.core.message.internal.JsonMessageCodec
import dev.tonholo.ssrcompose.core.message.internal.MAX_NESTING_DEPTH
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonMessageCodecTest {
    private val codec = JsonMessageCodec()
    private val version = ContractVersion(value = 1)
    private val device = TestDevices.portrait.copy(theme = Theme.Dark, language = "pt-BR")
    private val introduce = ConsumerMessage.Introduce(
        introduction = Introduction(
            contractVersion = version,
            catalogVersion = CatalogVersion(value = "home-2"),
            app = AppIdentity(version = "1.2.3", variant = "release", flavor = "paid"),
            device = device,
        ),
    )

    private fun consumerRoundTrip(message: ConsumerMessage): ConsumerMessage =
        codec.decodeConsumerMessage(frame = codec.encode(message = message))

    private fun producerRoundTrip(message: ProducerMessage): ProducerMessage =
        codec.decodeProducerMessage(frame = codec.encode(message = message))

    private fun failureClassOf(block: () -> Unit): KClass<out Throwable>? =
        runCatching(block = block).exceptionOrNull()?.let { failure -> failure::class }

    private fun nestedLists(levels: Int): DataValue =
        (1 until levels).fold(initial = DataValue.ListValue(items = emptyList())) { inner, _ ->
            DataValue.ListValue(items = listOf(inner))
        }

    private fun triggerWith(argument: DataValue) =
        ConsumerMessage.Trigger(action = ActionId(value = "a"), arguments = listOf(argument))

    private fun nestedSlots(levels: Int): PlacedComponent =
        (1 until levels).fold(initial = PlacedComponent(name = "Box")) { inner, _ ->
            PlacedComponent(name = "Box", slots = mapOf("content" to listOf(inner)))
        }

    private fun screenWith(component: PlacedComponent) =
        ProducerMessage.Screen(screen = "home", content = ScreenContent(components = listOf(component)))

    private fun deepTriggerText(levels: Int): String = buildString {
        append(/* language=json */ """{"type":"Trigger","action":"a","arguments":[""")
        repeat(times = levels) { append('[') }
        repeat(times = levels) { append(']') }
        append("]}")
    }

    private fun deepScreenText(levels: Int): String = buildString {
        append(/* language=json */ """{"type":"Screen","screen":"home","content":[""")
        repeat(times = levels) { append(/* language=json */ """{"name":"Box","slots":{"content":[""") }
        repeat(times = levels) { append("]}}") }
        append("]}")
    }

    @Test
    fun `given the codec, when its version is read, then it is contract version 1`() {
        // Arrange
        val expected = ContractVersion(value = 1)

        // Act
        val actual = codec.contractVersion

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given an Introduce, when it is encoded as a first frame then decoded as one, then it is equal`() {
        // Arrange
        val message = introduce

        // Act
        val actual = codec.decodeFirstFrame(frame = codec.encodeFirstFrame(introduce = message))

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given an OpenScreen, when it is encoded then decoded, then it is equal`() {
        // Arrange
        val message = ConsumerMessage.OpenScreen(screen = "home")

        // Act
        val actual = consumerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given a DeviceChanged, when it is encoded then decoded, then it is equal`() {
        // Arrange
        val message = ConsumerMessage.DeviceChanged(device = device.copy(theme = Theme.Light))

        // Act
        val actual = consumerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given a Trigger with no arguments, when it is encoded then decoded, then it is equal`() {
        // Arrange
        val message = ConsumerMessage.Trigger(action = ActionId(value = "a1"), arguments = emptyList())

        // Act
        val actual = consumerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given a Trigger with one argument, when it is encoded then decoded, then it is equal`() {
        // Arrange
        val message = ConsumerMessage.Trigger(
            action = ActionId(value = "a2"),
            arguments = listOf(DataValue.Text(value = "hello")),
        )

        // Act
        val actual = consumerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given a Trigger with four arguments, when it is encoded then decoded, then it is equal`() {
        // Arrange
        val message = ConsumerMessage.Trigger(
            action = ActionId(value = "a3"),
            arguments = listOf(
                DataValue.WholeNumber(value = Long.MAX_VALUE),
                DataValue.DecimalNumber(value = 1.5),
                DataValue.Flag(value = true),
                DataValue.Choice(entry = "Primary"),
            ),
        )

        // Act
        val actual = consumerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given a Trigger with bytes, when it is encoded then decoded, then the bytes are equal`() {
        // Arrange
        val message = ConsumerMessage.Trigger(
            action = ActionId(value = "a4"),
            arguments = listOf(DataValue.Bytes(value = ByteArray(size = 300) { index -> index.toByte() })),
        )

        // Act
        val actual = consumerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given a Trigger with a list and nothing, when it is encoded then decoded, then it is equal`() {
        // Arrange
        val message = ConsumerMessage.Trigger(
            action = ActionId(value = "a5"),
            arguments = listOf(
                DataValue.ListValue(items = listOf(DataValue.Text(value = "x"), DataValue.None)),
                DataValue.None,
            ),
        )

        // Act
        val actual = consumerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given an accepted answer, when it is encoded then decoded, then it is equal`() {
        // Arrange
        val message = ProducerMessage.Answered(answer = Answer.Accepted(contractVersion = version))

        // Act
        val actual = producerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given a refused answer, when it is encoded then decoded, then it is equal`() {
        // Arrange
        val message = ProducerMessage.Answered(
            answer = Answer.Refused(kind = RefusalKind.CatalogVersionNotSupported, reason = "Please update the app"),
        )

        // Act
        val actual = producerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given a Screen with nested slots, when it is encoded then decoded, then it is equal`() {
        // Arrange
        val leaf = PlacedComponent(name = "Label", values = mapOf("text" to DataValue.Text(value = "Hi")))
        val button = PlacedComponent(
            name = "Button",
            values = mapOf("enabled" to DataValue.Flag(value = true)),
            callbacks = mapOf("onClick" to ActionId(value = "a1")),
            slots = mapOf("content" to listOf(leaf)),
        )
        val card = PlacedComponent(
            name = "Card",
            slots = mapOf("header" to listOf(leaf), "body" to listOf(button, leaf)),
        )
        val message = screenWith(component = card)

        // Act
        val actual = producerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given a ScreenUnavailable, when it is encoded then decoded, then it is equal`() {
        // Arrange
        val message = ProducerMessage.ScreenUnavailable(screen = "home", reason = "This screen is not available")

        // Act
        val actual = producerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given an accepted answer, when it is encoded, then the JSON shows Accepted with its field`() {
        // Arrange
        val message = ProducerMessage.Answered(answer = Answer.Accepted(contractVersion = version))

        // Act
        val actual = codec.encode(message = message).decodeToString()

        // Assert
        assertEquals(expected = /* language=json */ """{"type":"Accepted","contractVersion":1}""", actual = actual)
    }

    @Test
    fun `given a refused answer, when it is encoded, then the JSON shows Refused with its fields`() {
        // Arrange
        val message = ProducerMessage.Answered(
            answer = Answer.Refused(kind = RefusalKind.IntroductionNotValid, reason = "Not valid"),
        )

        // Act
        val actual = codec.encode(message = message).decodeToString()

        // Assert
        assertEquals(
            // language=json
            expected = """{"type":"Refused","kind":"IntroductionNotValid","reason":"Not valid"}""",
            actual = actual,
        )
    }

    @Test
    fun `given the first frame of an Introduce, when the contract version is read, then it is 1`() {
        // Arrange
        val frame = codec.encodeFirstFrame(introduce = introduce)

        // Act
        val actual = MessageFrame.readContractVersion(frame = frame)

        // Assert
        assertEquals(expected = version, actual = actual)
    }

    @Test
    fun `given a first frame whose payload is broken, when the contract version is read, then it still reads`() {
        // Arrange
        val frame = codec.encodeFirstFrame(introduce = introduce)
        val broken = frame.copyOf(newSize = MessageFrame.CONTRACT_VERSION_BYTES) + byteArrayOf(0, 1, 2)

        // Act
        val actual = MessageFrame.readContractVersion(frame = broken)

        // Assert
        assertEquals(expected = version, actual = actual)
    }

    @Test
    fun `given an Introduce, when it is encoded as an ordinary frame, then it has no prefix`() {
        // Arrange
        val message = introduce

        // Act
        val actual = codec.encode(message = message).decodeToString()

        // Assert
        assertTrue(actual = actual.startsWith(prefix = /* language=json */ """{"type":"Introduce""""))
    }

    @Test
    fun `given a later frame, when it is encoded, then it is the JSON payload alone`() {
        // Arrange
        val message = ConsumerMessage.OpenScreen(screen = "home")

        // Act
        val actual = codec.encode(message = message).decodeToString()

        // Assert
        assertEquals(expected = /* language=json */ """{"type":"OpenScreen","screen":"home"}""", actual = actual)
    }

    @Test
    fun `given a later frame that starts with whitespace, when it is decoded, then it is read as a message`() {
        // Arrange
        // language=json
        val frame = """  {"type":"OpenScreen","screen":"home"}""".encodeToByteArray()

        // Act
        val actual = codec.decodeConsumerMessage(frame = frame)

        // Assert
        assertEquals(expected = ConsumerMessage.OpenScreen(screen = "home"), actual = actual)
    }

    @Test
    fun `given a first frame shorter than the prefix, when the version is read, then only a decoding error`() {
        // Arrange
        val frame = byteArrayOf(0, 0, 1)

        // Act
        val actual = failureClassOf { MessageFrame.readContractVersion(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a first frame shorter than the prefix, when it is decoded, then only a decoding error is thrown`() {
        // Arrange
        val frame = byteArrayOf(0, 0, 1)

        // Act
        val actual = failureClassOf { codec.decodeFirstFrame(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a first frame with no prefix, when it is decoded, then only a decoding error is thrown`() {
        // Arrange
        val frame = codec.encode(message = introduce)

        // Act
        val actual = failureClassOf { codec.decodeFirstFrame(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a prefixed first frame that is not an Introduce, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val payload = codec.encode(message = ConsumerMessage.OpenScreen(screen = "home"))
        val frame = MessageFrame.withContractVersion(version = version, payload = payload)

        // Act
        val actual = failureClassOf { codec.decodeFirstFrame(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a prefix that differs from the Introduce version, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val otherVersion = ContractVersion(value = 2)
        val inner = introduce.copy(introduction = introduce.introduction.copy(contractVersion = otherVersion))
        val frame = codec.encodeFirstFrame(introduce = inner)

        // Act
        val actual = failureClassOf { codec.decodeFirstFrame(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a first frame for another contract version, when it is decoded, then only a decoding error is thrown`() {
        // Arrange
        val payload = codec.encode(message = introduce)
        val frame = MessageFrame.withContractVersion(version = ContractVersion(value = 2), payload = payload)

        // Act
        val actual = failureClassOf { codec.decodeFirstFrame(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given an Introduce with an invalid catalog version, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val validText = codec.encode(message = introduce).decodeToString()
        val invalidVersions = listOf("", "v".repeat(n = 65), "a\\u0001b")
        val frames = invalidVersions.map { invalid ->
            val payload = validText.replace(oldValue = "home-2", newValue = invalid).encodeToByteArray()
            MessageFrame.withContractVersion(version = version, payload = payload)
        }

        // Act
        val failures = frames.map { frame -> failureClassOf { codec.decodeFirstFrame(frame = frame) } }

        // Assert
        assertEquals(expected = invalidVersions.map { MessageDecodingException::class }, actual = failures)
    }

    @Test
    fun `given an Introduce sent as a later frame, when it is decoded, then only a decoding error is thrown`() {
        // Arrange
        val frame = codec.encode(message = introduce)

        // Act
        val actual = failureClassOf { codec.decodeConsumerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given an empty frame, when it is decoded as a consumer message, then only a decoding error is thrown`() {
        // Arrange
        val frame = ByteArray(size = 0)

        // Act
        val actual = failureClassOf { codec.decodeConsumerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given an empty frame, when it is decoded as a producer message, then only a decoding error is thrown`() {
        // Arrange
        val frame = ByteArray(size = 0)

        // Act
        val actual = failureClassOf { codec.decodeProducerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given garbage bytes, when they are decoded as a consumer message, then only a decoding error is thrown`() {
        // Arrange
        val frame = ByteArray(size = 64) { index -> (index * 7 + 200).toByte() }

        // Act
        val actual = failureClassOf { codec.decodeConsumerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given garbage bytes, when they are decoded as a producer message, then only a decoding error is thrown`() {
        // Arrange
        val frame = ByteArray(size = 64) { index -> (index * 7 + 200).toByte() }

        // Act
        val actual = failureClassOf { codec.decodeProducerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a JSON array, when it is decoded as a consumer message, then only a decoding error is thrown`() {
        // Arrange
        val frame = "[1,2,3]".encodeToByteArray()

        // Act
        val actual = failureClassOf { codec.decodeConsumerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given an empty JSON object, when it is decoded as a consumer message, then only a decoding error is thrown`() {
        // Arrange
        val frame = "{}".encodeToByteArray()

        // Act
        val actual = failureClassOf { codec.decodeConsumerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given JSON null, when it is decoded as a consumer message, then only a decoding error is thrown`() {
        // Arrange
        val frame = "null".encodeToByteArray()

        // Act
        val actual = failureClassOf { codec.decodeConsumerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given an unknown message type, when decoded as a producer message, then only a decoding error is thrown`() {
        // Arrange
        // language=json
        val frame = """{"type":"Nothing"}""".encodeToByteArray()

        // Act
        val actual = failureClassOf { codec.decodeProducerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a message missing a field, when it is decoded, then only a decoding error is thrown`() {
        // Arrange
        // language=json
        val frame = """{"type":"OpenScreen"}""".encodeToByteArray()

        // Act
        val actual = failureClassOf { codec.decodeConsumerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a broken bytes value, when it is decoded, then only a decoding error is thrown`() {
        // Arrange
        // language=json
        val frame = """{"type":"Trigger","action":"a","arguments":[{"type":"Bytes","value":"%%not base64%%"}]}"""
            .encodeToByteArray()

        // Act
        val actual = failureClassOf { codec.decodeConsumerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a producer frame, when it is decoded as a consumer message, then only a decoding error is thrown`() {
        // Arrange
        val frame = codec.encode(message = ProducerMessage.ScreenUnavailable(screen = "home", reason = "No"))

        // Act
        val actual = failureClassOf { codec.decodeConsumerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a consumer frame, when it is decoded as a producer message, then only a decoding error is thrown`() {
        // Arrange
        val frame = codec.encode(message = ConsumerMessage.OpenScreen(screen = "home"))

        // Act
        val actual = failureClassOf { codec.decodeProducerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given lists nested to the deepest allowed level, when they are encoded then decoded, then they are equal`() {
        // Arrange
        val levels = (MAX_NESTING_DEPTH - TRIGGER_LEVELS) / LEVELS_PER_LIST
        val message = triggerWith(argument = nestedLists(levels = levels))

        // Act
        val actual = consumerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given lists nested one level past the limit, when they are decoded, then only a decoding error is thrown`() {
        // Arrange
        val levels = (MAX_NESTING_DEPTH - TRIGGER_LEVELS) / LEVELS_PER_LIST + 1
        val frame = codec.encode(message = triggerWith(argument = nestedLists(levels = levels)))

        // Act
        val actual = failureClassOf { codec.decodeConsumerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given slots nested to the deepest allowed level, when they are encoded then decoded, then they are equal`() {
        // Arrange
        val levels = (MAX_NESTING_DEPTH - SCREEN_LEVELS) / LEVELS_PER_SLOT_STEP + 1
        val message = screenWith(component = nestedSlots(levels = levels))

        // Act
        val actual = producerRoundTrip(message = message)

        // Assert
        assertEquals(expected = message, actual = actual)
    }

    @Test
    fun `given slots nested one level past the limit, when they are decoded, then only a decoding error is thrown`() {
        // Arrange
        val levels = (MAX_NESTING_DEPTH - SCREEN_LEVELS) / LEVELS_PER_SLOT_STEP + 2
        val frame = codec.encode(message = screenWith(component = nestedSlots(levels = levels)))

        // Act
        val actual = failureClassOf { codec.decodeProducerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a frame with a very deep list, when it is decoded, then only a decoding error is thrown`() {
        // Arrange
        val frame = deepTriggerText(levels = VERY_DEEP).encodeToByteArray()

        // Act
        val actual = failureClassOf { codec.decodeConsumerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    @Test
    fun `given a frame with very deep slots, when it is decoded, then only a decoding error is thrown`() {
        // Arrange
        val frame = deepScreenText(levels = VERY_DEEP).encodeToByteArray()

        // Act
        val actual = failureClassOf { codec.decodeProducerMessage(frame = frame) }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    private companion object {
        const val TRIGGER_LEVELS = 2
        const val LEVELS_PER_LIST = 2
        const val SCREEN_LEVELS = 3
        const val LEVELS_PER_SLOT_STEP = 3
        const val VERY_DEEP = 200_000
    }
}
