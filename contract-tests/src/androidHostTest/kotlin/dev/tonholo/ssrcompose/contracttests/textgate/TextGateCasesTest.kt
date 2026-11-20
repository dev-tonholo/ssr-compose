package dev.tonholo.ssrcompose.contracttests.textgate

import kotlin.test.Test
import kotlin.test.assertEquals

class TextGateCasesTest {
    @Test
    fun `given the cases, when grouped by font and sample, then each pair has every size and scale`() {
        // Arrange
        val expectedPairCount = SIZES_IN_SP.size * FONT_SCALES.size

        // Act
        val casesPerPair = TextGateCases.all.groupBy { it.font to it.sample }

        // Assert
        casesPerPair.forEach { (pair, cases) ->
            assertEquals(expected = expectedPairCount, actual = cases.size, message = "cases of $pair")
            assertEquals(
                expected = SIZES_IN_SP.flatMap { size -> FONT_SCALES.map { scale -> size to scale } }.toSet(),
                actual = cases.map { it.sizeInSp to it.fontScale }.toSet(),
                message = "sizes and scales of $pair",
            )
        }
    }

    @Test
    fun `given the text gate cases, when listed, then every font and every script is used`() {
        // Arrange
        val expectedFontCount = FONT_COUNT
        val expectedScripts = setOf(TextGateScript.Latin, TextGateScript.Greek)

        // Act
        val fonts = TextGateCases.all.map { it.font }.toSet()
        val scripts = TextGateCases.all.map { it.sample.script }.toSet()

        // Assert
        assertEquals(expected = expectedFontCount, actual = fonts.size)
        assertEquals(expected = expectedScripts, actual = scripts)
    }

    @Test
    fun `given the text gate cases, when ids are collected, then no id repeats`() {
        // Arrange
        val cases = TextGateCases.all

        // Act
        val ids = cases.map { it.id }

        // Assert
        assertEquals(expected = cases.size, actual = ids.toSet().size)
    }

    private companion object {
        val SIZES_IN_SP = listOf(12f, 16f, 24f)
        val FONT_SCALES = listOf(1f, 1.3f)
        const val FONT_COUNT = 2
    }
}
