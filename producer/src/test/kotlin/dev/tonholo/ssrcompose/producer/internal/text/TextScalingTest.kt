package dev.tonholo.ssrcompose.producer.internal.text

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.producer.testing.composeDensity
import dev.tonholo.ssrcompose.producer.testing.testDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TextScalingTest {
    @Test
    fun `given a textStyle, when fontSize is zero or negative, scaledOn returns null`() {
        // Arrange
        val deviceContext = testDevice()
        val scene = ScaledTextScene(curve = deviceContext.fontScaleCurve, fontScale = deviceContext.fontScale)
        val subjects = listOf(
            TextStyle(fontSize = 0.sp),
            TextStyle(fontSize = (-1).sp),
            TextStyle(fontSize = (-100).sp),
        )

        // Act
        val results = subjects.map {
            it.scaledOn(scene = scene, ambient = deviceContext.composeDensity)
        }
        // Assert
        results.forEach { assertNull(it) }
    }

    @Test
    fun `given a textStyle, when fontSize is unspecified, scaledOn returns TextScaling using default font size`() {
        // Arrange
        val deviceContext = testDevice()
        val scene = ScaledTextScene(curve = deviceContext.fontScaleCurve, fontScale = deviceContext.fontScale)
        val subject = TextStyle(fontSize = TextStyle.Default.fontSize)

        // Act
        val result = subject.scaledOn(scene = scene, ambient = deviceContext.composeDensity)

        // Assert
        assertNotNull(result)
        assertEquals(expected = DEFAULT_FONT_SIZE_SP, actual = result.mark.sizeSp)
    }

    @Test
    fun `given a textStyle, when fontSize is positive, scaledOn returns TextScaling with the same size`() {
        // Arrange
        val deviceContext = testDevice()
        val scene = ScaledTextScene(curve = deviceContext.fontScaleCurve, fontScale = deviceContext.fontScale)
        val subject = TextStyle(fontSize = 16.sp)

        // Act
        val result = subject.scaledOn(scene = scene, ambient = deviceContext.composeDensity)

        // Assert
        assertNotNull(result)
        assertEquals(expected = 16f, actual = result.mark.sizeSp)
    }

    @Test
    fun `given a textStyle, when fontSize is positive and lineHeight is specified, scaledOn returns TextScaling with the same size and lineHeight`() {
        // Arrange
        val deviceContext = testDevice()
        val scene = ScaledTextScene(curve = deviceContext.fontScaleCurve, fontScale = deviceContext.fontScale)
        val subject = TextStyle(fontSize = 16.sp, lineHeight = 20.sp)

        // Act
        val result = subject.scaledOn(scene = scene, ambient = deviceContext.composeDensity)

        // Assert
        assertNotNull(result)
        assertEquals(expected = 16f, actual = result.mark.sizeSp)
        assertEquals(expected = 20f, actual = result.mark.lineHeightSp)
    }
}
