package dev.tonholo.ssrcompose.producer.text.layoutlib

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

private const val WRAPPING_CASE = "roboto-regular/latin-wrapping/16sp/x1.3"

class LayoutlibTextEngineDeviceRowsTest {
    private val engine = testEngine()
    private val recording = DeviceRecording.tablet()

    @Test
    fun `given a text that wraps, when clipped at one line, then it gives the tablet's first line only`() {
        // Arrange
        val row = recording.row(WRAPPING_CASE)
        val font = engine.loadFont(classpathBytes(row.fontFile))

        // Act
        val measured = engine.measure(row.request(font).copy(maxLines = 1))

        // Assert
        val firstBaseline = row.firstBaselineInPx.roundToInt()
        val firstLineOnly = TextMeasurement(
            heightPx = row.lineBottomsInPx.first().roundToInt(),
            widthPx = row.textWidthInPx,
            lineEnds = row.lineVisibleEnds.take(1),
            lineRightsPx = row.lineRightsInPx.take(1),
            lineBottomsPx = listOf(row.lineBottomsInPx.first().roundToInt()),
            isLastLineEllipsized = false,
            firstBaselinePx = firstBaseline,
            lastBaselinePx = firstBaseline,
        )
        assertEquals(expected = firstLineOnly, actual = measured)
    }
}
