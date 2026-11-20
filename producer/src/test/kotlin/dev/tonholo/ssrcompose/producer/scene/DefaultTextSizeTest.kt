package dev.tonholo.ssrcompose.producer.scene

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.producer.internal.font.bundledRobotoFamily
import dev.tonholo.ssrcompose.producer.testing.withRenderedScene
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SAMPLE_TEXT = "Served by the producer"
private const val EXPECTED_DEFAULT_FONT_SIZE_SP = 14f

/**
 * Pins the size `BasicText` uses for a style with no font size, which the reader writes for such a text
 * (research.md R20, section 4). A Compose upgrade that changes it fails here.
 */
class DefaultTextSizeTest {
    @Test
    fun `given a text with no font size, when laid out, then it has the size of the reader's default size`() {
        // Arrange
        val explicitStyle = TextStyle(fontFamily = bundledRobotoFamily(), fontSize = EXPECTED_DEFAULT_FONT_SIZE_SP.sp)

        // Act
        val unsized = laidOutSize(style = TextStyle(fontFamily = bundledRobotoFamily()))
        val explicit = laidOutSize(style = explicitStyle)

        // Assert
        assertEquals(expected = explicit, actual = unsized)
    }

    private fun laidOutSize(style: TextStyle): IntSize {
        var size = IntSize.Zero
        withRenderedScene(
            content = { BasicText(text = SAMPLE_TEXT, style = style, onTextLayout = { size = it.size }) },
            read = {},
        )
        return size
    }
}
