package dev.tonholo.ssrcompose.core.message

import dev.tonholo.ssrcompose.core.document.Measurement
import dev.tonholo.ssrcompose.core.document.SlotSpace
import dev.tonholo.ssrcompose.core.document.Viewport
import dev.tonholo.ssrcompose.core.message.internal.JsonMessageCodec
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MeasurementMessagesTest {
    private val codec = JsonMessageCodec()

    private fun decode(text: String): ConsumerMessage = codec.decodeConsumerMessage(frame = text.encodeToByteArray())

    private fun failuresOf(messages: List<ConsumerMessage>): List<KClass<out Throwable>?> = messages.map { message ->
        runCatching { codec.decodeConsumerMessage(frame = codec.encode(message = message)) }
            .exceptionOrNull()?.let { failure -> failure::class }
    }

    private fun failuresOfTexts(texts: List<String>): List<KClass<out Throwable>?> = texts.map { text ->
        runCatching { decode(text = text) }.exceptionOrNull()?.let { failure -> failure::class }
    }

    private fun component(id: Int = 7, width: Float, height: Float, slots: JsonObject? = null): JsonObject =
        buildJsonObject {
            put(key = "id", value = id)
            put(key = "width", value = width)
            put(key = "height", value = height)
            slots?.let { put(key = "slots", element = it) }
        }

    private fun slotSpace(maxWidth: Float?, maxHeight: Float?): JsonObject = buildJsonObject {
        put(key = "maxWidth", value = maxWidth)
        put(key = "maxHeight", value = maxHeight)
    }

    private fun measurementFrame(components: List<JsonObject>): String = buildJsonObject {
        put(key = "type", value = "ComponentMeasured")
        put(key = "revision", value = 1)
        put(key = "components", element = JsonArray(content = components))
    }.toString()

    private fun measured(
        width: Float = DEFAULT_SIZE,
        height: Float = DEFAULT_SIZE,
        slotMaxWidth: Float? = null,
        slotMaxHeight: Float? = null,
    ): String {
        val slots = buildJsonObject {
            put(key = "content", element = slotSpace(maxWidth = slotMaxWidth, maxHeight = slotMaxHeight))
        }
        return measurementFrame(components = listOf(component(width = width, height = height, slots = slots)))
    }

    private fun viewportChanged(width: Float, height: Float): String = buildJsonObject {
        put(key = "type", value = "ViewportChanged")
        put(
            key = "viewport",
            element = buildJsonObject {
                put(key = "width", value = width)
                put(key = "height", value = height)
            },
        )
    }.toString()

    private fun measuredWithSlots(names: List<String>): String {
        val slots = buildJsonObject {
            names.forEach { name -> put(key = name, element = slotSpace(maxWidth = null, maxHeight = null)) }
        }
        return measurementFrame(components = listOf(component(width = 1f, height = 1f, slots = slots)))
    }

    private fun slotNames(count: Int, length: Int): List<String> =
        (1..count).map { index -> index.toString().padStart(length = length, padChar = 's') }

    private fun measurementsOf(count: Int) = ConsumerMessage.ComponentMeasured(
        revision = 1,
        components = (1..count).map { id -> Measurement(id = id, width = 1f, height = 1f) },
    )

    @Test
    fun `given R20 example 2's ComponentMeasured, when it is decoded, then it holds the Card's size`() {
        // Arrange
        val frame = EXAMPLE_2_MEASURED
        val expected = ConsumerMessage.ComponentMeasured(
            revision = 1,
            components = listOf(Measurement(id = 7, width = 379.4f, height = 120f)),
        )

        // Act
        val actual = decode(text = frame)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given R20 example 3's ComponentMeasured, when it is decoded, then it holds both sizes and the slot space`() {
        // Arrange
        val frame = EXAMPLE_3_MEASURED
        val expected = ConsumerMessage.ComponentMeasured(
            revision = 4,
            components = listOf(
                Measurement(
                    id = 9,
                    width = 379.4f,
                    height = 160f,
                    slots = mapOf("content" to SlotSpace(maxWidth = 347.4f, maxHeight = null)),
                ),
                Measurement(id = 12, width = 120f, height = 40f),
            ),
        )

        // Act
        val actual = decode(text = frame)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given the new consumer messages, when each is encoded then decoded, then it is equal`() {
        // Arrange
        val messages = listOf(
            decode(text = EXAMPLE_3_MEASURED),
            ConsumerMessage.ViewportChanged(viewport = Viewport(width = 411.4f, height = 843.4f)),
            ConsumerMessage.Switched(switcher = 2, variant = 1),
        )

        // Act
        val actual = messages.map { message -> codec.decodeConsumerMessage(frame = codec.encode(message = message)) }

        // Assert
        assertEquals(expected = messages, actual = actual)
    }

    @Test
    fun `given a Switched frame, when it is decoded, then it names the switcher and the variant`() {
        // Arrange
        // language=json
        val frame = """{"type":"Switched","switcher":2,"variant":1}"""

        // Act
        val actual = decode(text = frame)

        // Assert
        assertEquals(expected = ConsumerMessage.Switched(switcher = 2, variant = 1), actual = actual)
    }

    @Test
    fun `given a ViewportChanged frame, when it is decoded, then it holds the viewport`() {
        // Arrange
        val frame = viewportChanged(width = 411.4f, height = 843.4f)

        // Act
        val actual = decode(text = frame)

        // Assert
        assertEquals(
            expected = ConsumerMessage.ViewportChanged(viewport = Viewport(width = 411.4f, height = 843.4f)),
            actual = actual,
        )
    }

    @Test
    fun `given a measured size or slot space negative or above 100,000 dp, when decoded, then it is refused`() {
        // Arrange
        val texts = listOf(
            measured(width = NEGATIVE),
            measured(height = NEGATIVE),
            measured(slotMaxWidth = NEGATIVE),
            measured(slotMaxHeight = NEGATIVE),
            measured(width = ABOVE_MAX),
            measured(height = ABOVE_MAX),
            measured(slotMaxWidth = ABOVE_MAX),
        )

        // Act
        val actual = failuresOfTexts(texts = texts)

        // Assert
        assertEquals(expected = texts.map { MessageDecodingException::class }, actual = actual)
    }

    @Test
    fun `given a measured size of 0 or 100,000 dp, when decoded, then it is read`() {
        // Arrange
        val texts = listOf(
            measured(width = 0f, height = MAX, slotMaxWidth = MAX, slotMaxHeight = MAX),
            measured(width = MAX, height = 0f, slotMaxWidth = 0f, slotMaxHeight = 0f),
        )

        // Act
        val actual = failuresOfTexts(texts = texts)

        // Assert
        assertEquals(expected = texts.map { null }, actual = actual)
    }

    @Test
    fun `given a non-finite measured size, when a measurement is built, then it is rejected`() {
        // Arrange
        val infinite = Float.POSITIVE_INFINITY

        // Act
        val actual = runCatching { Measurement(id = 1, width = infinite, height = 1f) }
            .exceptionOrNull()?.let { it::class }

        // Assert
        assertEquals(expected = IllegalArgumentException::class, actual = actual)
    }

    @Test
    fun `given two measurements for one component, when decoded, then it is refused`() {
        // Arrange
        val text = measurementFrame(
            components = listOf(component(width = 1f, height = 1f), component(width = 2f, height = 2f)),
        )

        // Act
        val actual = failuresOfTexts(texts = listOf(text))

        // Assert
        assertEquals(expected = listOf(MessageDecodingException::class), actual = actual)
    }

    @Test
    fun `given 10,000 measurements in one message, when decoded, then they are read`() {
        // Arrange
        val messages = listOf(measurementsOf(count = MAX_MEASUREMENTS))

        // Act
        val actual = failuresOf(messages = messages)

        // Assert
        assertEquals(expected = listOf(null), actual = actual)
    }

    @Test
    fun `given 10,001 measurements in one message, when decoded, then it is refused`() {
        // Arrange
        val components = (1..MAX_MEASUREMENTS + 1).map { id -> component(id = id, width = 1f, height = 1f) }
        val text = measurementFrame(components = components)

        // Act
        val actual = failuresOfTexts(texts = listOf(text))

        // Assert
        assertEquals(expected = listOf(MessageDecodingException::class), actual = actual)
    }

    @Test
    fun `given a measurement with more than 118 slots, when decoded, then it is refused`() {
        // Arrange
        val texts = listOf(measuredWithSlots(names = slotNames(count = MAX_SLOTS + 1, length = 1)))

        // Act
        val actual = failuresOfTexts(texts = texts)

        // Assert
        assertEquals(expected = listOf(MessageDecodingException::class), actual = actual)
    }

    @Test
    fun `given a slot name longer than 64 characters, when decoded, then it is refused`() {
        // Arrange
        val texts = listOf(measuredWithSlots(names = slotNames(count = 1, length = MAX_SLOT_NAME_LENGTH + 1)))

        // Act
        val actual = failuresOfTexts(texts = texts)

        // Assert
        assertEquals(expected = listOf(MessageDecodingException::class), actual = actual)
    }

    @Test
    fun `given 118 slots with names of 64 characters, when decoded, then they are read`() {
        // Arrange
        val texts = listOf(measuredWithSlots(names = slotNames(count = MAX_SLOTS, length = MAX_SLOT_NAME_LENGTH)))

        // Act
        val actual = failuresOfTexts(texts = texts)

        // Assert
        assertEquals(expected = listOf(null), actual = actual)
    }

    @Test
    fun `given a viewport side not above 0 or above 100,000 dp, when decoded, then it is refused`() {
        // Arrange
        val texts = listOf(
            viewportChanged(width = 0f, height = 1f),
            viewportChanged(width = 1f, height = 0f),
            viewportChanged(width = ABOVE_MAX, height = 1f),
            viewportChanged(width = 1f, height = ABOVE_MAX),
        )

        // Act
        val actual = failuresOfTexts(texts = texts)

        // Assert
        assertEquals(expected = texts.map { MessageDecodingException::class }, actual = actual)
    }

    @Test
    fun `given a viewport of 100,000 dp, when decoded, then it is read`() {
        // Arrange
        val texts = listOf(viewportChanged(width = MAX, height = MAX))

        // Act
        val actual = failuresOfTexts(texts = texts)

        // Assert
        assertEquals(expected = listOf(null), actual = actual)
    }

    private companion object {
        const val DEFAULT_SIZE = 10f
        const val NEGATIVE = -0.5f
        const val MAX = 100_000f
        const val ABOVE_MAX = 100_000.5f
        const val MAX_MEASUREMENTS = 10_000
        const val MAX_SLOTS = 118
        const val MAX_SLOT_NAME_LENGTH = 64

        // language=json
        const val EXAMPLE_2_MEASURED =
            """{"type":"ComponentMeasured","revision":1,"components":[{"id":7,"width":379.4,"height":120.0,"slots":{}}]}"""

        // language=json
        const val EXAMPLE_3_MEASURED = """{"type":"ComponentMeasured","revision":4,"components":[{"id":9,"width":379.4,"height":160.0,"slots":{"content":{"maxWidth":347.4,"maxHeight":null}}},{"id":12,"width":120.0,"height":40.0,"slots":{}}]}"""
    }
}
