package dev.tonholo.ssrcompose.catalog.testing.measure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MeasurementSummaryTest {
    @Test
    fun `given an odd number of samples, when summarized, then the median is the middle sample`() {
        // Arrange
        val samples = listOf(9.0, 1.0, 5.0)

        // Act
        val summary = MeasurementSummary.of(samples = samples)

        // Assert
        assertEquals(expected = 5.0, actual = summary.median)
    }

    @Test
    fun `given an even number of samples, when summarized, then the median is the mean of the middle two`() {
        // Arrange
        val samples = listOf(8.0, 2.0, 4.0, 6.0)

        // Act
        val summary = MeasurementSummary.of(samples = samples)

        // Assert
        assertEquals(expected = 5.0, actual = summary.median)
    }

    @Test
    fun `given samples, when summarized, then the spread runs from the smallest to the largest`() {
        // Arrange
        val samples = listOf(7.0, 3.0, 11.0)

        // Act
        val summary = MeasurementSummary.of(samples = samples)

        // Assert
        assertEquals(expected = 3.0, actual = summary.min)
        assertEquals(expected = 11.0, actual = summary.max)
    }

    @Test
    fun `given no samples, when summarized, then it fails`() {
        // Arrange
        val samples = emptyList<Double>()

        // Act
        val failure = runCatching { MeasurementSummary.of(samples = samples) }.exceptionOrNull()

        // Assert
        assertIs<IllegalArgumentException>(value = failure)
    }
}
