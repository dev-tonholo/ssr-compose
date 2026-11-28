package dev.tonholo.ssrcompose.producer.text.layoutlib

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ONE_LINE_CASE = "roboto-regular/latin-short/12sp/x1.0"
private const val SIZE_PX_AS_SP = 12f
private const val LINE_HEIGHT_SP = 30f
private const val LINE_HEIGHT_PX = 40f
private const val LINE_HEIGHT_PX_BY_FONT_SIZE = 45
private const val SHORTER_THAN_THE_FONT_PX = 10f
private const val LARGEST_LINEAR_FONT_SCALE = 1.05f
private const val SMALLEST_NON_LINEAR_FONT_SCALE = 1.06f

/**
 * The tablet's line is 17 px above the baseline and 4 below. A 10 px line height is 11 px short, and Compose takes
 * ceil(-11 * 17 / 21) = -8 px of that below the baseline, so the first line ends at 17 + 4 - 8.
 */
private const val FIRST_SHORTENED_LINE_BOTTOM_PX = 13

/**
 * Material 3 `headlineMedium`, 28 sp on 36 sp, on a device at font scale 2.0 and density 1.5. Its Compose draws the
 * font at 56 px, converts that back to 27.999996 sp and lays the lines out ceil(36 / 27.999996 * 56) = 73 px apart.
 */
private const val HEADLINE_LINES_APART_PX = 73
private const val HEADLINE_SIZE_PX = 56f
private const val HEADLINE_SIZE_PX_AS_SP = 27.999996f
private val HEADLINE_LINE_HEIGHT =
    TextLineHeight(sp = 36f, px = 64.97143f, sizePxAsSp = HEADLINE_SIZE_PX_AS_SP, fontScale = 2f)

/**
 * A 40 px line height adds 19 px to the tablet's line of 17 px above the baseline and 4 below, and Compose takes
 * ceil(19 * 4 / 21) = 4 px of that below the baseline. So a first line ends at 17 + 4 + 4.
 */
private const val FIRST_STRETCHED_LINE_BOTTOM_PX = 25

private data class Vertical(val heightPx: Int, val firstBaselinePx: Int, val lastBaselinePx: Int)

class LayoutlibTextEngineLineHeightTest {
    private val engine = testEngine()
    private val oneLine = DeviceRecording.tablet().row(ONE_LINE_CASE)
    private val request = oneLine.request(engine.loadFont(classpathBytes(oneLine.fontFile)))
    private val naturalBaselinePx = oneLine.firstBaselineInPx.roundToInt()

    @Test
    fun `given a text ending in a new line, when it has a line height, then the empty line adds one line height`() {
        // Arrange
        val endingInNewLine = request.copy(text = "ab\n", lineHeight = lineHeight(px = LINE_HEIGHT_PX))

        // Act
        val measured = engine.measure(endingInNewLine).vertical()

        // Assert
        assertEquals(expected = twoLinesApart(LINE_HEIGHT_PX.toInt()), actual = measured)
    }

    @Test
    fun `given a text ending in a new line, when it has a line height, then the empty line ends a line height lower`() {
        // Arrange
        val endingInNewLine = request.copy(text = "ab\n", lineHeight = lineHeight(px = LINE_HEIGHT_PX))

        // Act
        val measured = engine.measure(endingInNewLine).lineBottomsPx

        // Assert
        val emptyLineBottom = oneLine.heightInPx + LINE_HEIGHT_PX.toInt()
        assertEquals(expected = listOf(FIRST_STRETCHED_LINE_BOTTOM_PX, emptyLineBottom), actual = measured)
    }

    @Test
    fun `given an empty text, when it has a line height, then it is as tall as the font's own line`() {
        // Arrange
        val empty = request.copy(text = "", lineHeight = lineHeight(px = LINE_HEIGHT_PX))

        // Act
        val measured = engine.measure(empty).vertical()

        // Assert
        val oneNaturalLine = Vertical(
            heightPx = oneLine.heightInPx,
            firstBaselinePx = naturalBaselinePx,
            lastBaselinePx = naturalBaselinePx,
        )
        assertEquals(expected = oneNaturalLine, actual = measured)
    }

    @Test
    fun `given a line height shorter than the font's line, when it wraps, then baselines are that far apart`() {
        // Arrange
        val tight = request.copy(text = "ab\ncd", lineHeight = lineHeight(px = SHORTER_THAN_THE_FONT_PX))

        // Act
        val measured = engine.measure(tight).vertical()

        // Assert
        assertEquals(expected = twoLinesApart(SHORTER_THAN_THE_FONT_PX.toInt()), actual = measured)
    }

    @Test
    fun `given a line height shorter than the font's line, when it wraps, then each line ends where Compose ends it`() {
        // Arrange
        val tight = request.copy(text = "ab\ncd", lineHeight = lineHeight(px = SHORTER_THAN_THE_FONT_PX))

        // Act
        val measured = engine.measure(tight).lineBottomsPx

        // Assert
        val lastLineBottom = oneLine.heightInPx + SHORTER_THAN_THE_FONT_PX.toInt()
        assertEquals(expected = listOf(FIRST_SHORTENED_LINE_BOTTOM_PX, lastLineBottom), actual = measured)
    }

    @Test
    fun `given a font scale of 1,05, when measured, then the line height is the density's px`() {
        // Arrange
        val linear = request.copy(
            text = "ab\ncd",
            lineHeight = lineHeight(px = LINE_HEIGHT_PX, fontScale = LARGEST_LINEAR_FONT_SCALE),
        )

        // Act
        val measured = engine.measure(linear).vertical()

        // Assert
        assertEquals(expected = twoLinesApart(LINE_HEIGHT_PX.toInt()), actual = measured)
    }

    @Test
    fun `given a font scale above 1,05, when measured, then the line height keeps its ratio to the font size`() {
        // Arrange
        val nonLinear = request.copy(
            text = "ab\ncd",
            lineHeight = lineHeight(px = LINE_HEIGHT_PX, fontScale = SMALLEST_NON_LINEAR_FONT_SCALE),
        )

        // Act
        val measured = engine.measure(nonLinear).vertical()

        // Assert
        assertEquals(expected = twoLinesApart(LINE_HEIGHT_PX_BY_FONT_SIZE), actual = measured)
    }

    @Test
    fun `given a font size whose sp round trip is not exact, when measured, then lines are as far apart as in Compose`() {
        // Arrange
        val headline = request.copy(text = "ab\ncd", sizePx = HEADLINE_SIZE_PX, lineHeight = HEADLINE_LINE_HEIGHT)

        // Act
        val measured = engine.measure(headline)

        // Assert
        val apart = measured.lastBaselinePx - measured.firstBaselinePx
        assertEquals(expected = HEADLINE_LINES_APART_PX, actual = apart)
    }

    private fun lineHeight(px: Float, fontScale: Float = 1f) =
        TextLineHeight(sp = LINE_HEIGHT_SP, px = px, sizePxAsSp = SIZE_PX_AS_SP, fontScale = fontScale)

    private fun twoLinesApart(distancePx: Int) = Vertical(
        heightPx = oneLine.heightInPx + distancePx,
        firstBaselinePx = naturalBaselinePx,
        lastBaselinePx = naturalBaselinePx + distancePx,
    )

    private fun TextMeasurement.vertical() =
        Vertical(heightPx = heightPx, firstBaselinePx = firstBaselinePx, lastBaselinePx = lastBaselinePx)
}
