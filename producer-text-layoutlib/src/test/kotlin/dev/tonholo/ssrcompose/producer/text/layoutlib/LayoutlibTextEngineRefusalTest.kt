package dev.tonholo.ssrcompose.producer.text.layoutlib

import kotlin.test.Test
import kotlin.test.assertFailsWith

private const val ANY_CASE = "roboto-regular/latin-wrapping/12sp/x1.0"

class LayoutlibTextEngineRefusalTest {
    private val engine = testEngine()
    private val row = DeviceRecording.tablet().row(ANY_CASE)
    private val request = row.request(engine.loadFont(classpathBytes(row.fontFile)))

    @Test
    fun `given a line height Android cannot lay out, when it is built, then it is refused`() {
        // Arrange
        val lineHeight = TextLineHeight(sp = 18f, px = 27f, sizePxAsSp = 12f, fontScale = 1f)

        // Act
        val builds = listOf<() -> TextLineHeight>(
            { lineHeight.copy(sp = Float.NaN) },
            { lineHeight.copy(px = -1f) },
            { lineHeight.copy(sizePxAsSp = 0f) },
            { lineHeight.copy(fontScale = Float.POSITIVE_INFINITY) },
        )

        // Assert
        builds.forEach { build -> assertFailsWith<IllegalArgumentException> { build() } }
    }

    @Test
    fun `given less than one line allowed, when a request is built, then it is refused`() {
        // Arrange
        val noLines = 0

        // Act
        val build = { request.copy(maxLines = noLines) }

        // Assert
        assertFailsWith<IllegalArgumentException> { build() }
    }

    @Test
    fun `given a font size Android would silently replace, when a request is built, then it is refused`() {
        // Arrange
        val sizes = listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY)

        // Act
        val builds = sizes.map { size -> { request.copy(sizePx = size) } }

        // Assert
        builds.forEach { build -> assertFailsWith<IllegalArgumentException> { build() } }
    }

    @Test
    fun `given bytes that are not a font, when loaded, then they are refused`() {
        // Arrange
        val notFonts = listOf(ByteArray(0), "not a font".encodeToByteArray())

        // Act
        val loads = notFonts.map { bytes -> { engine.loadFont(bytes) } }

        // Assert
        loads.forEach { load -> assertFailsWith<IllegalArgumentException> { load() } }
    }
}
