package dev.tonholo.ssrcompose.core.handshake

import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HandshakeTypesTest {
    private val app = AppIdentity(version = "1.0.0", variant = "release", flavor = "")
    private val portraitDevice = TestDevices.portrait

    @Test
    fun `given the four handshake values, when an Introduction is built, then it holds them`() {
        // Arrange
        val contractVersion = ContractVersion(value = 1)
        val catalogVersion = CatalogVersion(value = "home-2")

        // Act
        val introduction = Introduction(
            contractVersion = contractVersion,
            catalogVersion = catalogVersion,
            app = app,
            device = portraitDevice,
        )

        // Assert
        assertEquals(expected = contractVersion, actual = introduction.contractVersion)
        assertEquals(expected = catalogVersion, actual = introduction.catalogVersion)
        assertEquals(expected = app, actual = introduction.app)
        assertEquals(expected = portraitDevice, actual = introduction.device)
    }

    @Test
    fun `given an Introduction, when its fields are listed, then it holds no catalog`() {
        // Arrange
        val descriptor = Introduction.serializer().descriptor

        // Act
        val actual = (0 until descriptor.elementsCount).map(transform = descriptor::getElementName)

        // Assert
        assertEquals(
            expected = listOf("contractVersion", "catalogVersion", "app", "device"),
            actual = actual,
        )
    }

    @Test
    fun `given a taller than wide screen, when orientation is read, then it is portrait`() {
        // Arrange
        val device = portraitDevice

        // Act
        val actual = device.orientation

        // Assert
        assertEquals(expected = Orientation.Portrait, actual = actual)
    }

    @Test
    fun `given a wider than tall screen, when orientation is read, then it is landscape`() {
        // Arrange
        val device = portraitDevice.copy(screenWidthPx = 2400, screenHeightPx = 1080)

        // Act
        val actual = device.orientation

        // Assert
        assertEquals(expected = Orientation.Landscape, actual = actual)
    }

    @Test
    fun `given a device that rotates, when width and height swap, then orientation follows`() {
        // Arrange
        val rotated = portraitDevice.copy(
            screenWidthPx = portraitDevice.screenHeightPx,
            screenHeightPx = portraitDevice.screenWidthPx,
        )

        // Act
        val actual = rotated.orientation

        // Assert
        assertEquals(expected = Orientation.Landscape, actual = actual)
    }

    @Test
    fun `given two contract versions, when they are compared, then they follow numeric order`() {
        // Arrange
        val older = ContractVersion(value = 1)
        val newer = ContractVersion(value = 2)

        // Act
        val olderIsLess = older < newer
        val newerIsLess = newer < older

        // Assert
        assertTrue(actual = olderIsLess)
        assertFalse(actual = newerIsLess)
        assertEquals(expected = older, actual = ContractVersion(value = 1))
    }

    @Test
    fun `given catalog versions, when they are compared, then only equality applies`() {
        // Arrange
        val first: Any = CatalogVersion(value = "a")
        val sameAsFirst: Any = CatalogVersion(value = "a")
        val other: Any = CatalogVersion(value = "b")

        // Act
        val isOrdered = first is Comparable<*>
        val equalsOther = first == other

        // Assert
        assertEquals(expected = first, actual = sameAsFirst)
        assertFalse(actual = equalsOther)
        assertFalse(actual = isOrdered)
    }

    @Test
    fun `given the refusal kinds, when they are listed, then there are exactly four`() {
        // Arrange
        val expected = listOf(
            RefusalKind.ContractVersionTooOld,
            RefusalKind.ContractVersionTooNew,
            RefusalKind.CatalogVersionNotSupported,
            RefusalKind.IntroductionNotValid,
        )

        // Act
        val actual = RefusalKind.entries

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given an answer, when it is accepted or refused, then it keeps its fields`() {
        // Arrange
        val accepted = Answer.Accepted(contractVersion = ContractVersion(value = 1))
        val refused = Answer.Refused(kind = RefusalKind.IntroductionNotValid, reason = "Not valid")

        // Act
        val acceptedVersion = accepted.contractVersion
        val refusedKind = refused.kind

        // Assert
        assertEquals(expected = ContractVersion(value = 1), actual = acceptedVersion)
        assertEquals(expected = RefusalKind.IntroductionNotValid, actual = refusedKind)
        assertEquals(expected = "Not valid", actual = refused.reason)
    }
}
