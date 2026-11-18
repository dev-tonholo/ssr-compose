package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.catalog.testing.measure.SCALE_TAG
import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import dev.tonholo.ssrcompose.gradle.testing.NamedOnlyProbeFixture
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Tag(SCALE_TAG)
class NamedOnlyIncrementalProbeTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `given a clean compile, when both screens are compiled, then both named bridges are created`() {
        // Arrange
        val fixture = NamedOnlyProbeFixture(directory = projectDir)

        // Act
        fixture.compile()

        // Assert
        assertTrue(actual = fixture.bridgeClassesDeclare(component = ALPHA))
        assertTrue(actual = fixture.bridgeClassesDeclare(component = BETA))
    }

    @Test
    fun `given an edit to one screen, when compiled incrementally, then the other screen keeps its bridge`() {
        // Arrange
        val fixture = NamedOnlyProbeFixture(directory = projectDir)
        fixture.compile()
        val betaScreenTime = fixture.betaScreenClassTime()
        val plainClassTime = fixture.plainClassTime()
        fixture.editAlphaScreen()

        // Act
        val output = fixture.compile().output

        // Assert
        assertFalse(actual = output.contains(FixtureSources.FULL_REBUILD_MESSAGE), message = output)
        assertEquals(expected = plainClassTime, actual = fixture.plainClassTime(), message = output)
        assertTrue(actual = fixture.betaScreenCallsBridge(component = BETA))
        assertTrue(actual = fixture.bridgeClassesDeclare(component = BETA))
        assertNotEquals(illegal = betaScreenTime, actual = fixture.betaScreenClassTime())
    }

    private companion object {
        const val ALPHA = "Alpha"
        const val BETA = "Beta"
    }
}
