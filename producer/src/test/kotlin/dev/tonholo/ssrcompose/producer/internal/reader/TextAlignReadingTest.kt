package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import dev.tonholo.ssrcompose.core.document.TextStyleSpec
import kotlin.test.Test
import kotlin.test.assertEquals

class TextAlignReadingTest {
    @Test
    fun `given left and right alignments, when read in each direction, then they become start or end`() {
        // Arrange
        val expected = mapOf(
            (TextAlign.Left to LayoutDirection.Ltr) to TextStyleSpec.Align.Start,
            (TextAlign.Right to LayoutDirection.Ltr) to TextStyleSpec.Align.End,
            (TextAlign.Left to LayoutDirection.Rtl) to TextStyleSpec.Align.End,
            (TextAlign.Right to LayoutDirection.Rtl) to TextStyleSpec.Align.Start,
        )

        // Act
        val actual = expected.keys.associateWith { (align, direction) -> align.toAlign(direction = direction) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given direction free alignments, when read, then they keep their meaning`() {
        // Arrange
        val expected = mapOf(
            TextAlign.Start to TextStyleSpec.Align.Start,
            TextAlign.End to TextStyleSpec.Align.End,
            TextAlign.Center to TextStyleSpec.Align.Center,
            TextAlign.Justify to TextStyleSpec.Align.Justify,
            TextAlign.Unspecified to TextStyleSpec.Align.Start,
        )

        // Act
        val actual = expected.keys.associateWith { align -> align.toAlign(direction = LayoutDirection.Rtl) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }
}
