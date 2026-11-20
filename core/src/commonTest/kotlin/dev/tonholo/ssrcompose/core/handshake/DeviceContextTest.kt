package dev.tonholo.ssrcompose.core.handshake

import dev.tonholo.ssrcompose.core.message.ConsumerMessage
import dev.tonholo.ssrcompose.core.message.MessageDecodingException
import dev.tonholo.ssrcompose.core.message.internal.JsonMessageCodec
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceContextTest {
    private val codec = JsonMessageCodec()

    private fun deviceChanged(fontScale: String? = "1.0", direction: String? = "Rtl"): String {
        val added = listOfNotNull(
            fontScale?.let { scale -> /* language=json */ """"fontScale":$scale""" },
            direction?.let { name -> /* language=json */ """"layoutDirection":"$name"""" },
        )
        return /* language=json */ """{"type":"DeviceChanged","device":{"density":2.5,"screenWidthPx":1080,""" +
            """"screenHeightPx":2400,"theme":"Dark","language":"ar-EG",""" +
            added.joinToString(separator = ",") + "}}"
    }

    private fun failuresOf(texts: List<String>): List<KClass<out Throwable>?> = texts.map { text ->
        runCatching { codec.decodeConsumerMessage(frame = text.encodeToByteArray()) }
            .exceptionOrNull()?.let { failure -> failure::class }
    }

    @Test
    fun `given a right-to-left device at the largest font scale, when DeviceChanged is decoded, then it holds both`() {
        // Arrange
        val frame = deviceChanged(fontScale = MAX_FONT_SCALE.toString()).encodeToByteArray()

        // Act
        val actual = (codec.decodeConsumerMessage(frame = frame) as ConsumerMessage.DeviceChanged).device

        // Assert
        assertEquals(expected = MAX_FONT_SCALE, actual = actual.fontScale)
        assertEquals(expected = LayoutDirection.Rtl, actual = actual.layoutDirection)
    }

    @Test
    fun `given a font scale not above 0 or above 4, when DeviceChanged is decoded, then it is refused`() {
        // Arrange
        val texts = listOf("0.0", "-1.0", "4.5").map { scale -> deviceChanged(fontScale = scale) }

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
    fun `given a device without a font scale or a layout direction, when decoded, then it is refused`() {
        // Arrange
        val texts = listOf(
            deviceChanged(fontScale = null),
            deviceChanged(direction = null),
        )

        // Act
        val actual = failuresOf(texts = texts)

        // Assert
        assertEquals(expected = texts.map { MessageDecodingException::class }, actual = actual)
    }

    private companion object {
        const val MAX_FONT_SCALE = 4f
    }
}
