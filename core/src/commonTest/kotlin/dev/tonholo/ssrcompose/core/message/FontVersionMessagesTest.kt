package dev.tonholo.ssrcompose.core.message

import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import dev.tonholo.ssrcompose.core.handshake.AppIdentity
import dev.tonholo.ssrcompose.core.handshake.ContractVersion
import dev.tonholo.ssrcompose.core.handshake.Introduction
import dev.tonholo.ssrcompose.core.handshake.TestDevices
import dev.tonholo.ssrcompose.core.message.internal.DocumentScreenCodec
import dev.tonholo.ssrcompose.core.message.internal.JsonMessageCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FontVersionMessagesTest {
    private val codec = JsonMessageCodec()
    private val fontVersion = FontBundleVersion(value = "1")

    private fun introduce(fontVersion: FontBundleVersion?) = ConsumerMessage.Introduce(
        introduction = Introduction(
            contractVersion = ContractVersion(value = 1),
            catalogVersion = CatalogVersion(value = "home-2"),
            fontVersion = fontVersion,
            app = AppIdentity(version = "1.2.3", variant = "release"),
            device = TestDevices.portrait,
        ),
    )

    private fun screenText(fontVersion: String?): String = buildJsonObject {
        put(key = "type", value = "Screen")
        put(key = "screen", value = "home")
        put(key = "revision", value = 3)
        put(key = "pending", value = false)
        fontVersion?.let { version -> put(key = "fontVersion", value = version) }
        put(key = "content", element = buildJsonObject { put(key = "root", element = viewportBox()) })
    }.toString()

    private fun viewportBox(): JsonObject = buildJsonObject {
        put(key = "type", value = "Box")
        put(key = "id", value = 1)
        put(key = "x", value = 0f)
        put(key = "y", value = 0f)
        put(key = "width", value = 411.4f)
        put(key = "height", value = 843.4f)
    }

    @Test
    fun `given an Introduce with a font version, when it is sent as the first frame, then the version arrives`() {
        // Arrange
        val message = introduce(fontVersion = fontVersion)

        // Act
        val json = codec.encode(message = message).decodeToString()
        val actual = codec.decodeFirstFrame(frame = codec.encodeFirstFrame(introduce = message))

        // Assert
        assertEquals(expected = true, actual = json.contains(other = """"$FONT_VERSION_FIELD":"1""""))
        assertEquals(expected = fontVersion, actual = actual.introduction.fontVersion)
    }

    @Test
    fun `given an Introduce from an app with no font, when it is encoded, then the JSON has no fontVersion`() {
        // Arrange
        val message = introduce(fontVersion = null)

        // Act
        val actual = codec.encode(message = message).decodeToString()
        val decoded = codec.decodeFirstFrame(frame = codec.encodeFirstFrame(introduce = message))

        // Assert
        assertEquals(expected = false, actual = actual.contains(other = FONT_VERSION_FIELD))
        assertNull(actual = decoded.introduction.fontVersion)
    }

    @Test
    fun `given a FontVersionChanged frame, when it is decoded, then it names the new version`() {
        // Arrange
        // language=json
        val frame = """{"type":"FontVersionChanged","fontVersion":"2"}"""

        // Act
        val actual = codec.decodeConsumerMessage(frame = frame.encodeToByteArray())

        // Assert
        assertEquals(
            expected = ConsumerMessage.FontVersionChanged(fontVersion = FontBundleVersion(value = "2")),
            actual = actual,
        )
    }

    @Test
    fun `given a FontVersionChanged frame with no version, when it is decoded, then the app now has no font`() {
        // Arrange
        // language=json
        val frame = """{"type":"FontVersionChanged"}"""

        // Act
        val actual = codec.decodeConsumerMessage(frame = frame.encodeToByteArray())

        // Assert
        assertEquals(expected = ConsumerMessage.FontVersionChanged(fontVersion = null), actual = actual)
    }

    @Test
    fun `given a Screen laid out with a font version, when it is decoded, then it names that version`() {
        // Arrange
        val frame = screenText(fontVersion = "1")

        // Act
        val actual = DocumentScreenCodec.decode(frame = frame.encodeToByteArray())

        // Assert
        assertEquals(expected = fontVersion, actual = actual.fontVersion)
    }

    @Test
    fun `given a Screen laid out with no app font, when it is decoded, then it names no version`() {
        // Arrange
        val frame = screenText(fontVersion = null)

        // Act
        val actual = DocumentScreenCodec.decode(frame = frame.encodeToByteArray())

        // Assert
        assertNull(actual = actual.fontVersion)
    }

    private companion object {
        const val FONT_VERSION_FIELD = "fontVersion"
    }
}
