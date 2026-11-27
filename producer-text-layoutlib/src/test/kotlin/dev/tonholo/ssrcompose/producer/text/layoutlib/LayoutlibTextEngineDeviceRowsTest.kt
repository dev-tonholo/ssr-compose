package dev.tonholo.ssrcompose.producer.text.layoutlib

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

class LayoutlibTextEngineDeviceRowsTest {
    private val engine = testEngine()
    private val recording = DeviceRecording.tablet()

    @Test
    fun `given plain texts recorded on a tablet, when measured, then each gives the tablet's numbers`() {
        // Arrange
        val rows = PLAIN_CASES.map(recording::row)
        val fonts = engine.loadFonts(rows)

        // Act
        val measured = rows.associate { it.caseId to engine.measure(it.request(fonts.getValue(it.fontFile))) }

        // Assert
        assertEquals(expected = rows.associate { it.caseId to it.measurement() }, actual = measured)
    }
}
