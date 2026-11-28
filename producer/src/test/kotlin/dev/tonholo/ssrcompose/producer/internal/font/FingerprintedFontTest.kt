package dev.tonholo.ssrcompose.producer.internal.font

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.producer.testing.SceneSpec
import dev.tonholo.ssrcompose.producer.testing.WIDE_SCENE_PX
import dev.tonholo.ssrcompose.producer.testing.sha256Hex
import dev.tonholo.ssrcompose.producer.testing.widthAlone
import dev.tonholo.ssrcompose.producer.testing.withRenderedScene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private const val SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog 0123456789"
private const val SIZE_SP = 16

class FingerprintedFontTest {
    @Test
    fun `given two files at one weight and style, when one screen shows both, then each measures as its own file`() {
        // Arrange
        val first = familyOfRegularWeight(file = RobotoFace.Regular)
        val second = familyOfRegularWeight(file = RobotoFace.Bold)
        val expected = listOf(first, second).map { widthAlone(text = SAMPLE_TEXT, sizeSp = SIZE_SP, family = it) }
        assertNotEquals(illegal = expected.first(), actual = expected.last())

        // Act
        val actual = widthsOnOneScreen(families = listOf(first, second))

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    /** [file] declared as the app's weight 400, as a new version of a bundle may do with any file. */
    private fun familyOfRegularWeight(file: RobotoFace): FontFamily {
        val data = file.readBytes()
        return FontFamily(
            fingerprintedFont(
                data = data,
                fingerprint = sha256Hex(data = data),
                weight = FontWeight.Normal,
                style = FontStyle.Normal,
            ),
        )
    }

    private fun widthsOnOneScreen(families: List<FontFamily>): List<Int> {
        val widths = MutableList(families.size) { 0 }
        withRenderedScene(
            spec = SceneSpec(widthPx = WIDE_SCENE_PX),
            content = {
                Column {
                    families.forEachIndexed { index, family ->
                        BasicText(
                            text = SAMPLE_TEXT,
                            style = TextStyle(fontFamily = family, fontSize = SIZE_SP.sp),
                            onTextLayout = { widths[index] = it.size.width },
                        )
                    }
                }
            },
            read = {},
        )
        return widths
    }
}
