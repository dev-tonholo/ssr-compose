@file:Suppress("RestrictedApi")

package dev.tonholo.ssrcompose.server.screen

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.FragmentNode
import dev.tonholo.ssrcompose.server.tree.renderTree
import java.io.ByteArrayInputStream
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CounterBannerTest {
    private fun childrenOf(operation: Operation): List<Operation> = when (operation) {
        is ComponentModifiers -> operation.modifiersList.filterIsInstance<Operation>()
        is Container -> operation.list
        else -> emptyList()
    }

    private fun allOperations(operations: List<Operation>): List<Operation> =
        operations + operations.flatMap { allOperations(childrenOf(it)) }

    private suspend fun bannerOperations(): List<Operation> {
        val tree = renderTree { CounterBanner(count = 0, onTap = ActionId("action-2")) }
        val bytes = (tree as ColumnNode).children.filterIsInstance<FragmentNode>().single().document
        val document = CoreDocument()
        document.initFromBuffer(RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes)))
        return allOperations(document.operations)
    }

    private fun List<Operation>.containsKind(kind: KClass<*>): Boolean = any { kind.isInstance(it) }

    @Test
    fun `given the counter banner, when loaded, then it has a host action for the tap`() = runTest {
        // Arrange
        val operations = bannerOperations()

        // Act
        val hasHostAction = operations.containsKind(HostNamedActionOperation::class)

        // Assert
        assertTrue(actual = hasHostAction)
    }

    @Test
    fun `given the counter banner, when loaded, then it has a local toggle on a state layout`() = runTest {
        // Arrange
        val operations = bannerOperations()

        // Act
        val hasStateLayout = operations.containsKind(StateLayout::class)
        val hasToggleChange = operations.containsKind(ValueIntegerExpressionChangeActionOperation::class)

        // Assert
        assertTrue(actual = hasStateLayout)
        assertTrue(actual = hasToggleChange)
    }
}
