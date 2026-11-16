package dev.tonholo.ssrcompose.catalog.bundle

import kotlin.test.Test
import kotlin.test.assertFailsWith

class SizeCapTest {
    @Test
    fun `given a negative number of bytes, when a size cap is created, then it is rejected`() {
        // Arrange
        val bytes = -1

        // Act
        val create = { SizeCap(bytes = bytes) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }
}
