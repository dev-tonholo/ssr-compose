package dev.tonholo.ssrcompose.producer.text.layoutlib

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

private const val LATIN_FONT_CASE = "roboto-regular/latin-short/24sp/x1.3"
private const val SMALL_KANA_SIXTH = "あいうえおぁかきくけこ"
private const val FIVE_CHARACTERS_WIDE_PX = 100
private const val ONE_CHARACTER_TALL_PX = 20f

class LayoutlibTextEngineLocaleTest {
    private val engine = testEngine()
    private val row = DeviceRecording.tablet().row(LATIN_FONT_CASE)
    private val request = row.request(engine.loadFont(classpathBytes(row.fontFile))).copy(
        text = SMALL_KANA_SIXTH,
        sizePx = ONE_CHARACTER_TALL_PX,
        widthPx = FIVE_CHARACTERS_WIDE_PX,
    )

    @Test
    fun `given a Japanese request on an English server, when measured, then a small kana may start a line`() {
        // Arrange
        val japanese = request.copy(locale = Locale.JAPAN)

        // Act
        val measured = withServerLocale(Locale.US) { engine.measure(japanese) }

        // Assert
        assertEquals(expected = listOf(5, 10, 11), actual = measured.lineEnds)
    }

    @Test
    fun `given an English request on a Japanese server, when measured, then a small kana stays off the line start`() {
        // Arrange
        val english = request.copy(locale = Locale.US)

        // Act
        val measured = withServerLocale(Locale.JAPAN) { engine.measure(english) }

        // Assert
        assertEquals(expected = listOf(4, 9, 11), actual = measured.lineEnds)
    }

    private fun <T> withServerLocale(locale: Locale, block: () -> T): T {
        val before = Locale.getDefault()
        Locale.setDefault(locale)
        try {
            return block()
        } finally {
            Locale.setDefault(before)
        }
    }
}
