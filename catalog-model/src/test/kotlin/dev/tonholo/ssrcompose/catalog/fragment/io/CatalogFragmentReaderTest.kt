package dev.tonholo.ssrcompose.catalog.fragment.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogFragmentReaderTest {
    private val subject = "fragment app.Banner.kt.fragment.json"
    private val fragment = CatalogFragment(
        source = "app/Banner.kt",
        components = listOf(
            ComponentDescriptor(
                name = "Banner",
                parameters = listOf(
                    Parameter.Value(name = "title", kind = ValueKind.Text, required = true),
                    Parameter.Value(name = "subtitle", kind = ValueKind.Text, required = false),
                ),
            ),
        ),
    )
    private val canonical = CatalogFragmentWriter.write(fragment = fragment).decodeToString()

    @Test
    fun `given a fragment with text parameters, when written and read back, then the same fragment comes back`() {
        // Arrange
        val bytes = CatalogFragmentWriter.write(fragment = fragment)

        // Act
        val actual = CatalogFragmentReader.read(bytes = bytes)

        // Assert
        assertEquals(expected = fragment, actual = actual)
    }

    @Test
    fun `given a fragment of format 99, when it is read, then it is refused for its format and names the fragment`() {
        // Arrange
        val bytes = canonical.replace(oldValue = "\"format\": 1", newValue = "\"format\": 99").encodeToByteArray()

        // Act
        val read = { CatalogFragmentReader.read(bytes = bytes, subject = subject) }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(actual = message.contains(other = subject), message = message)
        assertTrue(actual = message.contains(other = "format 99"), message = message)
    }

    @Test
    fun `given a fragment one byte over the size cap, when it is read, then it is refused before it is parsed`() {
        // Arrange
        val bytes = canonical.encodeToByteArray()

        // Act
        val read = { CatalogFragmentReader.read(bytes = bytes, sizeCap = SizeCap(bytes = bytes.size - 1)) }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(actual = message.contains(other = "larger than ${bytes.size - 1} bytes"), message = message)
    }

    @Test
    fun `given a fragment of exactly the size cap, when it is read, then it is accepted`() {
        // Arrange
        val bytes = canonical.encodeToByteArray()

        // Act
        val actual = CatalogFragmentReader.read(bytes = bytes, sizeCap = SizeCap(bytes = bytes.size))

        // Assert
        assertEquals(expected = fragment, actual = actual)
    }

    @Test
    fun `given a fragment not in canonical form, when it is read, then each is refused`() {
        // Arrange
        val notCanonical = listOf(
            canonical.removeSuffix(suffix = "\n"),
            canonical.replace(oldValue = "\"format\": 1,", newValue = "\"format\":  1,"),
            canonical.replace(oldValue = "\"format\": 1,\n", newValue = "\"format\": 1\n"),
            "{\n  \"source\": \"app/Banner.kt\",\n  \"format\": 1,\n  \"components\": []\n}\n",
        )

        // Act
        val notRefused = notCanonical.filter { text ->
            runCatching { CatalogFragmentReader.read(bytes = text.encodeToByteArray()) }
                .exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a fragment with a repeated key, when it is read, then it is refused for the key and not the form`() {
        // Arrange
        val source = "\"source\": \"app/Banner.kt\","
        val text = canonical.replace(oldValue = source, newValue = "$source\n  \"source\": \"app/Other.kt\",")

        // Act
        val read = { CatalogFragmentReader.read(bytes = text.encodeToByteArray()) }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertFalse(actual = message.contains(other = "canonical"), message = message)
    }

    @Test
    fun `given a source that is a machine path, when the fragment is read, then it is refused and names it`() {
        // Arrange
        val text = canonical.replace(oldValue = "app/Banner.kt", newValue = "/home/dev/app/Banner.kt")

        // Act
        val read = { CatalogFragmentReader.read(bytes = text.encodeToByteArray(), subject = subject) }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(actual = message.contains(other = subject), message = message)
        assertFalse(actual = message.contains(other = "canonical"), message = message)
    }
}
