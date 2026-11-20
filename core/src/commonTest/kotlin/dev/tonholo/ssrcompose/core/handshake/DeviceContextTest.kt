package dev.tonholo.ssrcompose.core.handshake

import dev.tonholo.ssrcompose.core.message.ConsumerMessage
import dev.tonholo.ssrcompose.core.message.MessageDecodingException
import dev.tonholo.ssrcompose.core.message.internal.JsonMessageCodec
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DeviceContextTest {
    private val codec = JsonMessageCodec()

    private fun deviceChanged(
        fontScale: Float? = 1f,
        direction: String? = "Rtl",
        curve: JsonArray? = ONE_POINT_CURVE,
    ): String = buildJsonObject {
        put(key = "type", value = "DeviceChanged")
        put(
            key = "device",
            element = buildJsonObject {
                put(key = "density", value = 2.5f)
                put(key = "screenWidthPx", value = 1080)
                put(key = "screenHeightPx", value = 2400)
                put(key = "theme", value = "Dark")
                put(key = "language", value = "ar-EG")
                fontScale?.let { scale -> put(key = "fontScale", value = scale) }
                curve?.let { points -> put(key = "fontScaleCurve", element = points) }
                direction?.let { name -> put(key = "layoutDirection", value = name) }
            },
        )
    }.toString()

    private fun failuresOf(texts: List<String>): List<KClass<out Throwable>?> = texts.map { text ->
        runCatching { codec.decodeConsumerMessage(frame = text.encodeToByteArray()) }
            .exceptionOrNull()?.let { failure -> failure::class }
    }

    @Test
    fun `given a right-to-left device at the largest font scale, when DeviceChanged is decoded, then it holds both`() {
        // Arrange
        val frame = deviceChanged(fontScale = MAX_FONT_SCALE).encodeToByteArray()

        // Act
        val actual = (codec.decodeConsumerMessage(frame = frame) as ConsumerMessage.DeviceChanged).device

        // Assert
        assertEquals(expected = MAX_FONT_SCALE, actual = actual.fontScale)
        assertEquals(expected = LayoutDirection.Rtl, actual = actual.layoutDirection)
    }

    @Test
    fun `given a font scale not above 0 or above 4, when DeviceChanged is decoded, then it is refused`() {
        // Arrange
        val texts = listOf(0f, -1f, MAX_FONT_SCALE + 0.5f).map { scale -> deviceChanged(fontScale = scale) }

        // Act
        val actual = failuresOf(texts = texts)

        // Assert
        assertEquals(expected = texts.map { MessageDecodingException::class }, actual = actual)
    }

    @Test
    fun `given a font scale that is not a number, when a DeviceContext is built, then it is rejected`() {
        // Arrange
        val scale = Float.NaN

        // Act
        val actual = runCatching { TestDevices.portrait.copy(fontScale = scale) }.exceptionOrNull()?.let { it::class }

        // Assert
        assertEquals(expected = IllegalArgumentException::class, actual = actual)
    }

    @Test
    fun `given a layout direction other than Ltr or Rtl, when DeviceChanged is decoded, then it is refused`() {
        // Arrange
        val texts = listOf("RTL", "Up").map { direction -> deviceChanged(direction = direction) }

        // Act
        val actual = failuresOf(texts = texts)

        // Assert
        assertEquals(expected = texts.map { MessageDecodingException::class }, actual = actual)
    }

    @Test
    fun `given a device without a font scale, its curve or a layout direction, when decoded, then it is refused`() {
        // Arrange
        val texts = listOf(
            deviceChanged(fontScale = null),
            deviceChanged(direction = null),
            deviceChanged(curve = null),
        )

        // Act
        val actual = failuresOf(texts = texts)

        // Assert
        assertEquals(expected = texts.map { MessageDecodingException::class }, actual = actual)
    }

    private companion object {
        const val MAX_FONT_SCALE = 4f

        val ONE_POINT_CURVE = buildJsonArray {
            add(
                element = buildJsonObject {
                    put(key = "sp", value = 12f)
                    put(key = "dp", value = 12f)
                },
            )
        }
    }
}
