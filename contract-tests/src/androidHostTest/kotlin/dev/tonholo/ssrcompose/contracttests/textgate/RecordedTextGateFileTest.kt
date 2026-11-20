package dev.tonholo.ssrcompose.contracttests.textgate

import kotlin.test.Test
import kotlin.test.assertEquals

class RecordedTextGateFileTest {
    @Test
    fun `given the recorded device file, when decoded, then it holds every case with its text`() {
        // Arrange
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(RECORDED_FILE))
        val text = stream.use { it.readBytes().decodeToString() }

        // Act
        val file = TextGateMeasurementFiles.decode(text)

        // Assert
        assertEquals(
            expected = TextGateCases.all.map { it.id to it.sample.text },
            actual = file.measurements.map { it.caseId to it.text },
        )
    }

    private companion object {
        const val RECORDED_FILE = "textgate/${TextGateMeasurementFiles.FILE_NAME}"
    }
}
