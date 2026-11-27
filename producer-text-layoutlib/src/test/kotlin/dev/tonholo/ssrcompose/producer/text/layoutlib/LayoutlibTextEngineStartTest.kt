package dev.tonholo.ssrcompose.producer.text.layoutlib

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val PAPARAZZI_CLASS = "app.cash.paparazzi.PaparazziSdk"

class LayoutlibTextEngineStartTest {
    @Test
    fun `given a device row, when the engine starts and measures it, then it matches with no Paparazzi there`() {
        // Arrange
        val row = DeviceRecording.tablet().row("roboto-regular/latin-short/12sp/x1.0")

        // Act
        val engine = testEngine()
        val font = engine.loadFont(classpathBytes(row.fontFile))
        val measurement = engine.measure(row.request(font))

        // Assert
        assertEquals(expected = row.measurement(), actual = measurement)
        val classpath = System.getProperty("java.class.path").split(File.pathSeparator).map { File(it).name }
        assertTrue(classpath.any { it.startsWith("layoutlib-") })
        assertEquals(expected = emptyList(), actual = classpath.filter { "paparazzi" in it.lowercase() })
        assertFailsWith<ClassNotFoundException> { Class.forName(PAPARAZZI_CLASS) }
    }
}
