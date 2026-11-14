package dev.tonholo.ssrcompose.server.tree

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.GreetingCard
import dev.tonholo.ssrcompose.protocol.GreetingCardProps
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import dev.tonholo.ssrcompose.protocol.ScreenNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class RenderTreeTest {
    private fun card(title: String) = GreetingCardProps(title = title, message = "hello")

    private fun expectedReference(title: String): ScreenNode = ReferenceNode(
        componentId = GreetingCard.id,
        props = Json.encodeToJsonElement(GreetingCard.propsSerializer, card(title)).jsonObject,
    )

    @Test
    fun `given content with one reference, when the tree is rendered, then root holds the encoded reference`() =
        runTest {
            // Arrange
            val content: @Composable () -> Unit = { Reference(GreetingCard, card("a")) }

            // Act
            val tree = renderTree(content)

            // Assert
            assertEquals(expected = ColumnNode(children = listOf(expectedReference("a"))), actual = tree)
        }

    @Test
    fun `given reference behind a false if, when the tree is rendered, then it is not in the tree`() =
        runTest {
            // Arrange
            val show = false
            val content: @Composable () -> Unit = {
                if (show) Reference(GreetingCard, card("hidden"))
            }

            // Act
            val tree = renderTree(content)

            // Assert
            assertEquals(expected = ColumnNode(children = emptyList()), actual = tree)
        }

    @Test
    fun `given content that loops over three items, when the tree is rendered, then references keep the loop order`() =
        runTest {
            // Arrange
            val titles = listOf("one", "two", "three")
            val content: @Composable () -> Unit = {
                titles.forEach { Reference(GreetingCard, card(it)) }
            }

            // Act
            val tree = renderTree(content)

            // Assert
            val expected = ColumnNode(children = titles.map(::expectedReference))
            assertEquals(expected = expected, actual = tree)
        }

    @Test
    fun `given empty content, when the tree is rendered, then the root column has no children`() =
        runTest {
            // Arrange
            val content: @Composable () -> Unit = {}

            // Act
            val tree = renderTree(content)

            // Assert
            assertEquals(expected = ColumnNode(children = emptyList()), actual = tree)
        }

    @Test
    fun `given content that throws, when the tree is rendered, then the failure reaches the caller`() =
        runTest {
            // Arrange
            val content: @Composable () -> Unit = { error("boom") }

            // Act
            val failure = assertFailsWith<IllegalStateException> { renderTree(content) }

            // Assert
            assertEquals(expected = "boom", actual = failure.message)
        }
}
