package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.producer.testing.readSceneOf
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val VIEWPORT_DP = 20
private const val CONTENT_DP = 100
private const val TEXT_SIZE_SP = 14
private const val LONG_TEXT = "A text long enough to wrap over several lines inside a narrow scrolling box"

class ScrollRefusalTest {
    @Test
    fun `given a box that scrolls both ways, when read, then it is not sendable for several scrolls`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.SeveralScrolls)

        // Act
        val actual = readSceneOf {
            Box(
                Modifier
                    .size(VIEWPORT_DP.dp)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState()),
            ) {
                Box(Modifier.size(CONTENT_DP.dp))
            }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a box that scrolls one way, when read, then it is sent`() {
        // Arrange
        val content: @Composable () -> Unit = {
            Box(Modifier.size(VIEWPORT_DP.dp).verticalScroll(rememberScrollState())) {
                Box(Modifier.size(CONTENT_DP.dp))
            }
        }

        // Act
        val actual = readSceneOf(content = content)

        // Assert
        assertIs<ScreenRead.Document>(value = actual)
    }

    @Test
    fun `given a text that scrolls on its own, when read, then it is not sendable for a text with scroll`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.TextWithScroll)

        // Act
        val actual = readSceneOf {
            BasicText(
                text = LONG_TEXT,
                modifier = Modifier.size(VIEWPORT_DP.dp).verticalScroll(rememberScrollState()),
                style = robotoStyle(sizeSp = TEXT_SIZE_SP),
            )
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }
}
