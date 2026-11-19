package dev.tonholo.ssrcompose.catalog.fragment

import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CatalogFragmentTest {
    @Test
    fun `given same file name in two packages, when the fragment file names are asked, then they differ`() {
        // Arrange
        val home = CatalogFragment(source = "app/home/Banner.kt", components = emptyList())
        val settings = CatalogFragment(source = "app/settings/Banner.kt", components = emptyList())

        // Act
        val names = listOf(home.fileName, settings.fileName)

        // Assert
        assertNotEquals(illegal = names[0], actual = names[1])
        assertTrue(actual = names.none { name -> '/' in name }, message = names.toString())
    }

    @Test
    fun `given sources that differ only by where a dot is, when the file names are asked, then they differ`() {
        // Arrange
        val inPackage = CatalogFragment(source = "app/home/Card.kt", components = emptyList())
        val inFileName = CatalogFragment(source = "app/home.Card.kt", components = emptyList())

        // Act
        val names = listOf(inPackage.fileName, inFileName.fileName)

        // Assert
        assertNotEquals(illegal = names[0], actual = names[1])
    }

    @Test
    fun `given a source that is not a plain relative path, when a fragment is made, then it is refused`() {
        // Arrange
        val machinePaths = listOf(
            "/home/dev/app/Banner.kt",
            "C:\\dev\\app\\Banner.kt",
            "../app/Banner.kt",
            "./app/Banner.kt",
            "app/a..b/Banner.kt",
            "app/.hidden/Banner.kt",
            "app//Banner.kt",
            "",
        )

        // Act
        val accepted = machinePaths.filter { path ->
            runCatching { CatalogFragment(source = path, components = emptyList()) }.isSuccess
        }

        // Assert
        assertEquals(expected = emptyList(), actual = accepted)
    }

    @Test
    fun `given a source whose file name is exactly 255 bytes, when a fragment is made, then it is accepted`() {
        // Arrange
        val source = "a".repeat(n = MAX_FILE_NAME_BYTES - FILE_SUFFIX.length)

        // Act
        val fragment = CatalogFragment(source = source, components = emptyList())

        // Assert
        assertEquals(expected = MAX_FILE_NAME_BYTES, actual = fragment.fileName.encodeToByteArray().size)
    }

    @Test
    fun `given a source whose file name is 256 bytes, when a fragment is made, then it is refused`() {
        // Arrange
        val source = "a".repeat(n = MAX_FILE_NAME_BYTES - FILE_SUFFIX.length + 1)

        // Act
        val isAccepted = runCatching { CatalogFragment(source = source, components = emptyList()) }.isSuccess

        // Assert
        assertFalse(actual = isAccepted)
    }

    @Test
    fun `given a source of two byte letters that is short in characters, when a fragment is made, then bytes count`() {
        // Arrange
        val letters = (MAX_FILE_NAME_BYTES - FILE_SUFFIX.length) / 2 + 1
        val source = "é".repeat(n = letters)

        // Act
        val isAccepted = runCatching { CatalogFragment(source = source, components = emptyList()) }.isSuccess

        // Assert
        assertTrue(actual = source.length + FILE_SUFFIX.length < MAX_FILE_NAME_BYTES)
        assertFalse(actual = isAccepted)
    }

    @Test
    fun `given a choice parameter whose choice list is missing, when a fragment is made, then it is refused`() {
        // Arrange
        val parameter = Parameter.Value(name = "style", kind = ValueKind.Choice(listName = "Style"), required = true)
        val component = ComponentDescriptor(name = "Banner", parameters = listOf(parameter))

        // Act
        val create = {
            CatalogFragment(source = "app/Banner.kt", choices = emptyList(), components = listOf(component))
        }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a repeated choice list name, when a fragment is made, then it is refused`() {
        // Arrange
        val choices = listOf(
            ChoiceList(name = "Style", entries = listOf("a")),
            ChoiceList(name = "Style", entries = listOf("b")),
        )

        // Act
        val create = { CatalogFragment(source = "app/Banner.kt", choices = choices, components = emptyList()) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    private companion object {
        const val MAX_FILE_NAME_BYTES = 255
        const val FILE_SUFFIX = ".fragment.json"
    }
}
