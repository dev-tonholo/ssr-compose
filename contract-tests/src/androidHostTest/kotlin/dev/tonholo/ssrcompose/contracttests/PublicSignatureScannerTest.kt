package dev.tonholo.ssrcompose.contracttests

import dev.tonholo.ssrcompose.contracttests.fixture.ScreenDocument
import dev.tonholo.ssrcompose.contracttests.signature.CompiledClasses
import dev.tonholo.ssrcompose.contracttests.signature.MarkedTypes
import dev.tonholo.ssrcompose.contracttests.signature.PublicSignatureScanner
import dev.tonholo.ssrcompose.contracttests.signature.metadataName
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicSignatureScannerTest {
    @Test
    fun `given a marked parameter over several lines, when scanned, then the function is reported`() {
        // Arrange
        val expected = declaration(owner = EXPOSING_SIGNATURES, name = "multiLineParameter")

        // Act
        val reported = reportedFixtureDeclarations()

        // Assert
        assertEquals(expected = 1, actual = reported.count { it == expected }, message = reported.toString())
    }

    @Test
    fun `given a list of the marked type as parameter, when scanned, then the function is reported`() {
        // Arrange
        val expected = declaration(owner = EXPOSING_SIGNATURES, name = "listParameter")

        // Act
        val reported = reportedFixtureDeclarations()

        // Assert
        assertEquals(expected = 1, actual = reported.count { it == expected }, message = reported.toString())
    }

    @Test
    fun `given a map to the marked type as property type, when scanned, then the property is reported`() {
        // Arrange
        val expected = declaration(owner = EXPOSING_SIGNATURES, name = "documentsByName")

        // Act
        val reported = reportedFixtureDeclarations()

        // Assert
        assertEquals(expected = 1, actual = reported.count { it == expected }, message = reported.toString())
    }

    @Test
    fun `given a list of the marked type as return type, when scanned, then the function is reported`() {
        // Arrange
        val expected = declaration(owner = EXPOSING_SIGNATURES, name = "listReturn")

        // Act
        val reported = reportedFixtureDeclarations()

        // Assert
        assertEquals(expected = 1, actual = reported.count { it == expected }, message = reported.toString())
    }

    @Test
    fun `given an internal function with a marked parameter, when scanned, then it is not reported`() {
        // Arrange
        val internalDeclaration = declaration(owner = EXPOSING_SIGNATURES, name = "internalParameter")

        // Act
        val reported = reportedFixtureDeclarations()

        // Assert
        assertEquals(expected = 0, actual = reported.count { it == internalDeclaration }, message = reported.toString())
    }

    @Test
    fun `given public and hidden uses of the marked type, when scanned, then exactly the public ones are reported`() {
        // Arrange
        val expected = listOf(
            declaration(owner = EXPOSING_SIGNATURES, name = "multiLineParameter"),
            declaration(owner = EXPOSING_SIGNATURES, name = "listParameter"),
            declaration(owner = EXPOSING_SIGNATURES, name = "documentsByName"),
            declaration(owner = EXPOSING_SIGNATURES, name = "listReturn"),
            declaration(owner = EXPOSING_SIGNATURES, name = "deeplyNested"),
            declaration(owner = EXPOSING_SIGNATURES, name = "boundedTypeParameter"),
            declaration(owner = EXPOSING_SIGNATURES, name = "receiver"),
            declaration(owner = EXPOSING_SIGNATURES, name = "varargParameter"),
            declaration(owner = EXPOSING_SIGNATURES, name = "receiverProperty"),
            declaration(owner = EXPOSING_SIGNATURES, name = "boundedProperty"),
            declaration(owner = "$FIXTURE_FOLDER/ConstructorExposure", name = CONSTRUCTOR),
            declaration(owner = "$FIXTURE_FOLDER/SupertypeExposure", name = SUPERTYPES),
            declaration(owner = "$FIXTURE_FOLDER/BoundExposure", name = TYPE_PARAMETERS),
            declaration(owner = "$FIXTURE_FOLDER/ExposingClassesKt", name = "topLevelExposure"),
        )

        // Act
        val reported = reportedFixtureDeclarations()

        // Assert
        assertEquals(expected = expected.sorted(), actual = reported.sorted())
    }

    private fun reportedFixtureDeclarations(): List<String> {
        val scanner = PublicSignatureScanner(markedTypes = MarkedTypes(names = setOf(MARKED_FIXTURE_TYPE)))
        val fixtureClasses = CompiledClasses.inPackage(packageName = FIXTURE_PACKAGE)
        return scanner.violationsIn(classes = fixtureClasses).map { it.declaration }
    }

    private fun declaration(owner: String, name: String): String = "$owner.$name"

    private companion object {
        const val FIXTURE_PACKAGE = "dev.tonholo.ssrcompose.contracttests.fixture"
        val FIXTURE_FOLDER = FIXTURE_PACKAGE.replace(oldChar = '.', newChar = '/')
        val EXPOSING_SIGNATURES = "$FIXTURE_FOLDER/ExposingSignatures"
        val MARKED_FIXTURE_TYPE = ScreenDocument::class.java.metadataName()
        const val CONSTRUCTOR = "<init>"
        const val SUPERTYPES = "<supertypes>"
        const val TYPE_PARAMETERS = "<type parameters>"
    }
}
