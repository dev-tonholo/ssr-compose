package dev.tonholo.ssrcompose.contracttests.signature

import dev.tonholo.ssrcompose.contracttests.fixture.ExposingSignatures
import dev.tonholo.ssrcompose.contracttests.fixture.ScreenDocument
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkedTypesTest {
    @Test
    fun `given classes where one carries the marker, when marked types are read, then only that one is marked`() {
        // Arrange
        val classes = CompiledClasses.inPackage(packageName = FIXTURE_PACKAGE)
        val expected = mapOf(
            ScreenDocument::class.java.metadataName() to true,
            ExposingSignatures::class.java.metadataName() to false,
        )

        // Act
        val marked = MarkedTypes.readFrom(classes = classes)

        // Assert
        assertEquals(expected = expected, actual = expected.keys.associateWith(marked::marks))
    }

    @Test
    fun `given a marked type, when a nested type is asked, then it is marked but a similar name is not`() {
        // Arrange
        val marked = MarkedTypes(names = setOf(OUTER))
        val expected = mapOf(
            OUTER to true,
            "$OUTER.Inner" to true,
            "$OUTER.Inner.Deeper" to true,
            "${OUTER}Other" to false,
        )

        // Act
        val actual = expected.keys.associateWith(marked::marks)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    private companion object {
        const val FIXTURE_PACKAGE = "dev.tonholo.ssrcompose.contracttests.fixture"
        const val OUTER = "pkg/Outer"
    }
}
