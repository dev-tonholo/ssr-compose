package dev.tonholo.ssrcompose.producer.internal.font

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontSlant

private const val ROBOTO_FAMILY_NAME = "Roboto"
private const val REGULAR_WEIGHT = 400
private const val MEDIUM_WEIGHT = 500
private const val BOLD_WEIGHT = 700

private data class ExpectedFace(val face: RobotoFace, val weight: Int, val slant: FontSlant)

class BundledRobotoTest {
    @Test
    fun `given the bundled faces, when Skia reads their bytes, then they are Roboto at 400, 500, 700 and 400 italic`() {
        // Arrange
        val expected = listOf(
            ExpectedFace(face = RobotoFace.Regular, weight = REGULAR_WEIGHT, slant = FontSlant.UPRIGHT),
            ExpectedFace(face = RobotoFace.Medium, weight = MEDIUM_WEIGHT, slant = FontSlant.UPRIGHT),
            ExpectedFace(face = RobotoFace.Bold, weight = BOLD_WEIGHT, slant = FontSlant.UPRIGHT),
            ExpectedFace(face = RobotoFace.Italic, weight = REGULAR_WEIGHT, slant = FontSlant.ITALIC),
        )

        // Act
        val actual = RobotoFace.entries.map { face ->
            val typeface = FontMgr.default.makeFromData(Data.makeFromBytes(face.readBytes()))
            assertNotNull(actual = typeface, message = "Skia could not read ${face.name}")
            assertTrue(typeface.familyName.startsWith(ROBOTO_FAMILY_NAME))
            ExpectedFace(face = face, weight = typeface.fontStyle.weight, slant = typeface.fontStyle.slant)
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }
}
