package dev.tonholo.ssrcompose.server.tree

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.turbine.test
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.FragmentNode
import dev.tonholo.ssrcompose.protocol.GreetingCard
import dev.tonholo.ssrcompose.protocol.GreetingCardProps
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import dev.tonholo.ssrcompose.server.screen.BANNER_HEIGHT_PX
import dev.tonholo.ssrcompose.server.screen.BANNER_WIDTH_PX
import dev.tonholo.ssrcompose.server.screen.ServerBanner
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RemoteFragmentTest {
    private fun fragmentsOf(tree: ColumnNode): List<FragmentNode> =
        tree.children.filterIsInstance<FragmentNode>()

    private fun load(bytes: ByteArray): CoreDocument {
        val document = CoreDocument()
        document.initFromBuffer(RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes)))
        return document
    }

    @Test
    fun `given a remote fragment, when the tree is rendered, then the root column holds a fragment with bytes`() =
        runTest {
            // Arrange
            val content: @Composable () -> Unit = {
                RemoteFragment(widthPx = 100, heightPx = 50) { Box() }
            }

            // Act
            val tree = renderTree(content)

            // Assert
            val column = assertIs<ColumnNode>(tree)
            assertEquals(expected = 1, actual = column.children.size)
            assertTrue(fragmentsOf(column).single().document.isNotEmpty())
        }

    @Test
    fun `given the server banner, when its bytes are loaded, then a document with a root layout comes back`() =
        runTest {
            // Arrange
            val tree = renderTree { ServerBanner() }
            val bytes = fragmentsOf(assertIs<ColumnNode>(tree)).single().document

            // Act
            val document = load(bytes)

            // Assert
            val header = document.operations.filterIsInstance<Header>().single()
            assertEquals(expected = BANNER_WIDTH_PX, actual = header.get(Header.DOC_WIDTH))
            assertEquals(expected = BANNER_HEIGHT_PX, actual = header.get(Header.DOC_HEIGHT))
            assertTrue(document.operations.any { it is RootLayoutComponent })
        }

    @Test
    fun `given a reference then a fragment, when the tree is rendered, then both are in that order`() =
        runTest {
            // Arrange
            val props = GreetingCardProps(title = "t", message = "m")

            // Act
            val tree = renderTree {
                Reference(GreetingCard, props)
                ServerBanner()
            }

            // Assert
            val children = assertIs<ColumnNode>(tree).children
            assertEquals(expected = 2, actual = children.size)
            assertIs<ReferenceNode>(children[0])
            assertIs<FragmentNode>(children[1])
        }

    @Test
    fun `given a fragment whose key changes, when recomposed, then the fragment bytes change`() = runTest {
        // Arrange
        var label by mutableStateOf("one")
        val content: @Composable () -> Unit = {
            RemoteFragment(100, 50, label) { Text(text = label) }
        }

        liveTree(content).test {
            val before = fragmentsOf(assertIs<ColumnNode>(awaitItem())).single().document

            // Act
            label = "two"
            val after = fragmentsOf(assertIs<ColumnNode>(awaitItem())).single().document

            // Assert
            assertFalse(before.contentEquals(after))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given a fragment whose keys stay the same, when recomposed, then the fragment bytes are the same instance`() =
        runTest {
            // Arrange
            var unrelated by mutableStateOf(0)
            val content: @Composable () -> Unit = {
                Reference(GreetingCard, GreetingCardProps(title = "t", message = unrelated.toString()))
                RemoteFragment(100, 50, "key") { Box() }
            }

            liveTree(content).test {
                val before = fragmentsOf(assertIs<ColumnNode>(awaitItem())).single().document

                // Act
                unrelated = 1
                val after = fragmentsOf(assertIs<ColumnNode>(awaitItem())).single().document

                // Assert
                assertSame(expected = before, actual = after)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
