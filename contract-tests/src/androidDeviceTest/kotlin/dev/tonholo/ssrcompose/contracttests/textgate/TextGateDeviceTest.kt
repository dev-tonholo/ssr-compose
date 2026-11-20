package dev.tonholo.ssrcompose.contracttests.textgate

import android.os.Build
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class TextGateDeviceTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun given_the_text_gate_cases__when_drawn_on_the_device__then_every_case_is_written_to_the_file() {
        // Arrange
        val loader = FontFileLoader(copyDirectory = context.cacheDir, classLoader = checkNotNull(javaClass.classLoader))
        val fonts = TextGateFont.entries.associateWith(loader::load)
        val density = context.resources.displayMetrics.density
        val recorder = TextGateRecorder(composeRule = composeRule, fonts = fonts, density = density)
        val outputFile = File(checkNotNull(context.getExternalFilesDir(null)), TextGateMeasurementFiles.FILE_NAME)

        // Act
        val measurements = recorder.record(TextGateCases.all)
        val file = TextGateMeasurementFile(
            schemaVersion = TextGateMeasurementFiles.SCHEMA_VERSION,
            deviceModel = Build.MODEL,
            sdkInt = Build.VERSION.SDK_INT,
            measurements = measurements,
        )
        outputFile.writeText(TextGateMeasurementFiles.encode(file))

        // Assert
        val written = TextGateMeasurementFiles.decode(outputFile.readText())
        assertEquals(expected = TextGateCases.all.map { it.id }, actual = written.measurements.map { it.caseId })
        assertTrue(written.measurements.all { it.lineCount >= 1 && it.heightInPx > 0 })
    }
}
