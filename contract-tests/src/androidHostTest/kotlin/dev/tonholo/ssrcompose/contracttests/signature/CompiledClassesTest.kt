package dev.tonholo.ssrcompose.contracttests.signature

import dev.tonholo.ssrcompose.contracttests.fixture.AnonymousClassHolder
import dev.tonholo.ssrcompose.contracttests.fixture.ScreenDocument
import dev.tonholo.ssrcompose.contracttests.fixture.nested.NestedClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CompiledClassesTest {
    @Test
    fun `given a package with compiled classes, when read, then own and sub package classes are found`() {
        // Arrange
        val expected = setOf(ScreenDocument::class.java.name, NestedClass::class.java.name)

        // Act
        val found = CompiledClasses.inPackage(packageName = FIXTURE_PACKAGE).map { it.name }.toSet()

        // Assert
        assertEquals(expected = expected, actual = found.intersect(other = expected))
    }

    @Test
    fun `given a package with an anonymous class, when read, then the anonymous class is left out`() {
        // Arrange
        val anonymous = AnonymousClassHolder.anonymous.javaClass.name

        // Act
        val found = CompiledClasses.inPackage(packageName = FIXTURE_PACKAGE).map { it.name }

        // Assert
        assertEquals(expected = false, actual = anonymous in found)
    }

    @Test
    fun `given a package with no compiled classes, when read, then reading fails instead of returning nothing`() {
        // Arrange
        val packageWithoutClasses = "dev.tonholo.ssrcompose.contracttests.nothing"

        // Act
        val failure = runCatching { CompiledClasses.inPackage(packageName = packageWithoutClasses) }
            .exceptionOrNull()

        // Assert
        assertIs<IllegalStateException>(value = failure)
    }

    private companion object {
        const val FIXTURE_PACKAGE = "dev.tonholo.ssrcompose.contracttests.fixture"
    }
}
