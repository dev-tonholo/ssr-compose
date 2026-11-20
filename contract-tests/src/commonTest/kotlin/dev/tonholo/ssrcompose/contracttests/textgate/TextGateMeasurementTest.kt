package dev.tonholo.ssrcompose.contracttests.textgate

import kotlin.test.Test
import kotlin.test.assertEquals

class TextGateMeasurementTest {
    @Test
    fun `given 105 px at density 2_625, when recorded, then the height is 40 dp and the inputs are copied`() {
        // Arrange
        val case = TextGateCases.all.first()
        val layout = DeviceTextLayout(
            lineCount = LINE_COUNT,
            widthInPx = WIDTH_IN_PX,
            heightInPx = HEIGHT_IN_PX,
            fontSizeInPx = FONT_SIZE_IN_PX,
        )

        // Act
        val measurement = TextGateMeasurement.of(
            case = case,
            fontSha256 = FONT_SHA256,
            density = DENSITY,
            layout = layout,
        )

        // Assert
        assertEquals(expected = EXPECTED_HEIGHT_IN_DP, actual = measurement.heightInDp)
        assertEquals(expected = case.id, actual = measurement.caseId)
        assertEquals(expected = case.sample.text, actual = measurement.text)
        assertEquals(expected = case.font.resourcePath, actual = measurement.fontFile)
        assertEquals(expected = FONT_SHA256, actual = measurement.fontSha256)
        assertEquals(expected = EXPECTED_WIDTH_IN_DP, actual = measurement.widthInDp)
        assertEquals(expected = LINE_COUNT, actual = measurement.lineCount)
    }

    private companion object {
        const val LINE_COUNT = 3
        const val WIDTH_IN_PX = 525
        const val HEIGHT_IN_PX = 105
        const val FONT_SIZE_IN_PX = 31.5f
        const val DENSITY = 2.625f
        const val EXPECTED_HEIGHT_IN_DP = 40f
        const val EXPECTED_WIDTH_IN_DP = 200f
        const val FONT_SHA256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }
}
