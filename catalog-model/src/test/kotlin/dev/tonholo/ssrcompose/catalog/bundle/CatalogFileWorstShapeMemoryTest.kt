package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.validation.CatalogLimits
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CatalogFileWorstShapeMemoryTest {
    @Test
    fun `given a file full of one-letter choice entries, when it is read, then it is refused without running out`() {
        // Arrange
        val entries = ONE_LETTER_ENTRY.repeat(n = ONE_LETTER_ENTRIES)
        val text = """{"format":1,"version":"1","choices":[{"name":"A","entries":[$entries"a"]}],"components":[]}"""

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    private companion object {
        const val ENVELOPE_LENGTH = 1_024
        const val ONE_LETTER_ENTRY = """"a","""
        const val ONE_LETTER_ENTRIES = (CatalogLimits.MAX_FILE_LENGTH - ENVELOPE_LENGTH) / ONE_LETTER_ENTRY.length
    }
}
