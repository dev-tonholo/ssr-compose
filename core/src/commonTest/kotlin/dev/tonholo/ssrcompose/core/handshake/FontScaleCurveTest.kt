package dev.tonholo.ssrcompose.core.handshake

import dev.tonholo.ssrcompose.core.message.ConsumerMessage
import dev.tonholo.ssrcompose.core.message.MessageDecodingException
import dev.tonholo.ssrcompose.core.message.internal.JsonMessageCodec
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FontScaleCurveTest {
    private val codec = JsonMessageCodec()

    private fun point(sp: Float, dp: Float): JsonObject = buildJsonObject {
        put(key = "sp", value = sp)
        put(key = "dp", value = dp)
    }

    private fun deviceChanged(points: List<JsonObject>): String = buildJsonObject {
        put(key = "type", value = "DeviceChanged")
        put(
            key = "device",
            element = buildJsonObject {
                put(key = "density", value = 2.5f)
                put(key = "screenWidthPx", value = 1080)
                put(key = "screenHeightPx", value = 2400)
                put(key = "theme", value = "Dark")
                put(key = "language", value = "en-US")
                put(key = "fontScale", value = 1.3f)
                put(key = "fontScaleCurve", element = JsonArray(content = points))
                put(key = "layoutDirection", value = "Ltr")
            },
        )
    }.toString()

    private fun failuresOf(curves: List<List<JsonObject>>): List<KClass<out Throwable>?> = curves.map { points ->
        runCatching { codec.decodeConsumerMessage(frame = deviceChanged(points = points).encodeToByteArray()) }
            .exceptionOrNull()?.let { failure -> failure::class }
    }

    private fun linearPoints(count: Int): List<JsonObject> =
        (1..count).map { index -> point(sp = index.toFloat(), dp = index.toFloat()) }

    @Test
    fun `given the emulator's curve at 130 percent, when DeviceChanged is decoded, then it holds every point`() {
        // Arrange
        val frame = deviceChanged(
            points = listOf(point(sp = 12f, dp = 15.6f), point(sp = 16f, dp = 20.2f), point(sp = 24f, dp = 26.4f)),
        )
        val expected = FontScaleCurve(points = EMULATOR_POINTS)

        // Act
        val message = codec.decodeConsumerMessage(frame = frame.encodeToByteArray()) as ConsumerMessage.DeviceChanged
        val actual = message.device.fontScaleCurve

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a curve with no point or more than 32, when decoded, then it is refused`() {
        // Arrange
        val curves = listOf(emptyList(), linearPoints(count = MAX_POINTS + 1))

        // Act
        val actual = failuresOf(curves = curves)

        // Assert
        assertEquals(expected = curves.map { MessageDecodingException::class }, actual = actual)
    }

    @Test
    fun `given sp sizes not strictly increasing, when decoded, then it is refused`() {
        // Arrange
        val curves = listOf(
            listOf(point(sp = 12f, dp = 12f), point(sp = 12f, dp = 13f)),
            listOf(point(sp = 14f, dp = 14f), point(sp = 12f, dp = 14f)),
        )

        // Act
        val actual = failuresOf(curves = curves)

        // Assert
        assertEquals(expected = curves.map { MessageDecodingException::class }, actual = actual)
    }

    @Test
    fun `given an sp size not above 0 or above 1,000, when decoded, then it is refused`() {
        // Arrange
        val curves = listOf(0f, -1f, MAX_SP + 1f).map { sp -> listOf(point(sp = sp, dp = 1f)) }

        // Act
        val actual = failuresOf(curves = curves)

        // Assert
        assertEquals(expected = curves.map { MessageDecodingException::class }, actual = actual)
    }

    @Test
    fun `given a dp size not above 0, decreasing or over 10 times its sp, when decoded, then it is refused`() {
        // Arrange
        val curves = listOf(
            listOf(point(sp = 12f, dp = 0f)),
            listOf(point(sp = 12f, dp = -1f)),
            listOf(point(sp = 12f, dp = 14f), point(sp = 14f, dp = 13f)),
            listOf(point(sp = 12f, dp = 12f * MAX_RATIO + 1f)),
        )

        // Act
        val actual = failuresOf(curves = curves)

        // Assert
        assertEquals(expected = curves.map { MessageDecodingException::class }, actual = actual)
    }

    @Test
    fun `given a curve at the edge of every limit, when decoded, then it is read`() {
        // Arrange
        val curves = listOf(
            linearPoints(count = 1),
            linearPoints(count = MAX_POINTS),
            listOf(point(sp = MAX_SP, dp = MAX_SP * MAX_RATIO)),
            listOf(point(sp = 12f, dp = 14f), point(sp = 14f, dp = 14f)),
        )

        // Act
        val actual = failuresOf(curves = curves)

        // Assert
        assertEquals(expected = curves.map { null }, actual = actual)
    }

    @Test
    fun `given a non-finite dp size, when a point is checked in a curve, then it is rejected`() {
        // Arrange
        val points = listOf(FontScalePoint(sp = 12f, dp = Float.POSITIVE_INFINITY))

        // Act
        val actual = runCatching { FontScaleCurve(points = points) }
            .exceptionOrNull()?.let { failure -> failure::class }

        // Assert
        assertEquals(expected = IllegalArgumentException::class, actual = actual)
    }

    private companion object {
        const val MAX_POINTS = 32
        const val MAX_SP = 1_000f
        const val MAX_RATIO = 10f
        val EMULATOR_POINTS = listOf(
            FontScalePoint(sp = 12f, dp = 15.6f),
            FontScalePoint(sp = 16f, dp = 20.2f),
            FontScalePoint(sp = 24f, dp = 26.4f),
        )
    }
}
