package dev.tonholo.ssrcompose.catalog.testing.measure

import kotlin.test.Test
import kotlin.test.assertEquals

class ScaleMeasurementTest {
    @Test
    fun `given three actions, when measured in rotation, then each counted round starts with the next action`() {
        // Arrange
        val calls = mutableListOf<String>()
        val actions = listOf("a", "b", "c").associateWith { label -> { calls += label } }

        // Act
        ScaleMeasurement.measureInRotation(actions = actions, plan = MeasurementPlan(warmups = 1, runs = 3))

        // Assert
        val warmup = listOf("a", "b", "c")
        val rounds = listOf("a", "b", "c", "b", "c", "a", "c", "a", "b")
        assertEquals(expected = warmup + rounds, actual = calls)
    }

    @Test
    fun `given three actions, when measured in rotation, then there is one report per action in the given order`() {
        // Arrange
        val actions = listOf("a", "b", "c").associateWith { { } }
        val plan = MeasurementPlan(warmups = 0, runs = 2)

        // Act
        val reports = ScaleMeasurement.measureInRotation(actions = actions, plan = plan)

        // Assert
        assertEquals(expected = listOf("a", "b", "c"), actual = reports.map { it.label })
    }
}
