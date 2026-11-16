package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.readPart
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CatalogPartWorstShapeMemoryTest {
    @Test
    fun `given a file full of one-letter choice entries, when it is read, then it is refused without running out`() {
        // Arrange
        val entries = ONE_LETTER_ENTRY.repeat(n = ONE_LETTER_ENTRIES)
        val text = """{"format":1,"part":"app","choices":[{"name":"A","entries":[$entries"a"]}],"components":[]}"""

        // Act
        val read = { readPart(text = text) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    private companion object {
        const val ENVELOPE_LENGTH = 1_024
        const val ONE_LETTER_ENTRY = """"a","""
        val ONE_LETTER_ENTRIES = (SizeCap.DEFAULT.bytes - ENVELOPE_LENGTH) / ONE_LETTER_ENTRY.length
    }
}
