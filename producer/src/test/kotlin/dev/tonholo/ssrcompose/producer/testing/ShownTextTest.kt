package dev.tonholo.ssrcompose.producer.testing

import dev.tonholo.ssrcompose.core.document.DocumentColor
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.core.document.ScreenDocument
import dev.tonholo.ssrcompose.core.document.TextStyleSpec
import dev.tonholo.ssrcompose.producer.internal.ScreenFrame
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SIDE = 10f
private const val FONT_SIZE = 14f
private const val REGULAR_WEIGHT = 400
private const val VISIBLE = "Visible"
private const val HIDDEN = "Hidden"
private val BLACK = DocumentColor(argb = 0xFF000000u)

class ShownTextTest {
    @Test
    fun `given a switcher that selects its first variant, when read, then only that variant's text is shown`() {
        // Arrange
        val frame = frameWithSwitcher(selected = 0)

        // Act
        val actual = frame.shown().texts

        // Assert
        assertEquals(expected = listOf(VISIBLE), actual = actual)
    }

    @Test
    fun `given a switcher that selects its second variant, when read, then only that variant's text is shown`() {
        // Arrange
        val frame = frameWithSwitcher(selected = 1)

        // Act
        val actual = frame.shown().texts

        // Assert
        assertEquals(expected = listOf(HIDDEN), actual = actual)
    }

    private fun frameWithSwitcher(selected: Int): ScreenFrame.Sent {
        val switcher = DocumentNode.Switcher(
            id = 1,
            x = 0f,
            y = 0f,
            width = SIDE,
            height = SIDE,
            selected = selected,
            largestWidth = SIDE,
            largestHeight = SIDE,
            variants = listOf(listOf(textNode(id = 2, text = VISIBLE)), listOf(textNode(id = 3, text = HIDDEN))),
        )
        val root = DocumentNode.Box(id = 0, x = 0f, y = 0f, width = SIDE, height = SIDE, children = listOf(switcher))
        return ScreenFrame.Sent(revision = 1, document = ScreenDocument(root = root))
    }

    private fun textNode(id: Int, text: String) = DocumentNode.Text(
        id = id,
        x = 0f,
        y = 0f,
        width = SIDE,
        height = SIDE,
        text = text,
        style = TextStyleSpec(size = FONT_SIZE, weight = REGULAR_WEIGHT, color = BLACK),
    )
}
