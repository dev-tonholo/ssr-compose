package dev.tonholo.ssrcompose.contracttests.textgate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class TextGateMeasurementFileTest {
    @Test
    fun `given a file with measurements, when encoded and decoded, then it equals the original`() {
        // Arrange
        val original = fileWithMeasurements(count = MEASUREMENT_COUNT)

        // Act
        val decoded = TextGateMeasurementFiles.decode(TextGateMeasurementFiles.encode(original))

        // Assert
        assertEquals(expected = original, actual = decoded)
    }

    @Test
    fun `given a file of another schema version, when decoded, then it is refused`() {
        // Arrange
        val other = fileWithMeasurements(count = 1).copy(schemaVersion = OTHER_SCHEMA_VERSION)
        val text = TextGateMeasurementFiles.encode(other)

        // Act and Assert
        assertFailsWith<IllegalArgumentException> { TextGateMeasurementFiles.decode(text) }
    }

    @Test
    fun `given a file that lacks the line count, when decoded, then it is refused`() {
        // Arrange
        val text = withoutLineCount(TextGateMeasurementFiles.encode(fileWithMeasurements(count = 1)))

        // Act and Assert
        assertFailsWith<SerializationException> { TextGateMeasurementFiles.decode(text) }
    }

    private fun withoutLineCount(text: String): String {
        val file = Json.parseToJsonElement(string = text).jsonObject
        val measurements = file.getValue(key = MEASUREMENTS_KEY).jsonArray.map { measurement ->
            JsonObject(content = measurement.jsonObject - LINE_COUNT_KEY)
        }
        return JsonObject(content = file + (MEASUREMENTS_KEY to JsonArray(content = measurements))).toString()
    }

    private fun fileWithMeasurements(count: Int): TextGateMeasurementFile {
        val layout = DeviceTextLayout(lineCount = 2, widthInPx = 525, heightInPx = 105, fontSizeInPx = 31.5f)
        val measurements = TextGateCases.all.take(count).map { case ->
            TextGateMeasurement.of(case = case, fontSha256 = "00", density = 2.625f, layout = layout)
        }
        return TextGateMeasurementFile(
            schemaVersion = TextGateMeasurementFiles.SCHEMA_VERSION,
            deviceModel = "Test device",
            sdkInt = 36,
            measurements = measurements,
        )
    }

    private companion object {
        const val MEASUREMENT_COUNT = 3
        const val OTHER_SCHEMA_VERSION = 99
        const val LINE_COUNT_KEY = "lineCount"
        const val MEASUREMENTS_KEY = "measurements"
    }
}
