package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.producer.testing.readSceneOf
import kotlin.test.Test
import kotlin.test.assertEquals

private const val BOX_DP = 10
private const val TEXT = "text"

class TextLayoutRefusalTest {
    @Test
    fun `given a node with text semantics and no layout action, when read, then it is not sendable for no layout`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.NoTextLayout)

        // Act
        val actual = readSceneOf {
            Box(Modifier.size(BOX_DP.dp).semantics { text = AnnotatedString(text = TEXT) })
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a layout action that gives no result, when read, then it is not sendable for no layout`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.NoTextLayout)

        // Act
        val actual = readSceneOf {
            Box(
                Modifier.size(BOX_DP.dp).semantics {
                    text = AnnotatedString(text = TEXT)
                    getTextLayoutResult { false }
                },
            )
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a layout action that says it gave a result and gave none, when read, then it is not sendable`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.NoTextLayout)

        // Act
        val actual = readSceneOf {
            Box(
                Modifier.size(BOX_DP.dp).semantics {
                    text = AnnotatedString(text = TEXT)
                    getTextLayoutResult { true }
                },
            )
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }
}
