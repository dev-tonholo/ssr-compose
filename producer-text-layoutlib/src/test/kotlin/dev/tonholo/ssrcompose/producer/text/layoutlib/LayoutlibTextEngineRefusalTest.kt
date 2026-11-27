package dev.tonholo.ssrcompose.producer.text.layoutlib

import kotlin.test.Test
import kotlin.test.assertFailsWith

private const val LINE_HEIGHT_CASE = "roboto-regular/latin-wrapping/12sp/x1.0/lh18sp"

class LayoutlibTextEngineRefusalTest {
    private val engine = testEngine()
    private val row = DeviceRecording.tablet().row(LINE_HEIGHT_CASE)
    private val request = row.request(engine.loadFont(classpathBytes(row.fontFile)))

    @Test
    fun `given a text with a line height, when measured, then it is refused rather than measured wrong`() {
        // Arrange
        val withLineHeight = request

        // Act
        val measure = { engine.measure(withLineHeight) }

        // Assert
        assertFailsWith<UnsupportedOperationException> { measure() }
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
