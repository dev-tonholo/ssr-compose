package dev.tonholo.ssrcompose.catalog.fragment.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogFragmentWriterTest {
    private val source = "app/Pair.kt"
    private val alpha = ComponentDescriptor(name = "Alpha", parameters = emptyList())
    private val beta = ComponentDescriptor(name = "Beta", parameters = emptyList())

    @Test
    fun `given the same components in two orders, when written, then the bytes are equal`() {
        // Arrange
        val forward = CatalogFragment(source = source, components = listOf(alpha, beta))
        val backward = CatalogFragment(source = source, components = listOf(beta, alpha))

        // Act
        val forwardBytes = CatalogFragmentWriter.write(fragment = forward)
        val backwardBytes = CatalogFragmentWriter.write(fragment = backward)

        // Assert
        assertContentEquals(expected = forwardBytes, actual = backwardBytes)
    }

    @Test
    fun `given the same choices in two orders, when written, then the bytes are equal`() {
        // Arrange
        val sizeChoice = ChoiceList(name = "Size", entries = listOf("small", "large"))
        val styleChoice = ChoiceList(name = "Style", entries = listOf("bodyMedium"))
        val forward = CatalogFragment(
            source = source,
            choices = listOf(sizeChoice, styleChoice),
            components = listOf(alpha),
        )
        val backward = CatalogFragment(
            source = source,
            choices = listOf(styleChoice, sizeChoice),
            components = listOf(alpha),
        )

        // Act
        val forwardBytes = CatalogFragmentWriter.write(fragment = forward)
        val backwardBytes = CatalogFragmentWriter.write(fragment = backward)

        // Assert
        assertContentEquals(expected = forwardBytes, actual = backwardBytes)
    }

    @Test
    fun `given a fragment with no choices, when written, then the choices field is omitted`() {
        // Arrange
        val fragment = CatalogFragment(source = source, choices = emptyList(), components = listOf(alpha))

        // Act
        val text = CatalogFragmentWriter.write(fragment = fragment).decodeToString()

        // Assert
        assertFalse(actual = text.contains(other = "\"choices\""), message = text)
    }

    @Test
    fun `given a fragment of exactly the size cap, when written, then it is accepted`() {
        // Arrange
        val fragment = CatalogFragment(source = source, components = listOf(alpha, beta))
        val size = CatalogFragmentWriter.write(fragment = fragment).size

        // Act
        val bytes = CatalogFragmentWriter.write(fragment = fragment, sizeCap = SizeCap(bytes = size))

        // Assert
        assertEquals(expected = size, actual = bytes.size)
    }

    @Test
    fun `given a fragment one byte over the size cap, when written, then it is refused and names the fragment`() {
        // Arrange
        val fragment = CatalogFragment(source = source, components = listOf(alpha, beta))
        val size = CatalogFragmentWriter.write(fragment = fragment).size

        // Act
        val write = { CatalogFragmentWriter.write(fragment = fragment, sizeCap = SizeCap(bytes = size - 1)) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { write() }
        assertTrue(actual = error.message.orEmpty().contains(other = "fragment $source"), message = error.message)
        assertTrue(actual = error.message.orEmpty().contains(other = "larger than ${size - 1} bytes"))
    }
}
