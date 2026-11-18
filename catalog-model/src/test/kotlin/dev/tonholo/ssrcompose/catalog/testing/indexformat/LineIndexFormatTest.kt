package dev.tonholo.ssrcompose.catalog.testing.indexformat

import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LineIndexFormatTest {
    @Test
    fun `given a version line with no value, when the index is opened, then it is refused`() {
        // Arrange
        val text = "$HEADER\nversion\n"

        // Act
        val failure = openFailure(text = text)

        // Assert
        assertIs<IllegalArgumentException>(value = failure)
    }

    @Test
    fun `given a header alone, when the index is opened, then it is refused for having no version`() {
        // Arrange
        val text = "$HEADER\n"

        // Act
        val failure = openFailure(text = text)

        // Assert
        assertIs<IllegalArgumentException>(value = failure)
    }

    @Test
    fun `given a parameter line with a missing field, when the index is opened, then it is refused`() {
        // Arrange
        val text = "$HEADER\nversion\t1\ncomponent\tCard\nvalue\ttitle\ttext\ttrue\n"

        // Act
        val failure = openFailure(text = text)

        // Assert
        assertIs<IllegalArgumentException>(value = failure)
    }

    @Test
    fun `given a parameter line before any component, when the index is opened, then it is refused`() {
        // Arrange
        val text = "$HEADER\nversion\t1\nvalue\ttitle\ttext\ttrue\tfalse\n"

        // Act
        val failure = openFailure(text = text)

        // Assert
        assertIs<IllegalArgumentException>(value = failure)
    }

    @Test
    fun `given an unknown value kind, when the index is opened, then it is refused`() {
        // Arrange
        val text = "$HEADER\nversion\t1\ncomponent\tCard\nvalue\ttitle\tcolor\ttrue\tfalse\n"

        // Act
        val failure = openFailure(text = text)

        // Assert
        assertIs<IllegalArgumentException>(value = failure)
    }

    @Test
    fun `given a callback with an empty argument field, when the index is opened, then it has no arguments`() {
        // Arrange
        val text = "$HEADER\nversion\t1\ncomponent\tCard\ncallback\tonTap\t\ttrue\n"

        // Act
        val index = LineIndexFormat.open(bytes = text.encodeToByteArray())

        // Assert
        val expected = ComponentDescriptor(
            name = "Card",
            parameters = listOf(Parameter.Callback(name = "onTap", arguments = emptyList(), required = true)),
        )
        assertEquals(expected = expected, actual = index.component(name = "Card"))
    }

    private fun openFailure(text: String): Throwable? =
        runCatching { LineIndexFormat.open(bytes = text.encodeToByteArray()) }.exceptionOrNull()

    private companion object {
        const val HEADER = "ssr-catalog-index-lines 1"
    }
}
