package dev.tonholo.ssrcompose.producer.text.layoutlib

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

private val PLAIN_CASES = listOf(
    "roboto-regular/latin-short/24sp/x1.3",
    "roboto-regular/latin-wrapping/16sp/x1.3",
    "roboto-regular/latin-long/12sp/x1.0",
    "roboto-regular/greek-short/24sp/x1.3",
    "roboto-regular/greek-long/16sp/x1.04",
    "open-sans-regular/latin-wrapping/24sp/x1.3",
    "open-sans-regular/latin-long/16sp/x1.0",
    "open-sans-regular/greek-short/24sp/x1.3",
)

private val TWO_LINE_ELLIPSIS_CASES = listOf(
    "roboto-regular/latin-short/12sp/x1.0/max2",
    "roboto-regular/latin-wrapping/24sp/x1.0/max2",
    "roboto-regular/latin-long/12sp/x1.0/max2",
    "roboto-regular/greek-long/16sp/x1.3/max2",
    "open-sans-regular/latin-long/24sp/x1.04/max2",
    "open-sans-regular/greek-short/24sp/x1.3/max2",
)

private const val WRAPPING_CASE = "roboto-regular/latin-wrapping/16sp/x1.3"

class LayoutlibTextEngineDeviceRowsTest {
    private val engine = testEngine()
    private val recording = DeviceRecording.tablet()

    @Test
    fun `given plain texts recorded on a tablet, when measured, then each gives the tablet's numbers`() {
        // Arrange
        val rows = PLAIN_CASES.map(recording::row)

        // Act
        val measured = measure(rows)

        // Assert
        assertEquals(expected = rows.associate { it.caseId to it.measurement() }, actual = measured)
    }

    @Test
    fun `given texts recorded with a two line ellipsis, when measured, then each gives the tablet's numbers`() {
        // Arrange
        val rows = TWO_LINE_ELLIPSIS_CASES.map(recording::row)

        // Act
        val measured = measure(rows)

        // Assert
        assertEquals(expected = rows.associate { it.caseId to it.measurement() }, actual = measured)
    }

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
            lineEnds = row.lineVisibleEnds.take(1),
            firstBaselinePx = firstBaseline,
            lastBaselinePx = firstBaseline,
        )
        assertEquals(expected = firstLineOnly, actual = measured)
    }

    private fun measure(rows: List<DeviceRow>): Map<String, TextMeasurement> {
        val fonts = engine.loadFonts(rows)
        return rows.associate { it.caseId to engine.measure(it.request(fonts.getValue(it.fontFile))) }
    }
}
