package dev.tonholo.ssrcompose.contracttests

import dev.tonholo.ssrcompose.contracttests.signature.CompiledClasses
import dev.tonholo.ssrcompose.contracttests.signature.MarkedTypes
import dev.tonholo.ssrcompose.contracttests.signature.PublicSignatureScanner
import dev.tonholo.ssrcompose.contracttests.signature.Violation
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicSignatureMarkerTest {
    @Test
    fun `given the compiled core classes, when marked types are read, then each listed core type is marked`() {
        // Arrange
        val expected = ADR_0014_CORE_TYPES.associateWith { true }

        // Act
        val marked = coreMarkedTypes()

        // Assert
        assertEquals(expected = expected, actual = expected.keys.associateWith(marked::marks))
    }

    @Test
    fun `given the compiled consumer classes, when public signatures are scanned, then none names a marked type`() {
        // Arrange
        val packageName = CONSUMER_PACKAGE

        // Act
        val violations = publicViolationsIn(packageName = packageName)

        // Assert
        assertEquals(expected = emptyList(), actual = violations)
    }

    @Test
    fun `given the compiled producer classes, when public signatures are scanned, then none names a marked type`() {
        // Arrange
        val packageName = PRODUCER_PACKAGE

        // Act
        val violations = publicViolationsIn(packageName = packageName)

        // Assert
        assertEquals(expected = emptyList(), actual = violations)
    }

    @Test
    fun `given the compiled core-testing classes, when public signatures are scanned, then none names a marked type`() {
        // Arrange
        val packageName = CORE_TESTING_PACKAGE

        // Act
        val violations = publicViolationsIn(packageName = packageName)

        // Assert
        assertEquals(expected = emptyList(), actual = violations)
    }

    private fun publicViolationsIn(packageName: String): List<Violation> =
        PublicSignatureScanner(markedTypes = coreMarkedTypes())
            .violationsIn(classes = CompiledClasses.inPackage(packageName = packageName))

    private fun coreMarkedTypes(): MarkedTypes {
        val coreClasses = CompiledClasses.inPackage(packageName = CORE_PACKAGE)
            .filterNot { it.name.startsWith(prefix = "$CORE_TESTING_PACKAGE.") }
        return MarkedTypes.readFrom(classes = coreClasses)
    }

    private companion object {
        const val CORE_PACKAGE = "dev.tonholo.ssrcompose.core"
        const val CORE_TESTING_PACKAGE = "dev.tonholo.ssrcompose.core.testing"
        const val CONSUMER_PACKAGE = "dev.tonholo.ssrcompose.consumer"
        const val PRODUCER_PACKAGE = "dev.tonholo.ssrcompose.producer"
        val ADR_0014_CORE_TYPES = listOf(
            "dev/tonholo/ssrcompose/core/handshake/Answer",
            "dev/tonholo/ssrcompose/core/message/ConsumerMessage",
            "dev/tonholo/ssrcompose/core/message/ProducerMessage",
            "dev/tonholo/ssrcompose/core/message/MessageFrame",
            "dev/tonholo/ssrcompose/core/message/MessageCodec",
            "dev/tonholo/ssrcompose/core/message/MessageDecodingException",
        )
    }
}
