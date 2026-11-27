package dev.tonholo.ssrcompose.producer.text.layoutlib

import kotlin.test.Test
import kotlin.test.assertEquals

private const val LATIN_FONT_CASE = "roboto-regular/latin-short/24sp/x1.3"
private const val TEXT_THE_FONT_LACKS = "漢字かな"

class LayoutlibTextEngineFallbackFontTest {
    private val engine = testEngine()
    private val row = DeviceRecording.tablet().row(LATIN_FONT_CASE)
    private val request = row.request(engine.loadFont(classpathBytes(row.fontFile)))

    @Test
    fun `given a script the font lacks, when it fits on one line, then that line is as tall as each of two`() {
        // Arrange
        val oneLine = request.copy(text = TEXT_THE_FONT_LACKS)
        val twoLines = request.copy(text = "$TEXT_THE_FONT_LACKS\n$TEXT_THE_FONT_LACKS")

        // Act
        val one = engine.measure(oneLine)
        val two = engine.measure(twoLines)

        // Assert
        assertEquals(expected = two.heightPx, actual = one.heightPx * two.lineCount)
        assertEquals(expected = two.firstBaselinePx, actual = one.firstBaselinePx)
    }
}
