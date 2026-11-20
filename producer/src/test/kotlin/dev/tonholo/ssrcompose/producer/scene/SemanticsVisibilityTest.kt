package dev.tonholo.ssrcompose.producer.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.producer.testing.SemanticsSummary
import dev.tonholo.ssrcompose.producer.testing.rootChildrenSummary
import dev.tonholo.ssrcompose.producer.testing.withRenderedScene
import kotlin.test.Test
import kotlin.test.assertEquals

private const val FIRST_TEXT = "first"
private const val SECOND_TEXT = "second"
private const val BORDER_WIDTH_DP = 1
private const val SPACER_SIZE_DP = 4
private const val PADDING_DP = 2

/**
 * Pins what the unmerged semantics tree holds for each shape, as observed on Compose Multiplatform 1.12.2 and recorded
 * in research.md R20 (probes P5 and P6). A Compose upgrade that changes it fails here, not in the reader.
 */
class SemanticsVisibilityTest {
    @Test
    fun `given a box with background and border holding a text, when composed, then the box and its text are nodes`() {
        // Arrange
        val expected = listOf(
            SemanticsSummary(
                text = null,
                modifiers = listOf("background", "border"),
                hasClickAction = false,
                children = listOf(textNode(FIRST_TEXT)),
            ),
        )

        // Act
        val actual = summaryOf {
            Box(Modifier.background(Color.Red).border(width = BORDER_WIDTH_DP.dp, color = Color.Black)) {
                BasicText(text = FIRST_TEXT)
            }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given layouts with no drawing, when composed, then only their texts are nodes`() {
        // Arrange
        val expected = listOf(textNode(FIRST_TEXT), textNode(SECOND_TEXT))

        // Act
        val actual = summaryOf {
            Column(Modifier.padding(PADDING_DP.dp)) {
                Row {
                    BasicText(text = FIRST_TEXT)
                    Spacer(Modifier.size(SPACER_SIZE_DP.dp))
                }
                Box(Modifier.padding(PADDING_DP.dp)) { BasicText(text = SECOND_TEXT) }
                Box(Modifier.size(SPACER_SIZE_DP.dp))
            }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a column with vertical scroll, when composed, then a scroll node holds the text`() {
        // Arrange
        val expected = listOf(
            SemanticsSummary(
                text = null,
                modifiers = listOf("scroll"),
                hasClickAction = false,
                children = listOf(textNode(FIRST_TEXT)),
            ),
        )

        // Act
        val actual = summaryOf {
            Column(Modifier.verticalScroll(rememberScrollState())) { BasicText(text = FIRST_TEXT) }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a clickable box holding a text, when composed, then the click node and its text stay separate nodes`() {
        // Arrange
        val expected = listOf(
            SemanticsSummary(
                text = null,
                modifiers = listOf("clickable"),
                hasClickAction = true,
                children = listOf(textNode(FIRST_TEXT)),
            ),
        )

        // Act
        val actual = summaryOf {
            Box(Modifier.clickable { }) { BasicText(text = FIRST_TEXT) }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    private fun textNode(text: String) =
        SemanticsSummary(text = text, modifiers = emptyList(), hasClickAction = false, children = emptyList())

    private fun summaryOf(content: @Composable () -> Unit): List<SemanticsSummary> =
        withRenderedScene(content = content, read = { it.rootChildrenSummary() })
}
