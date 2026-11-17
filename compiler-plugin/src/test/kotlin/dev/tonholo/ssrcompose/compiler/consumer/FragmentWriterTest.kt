package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir

class FragmentWriterTest {
    @TempDir
    lateinit var directory: File

    private val source = "app/Shared.kt"
    private val withComponent = CatalogFragment(
        source = source,
        components = listOf(ComponentDescriptor(name = "Shared", parameters = emptyList())),
    )
    private val withoutComponent = CatalogFragment(source = source, components = emptyList())

    @Test
    fun `given two files with the same source, when the empty one is saved last, then the fragment stays`() {
        // Arrange
        val writer = FragmentWriter(directory = directory)

        // Act
        writer.save(fragment = withComponent)
        writer.save(fragment = withoutComponent)

        // Assert
        assertEquals(expected = listOf("app..Shared.kt.fragment.json"), actual = directory.list().orEmpty().toList())
    }

    @Test
    fun `given a fragment from an earlier compilation, when the same source is saved empty, then it is removed`() {
        // Arrange
        FragmentWriter(directory = directory).save(fragment = withComponent)

        // Act
        FragmentWriter(directory = directory).save(fragment = withoutComponent)

        // Assert
        assertEquals(expected = emptyList(), actual = directory.list().orEmpty().toList())
    }
}
